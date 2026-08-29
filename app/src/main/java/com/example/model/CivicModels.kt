package com.example.model

enum class NoticeCategory(val displayName: String, val iconEmoji: String, val description: String) {
    CONSTRUCTION("Construction & Infra", "🏗️", "Road widening, flyovers, bridges & residential high-rises"),
    ENVIRONMENT("Environment & Trees", "🌳", "MoEF&CC Parivesh clearances, tree cutting, quarrying & EIA"),
    TRANSPORT_METRO("Metro & Transport", "🚇", "Metro rail corridors, underground stations, PWD highway expansions"),
    RERA_REALESTATE("RERA Projects", "🏢", "New builder registrations, layout approvals & tower completions"),
    UTILITIES("Utilities & Water", "💧", "Stormwater drains, 24x7 water pipelines, sewage & power grids"),
    ZONING_LAND("Zoning & Land Gazettes", "📜", "Development plan changes, green zone reclassification & acquisition")
}

enum class NoticeStatus(val displayName: String, val badgeColorHex: Long) {
    OBJECTION_OPEN("⚖️ Objections Open", 0xFFF43F5E), // Red urgent
    ENVIRONMENTAL_HEARING("📢 Public Hearing Set", 0xFFF59E0B), // Amber
    TENDER_FLOATED("📋 Tender Floated", 0xFF0284C7), // Blue
    PROPOSED("💡 Proposed / DPR Ready", 0xFF8B5CF6), // Purple
    APPROVED("✅ Approved / Sanctioned", 0xFF0D9488), // Teal
    IN_PROGRESS("🚧 Work In-Progress", 0xFF64748B) // Slate
}

enum class ImpactSeverity(val label: String, val colorHex: Long) {
    LOW("Minor", 0xFF10B981),
    MODERATE("Moderate", 0xFFF59E0B),
    HIGH("High Impact", 0xFFF97316),
    CRITICAL("Severe Disruption", 0xFFEF4444)
}

data class CivicNotice(
    val id: String,
    val pincode: String,
    val locality: String,
    val city: String,
    val title: String,
    val category: NoticeCategory,
    val status: NoticeStatus,
    val impactRadiusMeters: Int,
    val publicationDate: String,
    val objectionDeadline: String? = null,
    val daysLeftForObjection: Int? = null,
    val sourcePortal: String,
    val sourceUrl: String,
    val referenceNumber: String,
    val rawSummary: String,
    val aiPlainSummary: String,
    val concernVotesCount: Int = 0,
    val hasUserFlagged: Boolean = false,
    val noiseDustRisk: ImpactSeverity = ImpactSeverity.MODERATE,
    val trafficDisruption: ImpactSeverity = ImpactSeverity.MODERATE,
    val greenCoverLossRisk: ImpactSeverity = ImpactSeverity.LOW,
    val longTermBenefit: String,
    val shortTermInconvenience: String,
    val legalActCitation: String,
    val distanceKmFromCenter: Double = 0.5,
    val angleDegree: Float = 45f // For radar visualization
)

data class PincodeArea(
    val pincode: String,
    val locality: String,
    val city: String,
    val landmark: String,
    val activeNoticesCount: Int
)

data class PincodeSubscription(
    val id: Int = 0,
    val pincode: String,
    val locality: String,
    val radiusKm: Float = 2.0f,
    val email: String = "",
    val whatsappNumber: String = "",
    val alertFrequency: String = "Instant (<24h scrape alert)",
    val notifyOnObjectionWindow: Boolean = true,
    val notifyOnHighImpactOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ObjectionDraft(
    val noticeId: String,
    val projectTitle: String,
    val authorityName: String,
    val referenceNumber: String,
    val citizenName: String,
    val societyName: String,
    val contactInfo: String,
    val selectedGrounds: List<String>,
    val customNotes: String,
    val formalLetterBody: String
)

data class RwaSocietyProfile(
    val societyName: String = "Silver Palms Co-operative Housing Society",
    val locality: String = "Wakad - Datta Mandir Road",
    val pincode: String = "411057",
    val city: String = "Pune",
    val registeredUnits: Int = 280,
    val boundaryRadiusMeters: Int = 1500,
    val rwaChairperson: String = "Dr. Aniruddh Deshmukh",
    val rwaEmail: String = "secretary.silverpalms@gmail.com"
)

data class RwaMonthlyReport(
    val reportId: String,
    val societyName: String,
    val monthYear: String,
    val pincode: String,
    val totalNoticesInRadius: Int,
    val urgentObjectionsCount: Int,
    val keyInfrastructureProjects: List<String>,
    val environmentalAlerts: List<String>,
    val utilityDisruptionSchedule: List<String>,
    val executiveSummary: String,
    val recommendedActionsForRwa: List<String>
)
