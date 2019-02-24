package io.pleo.antaeus.core.services

import getAntaeusDal
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.models.InvoiceStatus
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import setupInitialData

class InvoiceServiceTest {
    private val dal = getAntaeusDal()
    private val invoiceService = InvoiceService(dal)
    private val customerCount = 10
    private val invoicePerCustomerCount = 3
    private val invoiceCount = customerCount * invoicePerCustomerCount

    init {
        setupInitialData(dal, customerCount, invoicePerCustomerCount)
    }

    @Test
    fun `fetch will throw if customer is not found`() {
        assertThrows<InvoiceNotFoundException> {
            val nonExistId = invoiceCount + 5
            invoiceService.fetch(nonExistId)
        }
    }

    @Test
    fun `fetch will return invoice if it exists`() {
        val invoice = invoiceService.fetch(invoiceCount / 2)
        Assertions.assertEquals(invoice.id, invoiceCount / 2)
    }

    @Test
    fun `fetchAll will return all invoices`() {
        val invoices = invoiceService.fetchAll()
        Assertions.assertEquals(invoices.size, invoiceCount)
    }

    @Test
    fun `fetchAllByStatusAndStartDate will return all invoices by status`() {
        var invoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.PENDING, DateTime.now())
        Assertions.assertEquals(invoices.size, customerCount)
        invoices.forEach { invoice -> Assertions.assertEquals(invoice.status, InvoiceStatus.PENDING) }

        invoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.PAID, DateTime.now())
        Assertions.assertEquals(invoices.size, invoiceCount - customerCount)
        invoices.forEach { invoice -> Assertions.assertEquals(invoice.status, InvoiceStatus.PAID) }

        invoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.UNCOLLECTIBLE, DateTime.now())
        Assertions.assertEquals(invoices.size, 0)
    }

    @Test
    fun `fetchAllByStatusAndStartDate will return empty because startdate is greater than yesterday`() {
        val yesterday = DateTime.now().minusDays(1)
        var invoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.PENDING, yesterday)
        Assertions.assertEquals(invoices.size, 0)

        invoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.PAID, yesterday)
        Assertions.assertEquals(invoices.size, 0)

        invoices = invoiceService.fetchAllByStatusAndStartDate(InvoiceStatus.UNCOLLECTIBLE, yesterday)
        Assertions.assertEquals(invoices.size, 0)
    }

    @Test
    fun `updateInvoiceStatus should update the invoice status`() {
        val invoiceId = invoiceCount / 2
        val invoice = invoiceService.fetch(invoiceId)

        invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.UNCOLLECTIBLE)
        val updatedInvoice = invoiceService.fetch(invoiceId)
        Assertions.assertEquals(updatedInvoice.status, InvoiceStatus.UNCOLLECTIBLE)
    }

    @Test
    fun `updateStartDate should update the invoice startDate`() {
        val invoiceId = invoiceCount / 3
        val invoice = invoiceService.fetch(invoiceId)

        val fiveHoursLater = invoice.startDate.plusHours(5)
        invoiceService.updateStartDate(invoice.id, fiveHoursLater)
        val updatedInvoice = invoiceService.fetch(invoiceId)
        Assertions.assertEquals(updatedInvoice.startDate, fiveHoursLater)
    }

}