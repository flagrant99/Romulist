package io.github.flagrant99.romulist

import java.io.File

data class VolumeInfo(
    val directory: File?,
    val description: String,
    val isPrimary: Boolean,
    val isRemovable: Boolean
)
