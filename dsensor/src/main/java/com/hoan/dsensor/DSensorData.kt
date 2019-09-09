package com.hoan.dsensor

import androidx.collection.SparseArrayCompat
import kotlin.properties.Delegates

class DSensorData {
    var data: SparseArrayCompat<DSensorEvent> by Delegates.observable(SparseArrayCompat()) { _, oldValue, newValue ->
        onDataChanged?.invoke(oldValue, newValue)
    }

    var onDataChanged: ((SparseArrayCompat<DSensorEvent>, SparseArrayCompat<DSensorEvent>) -> Unit)? = null
}