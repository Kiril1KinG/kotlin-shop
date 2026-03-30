package com.shop.service

import com.shop.db.AuditLogs
import com.shop.db.Users
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert

class AuditService {
    fun log(
        userId: Long?,
        action: String,
        entityType: String,
        entityId: Long?,
        details: String?,
    ) {
        AuditLogs.insert {
            it[AuditLogs.userId] = userId?.let { id -> EntityID(id, Users) }
            it[AuditLogs.action] = action
            it[AuditLogs.entityType] = entityType
            it[AuditLogs.entityId] = entityId
            it[AuditLogs.details] = details
        }
    }
}
