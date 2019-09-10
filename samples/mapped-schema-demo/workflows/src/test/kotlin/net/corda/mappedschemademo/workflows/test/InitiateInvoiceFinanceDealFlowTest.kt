package net.corda.mappedschemademo.workflows.test

import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.utilities.getOrThrow
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
import kotlin.test.assertFailsWith

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
                listOf(Invoice(invoiceNumber="20000", supplier = "MegaCorp", value = 150)))
        val future = a.startFlow(flow)
        network.runNetwork()

        // The IOUContract specifies that IOUs cannot have negative values.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }
}