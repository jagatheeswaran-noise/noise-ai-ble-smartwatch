package com.dh.imagepick.preview.core

interface DataProvider {
    fun loadInitial(): List<Photo> = emptyList()
    fun loadAfter(key: Long, callback: (List<Photo>) -> Unit) {}
    fun loadBefore(key: Long, callback: (List<Photo>) -> Unit) {}
}