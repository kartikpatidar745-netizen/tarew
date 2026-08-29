package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.model.CivicNotice
import com.example.model.ImpactSeverity
import com.example.model.NoticeCategory
import com.example.model.NoticeStatus
import com.example.ui.components.NoticeCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleNotice = CivicNotice(
      id = "TEST-01",
      pincode = "411057",
      locality = "Wakad - Datta Mandir Chowk",
      city = "Pune",
      title = "4-Lane Elevated Flyover & Grade Separator",
      category = NoticeCategory.CONSTRUCTION,
      status = NoticeStatus.OBJECTION_OPEN,
      impactRadiusMeters = 1400,
      publicationDate = "26 Aug 2026",
      objectionDeadline = "04 Sep 2026",
      daysLeftForObjection = 5,
      sourcePortal = "PMC Tender Portal",
      sourceUrl = "https://pmc.gov.in",
      referenceNumber = "PMC/PWD/2026/TR-402",
      rawSummary = "Public notice under Section 37(1) of MRTP Act 1966 for 1.4 km four-lane elevated flyover.",
      aiPlainSummary = "• 1.4 km flyover being built over Datta Mandir Chowk.\n• 5 days left to submit objections.",
      concernVotesCount = 184,
      noiseDustRisk = ImpactSeverity.CRITICAL,
      trafficDisruption = ImpactSeverity.CRITICAL,
      greenCoverLossRisk = ImpactSeverity.MODERATE,
      longTermBenefit = "Eliminates 25-minute bottleneck for IT commuters.",
      shortTermInconvenience = "Lane closures and diversions.",
      legalActCitation = "Section 37(1), MRTP Act 1966"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        Box(modifier = Modifier.padding(16.dp)) {
          NoticeCard(
            notice = sampleNotice,
            onCardClick = {},
            onVoteConcern = {},
            onDraftObjection = {},
            onSimplifyAi = {}
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

