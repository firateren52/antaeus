package io.pleo.antaeus.core.services

import getAntaeusDal
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.models.InvoicePayment
import io.pleo.antaeus.models.InvoicePaymentStatus
import org.junit.jupiter.api.Assertions
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
        create(firstInvoiceId, InvoicePaymentStatus.FAIL)
        create(firstInvoiceId, InvoicePaymentStatus.SUCCESS)
        create(secondInvoiceId, InvoicePaymentStatus.CURRENCY_MISMATCH_ERROR)
        create(secondInvoiceId, InvoicePaymentStatus.CURRENCY_MISMATCH_ERROR)

        // assert firstInvoiceId
        var invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(firstInvoiceId, InvoicePaymentStatus.FAIL)
        Assertions.assertTrue(invoicePayments.size == 1)
        assertInvoicePayment(invoicePayments.get(0), firstInvoiceId, InvoicePaymentStatus.FAIL)

        invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(firstInvoiceId, InvoicePaymentStatus.SUCCESS)
        Assertions.assertTrue(invoicePayments.size == 1)
        assertInvoicePayment(invoicePayments.get(0), firstInvoiceId, InvoicePaymentStatus.SUCCESS)

        invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(firstInvoiceId, InvoicePaymentStatus.NETWORK_ERROR)
        Assertions.assertTrue(invoicePayments.size == 0)

        // assert secondInvoiceId
        invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(secondInvoiceId, InvoicePaymentStatus.CURRENCY_MISMATCH_ERROR)
        Assertions.assertTrue(invoicePayments.size == 2)
        assertInvoicePayment(invoicePayments.get(0), secondInvoiceId, InvoicePaymentStatus.CURRENCY_MISMATCH_ERROR)
        assertInvoicePayment(invoicePayments.get(1), secondInvoiceId, InvoicePaymentStatus.CURRENCY_MISMATCH_ERROR)
    }

    @Test
    fun `create should create new invoice payment`() {
        var invoiceId = invoiceCount / 2
        create(invoiceId, InvoicePaymentStatus.SUCCESS)

        create(invoiceId, InvoicePaymentStatus.FAIL)

        create(1, InvoicePaymentStatus.NETWORK_ERROR)
    }

    @Test
    fun `getPaymentStatus should get InvoicePaymentStatus`() {
        Assertions.assertEquals(InvoicePaymentStatus.OTHER_ERROR, invoicePaymentService.getPaymentStatus(Exception()))
        Assertions.assertEquals(InvoicePaymentStatus.CURRENCY_MISMATCH_ERROR, invoicePaymentService.getPaymentStatus(CurrencyMismatchException(1, 1)))
        Assertions.assertEquals(InvoicePaymentStatus.CUSTOMER_NOT_FOUND_ERROR, invoicePaymentService.getPaymentStatus(CustomerNotFoundException(1)))
        Assertions.assertEquals(InvoicePaymentStatus.NETWORK_ERROR, invoicePaymentService.getPaymentStatus(NetworkException()))
    }

    private fun create(invoiceId: Int, status: InvoicePaymentStatus): InvoicePayment? {
        val invoice = invoiceService.fetch(invoiceId)
        val invoicePayment = invoicePaymentService.create(invoice, status)
        assertInvoicePayment(invoicePayment, invoiceId, status)
        return invoicePayment
    }

    private fun assertInvoicePayment(invoicePayment: InvoicePayment?, invoiceId: Int, status: InvoicePaymentStatus) {
        Assertions.assertTrue(invoicePayment != null && invoicePayment.id > 0)
        Assertions.assertEquals(invoicePayment?.invoiceId, invoiceId)
        Assertions.assertEquals(invoicePayment?.status, status)
    }

}