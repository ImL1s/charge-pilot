package com.chargepilot.core.capability

import com.chargepilot.core.model.CapabilityType
import com.chargepilot.core.model.ControlMethod
import com.chargepilot.core.model.Evidence
import com.chargepilot.core.model.Manufacturer
import com.chargepilot.core.model.Precondition
import com.google.common.truth.Truth.assertThat
import java.io.File
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
                  "availableMethods": ["SHIZUKU_RPC", "OFFICIAL_GUIDANCE"],
                  "settingsKey": { "namespace": "system", "key": "pass_through", "type": "int" },
                  "officialIntent": {
                    "action": "android.intent.action.MAIN",
                    "categories": ["android.intent.category.LAUNCHER"],
                    "component": "com.samsung.android.game.gamehome/.app.MainActivity",
                    "fallbackPath": "Gaming Hub -> More -> Game Booster -> Pause USB PD charging when gaming"
                  },
                  "guidanceSteps": [
                    "Open Gaming Hub.",
                    "Open More, then Game Booster settings."
                  ],
                  "preconditions": [
                    { "type": "PdChargerPresent" },
                    { "type": "BatteryLevelAbove", "percent": 20 },
                    { "type": "GameInForeground" }
                  ],
                  "evidence": "OFFICIAL_DOC"
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
            .containsExactly(
                ControlMethod.SHIZUKU_RPC,
                ControlMethod.OFFICIAL_GUIDANCE,
            )
            .inOrder()
        assertThat(descriptor.availableMethods).doesNotContain(ControlMethod.WRITE_SETTINGS_KEY)
        assertThat(descriptor.settingsKey?.namespace).isEqualTo("system")
        assertThat(descriptor.settingsKey?.key).isEqualTo("pass_through")
        assertThat(descriptor.officialIntent?.component)
            .isEqualTo("com.samsung.android.game.gamehome/.app.MainActivity")
        assertThat(descriptor.officialIntent?.categories)
            .containsExactly("android.intent.category.LAUNCHER")
        assertThat(descriptor.guidanceSteps)
            .containsExactly("Open Gaming Hub.", "Open More, then Game Booster settings.")
        assertThat(descriptor.evidence).isEqualTo(Evidence.OFFICIAL_DOC)

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

    @Test
    fun `bundled registry documents every known manufacturer and gates direct control on a key`() {
        val raw = registryAsset().readText()
        val snapshot = json.decodeFromString(CapabilityRegistrySnapshot.serializer(), raw)
        val manufacturers = snapshot.rules.map { it.matchers.manufacturer }.toSet()

        assertThat(manufacturers).containsAtLeast(
            Manufacturer.SAMSUNG.name,
            Manufacturer.GOOGLE.name,
            Manufacturer.ONEPLUS.name,
            Manufacturer.XIAOMI.name,
            Manufacturer.HONOR.name,
            Manufacturer.HUAWEI.name,
        )

        val directWithoutKey = snapshot.rules.flatMap { rule ->
            rule.capabilities.filter { descriptor ->
                descriptor.availableMethods.any { it.isDirectControl() } &&
                    descriptor.settingsKey == null
            }
        }
        assertThat(directWithoutKey).isEmpty()

        val samsungS2xPause = snapshot.rules
            .single { it.id == "samsung-galaxy-s2x-pause-pd" }
            .capabilities
            .single { it.type == CapabilityType.PAUSE_PD_DURING_GAMING }
        assertThat(samsungS2xPause.availableMethods)
            .containsExactly(
                ControlMethod.SHIZUKU_RPC,
                ControlMethod.OFFICIAL_GUIDANCE,
            )
            .inOrder()
        assertThat(samsungS2xPause.availableMethods).doesNotContain(ControlMethod.WRITE_SETTINGS_KEY)

        val samsungPauseGuides = snapshot.rules
            .filter { it.matchers.manufacturer == Manufacturer.SAMSUNG.name }
            .flatMap { it.capabilities }
            .filter { it.type == CapabilityType.PAUSE_PD_DURING_GAMING }
            .flatMap { it.guidanceSteps }
        assertThat(samsungPauseGuides).isNotEmpty()
        assertThat(samsungPauseGuides.joinToString(separator = "\n"))
            .contains("Game Booster settings")
    }

    private fun registryAsset(): File {
        val modulePath = File("src/main/assets/capabilities-v1.json")
        if (modulePath.exists()) return modulePath
        return File("core/capability/src/main/assets/capabilities-v1.json")
    }

    private fun ControlMethod.isDirectControl(): Boolean =
        this == ControlMethod.WRITE_SETTINGS_KEY ||
            this == ControlMethod.SHIZUKU_RPC ||
            this == ControlMethod.ROOT_SHELL
}
