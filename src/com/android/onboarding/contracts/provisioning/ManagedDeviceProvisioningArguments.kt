package com.android.onboarding.contracts.provisioning

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.PersistableBundle
import android.webkit.URLUtil
import com.android.onboarding.contracts.IntentSerializer
import com.android.onboarding.contracts.provisioning.DeviceAdminChecksum.PackageChecksum
import com.android.onboarding.contracts.provisioning.DeviceAdminChecksum.SignatureChecksum
import com.android.onboarding.contracts.provisioning.WifiSecurityType.EAP
import com.android.onboarding.contracts.provisioning.WifiSecurityType.NONE
import com.android.onboarding.contracts.provisioning.WifiSecurityType.WEP
import com.android.onboarding.contracts.provisioning.WifiSecurityType.WPA
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

/** The Checksum used to validate a Device Admin app. */
sealed class DeviceAdminChecksum {

  abstract val checksum: String

  /** A base64 encoded SHA-256 hash of the Device Admin APK. */
  data class PackageChecksum(override val checksum: String) : DeviceAdminChecksum()

  /** A base64 encoded SHA-256 checksum of any signature applied to the Device Admin APK. */
  data class SignatureChecksum(override val checksum: String) : DeviceAdminChecksum()
}

/** Arguments related to the device admin app to be provisioned. */
data class DeviceAdmin(
  /**
   * The component name of the [android.app.admin.DeviceAdminReceiver] subclass which will be set as
   * active.
   */
  val componentName: ComponentName,

  /**
   * Details related to the download of the Device Admin APK.
   *
   * <p>If this is not provided, then it will be assumed that the APK is already installed on the
   * device
   */
  val download: DeviceAdminDownload? = null,
)

/**
 * Information used to control the download of a Device Admin APK.
 *
 * @param location The URL to download the APK from
 * @param cookieHeader An optional cookie header which will be used in the download request
 * @param checksum The checksum used to validate the device admin APK
 */
data class DeviceAdminDownload(
  val location: String,
  val cookieHeader: String? = null,
  val checksum: DeviceAdminChecksum
)

/** The security of a Wifi Network. */
sealed class WifiSecurityType {
  /** An unsecured network. */
  object NONE : WifiSecurityType()

  /**
   * A WPA-secured network
   *
   * @param password Plaintext representation of the password
   */
  data class WPA(val password: String) : WifiSecurityType()

  /**
   * A WEP-secured network
   *
   * @param password Plaintext representation of the password
   */
  data class WEP(val password: String) : WifiSecurityType()

  /**
   * A EAP-secured network
   *
   * @param password Plaintext representation of the password
   */
  data class EAP(val password: String) : WifiSecurityType()
}

/**
 * Arguments for proxy settings when specifying wifi
 *
 * @param host The hostname of the proxy
 * @param port The port of the proxy
 * @param bypassHosts A list of hostnames for whom the proxy will be bypassed
 * @param autoConfigUrl TODO: What does this do?
 */
data class ProxyArguments(
  val host: String,
  val port: Int,
  val bypassHosts: List<String> = listOf(),
  val autoConfigUrl: String?
)

/** Arguments for specifying wifi connectivity */
data class WifiArguments(
  /** SSID of Wifi Network to be used during provisioning. */
  val ssid: String,

  /** {@code true} if the SSID represents a hidden network. */
  val hidden: Boolean = false,

  /** The security type of the network. */
  val security: WifiSecurityType,

  /** The proxy to use with this connection, if any. */
  val proxy: ProxyArguments? = null
)

enum class ProvisioningTrigger(val intVal: Int) {
  UNSPECIFIED(0),
  CLOUD_ENROLLMENT(1),
  QR_CODE(2),
  PERSISTENT_DEVICE_OWNER(3),
  MANAGED_ACCOUNT(4),
  NFC(5)
}

enum class ProvisioningMode(val intVal: Int) {
  FULLY_MANAGED_DEVICE(1),
  MANAGED_PROFILE(2);

  companion object {
    fun fromIntVal(intVal: Int): ProvisioningMode =
      when (intVal) {
        FULLY_MANAGED_DEVICE.intVal -> FULLY_MANAGED_DEVICE
        MANAGED_PROFILE.intVal -> MANAGED_PROFILE
        else -> throw IllegalArgumentException("Unknown provisioning mode: $intVal")
      }
  }
}

