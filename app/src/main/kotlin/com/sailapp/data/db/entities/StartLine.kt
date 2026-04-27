package com.sailapp.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "start_lines",
    foreignKeys = [ForeignKey(
        entity = RegattaSession::class,
        parentColumns = ["id"],
        childColumns = ["regattaSessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("regattaSessionId")]
)
data class StartLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val regattaSessionId: Long,
    val pinWaypointId: Long? = null,
    val committeeWaypointId: Long? = null
)
