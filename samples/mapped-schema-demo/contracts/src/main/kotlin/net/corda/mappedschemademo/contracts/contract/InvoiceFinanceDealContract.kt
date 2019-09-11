package net.corda.mappedschemademo.contracts.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.mappedschemademo.contracts.state.InvoiceFinanceDealState
import java.security.PublicKey

class InvoiceFinanceDealContract : Contract {
    companion object {
        @JvmStatic
        val ID = "net.corda.mappedschemademo.contracts.contract.InvoiceFinanceDealContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.Create -> verifyCreate(tx, setOfSigners)
            is Commands.PayInvoice -> verifyPayInvoice(tx, setOfSigners)
            is Commands.Discharge -> verifyDischarge(tx, setOfSigners)
        }
    }

    private fun keysFromParticipants(deal: InvoiceFinanceDealState): Set<PublicKey> {
        return deal.participants.map {
            it.owningKey
        }.toSet()
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) {
        requireThat {
            "No inputs should be consumed when creating an invoice finance deal" using (tx.inputStates.isEmpty())
            "Only one Invoice Finance Deal state should be created" using (tx.outputStates.size == 1)
            val deal = tx.outputsOfType<InvoiceFinanceDealState>().single()
            "The loan amount must be greater than zero" using (deal.loan > 0)
            "The fee must be greater than or equal to zero" using (deal.fee >= 0)
            "The lender and the borrower cannot be the same identity" using (deal.borrower != deal.lender)
            "Both lender and borrower together only may sign obligation issue transaction." using (signers == keysFromParticipants(deal))
            val loanTotal = deal.loan + deal.fee
            val invoiceTotal = deal.invoiceList.map { it.value }.sum()
            "Invoice total must be greater than or equal to loan total" using (invoiceTotal >= loanTotal)
            "Invoices must not have any paid amount" using (deal.invoiceList.all {it.paid == 0L})
            "Invoice values must be greater than zero" using (deal.invoiceList.all { it.value > 0})
        }
    }

    private fun verifyPayInvoice(tx: LedgerTransaction, signers: Set<PublicKey>) {
    }

    private fun verifyDischarge(tx: LedgerTransaction, signers: Set<PublicKey>) {

    }

    interface Commands: CommandData {
        class Create: Commands
        class PayInvoice: Commands
        class Discharge: Commands

    }
}