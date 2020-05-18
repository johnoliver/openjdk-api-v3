package net.adoptopenjdk.api.v3.models

import java.time.YearMonth

class MonthlyDownloadDiff(
    val month: YearMonth,
    val total: Long,
    val monthly: Long
)