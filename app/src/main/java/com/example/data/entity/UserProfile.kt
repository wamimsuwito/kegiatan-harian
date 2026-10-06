package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Long = 1L,
    val name: String = "",
    val position: String = "",
    val signaturePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
