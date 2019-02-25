package io.pleo.antaeus.core.services

import assertInvoicePayment
import getAntaeusDal
import getPaymentProviderWithExceptions
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoicePaymentStatus
import io.pleo.antaeus.models.InvoiceStatus
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import setupInitialData

class BillingServiceTest {
    val dal = getAntaeusDal()
    val invoiceService = InvoiceService(dal = dal)
    val invoicePaymentService = InvoicePaymentService(dal = dal)
    val billingService = BillingService.getInstance(getPaymentProviderWithExceptions(), invoiceService, invoicePaymentService)
    val pendingInvoices: List<Invoice>

    private val customerCount = 10
    private val invoicePerCustomerCount = 3

    init {
        setupInitialData(dal, customerCount, invoicePerCustomerCount, invoicePerCustomerCount)
        pendingInvoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.PENDING, DateTime.now())
    }

    @Test
    fun `chargeInvoice should charge for SUCCESS status'`() {
        val invoice = invoiceService.fetch(10)

        val invoicePayment = billingService.chargeInvoice(invoice)
        val updatedInvoice = invoiceService.fetch(invoice.id)

        assertInvoicePayment(invoicePayment, invoice.id, InvoicePaymentStatus.SUCCESS)
        assertEquals(InvoiceStatus.PAID, updatedInvoice.status)
    }

    @Test
    fun `chargeInvoice should not charge for FAIL status'`() {
        val invoice = invoiceService.fetch(5)
        assertFailedChargeResult(invoice, InvoicePaymentStatus.FAIL)
    }

    @Test
    fun `chargeInvoice should not charge for OTHER_ERROR status'`() {
        val invoice = invoiceService.fetch(4)
        assertFailedChargeResult(invoice, InvoicePaymentStatus.OTHER_ERROR)
    }

    private fun assertFailedChargeResult(invoice: Invoice, invoicePaymentStatus: InvoicePaymentStatus) {
        (1..invoicePaymentStatus.attemptCount).forEach {
            val invoicePayment = billingService.chargeInvoice(invoice)
            val updatedInvoice = invoiceService.fetch(invoice.id)
            val invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(invoice.id, invoicePaymentStatus)

            assertInvoicePayment(invoicePayment, invoice.id, invoicePaymentStatus)
            if (invoicePayments.size < invoicePaymentStatus.attemptCount) {
                assertEquals(InvoiceStatus.PENDING, updatedInvoice.status)
                assertEquals(invoice.startDate.plusSeconds(invoicePaymentStatus.delaySec), updatedInvoice.startDate)
            } else {
                assertEquals(InvoiceStatus.UNCOLLECTIBLE, updatedInvoice.status)
            }
        }
    }

}