package io.pleo.antaeus.core.services

import assertInvoicePayment
import getAntaeusDal
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.models.InvoicePayment
import io.pleo.antaeus.models.InvoicePaymentStatus
import io.pleo.antaeus.models.InvoicePaymentStatus.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import setupInitialData

class InvoicePaymentServiceTest {
    private val dal = getAntaeusDal()
    private val invoicePaymentService = InvoicePaymentService(dal)
    private val invoiceService = InvoiceService(dal)
    private val customerCount = 10
    private val invoicePerCustomerCount = 3
    private val invoiceCount = customerCount * invoicePerCustomerCount

    init {
        setupInitialData(dal, customerCount, invoicePerCustomerCount)
    }

    @Test
    fun `fetchAllByInvoiceAndStatus will return the invoice payments by status`() {
        var firstInvoiceId = 1
        var secondInvoiceId = invoiceCount / 4
        create(firstInvoiceId, FAIL)
        create(firstInvoiceId, SUCCESS)
        create(secondInvoiceId, CURRENCY_MISMATCH_ERROR)
        create(secondInvoiceId, CURRENCY_MISMATCH_ERROR)

        // assert firstInvoiceId
        var invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(firstInvoiceId, FAIL)
        assertTrue(invoicePayments.size == 1)
        assertInvoicePayment(invoicePayments.get(0), firstInvoiceId, FAIL)

        invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(firstInvoiceId, SUCCESS)
        assertTrue(invoicePayments.size == 1)
        assertInvoicePayment(invoicePayments.get(0), firstInvoiceId, SUCCESS)

        invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(firstInvoiceId, NETWORK_ERROR)
        assertTrue(invoicePayments.size == 0)

        // assert secondInvoiceId
        invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(secondInvoiceId, CURRENCY_MISMATCH_ERROR)
        assertTrue(invoicePayments.size == 2)
        assertInvoicePayment(invoicePayments.get(0), secondInvoiceId, CURRENCY_MISMATCH_ERROR)
        assertInvoicePayment(invoicePayments.get(1), secondInvoiceId, CURRENCY_MISMATCH_ERROR)
    }

    @Test
    fun `create should create new invoice payment`() {
        var invoiceId = invoiceCount / 2
        create(invoiceId, SUCCESS)

        create(invoiceId, FAIL)

        create(1, NETWORK_ERROR)
    }

    @Test
    fun `getPaymentStatus should get InvoicePaymentStatus`() {
        assertEquals(OTHER_ERROR, invoicePaymentService.getPaymentStatus(Exception()))
        assertEquals(CURRENCY_MISMATCH_ERROR, invoicePaymentService.getPaymentStatus(CurrencyMismatchException(1, 1)))
        assertEquals(CUSTOMER_NOT_FOUND_ERROR, invoicePaymentService.getPaymentStatus(CustomerNotFoundException(1)))
        assertEquals(NETWORK_ERROR, invoicePaymentService.getPaymentStatus(NetworkException()))
    }

    private fun create(invoiceId: Int, status: InvoicePaymentStatus): InvoicePayment? {
        val invoice = invoiceService.fetch(invoiceId)
        val invoicePayment = invoicePaymentService.create(invoice, status)
        assertInvoicePayment(invoicePayment, invoiceId, status)
        return invoicePayment
    }

}