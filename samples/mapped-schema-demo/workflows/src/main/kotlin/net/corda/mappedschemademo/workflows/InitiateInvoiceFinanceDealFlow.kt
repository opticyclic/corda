package net.corda.mappedschemademo.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.mappedschemademo.contracts.contract.InvoiceFinanceDealContract
import net.corda.mappedschemademo.contracts.state.Invoice
import net.corda.mappedschemademo.contracts.state.InvoiceFinanceDealState
import java.util.*

object InitiateInvoiceFinanceDealFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
            val reference: String,
            val lender: Party,
            val loan: Long,
            val fee: Long,
            val invoiceList: List<Invoice>
    ): FlowLogic<UUID>() {
        companion object {
            object GENERATING_DEAL: ProgressTracker.Step("Generating transaction based on new invoice finance deal.")
            object VERIFYING_DEAL : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_DEAL : ProgressTracker.Step("Signing transaction with our private key.")

            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_DEAL : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_DEAL,
                    VERIFYING_DEAL,
                    SIGNING_DEAL,
                    GATHERING_SIGS,
                    FINALISING_DEAL
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): UUID {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_DEAL
            // Generate an unsigned transaction.
            val iouState = InvoiceFinanceDealState(
                    reference = reference,
                    borrower = serviceHub.myInfo.legalIdentities.first(),
                    lender = lender,
                    loan = loan,
                    fee = fee,
                    invoiceList = invoiceList)
            val txCommand = Command(InvoiceFinanceDealContract.Commands.Create(), iouState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(iouState, InvoiceFinanceDealContract.ID)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_DEAL
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_DEAL
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)


            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            val lenderSession = initiateFlow(lender)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(lenderSession), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_DEAL
            // Notarise and record the transaction in both parties' vaults.
            subFlow(FinalityFlow(fullySignedTx, setOf(lenderSession), FINALISING_DEAL.childProgressTracker()))

            return iouState.linearId.id
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Asset transaction." using (output is InvoiceFinanceDealState)
                }
            }
            val txId = subFlow(signTransactionFlow).id

            subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}