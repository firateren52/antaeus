package io.pleo.antaeus.models

import org.joda.time.DateTime

data class InvoicePayment(
        val id: Int,
        val invoiceId: Int,
        val status: InvoicePaymentStatus,
        val createDate: DateTime
)
