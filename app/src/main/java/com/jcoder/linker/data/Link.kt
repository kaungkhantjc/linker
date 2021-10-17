package com.jcoder.linker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Link(
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}