/** Arguments for provisioning a managed device. */
data class ManagedDeviceProvisioningArguments(

  /** The Device Admin to be provisioned. */
  val deviceAdmin: DeviceAdmin,

  /**
   * If not set to {@code true}, will require that a connection is made before proceeding with
   * enterprise provisioning.
   */
  val allowOfflineProvisioning: Boolean = false,
  /** Indicates if mobile data should be used when wifi is unavailable. */
  val useMobileData: Boolean = false,

  /** Configures wifi connectivity. */
  val wifi: WifiArguments? = null,
  /**
   * The time which will be set as the device's local time upon provisioning complete.
   *
   * <p>If the provisioning request results in any mode other than Device Owner, this argument will
   * be ignored.
   */
  val localTime: Instant? = null,

  /**
   * The time zone which will be set as the device's time zone upon provisioning complete.
   *
   * <p>If the provisioning request results in any mode other than Device Owner, this argument will
   * be ignored.
   */
  val timeZone: TimeZone? = null,

  /**
   * The locale which will be set as the device's locale upon provisioning complete.
   *
   * <p>If the provisioning request results in any mode other than Device Owner, this argument will
   * be ignored.
   */
  val locale: Locale? = null,

  /** The name of the organization under management. */
  val organizationName: String? = null,

  /**
   * The modes which are allowed to be provisioned into.
   *
   * <p>Defaults to all modes
   */
  val allowedProvisioningModes: List<ProvisioningMode> = ProvisioningMode.values().asList(),

  /**
   * Indicates if system apps should be left enabled after provisioning.
   *
   * <p>By default (if left to false), system apps will be disabled.
   */
  val leaveAllSystemAppsEnabled: Boolean = false,

  /** Indicates if device encryption should be skipped. */
  val skipEncryption: Boolean = false,

  /** The action which triggered enterprise provisioning. */
  val provisioningTrigger: ProvisioningTrigger = ProvisioningTrigger.UNSPECIFIED,

  /**
   * A URL to give more information to the user during provisioning.
   *
   * <p>This URL must use HTTPS.
   */
  val supportUrl: String? = null,

  /** Arbitrary extras which will be passed to the Device Admin app upon provisioning. */
  val adminExtras: PersistableBundle? = null,

  /** Indicates if the provisioning education screens should be skipped. */
  val skipEducationScreens: Boolean = false
) {
  init {
    if (supportUrl != null && !URLUtil.isHttpsUrl(supportUrl)) {
      throw IllegalArgumentException(
        "If supplied, supportUrl must be a HTTPS URL. Was: $supportUrl"
      )
    }
    if (allowedProvisioningModes.isEmpty()) {
      throw IllegalArgumentException("There must be at least one allowed provisioning mode")
    }
  }
}

