package com.jcoder.linker.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.jcoder.linker.data.Link

@Dao
interface LinkDao {
    @Query("SELECT * FROM link")
    fun getAll(): List<Link>

    @Insert
    fun insertAll(vararg link: Link): Array<Long>

    @Delete
    fun delete(link: Link)
}