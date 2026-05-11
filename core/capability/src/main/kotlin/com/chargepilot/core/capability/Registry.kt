package com.chargepilot.core.capability

import com.chargepilot.core.model.CapabilityDescriptor
import kotlinx.serialization.Serializable

@Serializable
data class CapabilityRegistrySnapshot(
    val schemaVersion: Int,
    val lastUpdated: String,
    val rules: List<CapabilityRule>,
)

@Serializable
data class CapabilityRule(
    val id: String,
    val matchers: Matchers,
    val capabilities: List<CapabilityDescriptor>,
)

@Serializable
data class Matchers(
    val manufacturer: String,
    val modelRegex: String? = null,
    val minRomVersion: RomVersionRequirement? = null,
)

@Serializable
data class RomVersionRequirement(
    val flavor: String,
    val version: String,
)
