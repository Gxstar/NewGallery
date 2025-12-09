package com.example.newgallery.ui.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel

/**
 * ViewModel to manage scroll state across screens
 */
class ScrollStateViewModel : ViewModel() {
    private var firstVisibleItemIndex: Int = 0
    private var firstVisibleItemScrollOffset: Int = 0
    
    /**
     * Save the current scroll state
     */
    fun saveScrollState(state: LazyGridState) {
        firstVisibleItemIndex = state.firstVisibleItemIndex
        firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffset
    }
    
    /**
     * Restore the scroll state
     */
    fun getSavedFirstVisibleItemIndex(): Int = firstVisibleItemIndex
    fun getSavedFirstVisibleItemScrollOffset(): Int = firstVisibleItemScrollOffset
    
    /**
     * Clear the saved scroll state
     */
    fun clearScrollState() {
        firstVisibleItemIndex = 0
        firstVisibleItemScrollOffset = 0
    }
}