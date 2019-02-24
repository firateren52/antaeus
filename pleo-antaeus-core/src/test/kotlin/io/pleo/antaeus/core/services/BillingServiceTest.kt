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
    val pendingInvoices: List<Invoice>

    private val customerCount = 10
    private val invoicePerCustomerCount = 3

    init {
        setupInitialData(dal, customerCount, invoicePerCustomerCount)
        pendingInvoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.PENDING, DateTime.now())
    }

    @Test
    fun `chargeInvoice should charge for SUCCESS status'`() {
        val billingService = BillingService(getPaymentProviderWithExceptions(80), invoiceService, invoicePaymentService)
        val invoice = pendingInvoices.get(pendingInvoices.size /5)

        val invoicePayment = billingService.chargeInvoice(invoice)
        val updatedInvoice = invoiceService.fetch(invoice.id)

        assertInvoicePayment(invoicePayment, invoice.id, InvoicePaymentStatus.SUCCESS)
        assertEquals(InvoiceStatus.PAID, updatedInvoice.status)
    }

    @Test
    fun `chargeInvoice should not charge for FAIL status'`() {
        val billingService = BillingService(getPaymentProviderWithExceptions(30), invoiceService, invoicePaymentService)
        val invoice = pendingInvoices.get(pendingInvoices.size /4)

        (1..InvoicePaymentStatus.FAIL.attemptCount).forEach {
            val invoicePayment = billingService.chargeInvoice(invoice)
            val updatedInvoice = invoiceService.fetch(invoice.id)
            val invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(invoice.id, InvoicePaymentStatus.FAIL)

            assertInvoicePayment(invoicePayment, invoice.id, InvoicePaymentStatus.FAIL)
            if (invoicePayments.size < InvoicePaymentStatus.FAIL.attemptCount) {
                assertEquals(InvoiceStatus.PENDING, updatedInvoice.status)
                assertEquals(invoice.startDate.plusSeconds(InvoicePaymentStatus.FAIL.delaySec), updatedInvoice.startDate)
            } else {
                assertEquals(InvoiceStatus.UNCOLLECTIBLE, updatedInvoice.status)
            }
        }
    }

    @Test
    fun `chargeInvoice should not charge for OTHER_ERROR status'`() {
        val billingService = BillingService(getPaymentProviderWithExceptions(18), invoiceService, invoicePaymentService)
        val invoice = pendingInvoices.get(pendingInvoices.size /4)

        (1..InvoicePaymentStatus.OTHER_ERROR.attemptCount).forEach {
            val invoicePayment = billingService.chargeInvoice(invoice)
            val updatedInvoice = invoiceService.fetch(invoice.id)
            val invoicePayments = invoicePaymentService.fetchAllByInvoiceAndStatus(invoice.id, InvoicePaymentStatus.OTHER_ERROR)

            assertInvoicePayment(invoicePayment, invoice.id, InvoicePaymentStatus.OTHER_ERROR)
            if (invoicePayments.size < InvoicePaymentStatus.OTHER_ERROR.attemptCount) {
                assertEquals(InvoiceStatus.PENDING, updatedInvoice.status)
                assertEquals(invoice.startDate.plusSeconds(InvoicePaymentStatus.OTHER_ERROR.delaySec), updatedInvoice.startDate)
            } else {
                assertEquals(InvoiceStatus.UNCOLLECTIBLE, updatedInvoice.status)
            }
        }
    }

}