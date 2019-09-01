package com.hoan.dsensor.storage

import androidx.room.Insert

interface BaseDao<T> {
    @Insert
    fun insert(Obj: T)
}