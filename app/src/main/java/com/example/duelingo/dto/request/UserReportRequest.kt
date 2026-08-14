package com.example.duelingo.dto.request

import java.util.UUID

data class UserReportRequest(val reportedUserId: UUID, val reason: String)
