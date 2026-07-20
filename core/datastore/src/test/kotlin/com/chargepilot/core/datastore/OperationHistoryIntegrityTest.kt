package com.chargepilot.core.datastore

import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.OperationRecord
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * Pure JSON integrity tests for the history append/decode contract.
 * Full DataStore integration requires Android; decode/backup policy is unit-tested here.
 */
class OperationHistoryIntegrityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `valid records decode`() {
        val records = listOf(
            OperationRecord(
                id = "1",
                capability = CapabilityType.PAUSE_PD_DURING_GAMING,
                method = ControlMethod.SHIZUKU_RPC,
                before = "Inactive",
                after = "Active",
                success = true,
                timestampMs = 1L,
                capabilityId = "rule::PAUSE_PD_DURING_GAMING",
                rawBefore = "0",
                rawAfter = "1",
            ),
        )
        val encoded = json.encodeToString(records)
        val decoded = json.decodeFromString<List<OperationRecord>>(encoded)
        assertThat(decoded).hasSize(1)
        assertThat(decoded[0].rawBefore).isEqualTo("0")
        assertThat(decoded[0].capabilityId).isEqualTo("rule::PAUSE_PD_DURING_GAMING")
    }

    @Test
    fun `corrupt json fails decodeOrNull style handling`() {
        val raw = "{not-valid-json"
        val decoded = runCatching {
            json.decodeFromString<List<OperationRecord>>(raw)
        }.getOrNull()
        assertThat(decoded).isNull()
    }

    @Test
    fun `old records without new fields still deserialize`() {
        val legacy = """
            [{"id":"x","capability":"PAUSE_PD_DURING_GAMING","method":"OFFICIAL_GUIDANCE",
              "before":null,"after":"Navigated","success":true,"timestampMs":1}]
        """.trimIndent()
        val decoded = json.decodeFromString<List<OperationRecord>>(legacy)
        assertThat(decoded).hasSize(1)
        assertThat(decoded[0].capabilityId).isNull()
        assertThat(decoded[0].reverted).isFalse()
    }
}
