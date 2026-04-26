package com.humblesolutions.cutq.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.cutq.model.ExploreItem
import com.humblesolutions.cutq.model.HeaderImage
import com.humblesolutions.cutq.model.Salon
import com.humblesolutions.cutq.model.UserLocation
import com.humblesolutions.cutq.repository.BackendAppConfigRepository
import com.humblesolutions.cutq.repository.BackendSalonRepository
import com.humblesolutions.cutq.usecase.ComputeSalonDistancesUseCase
import com.humblesolutions.cutq.usecase.GetAllSalonsUseCase
import com.humblesolutions.cutq.usecase.GetExploreItemsUseCase
import com.humblesolutions.cutq.usecase.GetFeaturedSalonsUseCase
import com.humblesolutions.cutq.usecase.GetHeaderImagesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SalonsFilter { ALL, OPEN_NOW, TOP_RATED, PREMIUM, MEN, WOMEN, UNISEX }

data class HomeUiState(
    val isLoading: Boolean = false,
    val isSalonsLoading: Boolean = false,
    val isLocationPending: Boolean = true,   // true until first city is chosen
    val featuredSalons: List<Salon> = emptyList(),
    val filteredFeaturedSalons: List<Salon> = emptyList(),
    val displayedSalons: List<Salon> = emptyList(),
    val headerImages: List<HeaderImage> = emptyList(),
    val exploreItems: List<ExploreItem> = emptyList(),
    val selectedCategory: String? = null,
    val error: String? = null,
    // Salons tab
    val allSalons: List<Salon> = emptyList(),
    val salonsFilter: SalonsFilter = SalonsFilter.ALL,
    val salonsSearchQuery: String = "",
    val salonsVisibleCount: Int = 10,
    val filteredSalonsCount: Int = 0,
    val visibleSalons: List<Salon> = emptyList(),
    val userLocation: UserLocation? = null,
    val locationCity: String = "",
)

private fun HomeUiState.withFiltersApplied(): HomeUiState {
    // City filtering is done at the backend — apply only type/search filters here
    var list = allSalons
    when (salonsFilter) {
        SalonsFilter.ALL       -> { /* no filter */ }
        SalonsFilter.OPEN_NOW  -> list = list.filter { it.isOpen }
        SalonsFilter.TOP_RATED -> list = list.filter { it.avgRating >= 4.5 }
        SalonsFilter.PREMIUM   -> list = list.filter { it.tier.equals("premium", ignoreCase = true) }
        SalonsFilter.MEN       -> list = list.filter { it.targetedGender.equals("male", ignoreCase = true) || it.targetedGender.equals("unisex", ignoreCase = true) }
        SalonsFilter.WOMEN     -> list = list.filter { it.targetedGender.equals("female", ignoreCase = true) || it.targetedGender.equals("unisex", ignoreCase = true) }
        SalonsFilter.UNISEX    -> list = list.filter { it.targetedGender.equals("unisex", ignoreCase = true) }
    }
    if (salonsSearchQuery.isNotBlank()) {
        list = list.filter {
            it.name.contains(salonsSearchQuery, ignoreCase = true) ||
            it.category.contains(salonsSearchQuery, ignoreCase = true)
        }
    }
    return copy(
        filteredFeaturedSalons = featuredSalons, // backend already filtered by city
        filteredSalonsCount    = list.size,
        visibleSalons          = list.take(salonsVisibleCount)
    )
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val salonRepo     = BackendSalonRepository()
    private val appConfigRepo = BackendAppConfigRepository()

    private val getFeaturedSalonsUseCase     = GetFeaturedSalonsUseCase(salonRepo)
    private val getAllSalonsUseCase           = GetAllSalonsUseCase(salonRepo)
    private val getHeaderImagesUseCase       = GetHeaderImagesUseCase(appConfigRepo)
    private val getExploreItemsUseCase       = GetExploreItemsUseCase(appConfigRepo)
    private val computeSalonDistancesUseCase = ComputeSalonDistancesUseCase()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Fetch city-agnostic content immediately; salons wait for city selection
        viewModelScope.launch {
            val headersDeferred = async { runCatching { getHeaderImagesUseCase.execute() }.getOrDefault(emptyList()) }
            val exploreDeferred = async { runCatching { getExploreItemsUseCase.execute() }.getOrDefault(emptyList()) }
            _uiState.update {
                it.copy(
                    headerImages = headersDeferred.await(),
                    exploreItems = exploreDeferred.await()
                )
            }
        }
    }

    fun loadDataForCity(location: UserLocation) {
        if (location.cityName.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading         = true,
                    isSalonsLoading   = true,
                    isLocationPending = false,
                    locationCity      = location.cityName,
                    error             = null
                )
            }
            val city = location.cityName
            val featuredDeferred = async { runCatching { getFeaturedSalonsUseCase.execute(city) }.getOrDefault(emptyList()) }
            val allDeferred      = async { runCatching { getAllSalonsUseCase.execute(city) }.getOrDefault(emptyList()) }
            val featured  = featuredDeferred.await()
            val allSalons = allDeferred.await()
            val featuredWithDist = computeSalonDistancesUseCase.execute(featured, location)
            val allWithDist      = computeSalonDistancesUseCase.execute(allSalons, location)
            _uiState.update {
                it.copy(
                    isLoading       = false,
                    isSalonsLoading = false,
                    featuredSalons  = featuredWithDist,
                    displayedSalons = featuredWithDist,
                    allSalons       = allWithDist,
                    userLocation    = location,
                    salonsVisibleCount = 10,
                ).withFiltersApplied()
            }
        }
    }

    fun onCategorySelected(categoryId: String) {
        val current = _uiState.value
        if (categoryId == current.selectedCategory) {
            _uiState.update { it.copy(selectedCategory = null, displayedSalons = it.featuredSalons) }
            return
        }
        _uiState.update { it.copy(selectedCategory = categoryId) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val salons = salonRepo.getSalonsByCategory(categoryId)
                _uiState.update { it.copy(isLoading = false, displayedSalons = salons) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, displayedSalons = current.featuredSalons) }
            }
        }
    }

    fun onSalonsSearchChanged(query: String) {
        _uiState.update { it.copy(salonsSearchQuery = query, salonsVisibleCount = 10).withFiltersApplied() }
    }

    fun onSalonsFilterChanged(filter: SalonsFilter) {
        _uiState.update { it.copy(salonsFilter = filter, salonsVisibleCount = 10).withFiltersApplied() }
    }

    fun loadMoreSalons() {
        _uiState.update { it.copy(salonsVisibleCount = it.salonsVisibleCount + 10).withFiltersApplied() }
    }

    // Called when GPS precision improves within the same city — updates distances only, no re-fetch
    fun updateLocation(location: UserLocation?) {
        if (location == null || _uiState.value.isLocationPending) return
        _uiState.update { state ->
            val updatedAll      = computeSalonDistancesUseCase.execute(state.allSalons, location)
            val updatedFeatured = computeSalonDistancesUseCase.execute(state.featuredSalons, location)
            state.copy(
                userLocation    = location,
                allSalons       = updatedAll,
                featuredSalons  = updatedFeatured,
                displayedSalons = if (state.selectedCategory == null) updatedFeatured else state.displayedSalons
            ).withFiltersApplied()
        }
    }
}
