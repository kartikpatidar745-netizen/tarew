package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedObjectionEntity
import com.example.data.local.TarewDatabase
import com.example.data.repository.TarewRepository
import com.example.model.CivicNotice
import com.example.model.NoticeCategory
import com.example.model.ObjectionDraft
import com.example.model.PincodeArea
import com.example.model.PincodeSubscription
import com.example.model.RwaMonthlyReport
import com.example.model.RwaSocietyProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TarewUiState(
    val currentPincode: String = "411057",
    val currentLocality: String = "Wakad - Hinjawadi Corridor",
    val currentCity: String = "Pune",
    val searchQuery: String = "",
    val selectedCategory: NoticeCategory? = null,
    val onlyObjectionsFilter: Boolean = false,
    val isRadarViewMode: Boolean = true,
    val selectedTab: Int = 0, // 0: Radar & Feed, 1: Objections Hub, 2: Alerts & Scrapers, 3: RWA Dashboard
    val activeNoticeDetail: CivicNotice? = null,
    val isScraping: Boolean = false,
    val scrapeMessage: String? = null,
    val isGeneratingAi: Boolean = false,
    val generatedLetter: String? = null,
    val rwaProfile: RwaSocietyProfile = RwaSocietyProfile(),
    val rwaReport: RwaMonthlyReport? = null,
    val isGeneratingRwa: Boolean = false,
    val isAiSimplifyingNotice: Boolean = false,
    val simplifiedNoticeMap: Map<String, String> = emptyMap(),
    val showPincodePicker: Boolean = false,
    val showObjectionDialog: Boolean = false
)

class TarewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TarewRepository

    init {
        val db = TarewDatabase.getInstance(application)
        repository = TarewRepository(db.tarewDao())
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
    }

    private val _uiState = MutableStateFlow(TarewUiState())
    val uiState: StateFlow<TarewUiState> = _uiState

    val popularAreas: List<PincodeArea> = repository.popularAreas

    val allNotices: StateFlow<List<CivicNotice>> = repository.allNotices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val openObjectionNotices: StateFlow<List<CivicNotice>> = repository.openObjectionNotices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val subscriptions: StateFlow<List<PincodeSubscription>> = repository.subscriptions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedObjections: StateFlow<List<SavedObjectionEntity>> = repository.savedObjections.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredNotices: StateFlow<List<CivicNotice>> = combine(
        allNotices,
        _uiState
    ) { notices, state ->
        notices.filter { notice ->
            val matchesPincodeOrCity = (notice.pincode == state.currentPincode ||
                    notice.city.equals(state.currentCity, ignoreCase = true) ||
                    state.currentPincode.isBlank())
            val matchesCategory = state.selectedCategory == null || notice.category == state.selectedCategory
            val matchesObjection = !state.onlyObjectionsFilter || (notice.daysLeftForObjection != null && notice.daysLeftForObjection > 0)
            val matchesQuery = state.searchQuery.isBlank() ||
                    notice.title.contains(state.searchQuery, ignoreCase = true) ||
                    notice.locality.contains(state.searchQuery, ignoreCase = true) ||
                    notice.rawSummary.contains(state.searchQuery, ignoreCase = true) ||
                    notice.referenceNumber.contains(state.searchQuery, ignoreCase = true)

            matchesPincodeOrCity && matchesCategory && matchesObjection && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun toggleRadarView() {
        _uiState.value = _uiState.value.copy(isRadarViewMode = !_uiState.value.isRadarViewMode)
    }

    fun selectPincode(area: PincodeArea) {
        _uiState.value = _uiState.value.copy(
            currentPincode = area.pincode,
            currentLocality = area.locality,
            currentCity = area.city,
            showPincodePicker = false
        )
    }

    fun setCustomPincode(pincode: String, locality: String, city: String) {
        _uiState.value = _uiState.value.copy(
            currentPincode = pincode,
            currentLocality = locality.ifBlank { "Locality Area ($pincode)" },
            currentCity = city.ifBlank { "Custom Location" },
            showPincodePicker = false
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setCategoryFilter(category: NoticeCategory?) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = if (_uiState.value.selectedCategory == category) null else category
        )
    }

    fun toggleOnlyObjections() {
        _uiState.value = _uiState.value.copy(onlyObjectionsFilter = !_uiState.value.onlyObjectionsFilter)
    }

    fun openNoticeDetail(notice: CivicNotice?) {
        _uiState.value = _uiState.value.copy(activeNoticeDetail = notice)
    }

    fun setShowPincodePicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPincodePicker = show)
    }

    fun setShowObjectionDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showObjectionDialog = show)
    }

    fun voteConcern(noticeId: String) {
        viewModelScope.launch {
            repository.incrementConcernVote(noticeId)
            // Update active notice detail if opened
            _uiState.value.activeNoticeDetail?.let { current ->
                if (current.id == noticeId) {
                    _uiState.value = _uiState.value.copy(
                        activeNoticeDetail = current.copy(
                            concernVotesCount = current.concernVotesCount + 1,
                            hasUserFlagged = true
                        )
                    )
                }
            }
        }
    }

    fun triggerScrapeSimulation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScraping = true,
                scrapeMessage = "Scraping State RERA, Parivesh.nic.in & Municipal Tender Portals for pincode ${_uiState.value.currentPincode}..."
            )
            kotlinx.coroutines.delay(1200)
            val count = repository.triggerScrapeSimulation(_uiState.value.currentPincode)
            _uiState.value = _uiState.value.copy(
                isScraping = false,
                scrapeMessage = "✅ Aggregation complete! Found $count new official notice for ${_uiState.value.currentPincode}."
            )
        }
    }

    fun clearScrapeMessage() {
        _uiState.value = _uiState.value.copy(scrapeMessage = null)
    }

    fun simplifyNoticeWithAi(notice: CivicNotice) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiSimplifyingNotice = true)
            val simplified = repository.simplifyNoticeWithAi(notice)
            val updatedMap = _uiState.value.simplifiedNoticeMap.toMutableMap()
            updatedMap[notice.id] = simplified
            _uiState.value = _uiState.value.copy(
                isAiSimplifyingNotice = false,
                simplifiedNoticeMap = updatedMap
            )
        }
    }

    fun generateObjectionLetter(
        notice: CivicNotice,
        citizenName: String,
        societyName: String,
        selectedGrounds: List<String>,
        customNotes: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingAi = true, generatedLetter = null)
            val letter = repository.generateObjectionLetterWithAi(
                notice = notice,
                citizenName = citizenName.ifBlank { "Aggrieved Resident" },
                societyName = societyName.ifBlank { "Local Resident Welfare Association" },
                selectedGrounds = selectedGrounds,
                customNotes = customNotes
            )
            _uiState.value = _uiState.value.copy(
                isGeneratingAi = false,
                generatedLetter = letter
            )
        }
    }

    fun saveCurrentObjection(
        notice: CivicNotice,
        citizenName: String,
        societyName: String,
        selectedGrounds: List<String>,
        customNotes: String,
        letterBody: String
    ) {
        viewModelScope.launch {
            val draft = ObjectionDraft(
                noticeId = notice.id,
                projectTitle = notice.title,
                authorityName = notice.sourcePortal,
                referenceNumber = notice.referenceNumber,
                citizenName = citizenName,
                societyName = societyName,
                contactInfo = "",
                selectedGrounds = selectedGrounds,
                customNotes = customNotes,
                formalLetterBody = letterBody
            )
            repository.saveObjection(draft)
            _uiState.value = _uiState.value.copy(
                scrapeMessage = "✅ Formal objection draft saved to your vault!"
            )
        }
    }

    fun addSubscription(
        pincode: String,
        locality: String,
        radiusKm: Float,
        email: String,
        whatsapp: String,
        alertFrequency: String
    ) {
        viewModelScope.launch {
            val sub = PincodeSubscription(
                pincode = pincode,
                locality = locality,
                radiusKm = radiusKm,
                email = email,
                whatsappNumber = whatsapp,
                alertFrequency = alertFrequency
            )
            repository.addSubscription(sub)
            _uiState.value = _uiState.value.copy(
                scrapeMessage = "✅ Subscribed to 24-hr alerts for Pincode $pincode ($locality)!"
            )
        }
    }

    fun deleteSubscription(id: Int) {
        viewModelScope.launch {
            repository.deleteSubscription(id)
        }
    }

    fun updateRwaProfile(profile: RwaSocietyProfile) {
        _uiState.value = _uiState.value.copy(rwaProfile = profile)
    }

    fun generateRwaReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingRwa = true)
            val notices = allNotices.value
            val report = repository.generateRwaReportWithAi(_uiState.value.rwaProfile, notices)
            _uiState.value = _uiState.value.copy(
                isGeneratingRwa = false,
                rwaReport = report
            )
        }
    }
}
