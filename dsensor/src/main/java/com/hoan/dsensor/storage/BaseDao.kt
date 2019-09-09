package com.hoan.dsensor.storage

import androidx.room.Insert

internal interface BaseDao<T> {
    @Insert
    suspend fun insert(Obj: T): Long

    @Insert
    suspend fun insert(listObj: List<T>)
}