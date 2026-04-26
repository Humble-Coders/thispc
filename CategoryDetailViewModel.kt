package com.humblesolutions.cutq.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.humblesolutions.cutq.model.SalonWithSubcategoryServices
import com.humblesolutions.cutq.model.ServiceSubcategory
import com.humblesolutions.cutq.navigation.AppRoute
import com.humblesolutions.cutq.repository.BackendSalonRepository
import com.humblesolutions.cutq.usecase.GetSalonsBySubcategoryUseCase
import com.humblesolutions.cutq.usecase.GetSubcategoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryDetailUiState(
    val categoryName: String = "",
    val isLoadingSubcategories: Boolean = false,
    val subcategories: List<ServiceSubcategory> = emptyList(),
    val selectedSubcategoryIndex: Int = 0,
    val isLoadingSalons: Boolean = false,
    val salonsWithServices: List<SalonWithSubcategoryServices> = emptyList(),
    val error: String? = null
)

class CategoryDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val categoryId: String   = savedStateHandle[AppRoute.CategoryDetail.ARG_CATEGORY_ID]   ?: ""
    private val categoryName: String = savedStateHandle[AppRoute.CategoryDetail.ARG_CATEGORY_NAME] ?: ""

    private val salonRepo                  = BackendSalonRepository()
    private val getSubcategoriesUseCase    = GetSubcategoriesUseCase(salonRepo)
    private val getSalonsBySubcategoryUseCase = GetSalonsBySubcategoryUseCase(salonRepo)

    private val _uiState = MutableStateFlow(CategoryDetailUiState(categoryName = categoryName))
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    init {
        loadSubcategories()
    }

    private fun loadSubcategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSubcategories = true, error = null) }
            try {
                val subcategories = getSubcategoriesUseCase.execute(categoryId)
                _uiState.update { it.copy(isLoadingSubcategories = false, subcategories = subcategories) }
                if (subcategories.isNotEmpty()) {
                    loadSalonsForSubcategory(0)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSubcategories = false, error = e.message) }
            }
        }
    }

    fun selectSubcategory(index: Int) {
        val current = _uiState.value
        if (index == current.selectedSubcategoryIndex) return
        _uiState.update { it.copy(selectedSubcategoryIndex = index) }
        loadSalonsForSubcategory(index)
    }

    private fun loadSalonsForSubcategory(index: Int) {
        val subcategories = _uiState.value.subcategories
        if (index !in subcategories.indices) return
        val subcategoryId = subcategories[index].id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSalons = true, salonsWithServices = emptyList()) }
            try {
                val salons = getSalonsBySubcategoryUseCase.execute(subcategoryId)
                _uiState.update { it.copy(isLoadingSalons = false, salonsWithServices = salons) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSalons = false, error = e.message) }
            }
        }
    }
}
