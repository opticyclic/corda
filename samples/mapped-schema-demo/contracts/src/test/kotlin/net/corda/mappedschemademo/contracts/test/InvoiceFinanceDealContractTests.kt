package net.corda.mappedschemademo.contracts.test

import net.corda.core.identity.CordaX500Name
import net.corda.mappedschemademo.contracts.contract.InvoiceFinanceDealContract
import net.corda.mappedschemademo.contracts.state.Invoice
import net.corda.mappedschemademo.contracts.state.InvoiceFinanceDealState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class InvoiceFinanceDealContractTests {
    private val ledgerServices = MockServices(listOf("net.corda.mappedschemademo.contracts"))
    private val lender = TestIdentity(CordaX500Name("LendingCorp", "London", "GB"))
    private val borrower = TestIdentity(CordaX500Name("BorrowingCorp", "New York", "US"))

    @Test
    fun `loan amount must be less than invoice amount`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceFinanceDealContract.ID, InvoiceFinanceDealState(
                        borrower = borrower.party,
                        lender = lender.party,
                        loan = 100,
                        fee = 5,
                        invoiceList = listOf(Invoice(value = 80))))
                command(listOf(lender.publicKey, borrower.publicKey), InvoiceFinanceDealContract.Commands.Create())
                failsWith("Invoice total must be greater than or equal to loan total")
            }
        }
    }

    @Test
    fun `loan amount must be greater than zero`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceFinanceDealContract.ID, InvoiceFinanceDealState(
                        borrower = borrower.party,
                        lender = lender.party,
                        loan = 0,
                        fee = 5,
                        invoiceList = listOf(Invoice(value = 80))))
                command(listOf(lender.publicKey, borrower.publicKey), InvoiceFinanceDealContract.Commands.Create())
                failsWith("The loan amount must be greater than zero")
            }
        }
    }

    @Test
    fun `fee amount must be greater than or equal to zero`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceFinanceDealContract.ID, InvoiceFinanceDealState(
                        borrower = borrower.party,
                        lender = lender.party,
                        loan = 50,
                        fee = -1,
                        invoiceList = listOf(Invoice(value = 80))))
                command(listOf(lender.publicKey, borrower.publicKey), InvoiceFinanceDealContract.Commands.Create())
                failsWith("The fee must be greater than or equal to zero")
            }
        }
    }

    @Test
    fun `invoices must have no payment amount on deal creation`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceFinanceDealContract.ID, InvoiceFinanceDealState(
                        borrower = borrower.party,
                        lender = lender.party,
                        loan = 50,
                        fee = 5,
                        invoiceList = listOf(Invoice(value = 80, paid = 20))))
                command(listOf(lender.publicKey, borrower.publicKey), InvoiceFinanceDealContract.Commands.Create())
                failsWith("Invoices must not have any paid amount")
            }
        }
    }

    @Test
    fun `invoices must have value greater than or equal to zero`() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceFinanceDealContract.ID, InvoiceFinanceDealState(
                        borrower = borrower.party,
                        lender = lender.party,
                        loan = 50,
                        fee = 5,
                        invoiceList = listOf(Invoice(value = 0))))
                command(listOf(lender.publicKey, borrower.publicKey), InvoiceFinanceDealContract.Commands.Create())
                failsWith("Invoice values must be greater than zero")
            }
        }
    }
}