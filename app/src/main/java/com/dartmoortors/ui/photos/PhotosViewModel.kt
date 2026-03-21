package com.dartmoortors.ui.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartmoortors.data.model.Photo
import com.dartmoortors.data.model.PhotoScanResult
import com.dartmoortors.data.repository.PhotoRepository
import com.dartmoortors.data.repository.ScanProgress
import com.dartmoortors.service.PhotoService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Scan flow states for the Photos tab.
 */
sealed class ScanState {
    /** Initial state - ready to scan */
    object Ready : ScanState()
    
    /** Currently scanning */
    data class Scanning(val progress: ScanProgress) : ScanState()
    
    /** Scan complete, reviewing results */
    data class Reviewing(
        val results: List<PhotoScanResult>,
        val currentIndex: Int,
        val currentPhotoIndex: Int,
        val photosAdded: Int
    ) : ScanState() {
        val currentResult: PhotoScanResult? get() = results.getOrNull(currentIndex)
        val currentPhoto: Photo? get() = currentResult?.photos?.getOrNull(currentPhotoIndex)
        val isComplete: Boolean get() = currentIndex >= results.size
        val totalResults: Int get() = results.size
    }
    
    /** Scan and review complete */
    data class Complete(val photosAdded: Int) : ScanState()
}

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val photoService: PhotoService
) : ViewModel() {
    
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Ready)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    private val _hasPhotoPermission = MutableStateFlow(false)
    val hasPhotoPermission: StateFlow<Boolean> = _hasPhotoPermission.asStateFlow()
    
    init {
        checkPhotoPermission()
    }
    
    fun checkPhotoPermission() {
        _hasPhotoPermission.value = photoService.hasPhotoPermission()
    }
    
    fun getRequiredPhotoPermission(): String {
        return photoService.getRequiredPhotoPermission()
    }
    
    /**
     * Start scanning the photo library for matches.
     */
    fun startScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning(ScanProgress(0, 0, 0))
            
            // Collect progress updates
            val progressJob = launch {
                photoRepository.scanProgress.collect { progress ->
                    progress?.let {
                        _scanState.value = ScanState.Scanning(it)
                    }
                }
            }
            
            // Run the scan
            val results = photoRepository.scanForTorMatches()
            
            progressJob.cancel()
            photoRepository.clearScanProgress()
            
            if (results.isEmpty()) {
                _scanState.value = ScanState.Complete(0)
            } else {
                _scanState.value = ScanState.Reviewing(
                    results = results,
                    currentIndex = 0,
                    currentPhotoIndex = 0,
                    photosAdded = 0
                )
            }
        }
    }
    
    /**
     * Add the current photo to the current tor.
     */
    fun addCurrentPhoto() {
        val state = _scanState.value as? ScanState.Reviewing ?: return
        val result = state.currentResult ?: return
        val photo = state.currentPhoto ?: return
        
        viewModelScope.launch {
            photoRepository.associatePhotoWithTor(result.torId, photo)
            
            // Move to next result
            moveToNextResult(state.photosAdded + 1)
        }
    }
    
    /**
     * Skip the current tor without adding a photo.
     */
    fun skipCurrentTor() {
        val state = _scanState.value as? ScanState.Reviewing ?: return
        moveToNextResult(state.photosAdded)
    }
    
    /**
     * View the next photo for the current tor.
     */
    fun nextPhoto() {
        val state = _scanState.value as? ScanState.Reviewing ?: return
        val result = state.currentResult ?: return
        
        val nextPhotoIndex = state.currentPhotoIndex + 1
        if (nextPhotoIndex < result.photos.size) {
            _scanState.value = state.copy(currentPhotoIndex = nextPhotoIndex)
        }
    }
    
    /**
     * View the previous photo for the current tor.
     */
    fun previousPhoto() {
        val state = _scanState.value as? ScanState.Reviewing ?: return
        
        val prevPhotoIndex = state.currentPhotoIndex - 1
        if (prevPhotoIndex >= 0) {
            _scanState.value = state.copy(currentPhotoIndex = prevPhotoIndex)
        }
    }
    
    private fun moveToNextResult(photosAdded: Int) {
        val state = _scanState.value as? ScanState.Reviewing ?: return
        
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.results.size) {
            _scanState.value = ScanState.Complete(photosAdded)
        } else {
            _scanState.value = state.copy(
                currentIndex = nextIndex,
                currentPhotoIndex = 0,
                photosAdded = photosAdded
            )
        }
    }
    
    /**
     * Reset to ready state.
     */
    fun resetToReady() {
        _scanState.value = ScanState.Ready
    }
    
    /**
     * Cancel the current scan/review.
     */
    fun cancel() {
        photoRepository.clearScanProgress()
        _scanState.value = ScanState.Ready
    }
}
