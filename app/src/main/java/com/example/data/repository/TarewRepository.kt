package com.example.data.repository

import com.example.data.local.CivicNoticeEntity
import com.example.data.local.SavedObjectionEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.local.TarewDao
import com.example.data.remote.GeminiClient
import com.example.model.CivicNotice
import com.example.model.ImpactSeverity
import com.example.model.NoticeCategory
import com.example.model.NoticeStatus
import com.example.model.ObjectionDraft
import com.example.model.PincodeArea
import com.example.model.PincodeSubscription
import com.example.model.RwaMonthlyReport
import com.example.model.RwaSocietyProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TarewRepository(private val dao: TarewDao) {

    val allNotices: Flow<List<CivicNotice>> = dao.getAllNotices().map { entities ->
        entities.map { it.toDomain() }
    }

    val openObjectionNotices: Flow<List<CivicNotice>> = dao.getOpenObjectionNotices().map { entities ->
        entities.map { it.toDomain() }
    }

    val subscriptions: Flow<List<PincodeSubscription>> = dao.getAllSubscriptions().map { entities ->
        entities.map { it.toDomain() }
    }

    val savedObjections: Flow<List<SavedObjectionEntity>> = dao.getAllSavedObjections()

    val popularAreas = listOf(
        PincodeArea("411057", "Wakad - Hinjawadi Corridor", "Pune", "Datta Mandir Chowk", 5),
        PincodeArea("411045", "Baner - Balewadi", "Pune", "High Street Junction", 4),
        PincodeArea("302020", "Mansarovar", "Jaipur", "B2 Bypass Road", 4),
        PincodeArea("302017", "Malviya Nagar", "Jaipur", "Gaurav Tower Circle", 3),
        PincodeArea("560102", "HSR Layout", "Bengaluru", "27th Main Rd / Agara", 4),
        PincodeArea("560066", "Whitefield", "Bengaluru", "ITPL Main Road", 3),
        PincodeArea("400053", "Andheri West (Lokhandwala)", "Mumbai", "Link Road Crossway", 3),
        PincodeArea("110075", "Dwarka Sector 10-14", "Delhi NCR", "Metro Blue Line Belt", 3)
    )

    suspend fun checkAndSeedDatabase() {
        val count = dao.getNoticesCount()
        if (count == 0) {
            dao.insertNotices(getSeedNotices().map { CivicNoticeEntity.fromDomain(it) })
            // Add default starter subscription
            dao.insertSubscription(
                SubscriptionEntity(
                    pincode = "411057",
                    locality = "Wakad - Datta Mandir Road",
                    radiusKm = 2.5f,
                    email = "resident.wakad@gmail.com",
                    whatsappNumber = "+91 98230 45892",
                    alertFrequency = "Instant (<24h scrape alert)",
                    notifyOnObjectionWindow = true,
                    notifyOnHighImpactOnly = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun incrementConcernVote(noticeId: String) {
        dao.incrementConcernVote(noticeId)
    }

    suspend fun addSubscription(sub: PincodeSubscription): Long {
        return dao.insertSubscription(SubscriptionEntity.fromDomain(sub))
    }

    suspend fun deleteSubscription(id: Int) {
        dao.deleteSubscription(id)
    }

    suspend fun saveObjection(draft: ObjectionDraft) {
        val entity = SavedObjectionEntity(
            id = "OBJ-${System.currentTimeMillis()}",
            noticeId = draft.noticeId,
            projectTitle = draft.projectTitle,
            authorityName = draft.authorityName,
            citizenName = draft.citizenName,
            formalLetterBody = draft.formalLetterBody,
            createdAt = System.currentTimeMillis()
        )
        dao.insertSavedObjection(entity)
    }

    suspend fun triggerScrapeSimulation(pincode: String): Int {
        // Simulates scraping 2 new notices from RERA / Parivesh / Municipal Portal for the selected pincode
        val newNotice1 = CivicNotice(
            id = "SCRAPE-RERA-${System.currentTimeMillis().toString().takeLast(5)}",
            pincode = pincode,
            locality = "Sector 4 / Near Main Ring Road",
            city = if (pincode.startsWith("411")) "Pune" else if (pincode.startsWith("302")) "Jaipur" else if (pincode.startsWith("560")) "Bengaluru" else "Local Area",
            title = "New RERA Filing: 28-Storey Mixed Commercial Tower & Mall Complex",
            category = NoticeCategory.RERA_REALESTATE,
            status = NoticeStatus.OBJECTION_OPEN,
            impactRadiusMeters = 950,
            publicationDate = "Today (Live Scrape)",
            objectionDeadline = "14 Days Remaining",
            daysLeftForObjection = 14,
            sourcePortal = "State RERA Real Estate Portal",
            sourceUrl = "https://rera.gov.in/project/PR2026",
            referenceNumber = "RERA/EXP/2026/9028",
            rawSummary = "Public notice is hereby issued under Real Estate (Regulation & Development) Act 2016 Section 4(2) for grant of layout revision approving additional 6 floors with baseline FAR 3.25. Objections regarding common amenities or access road width to be lodged within 14 days.",
            aiPlainSummary = "• Builder filed to add 6 extra floors (total 28 storeys) to the commercial tower.\n• Might increase traffic congestion at the entry road by 30%.\n• You have 14 days to submit objections regarding road width and parking access.",
            concernVotesCount = 18,
            noiseDustRisk = ImpactSeverity.HIGH,
            trafficDisruption = ImpactSeverity.HIGH,
            greenCoverLossRisk = ImpactSeverity.LOW,
            longTermBenefit = "Brings retail supermarket, EV charging stations, and multiplex within 400m walking distance.",
            shortTermInconvenience = "Heavy concrete mixer trucks moving through residential crossroad for 18 months.",
            legalActCitation = "Section 4(2)(l) & Section 14(2), RERA Act 2016",
            distanceKmFromCenter = 0.8,
            angleDegree = 110f
        )
        dao.insertNotice(CivicNoticeEntity.fromDomain(newNotice1))
        return 1
    }

    suspend fun simplifyNoticeWithAi(notice: CivicNotice): String {
        val prompt = """
            You are Tarew AI, an expert civic analyst for Indian neighborhoods.
            Summarize this complex Indian government/RERA/Parivesh public notice in crystal clear, everyday plain language for neighborhood WhatsApp groups.
            
            Notice Title: ${notice.title}
            Category: ${notice.category.displayName}
            Official Legal Text:
            "${notice.rawSummary}"
            Act/Clause: ${notice.legalActCitation}
            
            Provide exactly:
            1. "What is actually happening" (1 plain sentence)
            2. "Impact on your daily life" (noise, traffic, water, trees) (2 bullet points)
            3. "Key deadline / Next action for residents" (1 sentence)
            Keep it strictly objective, crisp, and conversational.
        """.trimIndent()

        val aiResult = GeminiClient.generateText(prompt)
        if (aiResult.isNotBlank()) return aiResult

        // Offline smart template fallback
        return """
            📌 What is happening:
            ${notice.title} is officially moving forward under ${notice.legalActCitation}.
            
            🚦 Impact on your street:
            • Short-term: ${notice.shortTermInconvenience}
            • Long-term: ${notice.longTermBenefit}
            
            ⏳ Resident Action:
            ${if (notice.daysLeftForObjection != null && notice.daysLeftForObjection > 0) "Objection window closes in ${notice.daysLeftForObjection} days! File feedback with ${notice.sourcePortal}." else "Track project execution on ${notice.sourcePortal}."}
        """.trimIndent()
    }

    suspend fun generateObjectionLetterWithAi(
        notice: CivicNotice,
        citizenName: String,
        societyName: String,
        selectedGrounds: List<String>,
        customNotes: String
    ): String {
        val groundsList = selectedGrounds.joinToString("\n- ")
        val prompt = """
            You are a legal assistant specializing in Indian urban planning, municipal bylaws, MRTP/Town Planning Acts, and MoEF&CC environmental regulations.
            Write a formal, legally structured Citizen Objection Letter for an Indian resident opposing or seeking modifications to an upcoming municipal/infrastructure/RERA project.
            
            Citizen Name: $citizenName
            Resident Association / Society: $societyName
            Project Notice: ${notice.title}
            Reference Number: ${notice.referenceNumber}
            Source / Authority: ${notice.sourcePortal}
            Relevant Act Citation: ${notice.legalActCitation}
            
            Grounds of Objection:
            - $groundsList
            
            Additional Citizen Notes: $customNotes
            
            Write the formal letter adhering to standard Indian administrative petition format:
            - Addressed to the Competent Authority / Municipal Commissioner / SEAC Member Secretary
            - Subject Line citing Notice Ref No.
            - Background / Locus Standi of the aggrieved resident
            - Key Legal & Civic Grounds (traffic congestion, tree canopy loss, lack of public notice compliance, water load)
            - Specific Prayers / Demands (e.g. stay on execution until traffic audit and public hearing is conducted)
            - Formal closing
        """.trimIndent()

        val aiResult = GeminiClient.generateText(prompt)
        if (aiResult.isNotBlank()) return aiResult

        // Fallback robust legal template
        return """
To,
The Competent Authority / Municipal Commissioner,
${notice.sourcePortal},
Subject: Formal Objection / Representation regarding Notice No. ${notice.referenceNumber} — "${notice.title}"

Respected Sir/Madam,

I, $citizenName, resident of $societyName, ${notice.locality}, ${notice.city} (Pincode: ${notice.pincode}), hereby submit my formal objection against the proposed project under ${notice.legalActCitation}.

The proposed execution in its current layout will cause severe irreversible civic and environmental hardship to residents within a ${notice.impactRadiusMeters}m radius due to the following grounds:

${selectedGrounds.mapIndexed { idx, ground -> "${idx + 1}. $ground" }.joinToString("\n")}
${if (customNotes.isNotBlank()) "\nAdditional Local Context:\n$customNotes\n" else ""}

In light of the statutory provisions under the Town Planning & Environmental Protection frameworks, I earnestly pray that:
a) A comprehensive public hearing and Environmental & Traffic Impact Assessment be conducted before grant of final execution sanction.
b) The current design be modified to safeguard pedestrian safety, green canopy, and resident access.
c) Work be stayed until public objections are duly addressed on record.

Yours faithfully,
$citizenName
$societyName, ${notice.locality}
Date: ${notice.publicationDate}
        """.trimIndent()
    }

    suspend fun generateRwaReportWithAi(profile: RwaSocietyProfile, notices: List<CivicNotice>): RwaMonthlyReport {
        val radiusNotices = notices.filter { it.pincode == profile.pincode || it.distanceKmFromCenter <= (profile.boundaryRadiusMeters / 1000.0) }
        val urgentObjections = radiusNotices.filter { it.daysLeftForObjection != null && it.daysLeftForObjection > 0 }

        val prompt = """
            You are Tarew's RWA Intelligence Engine.
            Generate an executive Future-Impact Intelligence Briefing for the Managing Committee of ${profile.societyName}, located at ${profile.locality}, ${profile.city} (Pincode: ${profile.pincode}).
            
            Society Boundary Radius: ${profile.boundaryRadiusMeters}m
            Upcoming Projects in Perimeter (${radiusNotices.size}):
            ${radiusNotices.map { "• ${it.title} [Status: ${it.status.displayName}] [Impact: ${it.impactRadiusMeters}m] [Ref: ${it.referenceNumber}]" }.joinToString("\n")}
            
            Urgent Objections Pending (${urgentObjections.size}):
            ${urgentObjections.map { "• ${it.title} (Deadline: ${it.daysLeftForObjection} days)" }.joinToString("\n")}
            
            Produce:
            1. Executive Overview (3 concise sentences highlighting major upcoming transformations)
            2. Actionable Resolutions for Society AGM / Committee Meeting (3 bullet points)
        """.trimIndent()

        val aiResult = GeminiClient.generateText(prompt)
        val execSummary = if (aiResult.isNotBlank()) aiResult else {
            "Within your society's ${profile.boundaryRadiusMeters}m perimeter, ${radiusNotices.size} municipal and private infrastructure works are scheduled. There are ${urgentObjections.size} notices with active citizen objection windows that directly affect your access roads and groundwater. Proactive representation to the Municipal Ward Office is strongly advised before civil work commences."
        }

        return RwaMonthlyReport(
            reportId = "RWA-REP-${profile.pincode}-${System.currentTimeMillis().toString().takeLast(4)}",
            societyName = profile.societyName,
            monthYear = "September 2026",
            pincode = profile.pincode,
            totalNoticesInRadius = radiusNotices.size,
            urgentObjectionsCount = urgentObjections.size,
            keyInfrastructureProjects = radiusNotices.map { it.title },
            environmentalAlerts = radiusNotices.filter { it.category == NoticeCategory.ENVIRONMENT }.map { it.title },
            utilityDisruptionSchedule = listOf(
                "Water feeder line trenching: Estimated night supply drops (11 PM - 4 AM)",
                "Flyover pillar construction: Alternate route via service road starting next month"
            ),
            executiveSummary = execSummary,
            recommendedActionsForRwa = listOf(
                "Pass a formal committee resolution regarding noise mitigation for night works.",
                "File collective RWA objection for Notice ${urgentObjections.firstOrNull()?.referenceNumber ?: "PMC-TR-402"} before deadline.",
                "Coordinate with Ward Assistant Engineer for dedicated tanker filling points during water main pipe realignment."
            )
        )
    }

    private fun getSeedNotices(): List<CivicNotice> {
        return listOf(
            CivicNotice(
                id = "PUNE-PMC-2026-102",
                pincode = "411057",
                locality = "Wakad - Datta Mandir Chowk",
                city = "Pune",
                title = "4-Lane Elevated Flyover & Grade Separator - Datta Mandir Chowk to Bhumkar Chowk",
                category = NoticeCategory.CONSTRUCTION,
                status = NoticeStatus.OBJECTION_OPEN,
                impactRadiusMeters = 1400,
                publicationDate = "26 Aug 2026",
                objectionDeadline = "04 Sep 2026",
                daysLeftForObjection = 5,
                sourcePortal = "Pune Municipal Corp Tender Portal (pmc.gov.in)",
                sourceUrl = "https://pmc.gov.in/tenders/2026/TR-402",
                referenceNumber = "PMC/PWD/2026/TR-402",
                rawSummary = "Public notice under Section 37(1) of Maharashtra Regional & Town Planning (MRTP) Act 1966 for alignment modification and construction of 1.4 km four-lane elevated flyover spanning Datta Mandir Junction to Bhumkar Chowk. Notice is hereby given calling for suggestions and objections in writing within 15 days.",
                aiPlainSummary = "• 1.4 km flyover being built over Datta Mandir Chowk toward Bhumkar Chowk.\n• Construction will cause major dust & lane closures for 18 months.\n• 5 days left to submit objections regarding pedestrian skywalks and noise barriers.",
                concernVotesCount = 184,
                noiseDustRisk = ImpactSeverity.CRITICAL,
                trafficDisruption = ImpactSeverity.CRITICAL,
                greenCoverLossRisk = ImpactSeverity.MODERATE,
                longTermBenefit = "Eliminates 25-minute bottleneck for IT commuters going to Hinjawadi Phase 1 & 2.",
                shortTermInconvenience = "Datta Mandir junction reduced to 1 lane each side; heavy peak-hour diversions via Kankar Chowk.",
                legalActCitation = "Section 37(1), MRTP Act 1966",
                distanceKmFromCenter = 0.4,
                angleDegree = 30f
            ),
            CivicNotice(
                id = "PARIVESH-EC-MAH-491",
                pincode = "411057",
                locality = "Hinjawadi - Wakad Border Zone",
                city = "Pune",
                title = "MoEF&CC Parivesh EIA Clearance: 45-Acre High-Density IT SEZ & Commercial Hub",
                category = NoticeCategory.ENVIRONMENT,
                status = NoticeStatus.ENVIRONMENTAL_HEARING,
                impactRadiusMeters = 2500,
                publicationDate = "22 Aug 2026",
                objectionDeadline = "10 Sep 2026",
                daysLeftForObjection = 12,
                sourcePortal = "Parivesh MoEF&CC (parivesh.nic.in)",
                sourceUrl = "https://parivesh.nic.in/clearance/EC-2026-MAH-INFRA-491",
                referenceNumber = "EC/2026/MAH/INFRA/491",
                rawSummary = "Application for Environmental Clearance (Category B1 under EIA Notification 2006) for proposed Commercial & IT SEZ with built-up area 3,40,000 sq.m. Public hearing scheduled by Maharashtra Pollution Control Board (MPCB) regarding groundwater withdrawal and felling of 118 indigenous trees.",
                aiPlainSummary = "• Massive 45-acre IT tech park cleared for environmental review.\n• Involves cutting 118 trees and daily groundwater extraction of 4.5 lakh litres.\n• Public hearing set for Sept 10 at MPCB regional office — residents can voice concerns.",
                concernVotesCount = 219,
                noiseDustRisk = ImpactSeverity.HIGH,
                trafficDisruption = ImpactSeverity.HIGH,
                greenCoverLossRisk = ImpactSeverity.CRITICAL,
                longTermBenefit = "Creates 22,000 tech jobs, brings multi-speciality medical center and public plaza.",
                shortTermInconvenience = "Groundwater table stress for neighboring housing societies; 200+ dumpers daily on Hinjawadi link road.",
                legalActCitation = "Clause 7(i), EIA Notification 2006 (MoEF&CC)",
                distanceKmFromCenter = 1.2,
                angleDegree = 160f
            ),
            CivicNotice(
                id = "MAHARERA-P521-089",
                pincode = "411057",
                locality = "Wakad - Choudhary Park",
                city = "Pune",
                title = "MahaRERA Registration: 'Aura Grand' 4 Towers (32 Floors each, 540 Luxury Units)",
                category = NoticeCategory.RERA_REALESTATE,
                status = NoticeStatus.APPROVED,
                impactRadiusMeters = 700,
                publicationDate = "18 Aug 2026",
                sourcePortal = "MahaRERA Portal (maharera.mahaonline.gov.in)",
                sourceUrl = "https://maharera.mahaonline.gov.in/project/P52100052918",
                referenceNumber = "MahaRERA: P52100052918",
                rawSummary = "Project registered with MahaRERA for development of 4 residential towers comprising 32 upper floors, 3 level basements, with proposed completion date December 2029. Sanctioned under PCMC Unified DCR 2020.",
                aiPlainSummary = "• 4 new 32-storey towers approved behind Choudhary Park.\n• 540 new families moving in by 2029.\n• Will increase demand on 12-meter access road and electricity transformer capacity.",
                concernVotesCount = 64,
                noiseDustRisk = ImpactSeverity.MODERATE,
                trafficDisruption = ImpactSeverity.MODERATE,
                greenCoverLossRisk = ImpactSeverity.LOW,
                longTermBenefit = "Adds retail grocery, paved pedestrian sidewalks, and upgraded municipal stormwater pipe along the street.",
                shortTermInconvenience = "Piling rig vibration and concrete pouring noise during daytime hours for 9 months.",
                legalActCitation = "Section 5, Real Estate (Regulation & Development) Act 2016",
                distanceKmFromCenter = 0.6,
                angleDegree = 290f
            ),
            CivicNotice(
                id = "MAHAMETRO-PUNE-L4",
                pincode = "411045",
                locality = "Baner - Balewadi High Street",
                city = "Pune",
                title = "MahaMetro Line 4 Extension: Underground Cut & Cover Tunnel & Station Entry Shaft",
                category = NoticeCategory.TRANSPORT_METRO,
                status = NoticeStatus.IN_PROGRESS,
                impactRadiusMeters = 1800,
                publicationDate = "15 Aug 2026",
                sourcePortal = "MahaMetro Public Notice",
                sourceUrl = "https://punemetrorail.org/notices/line4-shaft",
                referenceNumber = "PMRDA/METRO/2026/S-14",
                rawSummary = "Notice to public regarding barricading of 600m stretch on Balewadi High Street for underground station diaphragm wall construction and utility shifting under Metro Railways (Construction of Works) Act 1978.",
                aiPlainSummary = "• Metro Line 4 construction barricades set up on Balewadi High Street.\n• 2 lanes blocked for underground station shaft.\n• Station will be operational by late 2028 with direct link to Shivajinagar & Hinjawadi.",
                concernVotesCount = 92,
                noiseDustRisk = ImpactSeverity.HIGH,
                trafficDisruption = ImpactSeverity.HIGH,
                greenCoverLossRisk = ImpactSeverity.LOW,
                longTermBenefit = "High-speed zero-emission metro connectivity within 300m walking distance; boosts rental yields.",
                shortTermInconvenience = "Parking banned along high street; speed limit restricted to 20 km/h.",
                legalActCitation = "Section 21, Metro Railways (Construction of Works) Act 1978",
                distanceKmFromCenter = 1.8,
                angleDegree = 210f
            ),
            CivicNotice(
                id = "PMC-WATER-24X7-88",
                pincode = "411057",
                locality = "Wakad - Kankar Chowk & Shankar Kalat Nagar",
                city = "Pune",
                title = "PMC 24x7 Equated Water Distribution: 900mm DI Feeder Main Trenching",
                category = NoticeCategory.UTILITIES,
                status = NoticeStatus.TENDER_FLOATED,
                impactRadiusMeters = 800,
                publicationDate = "27 Aug 2026",
                sourcePortal = "PMC Water Supply Dept",
                sourceUrl = "https://pmc.gov.in/water/tenders/900di",
                referenceNumber = "PMC/WSD/2026/900-DI",
                rawSummary = "Tenders invited for excavation, laying, testing and commissioning of 900mm Ductile Iron water transmission main pipe under Smart City Amrut 2.0 Scheme. Work duration 120 days.",
                aiPlainSummary = "• New 900mm large water supply main pipe being laid under the road.\n• Will solve low water pressure issues in Wakad societies.\n• Road will be dug up in 200m batches over the next 4 months.",
                concernVotesCount = 47,
                noiseDustRisk = ImpactSeverity.MODERATE,
                trafficDisruption = ImpactSeverity.MODERATE,
                greenCoverLossRisk = ImpactSeverity.LOW,
                longTermBenefit = "Guaranteed 135 LPCD pressurized municipal water supply, reducing reliance on private water tankers.",
                shortTermInconvenience = "Trenching on Shankar Kalat road; temporary alternate-day evening supply shutdowns during pipe tie-ins.",
                legalActCitation = "Maharashtra Municipal Corporations Act Section 67",
                distanceKmFromCenter = 0.5,
                angleDegree = 75f
            ),
            CivicNotice(
                id = "JAIPUR-JDA-2026-302",
                pincode = "302020",
                locality = "Mansarovar - B2 Bypass Link",
                city = "Jaipur",
                title = "JDA Sector Road Widening (100 ft) & Commercial Mixed-Use Corridor Rezoning",
                category = NoticeCategory.ZONING_LAND,
                status = NoticeStatus.OBJECTION_OPEN,
                impactRadiusMeters = 2000,
                publicationDate = "24 Aug 2026",
                objectionDeadline = "08 Sep 2026",
                daysLeftForObjection = 8,
                sourcePortal = "Jaipur Development Authority (jda.urban.rajasthan.gov.in)",
                sourceUrl = "https://jda.urban.rajasthan.gov.in/notices/mansarovar-100ft",
                referenceNumber = "JDA/TP/2026/REV-819",
                rawSummary = "Public notice under Section 25 of Jaipur Development Authority Act 1982 for modification of Master Development Plan 2025 to rezone peripheral agricultural green belts into commercial mixed-use zone with setback acquisitions. Objections in writing to Secretary, JDA.",
                aiPlainSummary = "• JDA proposes to widen B2 Bypass connector to 100 feet and allow commercial shops/hotels.\n• Requires acquiring front boundary setbacks from roadside plots.\n• 8 days left to file objections with JDA Town Planning.",
                concernVotesCount = 156,
                noiseDustRisk = ImpactSeverity.HIGH,
                trafficDisruption = ImpactSeverity.HIGH,
                greenCoverLossRisk = ImpactSeverity.HIGH,
                longTermBenefit = "Wider road eliminates bottle-necks connecting Mansarovar to Jaipur International Airport.",
                shortTermInconvenience = "Boundary wall demolition along the corridor; utility pole shifting causes intermittent power cuts.",
                legalActCitation = "Section 25, Jaipur Development Authority Act 1982",
                distanceKmFromCenter = 1.1,
                angleDegree = 135f
            ),
            CivicNotice(
                id = "BLR-BDA-HSR-560",
                pincode = "560102",
                locality = "HSR Layout - 27th Main to Agara Lake",
                city = "Bengaluru",
                title = "BDA Elevated Rotary Flyover & Stormwater Drain (SWD) RCC Box Culvert",
                category = NoticeCategory.CONSTRUCTION,
                status = NoticeStatus.OBJECTION_OPEN,
                impactRadiusMeters = 1600,
                publicationDate = "25 Aug 2026",
                objectionDeadline = "02 Sep 2026",
                daysLeftForObjection = 4,
                sourcePortal = "Bangalore Development Authority (bda.karnataka.gov.in)",
                sourceUrl = "https://bda.karnataka.gov.in/tenders/hsr-flyover",
                referenceNumber = "BDA/EE/INFRA/2026/812",
                rawSummary = "Notification calling for stakeholder objections regarding construction of 3-arm elevated flyover and realignment of Raja Kaluve stormwater drain at Agara junction under KTCP Act 1961 Section 14.",
                aiPlainSummary = "• 3-arm elevated flyover planned at Agara Junction on Outer Ring Road.\n• Involves redesigning the main storm drain next to Agara lake.\n• Objections open for 4 more days regarding flood risks and tree felling.",
                concernVotesCount = 278,
                noiseDustRisk = ImpactSeverity.CRITICAL,
                trafficDisruption = ImpactSeverity.CRITICAL,
                greenCoverLossRisk = ImpactSeverity.HIGH,
                longTermBenefit = "Bypasses chronic 35-minute Silk Board / Agara bottleneck toward Bellandur and Marathahalli.",
                shortTermInconvenience = "Severe traffic snarls on 27th Main; potential monsoon waterlogging during box culvert excavation.",
                legalActCitation = "Section 14, Karnataka Town & Country Planning Act 1961",
                distanceKmFromCenter = 0.9,
                angleDegree = 320f
            ),
            CivicNotice(
                id = "MUM-BMC-ANDHERI-400",
                pincode = "400053",
                locality = "Andheri West - Lokhandwala Complex",
                city = "Mumbai",
                title = "BMC Underground Stormwater Holding Tank & Pumping Station - Yamuna Nagar",
                category = NoticeCategory.UTILITIES,
                status = NoticeStatus.IN_PROGRESS,
                impactRadiusMeters = 900,
                publicationDate = "10 Aug 2026",
                sourcePortal = "Brihanmumbai Municipal Corp (portal.mcgm.gov.in)",
                sourceUrl = "https://portal.mcgm.gov.in/swd/lokhandwala-tank",
                referenceNumber = "BMC/SWD/WS/2026/304",
                rawSummary = "Construction of 1.2 crore litre capacity holding tank beneath public open space to prevent chronic waterlogging in Lokhandwala and Millat Nagar under Mumbai Municipal Corporation Act Section 220.",
                aiPlainSummary = "• Massive underground holding tank being built under Yamuna Nagar open ground.\n• Will store rainwater during high tide and pump it out to prevent waterlogging.\n• Ground will be restored with grass & running track upon completion.",
                concernVotesCount = 88,
                noiseDustRisk = ImpactSeverity.MODERATE,
                trafficDisruption = ImpactSeverity.MODERATE,
                greenCoverLossRisk = ImpactSeverity.LOW,
                longTermBenefit = "Prevents chronic knee-deep flooding on Lokhandwala Main Road during monsoon cloudbursts.",
                shortTermInconvenience = "Public park closed for 14 months; piling sound during day hours.",
                legalActCitation = "Section 220, Mumbai Municipal Corporation Act 1888",
                distanceKmFromCenter = 0.7,
                angleDegree = 45f
            )
        )
    }
}
