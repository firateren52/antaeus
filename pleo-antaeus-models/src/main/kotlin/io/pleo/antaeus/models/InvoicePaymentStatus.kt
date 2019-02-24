package io.pleo.antaeus.models

import org.joda.time.Days
import org.joda.time.Hours

enum class InvoicePaymentStatus(val attemptCount: Int, val delaySec: Int) {
    SUCCESS(0, 0),
    FAIL(5, Days.ONE.toStandardSeconds().seconds),
    CURRENCY_MISMATCH_ERROR(3, Days.THREE.toStandardSeconds().seconds),
    CUSTOMER_NOT_FOUND_ERROR(3, Days.THREE.toStandardSeconds().seconds),
    NETWORK_ERROR(1000, 0),
    OTHER_ERROR(10, Hours.ONE.toStandardSeconds().seconds)
}
