/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoicePayment
import io.pleo.antaeus.models.InvoicePaymentStatus

class InvoicePaymentService(private val dal: AntaeusDal) {

    fun fetchAllByInvoiceAndStatus(invoiceId: Int, status: InvoicePaymentStatus): List<InvoicePayment> {
        return dal.fetchInvoicePaymentsByInvoiceAndStatus(invoiceId, status)
    }

    fun create(invoice: Invoice, status: InvoicePaymentStatus): InvoicePayment? {
        return dal.createInvoicePayment(invoice, status)
    }

    fun getPaymentStatus(e: Exception): InvoicePaymentStatus {
        val status = when(e) {
            is CurrencyMismatchException -> InvoicePaymentStatus.CURRENCY_MISMATCH_ERROR
            is CustomerNotFoundException -> InvoicePaymentStatus.CUSTOMER_NOT_FOUND_ERROR
            is NetworkException -> InvoicePaymentStatus.NETWORK_ERROR
            else -> InvoicePaymentStatus.OTHER_ERROR
        }
        return status
    }

}
