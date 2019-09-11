package net.corda.mappedschemademo.workflows.test

import net.corda.core.utilities.getOrThrow
import net.corda.mappedschemademo.contracts.schema.InvoiceFinanceDealSchemaV1
import net.corda.mappedschemademo.contracts.state.Invoice
import net.corda.mappedschemademo.workflows.InitiateInvoiceFinanceDealFlow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class InitiateInvoiceFinanceDealFlowTest {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.mappedschemademo.contracts"),
                TestCordapp.findCordapp("net.corda.mappedschemademo.workflows")
        )))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flow rejects invalid IOUs`() {
        val flow = InitiateInvoiceFinanceDealFlow.Initiator(
                "10000",
                b.info.singleIdentity(),
                100,
                5,
                listOf(Invoice(invoiceNumber = "20000", supplier = "MegaCorp", value = 150), Invoice(invoiceNumber = "20001", supplier = "MegaCorp", value = 150)))
        val future = a.startFlow(flow)
        network.runNetwork()

        val id = future.getOrThrow()

        b.transaction {
            val deals = a.services.withEntityManager {
                val query = criteriaBuilder.createQuery(InvoiceFinanceDealSchemaV1.PersistentInvoiceFinanceDeal::class.java)
                val type = query.from(InvoiceFinanceDealSchemaV1.PersistentInvoiceFinanceDeal::class.java)
                query.select(type)
                createQuery(query).resultList
            }

            deals.forEach {
                println("Deal reference ${it.reference} for ${it.loanAmount} and ${it.feeAmount} fee")
                it.invoiceList.forEach { invoice ->
                    println("Invoice ${invoice.supplier} for ${invoice.valueAmount}")
                }
            }

        }
    }
}