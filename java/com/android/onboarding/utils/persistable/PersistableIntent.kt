package com.android.onboarding.utils.persistable

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import androidx.core.net.toUri
import com.android.onboarding.compatibility.compatGetBoolean
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.contentDeepEquals
import com.android.onboarding.utils.forceToString
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlin.reflect.KClass

/**
 * A wrapper around [Intent] which can be persisted to disk.
 *
 * Note that this is a very limited representation of [Intent].
 *
 * It only contains:
 * - The action
 * - The package
 * - The categories
 * - Any extras for which the value is Persistable
 * - The flags
 */
data class PersistableIntent
@JvmOverloads
constructor(
  @set:JvmName("action") var action: String? = null,
  @set:JvmName("type") var type: String? = null,
  @set:JvmName("extras") var extras: PersistableBundle = PersistableBundle(),
  @set:JvmName("package") var `package`: String? = null,
  @set:JvmName("component") var component: ComponentName? = null,
  @set:JvmName("categories") var categories: Set<String>? = null,
  @set:JvmName("flags") var flags: Int = 0,
  @set:JvmName("data") var data: Uri? = null,
) : Persistable, Parcelable, StableEquality {

  /** Build a [PersistableIntent] from [packageContext] and [klass] via [ComponentName]. */
  constructor(
    packageContext: Context,
    klass: KClass<*>,
  ) : this(packageContext = packageContext, cls = klass.java)

  /** Build a [PersistableIntent] from [packageContext] and [cls] via [ComponentName]. */
  constructor(
    packageContext: Context,
    cls: Class<*>,
  ) : this(component = ComponentName(packageContext, cls))

  /** Build a [PersistableIntent] from [intent] by copying all of its data. */
  constructor(
    intent: PersistableIntent
  ) : this(
    action = intent.action,
    type = intent.type,
    extras = PersistableBundle(intent.extras),
    `package` = intent.`package`,
    component = intent.component,
    categories = intent.categories,
    flags = intent.flags,
    data = intent.data,
  )

  /** Build a [PersistableIntent] from [action] and [uri]. */
  constructor(action: String?, uri: Uri?) : this(action = action, data = uri)

  /**
   * Build a [PersistableIntent] from [action], [uri], [packageContext] and [cls] via
   * [ComponentName].
   */
  constructor(
    action: String?,
    uri: Uri?,
    packageContext: Context,
    cls: Class<*>,
  ) : this(action = action, data = uri, component = ComponentName(packageContext, cls))

  /**
   * Convert an [Intent] into a [PersistableIntent].
   *
   * If [strict] is true then non-persistable extras will result in an exception on development
   * builds, otherwise any non-persistable extras will be logged and dropped.
   */
  @JvmOverloads
  constructor(
    intent: Intent,
    strict: Boolean = true,
  ) : this(
    action = intent.action,
    type = intent.type,
    `package` = intent.`package`,
    component = intent.component,
    flags = intent.flags,
    data = intent.data,
    categories = intent.categories,
    extras =
      intent.extras?.toPersistableBundle { key, error ->
        if (strict) Log.wtf(LOG_TAG, "Found non-persistable extra with key $key: $error")
        else Log.w(LOG_TAG, "Found non-persistable extra with key $key: $error")
      } ?: PersistableBundle(),
  )

  override fun describeContents() = toIntent().describeContents()

  override fun writeToParcel(dest: Parcel, flags: Int) = toIntent().writeToParcel(dest, flags)

  /**
   * Convert this [PersistableIntent] to regular intent by writing all data to a new [Intent]
   * instance.
   */
  fun toIntent() =
    Intent().also { intent ->
      intent.action = action
      intent.setDataAndType(data, type)
      intent.`package` = `package`
      intent.component = component
      intent.flags = flags
      categories?.forEach(intent::addCategory)
      extras.toBundle().let(intent::putExtras)
    }

  override fun toPersistableBundle(): PersistableBundle = PersistableBundle {
    putString(KEY_ACTION, action)
    putString(KEY_TYPE, type)
    putString(KEY_PACKAGE, `package`)
    putString(KEY_COMPONENT_NAME, component?.flattenToString())
    putInt(KEY_FLAGS, flags)
    putString(KEY_DATA, data?.toString())
    putStringArray(KEY_CATEGORIES, categories?.toTypedArray())
    putPersistableBundle(KEY_EXTRAS, extras)
  }

  override fun hashCode(): Int {
    var result = action.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + extras.generateHash()
    result = 31 * result + `package`.hashCode()
    result = 31 * result + component.hashCode()
    result = 31 * result + flags.hashCode()
    result = 31 * result + data.hashCode()
    result = 31 * result + categories.hashCode()
    result = 31 * result + extras.generateHash()

    return result
  }

  override fun equals(other: Any?): Boolean {
    return if (other is PersistableIntent) {
      action == other.action &&
        type == other.type &&
        extras contentDeepEquals other.extras &&
        `package` == other.`package` &&
        component == other.component &&
        flags == other.flags &&
        data == other.data &&
        categories == other.categories &&
        extras contentDeepEquals other.extras
    } else {
      false
    }
  }

  override fun toString(): String {
    return "PersistableIntent(" +
      "action=$action," +
      "type=$type," +
      "package=$`package`," +
      "component=$component," +
      "categories=$categories," +
      "flags=$flags," +
      "data=$data," +
      "extras=${extras.forceToString()})"
  }

  // --- Builders ---
  private inline fun build(action: () -> Unit): PersistableIntent {
    action()
    return this
  }

  /** @see [Intent.setAction] */
  @CanIgnoreReturnValue
  fun setAction(action: String?): PersistableIntent = build { this.action = action }

  /** @see [Intent.setPackage] */
  @CanIgnoreReturnValue
  fun setPackage(`package`: String?): PersistableIntent = build { this.`package` = `package` }

  /** @see [Intent.setComponent] */
  @CanIgnoreReturnValue
  fun setComponent(component: ComponentName?): PersistableIntent = build {
    this.component = component
  }

  /** @see [Intent.addCategory] */
  @CanIgnoreReturnValue
  fun addCategory(category: String): PersistableIntent = build {
    this.categories = (categories ?: setOf()) + category
  }

  /** @see [Intent.removeCategory] */
  fun removeCategory(category: String) {
    val newCategories = categories?.let { it - category }
    this.categories = if (newCategories.isNullOrEmpty()) null else newCategories
  }

  /** @see [Intent.getDataString] */
  val dataString: String?
    get() = data?.toString()

  /** @see [Intent.setData] */
  @CanIgnoreReturnValue fun setData(data: Uri?): PersistableIntent = build { this.data = data }

  /** @see [Intent.setFlags] */
  @CanIgnoreReturnValue fun setFlags(flags: Int): PersistableIntent = build { this.flags = flags }

  /** @see [Intent.putExtra] */
  @CanIgnoreReturnValue
  fun putExtra(key: String?, value: String?): PersistableIntent = build {
    this.extras.putString(key, value)
  }

  /** @see [Intent.getStringExtra] */
  fun getStringExtra(key: String?): String? = extras.getString(key)

  /** @see [Intent.putExtra] */
  @CanIgnoreReturnValue
  fun putExtra(key: String?, value: Int): PersistableIntent = build {
    this.extras.putInt(key, value)
  }

  /** @see [Intent.getIntExtra] */
  @JvmOverloads
  fun getIntExtra(key: String?, defaultValue: Int = 0): Int = extras.getInt(key, defaultValue)

  /** @see [Intent.putExtra] */
  @CanIgnoreReturnValue
  fun putExtra(key: String?, value: PersistableBundle?): PersistableIntent = build {
    this.extras.putPersistableBundle(key, value)
  }

  /** @see [Intent.getBundleExtra] */
  fun getBundleExtra(key: String?): PersistableBundle? = extras.getPersistableBundle(key)

  /** @see [PersistableBundle.putPersistable] */
  @CanIgnoreReturnValue
  fun putExtra(key: String?, value: Persistable?): PersistableIntent = build {
    this.extras.putPersistable(key, value)
  }

  /** @see [PersistableBundle.putPersistable] */
  @CanIgnoreReturnValue
  fun <T> putExtra(
    key: String?,
    value: T?,
    serializer: PersistableSerializer<T>,
  ): PersistableIntent = build { this.extras.putPersistable(key, value, serializer) }

  /** @see [PersistableBundle.getPersistable] */
  fun <T> getPersistableExtra(key: String?, deserializer: PersistableDeserializer<T>): T? =
    this.extras.getPersistable(key, deserializer)

  /** @see [PersistableBundle.putPersistableList] */
  @CanIgnoreReturnValue
  fun <T> putExtra(
    key: String?,
    value: List<T>?,
    serializer: PersistableSerializer<T>,
  ): PersistableIntent = build { this.extras.putPersistableList(key, value, serializer) }

  /** @see [PersistableBundle.putPersistableList] */
  @CanIgnoreReturnValue
  fun <T : Persistable> putExtra(key: String?, value: List<T>?): PersistableIntent = build {
    this.extras.putPersistableList(key, value)
  }

  /** @see [PersistableBundle.getPersistableList] */
  fun <T> getPersistableListExtra(
    key: String?,
    deserializer: PersistableDeserializer<T>,
  ): List<T?>? = this.extras.getPersistableList(key, deserializer)

  /** @see [PersistableBundle.compatPutBoolean] */
  @CanIgnoreReturnValue
  fun putExtra(key: String?, value: Boolean): PersistableIntent = build {
    this.extras.compatPutBoolean(key, value)
  }

  /** @see [PersistableBundle.compatGetBoolean] */
  @JvmOverloads
  fun getBooleanExtra(key: String?, defaultValue: Boolean = false): Boolean =
    this.extras.compatGetBoolean(key, defaultValue)

  /** @see [Intent.putExtras] */
  @CanIgnoreReturnValue
  fun putExtras(intent: PersistableIntent): PersistableIntent = build {
    this.extras.putAll(intent.extras)
  }

  /** @see [Intent.putExtras] */
  @CanIgnoreReturnValue
  fun putExtras(extras: PersistableBundle): PersistableIntent = build { this.extras.putAll(extras) }

  /** @see [Intent.hasExtra] */
  fun hasExtra(key: String?): Boolean = extras.containsKey(key)

  /** @see [Intent.removeExtra] */
  fun removeExtra(key: String?): Unit = extras.remove(key)

  /** @see [Intent.addFlags] */
  @CanIgnoreReturnValue fun addFlags(flags: Int) = build { this.flags = this.flags or flags }

  /** @see [Intent.putExtras] */
  fun putExtras(src: Intent) = build { src.extras?.toPersistableBundle()?.let(this.extras::putAll) }

  companion object CREATOR :
    PersistableDeserializer<PersistableIntent>,
    Parcelable.Creator<PersistableIntent>,
    PersistableSerializer<Intent> {
    private const val LOG_TAG = "PersistableIntent"

    private const val KEY_ACTION = "ACTION"
    private const val KEY_TYPE = "TYPE"
    private const val KEY_PACKAGE = "PACKAGE"
    private const val KEY_CATEGORIES = "CATEGORIES"
    private const val KEY_COMPONENT_NAME = "COMPONENT_NAME"
    private const val KEY_EXTRAS = "EXTRAS"
    private const val KEY_FLAGS = "FLAGS"
    private const val KEY_DATA = "DATA"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): PersistableIntent =
      PersistableIntent(
        action = persistableBundle.getString(KEY_ACTION),
        type = persistableBundle.getString(KEY_TYPE),
        `package` = persistableBundle.getString(KEY_PACKAGE),
        component =
          persistableBundle.getString(KEY_COMPONENT_NAME)?.let {
            ComponentName.unflattenFromString(it)
          },
        flags = persistableBundle.getInt(KEY_FLAGS),
        data = persistableBundle.getString(KEY_DATA)?.toUri(),
        categories = persistableBundle.getStringArray(KEY_CATEGORIES)?.toSet(),
        extras = persistableBundle.getPersistableBundle(KEY_EXTRAS) ?: PersistableBundle(),
      )

    /** Build [PersistableIntent] from the [intent]. */
    fun fromIntent(intent: Intent): PersistableIntent = PersistableIntent(intent)

    override fun createFromParcel(source: Parcel) =
      PersistableIntent(Intent.CREATOR.createFromParcel(source))

    override fun newArray(size: Int) = arrayOfNulls<PersistableIntent>(size)

    override fun toPersistableBundle(value: Intent) = PersistableIntent(value).toPersistableBundle()
  }
}

/**
 * Serialize this [Persistable] to [Intent] extras.
 *
 * Serialization happens at intent extras root and matches the properties that would be written to a
 * [PersistableBundle] via [toPersistableBundle][Persistable.toPersistableBundle]. Returned intent
 * has nothing configured except extras.
 */
fun Persistable.toIntent(base: Intent? = null): Intent = (base ?: Intent()).putExtras(toBundle())

/**
 * [toIntent] for [PersistableDeserializer].
 *
 * @see Persistable.toIntent
 */
@Suppress("UnusedReceiverParameter", "NOTHING_TO_INLINE")
inline fun <P : Persistable> PersistableDeserializer<P>.toIntent(
  value: P,
  base: Intent? = null,
): Intent = value.toIntent(base)

/**
 * Deserialize [P] from a supplied [intent] extras.
 *
 * Assumes intent extras were serialized via [toIntent][Persistable.toIntent].
 */
fun <P> PersistableDeserializer<P>.fromIntent(intent: Intent): P =
  fromPersistableBundle(intent.extras?.toPersistableBundle() ?: PersistableBundle())
