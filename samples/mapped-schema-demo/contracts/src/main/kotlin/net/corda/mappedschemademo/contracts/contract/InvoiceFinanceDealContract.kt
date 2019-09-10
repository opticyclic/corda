package net.corda.mappedschemademo.contracts.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class InvoiceFinanceDealContract : Contract {
    companion object {
        @JvmStatic
        val ID = "net.corda.mappedschemademo.contracts.contract.InvoiceFinanceDealContract"
    }

    override fun verify(tx: LedgerTransaction) {
        requireThat {
            //Something something requirements


        }
    }

    interface Commands: CommandData {
        class Create: Commands
        class PayInvoice: Commands
        class Discharge: Commands

    }
}