/** Argument Parser for [ManagedDeviceProvisioningArguments]. */
class ManagedDeviceProvisioningArgumentsSerializer :
  IntentSerializer<ManagedDeviceProvisioningArguments> {
  override fun write(intent: Intent, value: ManagedDeviceProvisioningArguments) {
    encodeDeviceAdmin(intent, value.deviceAdmin)
    intent.putExtra(EXTRAS.EXTRA_PROVISIONING_ALLOW_OFFLINE, value.allowOfflineProvisioning)
    intent.putExtra(EXTRAS.EXTRA_PROVISIONING_USE_MOBILE_DATA, value.useMobileData)
    if (value.wifi != null) {
      encodeWifi(intent, value.wifi)
    }
    intent.putExtra(
      DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED,
      value.leaveAllSystemAppsEnabled
    )
    intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, value.skipEncryption)
    intent.putExtra(EXTRAS.EXTRA_PROVISIONING_TRIGGER, value.provisioningTrigger.intVal)
    if (value.localTime != null) {
      intent.putExtra(EXTRAS.EXTRA_PROVISIONING_LOCAL_TIME, value.localTime.toEpochMilli())
    }
    if (value.timeZone != null) {
      intent.putExtra(EXTRAS.EXTRA_PROVISIONING_TIME_ZONE, value.timeZone.toZoneId().id)
    }
    if (value.locale != null) {
      intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LOCALE, value.locale.toLanguageTag())
    }
    if (value.organizationName != null) {
      intent.putExtra(EXTRAS.EXTRA_PROVISIONING_ORGANIZATION_NAME, value.organizationName)
    }
    if (value.supportUrl != null) {
      intent.putExtra(EXTRAS.EXTRA_PROVISIONING_SUPPORT_URL, value.supportUrl)
    }
    if (value.adminExtras != null) {
      intent.putExtra(EXTRAS.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, value.adminExtras)
    }
    intent.putIntegerArrayListExtra(
      EXTRAS.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES,
      ArrayList(value.allowedProvisioningModes.map { it.intVal }.toList())
    )
    intent.putExtra(EXTRAS.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, value.skipEducationScreens)
  }

  private fun encodeWifi(intent: Intent, wifi: WifiArguments) {
    intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID, wifi.ssid)
    intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_HIDDEN, wifi.hidden)
    encodeWifiSecurity(intent, wifi.security)
    if (wifi.proxy != null) {
      encodeWifiProxy(intent, wifi.proxy)
    }
  }

  private fun encodeWifiProxy(intent: Intent, proxy: ProxyArguments) {
    intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_HOST, proxy.host)
    intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_PORT, proxy.port)
    if (proxy.bypassHosts.isNotEmpty()) {
      intent.putExtra(
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_BYPASS,
        proxy.bypassHosts.joinToString(",")
      )
    }
    if (proxy.autoConfigUrl != null) {
      intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PAC_URL, proxy.autoConfigUrl)
    }
  }

  private fun encodeWifiSecurity(intent: Intent, security: WifiSecurityType) {
    when (security) {
      is WPA -> {
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SECURITY_TYPE, "WPA")
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PASSWORD, security.password)
      }
      is WEP -> {
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SECURITY_TYPE, "WEP")
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PASSWORD, security.password)
      }
      is EAP -> {
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SECURITY_TYPE, "EAP")
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PASSWORD, security.password)
      }
      is NONE -> intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SECURITY_TYPE, "NONE")
    }
  }

  private fun encodeDeviceAdmin(intent: Intent, deviceAdmin: DeviceAdmin) {
    intent.putExtra(
      DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
      deviceAdmin.componentName
    )

    if (deviceAdmin.download != null) {
      encodeDeviceAdminDownload(intent, deviceAdmin.download)
    }
  }

  private fun encodeDeviceAdminDownload(intent: Intent, download: DeviceAdminDownload) {
    intent.putExtra(
      DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION,
      download.location
    )
    if (download.cookieHeader != null) {
      intent.putExtra(
        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER,
        download.cookieHeader
      )
    }
    when (download.checksum) {
      is PackageChecksum ->
        intent.putExtra(
          DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM,
          download.checksum.checksum
        )
      is SignatureChecksum ->
        intent.putExtra(
          DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM,
          download.checksum.checksum
        )
    }
  }

  override fun read(intent: Intent) =
    ManagedDeviceProvisioningArguments(
      deviceAdmin = extractDeviceAdmin(intent),
      allowOfflineProvisioning =
        intent.getBooleanExtra(EXTRAS.EXTRA_PROVISIONING_ALLOW_OFFLINE, false),
      useMobileData = intent.getBooleanExtra(EXTRAS.EXTRA_PROVISIONING_USE_MOBILE_DATA, false),
      wifi = extractWifi(intent),
      leaveAllSystemAppsEnabled =
        intent.getBooleanExtra(
          DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED,
          false
        ),
      skipEncryption =
        intent.getBooleanExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, false),
      provisioningTrigger = extractProvisioningTrigger(intent),
      localTime = extractLocalTime(intent),
      timeZone = extractTimeZone(intent),
      locale = extractLocale(intent),
      organizationName = intent.getStringExtra(EXTRAS.EXTRA_PROVISIONING_ORGANIZATION_NAME),
      supportUrl = intent.getStringExtra(EXTRAS.EXTRA_PROVISIONING_SUPPORT_URL),
      allowedProvisioningModes =
        (intent
          .getIntegerArrayListExtra(EXTRAS.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES)
          ?.map { ProvisioningMode.fromIntVal(it) })
          ?: ProvisioningMode.values().asList(),
      adminExtras =
        intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
          as PersistableBundle?,
      skipEducationScreens =
        intent.getBooleanExtra(EXTRAS.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, false),
    )

  private fun extractProvisioningTrigger(intent: Intent): ProvisioningTrigger {
    return when (val c = intent.getIntExtra(EXTRAS.EXTRA_PROVISIONING_TRIGGER, 0)) {
      0 -> ProvisioningTrigger.UNSPECIFIED
      1 -> ProvisioningTrigger.CLOUD_ENROLLMENT
      2 -> ProvisioningTrigger.QR_CODE
      3 -> ProvisioningTrigger.PERSISTENT_DEVICE_OWNER
      4 -> ProvisioningTrigger.MANAGED_ACCOUNT
      5 -> ProvisioningTrigger.NFC
      else -> throw IllegalArgumentException("Unknown value for EXTRA_PROVISIONING_TRIGGER: $c")
    }
  }

  private fun extractLocale(intent: Intent): Locale? =
    if (intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_LOCALE)) {
      Locale.Builder()
        // I don't know why this replacement is needed - it came from the existing
        // ManagedProvisioning code
        .setLanguageTag(
          intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_LOCALE)!!.replace("_", "-")
        )
        .build()
    } else {
      null
    }

  private fun extractTimeZone(intent: Intent): TimeZone? =
    if (intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_TIME_ZONE)) {
      TimeZone.getTimeZone(intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_TIME_ZONE))
    } else {
      null
    }

  private fun extractLocalTime(intent: Intent): Instant? =
    if (intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_LOCAL_TIME)) {
      Instant.ofEpochMilli(
        intent.getLongExtra(DevicePolicyManager.EXTRA_PROVISIONING_LOCAL_TIME, 0)
      ) // Will never be 0 due to previous check
    } else {
      null
    }

  private fun extractWifi(intent: Intent): WifiArguments? =
    if (intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID)) {
      WifiArguments(
        ssid =
          intent.getStringExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID
          )!!, // Must exist due to previous check
        hidden = intent.getBooleanExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_HIDDEN, false),
        security = extractWifiSecurity(intent),
        proxy = extractWifiProxy(intent)
      )
    } else {
      null
    }

  private fun extractWifiProxy(intent: Intent): ProxyArguments? =
    if (intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_HOST)) {
      if (!intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_PORT)) {
        throw IllegalArgumentException(
          "Cannot specify EXTRA_PROVISIONING_WIFI_PROXY_HOST without EXTRA_PROVISIONING_WIFI_PROXY_PORT"
        )
      }

      ProxyArguments(
        host =
          intent.getStringExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_HOST
          )!!, // Must exist due to previous check
        port =
          intent.getIntExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_PORT,
            0
          ), // Will never be 0 due to previous check
        bypassHosts = extractProxyBypassHosts(intent),
        autoConfigUrl = intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PAC_URL)
      )
    } else {
      null
    }

  private fun extractProxyBypassHosts(intent: Intent): List<String> =
    (intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_BYPASS) ?: "").split(
      ","
    )

  private fun extractWifiSecurity(intent: Intent): WifiSecurityType =
    when (
      val type = intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SECURITY_TYPE)
    ) {
      null -> NONE
      "NONE" -> NONE
      "WEP" -> WEP(password = extractWifiPassword(intent))
      "WPA" -> WPA(password = extractWifiPassword(intent))
      "EAP" -> EAP(password = extractWifiPassword(intent))
      else ->
        throw IllegalArgumentException(
          "Unknown value for EXTRA_PROVISIONING_WIFI_SECURITY_TYPE: $type"
        )
    }

  private fun extractWifiPassword(intent: Intent): String =
    intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PASSWORD)
      ?: throw IllegalArgumentException("EXTRA_PROVISIONING_WIFI_PASSWORD must be specified")

  private fun extractDeviceAdmin(intent: Intent): DeviceAdmin =
    DeviceAdmin(
      componentName = extractDeviceAdminComponentName(intent),
      download = extractDeviceAdminDownload(intent)
    )

  private fun extractDeviceAdminDownload(intent: Intent): DeviceAdminDownload? =
    if (
      intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION)
    ) {
      DeviceAdminDownload(
        location =
          intent.getStringExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION
          )!!, // Must exist due to previous check
        cookieHeader =
          intent.getStringExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER
          ),
        checksum = extractDeviceAdminChecksum(intent)
      )
    } else {
      null
    }

  private fun extractDeviceAdminComponentName(intent: Intent): ComponentName {
    return intent.getParcelableExtra(
      DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME
    )
      ?: throw IllegalArgumentException(
        "Intent is missing EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME extra"
      )
  }

  private fun extractDeviceAdminChecksum(intent: Intent): DeviceAdminChecksum {
    val packageChecksum =
      intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM)
    val signatureChecksum =
      intent.getStringExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM)

    return if (packageChecksum != null) {
      if (signatureChecksum != null) {
        throw IllegalArgumentException(
          "Intent has both " +
            "EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM and " +
            "EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"
        )
      }

      PackageChecksum(checksum = packageChecksum)
    } else if (signatureChecksum != null) {
      SignatureChecksum(checksum = signatureChecksum)
    } else {
      throw IllegalArgumentException(
        "Intent has no " +
          "EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM or " +
          "EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"
      )
    }
  }
}
