package com.dh.imagepick.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.dh.imagepick.preview.adapter.Repository

class ImageViewerViewModel : ViewModel() {
    val dataList = Repository().dataSourceFactory().toLiveData(PagedList.Config.Builder().setPageSize(1).build())
    val viewerUserInputEnabled = MutableLiveData<Boolean>()

    fun setViewerUserInputEnabled(enable: Boolean) {
        if (viewerUserInputEnabled.value != enable) viewerUserInputEnabled.value = enable
    }
}