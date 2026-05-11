package com.chargepilot.core.capability

import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.Evidence
import com.chargepilot.core.model.Precondition
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class RegistrySerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `parses a samsung pause-PD rule into the typed registry shape`() {
        val raw = """
        {
          "schemaVersion": 1,
          "lastUpdated": "2026-05-11",
          "rules": [
            {
              "id": "samsung-galaxy-s2x-pause-pd",
              "matchers": {
                "manufacturer": "SAMSUNG",
                "modelRegex": "SM-S(91|92|93|94)[0-9].*",
                "minRomVersion": { "flavor": "ONE_UI", "version": "6.0" }
              },
              "capabilities": [
                {
                  "id": "samsung-galaxy-s2x-pause-pd::PAUSE_PD_DURING_GAMING",
                  "type": "PAUSE_PD_DURING_GAMING",
                  "availableMethods": ["OFFICIAL_GUIDANCE", "WRITE_SETTINGS_KEY"],
                  "settingsKey": { "namespace": "system", "key": "pass_through", "type": "int" },
                  "preconditions": [
                    { "type": "PdChargerPresent" },
                    { "type": "BatteryLevelAbove", "percent": 20 },
                    { "type": "GameInForeground" }
                  ],
                  "evidence": "PROJECT_VERIFIED"
                }
              ]
            }
          ]
        }
        """.trimIndent()

        val snapshot = json.decodeFromString(CapabilityRegistrySnapshot.serializer(), raw)
        assertThat(snapshot.schemaVersion).isEqualTo(1)
        assertThat(snapshot.rules).hasSize(1)

        val rule = snapshot.rules.single()
        assertThat(rule.id).isEqualTo("samsung-galaxy-s2x-pause-pd")
        assertThat(rule.matchers.manufacturer).isEqualTo("SAMSUNG")
        assertThat(rule.matchers.minRomVersion?.flavor).isEqualTo("ONE_UI")

        val descriptor = rule.capabilities.single()
        assertThat(descriptor.id).isEqualTo("samsung-galaxy-s2x-pause-pd::PAUSE_PD_DURING_GAMING")
        assertThat(descriptor.type).isEqualTo(CapabilityType.PAUSE_PD_DURING_GAMING)
        assertThat(descriptor.availableMethods)
            .containsExactly(ControlMethod.OFFICIAL_GUIDANCE, ControlMethod.WRITE_SETTINGS_KEY)
            .inOrder()
        assertThat(descriptor.settingsKey?.key).isEqualTo("pass_through")
        assertThat(descriptor.evidence).isEqualTo(Evidence.PROJECT_VERIFIED)

        assertThat(descriptor.preconditions).hasSize(3)
        assertThat(descriptor.preconditions).containsExactly(
            Precondition.PdChargerPresent,
            Precondition.BatteryLevelAbove(percent = 20),
            Precondition.GameInForeground(),
        )
    }

    @Test
    fun `negative-list rule with empty availableMethods round-trips correctly`() {
        val raw = """
        {
          "schemaVersion": 1,
          "lastUpdated": "2026-05-11",
          "rules": [
            {
              "id": "negative",
              "matchers": { "manufacturer": "XIAOMI", "modelRegex": "(2503CXC0).*" },
              "capabilities": [
                {
                  "id": "negative::BYPASS_CHARGING",
                  "type": "BYPASS_CHARGING",
                  "availableMethods": [],
                  "evidence": "OFFICIAL_DOC"
                }
              ]
            }
          ]
        }
        """.trimIndent()

        val snapshot = json.decodeFromString(CapabilityRegistrySnapshot.serializer(), raw)
        val descriptor = snapshot.rules.single().capabilities.single()
        assertThat(descriptor.availableMethods).isEmpty()
        assertThat(descriptor.evidence).isEqualTo(Evidence.OFFICIAL_DOC)
    }

    @Test
    fun `rule without minRomVersion deserializes minRomVersion as null`() {
        val raw = """
        {
          "schemaVersion": 1,
          "lastUpdated": "2026-05-11",
          "rules": [
            {
              "id": "all-pixels",
              "matchers": { "manufacturer": "GOOGLE" },
              "capabilities": []
            }
          ]
        }
        """.trimIndent()

        val snapshot = json.decodeFromString(CapabilityRegistrySnapshot.serializer(), raw)
        assertThat(snapshot.rules.single().matchers.minRomVersion).isNull()
    }
}
