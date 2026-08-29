package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CivicNotice
import com.example.model.ImpactSeverity
import com.example.model.NoticeCategory
import com.example.model.NoticeStatus
import com.example.model.PincodeSubscription

@Entity(tableName = "civic_notices")
data class CivicNoticeEntity(
    @PrimaryKey val id: String,
    val pincode: String,
    val locality: String,
    val city: String,
    val title: String,
    val category: String, // Stored as enum name
    val status: String, // Stored as enum name
    val impactRadiusMeters: Int,
    val publicationDate: String,
    val objectionDeadline: String?,
    val daysLeftForObjection: Int?,
    val sourcePortal: String,
    val sourceUrl: String,
    val referenceNumber: String,
    val rawSummary: String,
    val aiPlainSummary: String,
    val concernVotesCount: Int,
    val hasUserFlagged: Boolean,
    val noiseDustRisk: String,
    val trafficDisruption: String,
    val greenCoverLossRisk: String,
    val longTermBenefit: String,
    val shortTermInconvenience: String,
    val legalActCitation: String,
    val distanceKmFromCenter: Double,
    val angleDegree: Float
) {
    fun toDomain(): CivicNotice {
        return CivicNotice(
            id = id,
            pincode = pincode,
            locality = locality,
            city = city,
            title = title,
            category = try { NoticeCategory.valueOf(category) } catch (e: Exception) { NoticeCategory.CONSTRUCTION },
            status = try { NoticeStatus.valueOf(status) } catch (e: Exception) { NoticeStatus.PROPOSED },
            impactRadiusMeters = impactRadiusMeters,
            publicationDate = publicationDate,
            objectionDeadline = objectionDeadline,
            daysLeftForObjection = daysLeftForObjection,
            sourcePortal = sourcePortal,
            sourceUrl = sourceUrl,
            referenceNumber = referenceNumber,
            rawSummary = rawSummary,
            aiPlainSummary = aiPlainSummary,
            concernVotesCount = concernVotesCount,
            hasUserFlagged = hasUserFlagged,
            noiseDustRisk = try { ImpactSeverity.valueOf(noiseDustRisk) } catch (e: Exception) { ImpactSeverity.MODERATE },
            trafficDisruption = try { ImpactSeverity.valueOf(trafficDisruption) } catch (e: Exception) { ImpactSeverity.MODERATE },
            greenCoverLossRisk = try { ImpactSeverity.valueOf(greenCoverLossRisk) } catch (e: Exception) { ImpactSeverity.LOW },
            longTermBenefit = longTermBenefit,
            shortTermInconvenience = shortTermInconvenience,
            legalActCitation = legalActCitation,
            distanceKmFromCenter = distanceKmFromCenter,
            angleDegree = angleDegree
        )
    }

    companion object {
        fun fromDomain(model: CivicNotice): CivicNoticeEntity {
            return CivicNoticeEntity(
                id = model.id,
                pincode = model.pincode,
                locality = model.locality,
                city = model.city,
                title = model.title,
                category = model.category.name,
                status = model.status.name,
                impactRadiusMeters = model.impactRadiusMeters,
                publicationDate = model.publicationDate,
                objectionDeadline = model.objectionDeadline,
                daysLeftForObjection = model.daysLeftForObjection,
                sourcePortal = model.sourcePortal,
                sourceUrl = model.sourceUrl,
                referenceNumber = model.referenceNumber,
                rawSummary = model.rawSummary,
                aiPlainSummary = model.aiPlainSummary,
                concernVotesCount = model.concernVotesCount,
                hasUserFlagged = model.hasUserFlagged,
                noiseDustRisk = model.noiseDustRisk.name,
                trafficDisruption = model.trafficDisruption.name,
                greenCoverLossRisk = model.greenCoverLossRisk.name,
                longTermBenefit = model.longTermBenefit,
                shortTermInconvenience = model.shortTermInconvenience,
                legalActCitation = model.legalActCitation,
                distanceKmFromCenter = model.distanceKmFromCenter,
                angleDegree = model.angleDegree
            )
        }
    }
}

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pincode: String,
    val locality: String,
    val radiusKm: Float,
    val email: String,
    val whatsappNumber: String,
    val alertFrequency: String,
    val notifyOnObjectionWindow: Boolean,
    val notifyOnHighImpactOnly: Boolean,
    val createdAt: Long
) {
    fun toDomain() = PincodeSubscription(
        id = id,
        pincode = pincode,
        locality = locality,
        radiusKm = radiusKm,
        email = email,
        whatsappNumber = whatsappNumber,
        alertFrequency = alertFrequency,
        notifyOnObjectionWindow = notifyOnObjectionWindow,
        notifyOnHighImpactOnly = notifyOnHighImpactOnly,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(sub: PincodeSubscription) = SubscriptionEntity(
            id = sub.id,
            pincode = sub.pincode,
            locality = sub.locality,
            radiusKm = sub.radiusKm,
            email = sub.email,
            whatsappNumber = sub.whatsappNumber,
            alertFrequency = sub.alertFrequency,
            notifyOnObjectionWindow = sub.notifyOnObjectionWindow,
            notifyOnHighImpactOnly = sub.notifyOnHighImpactOnly,
            createdAt = sub.createdAt
        )
    }
}

@Entity(tableName = "saved_objections")
data class SavedObjectionEntity(
    @PrimaryKey val id: String,
    val noticeId: String,
    val projectTitle: String,
    val authorityName: String,
    val citizenName: String,
    val formalLetterBody: String,
    val createdAt: Long = System.currentTimeMillis()
)
