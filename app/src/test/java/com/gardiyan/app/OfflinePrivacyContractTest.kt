package com.gardiyan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class OfflinePrivacyContractTest {

    @Test
    fun `network and advertising permissions are explicitly removed`() {
        val manifest = File("src/main/AndroidManifest.xml")
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifest)

        val permissions = document.getElementsByTagName("uses-permission")
        val removalRules = buildMap {
            for (index in 0 until permissions.length) {
                val element = permissions.item(index)
                val name = element.attributes
                    .getNamedItemNS(ANDROID_NAMESPACE, "name")
                    ?.nodeValue
                    ?: continue
                val action = element.attributes
                    .getNamedItemNS(TOOLS_NAMESPACE, "node")
                    ?.nodeValue
                put(name, action)
            }
        }

        OFFLINE_PERMISSIONS.forEach { permission ->
            assertEquals("$permission must be removed by manifest merger", "remove", removalRules[permission])
        }
    }

    @Test
    fun `advertising SDKs and runtime switches are absent from build`() {
        val buildScript = File("build.gradle.kts").readText()

        listOf(
            "gma.nextgen",
            "google.ump",
            "ADS_ENABLED",
            "ADMOB_APP_ID"
        ).forEach { forbiddenToken ->
            assertFalse("Build must not contain $forbiddenToken", buildScript.contains(forbiddenToken))
        }
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val TOOLS_NAMESPACE = "http://schemas.android.com/tools"

        val OFFLINE_PERMISSIONS = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "com.google.android.gms.permission.AD_ID",
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "android.permission.ACCESS_ADSERVICES_TOPICS"
        )
    }
}
