package com.android.onboarding.versions.changes

import com.android.onboarding.versions.annotations.ChangeId
import com.android.onboarding.versions.annotations.ChangeRadius
import com.android.onboarding.versions.changes.Tags.NEXT
import com.android.onboarding.versions.changes.Tags.P10_DVT
import com.android.onboarding.versions.changes.Tags.P11_EVT

const val USE_FRAGMENT_LOGIC = "use_fragment_logic"
const val MOVE_TO_BACKGROUND = "move_to_background"
const val ELIMINATE_LOADING = "eliminate_loading"
const val ENABLE_AUTOMATIC_NODE_DETECTION_FOR_OEMS =
  "com.google.android.setupwizard.enable_automatic_node_detection_for_oems"

@ChangeId(
  bugId = 392794832L,
  owner = "temirlanu",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [P10_DVT],
  available = "2025-02-01",
)
const val UX_DVT_IMPROVEMENTS_AND_BUG_FIXES = 392794832L

@ChangeId(
  bugId = 390215035L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [ELIMINATE_LOADING],
  available = "2024-12-20",
)
const val JOINED_UP_LOADING = 390215035L

@ChangeId(
  bugId = 341941999L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val TRANSFER_WIFI_INFO_USE_FRAGMENT_LOGIC = 341941999L

@ChangeId(
  bugId = 342221069L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = TRANSFER_WIFI_INFO_USE_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val TRANSFER_WIFI_INFO_RUNS_WITHOUT_UI_DEPENDENCY = 342221069L

@ChangeId(
  bugId = 342441204L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val PROVISIONING_PROFILE_CHECKER_USE_FRAGMENT_LOGIC = 342441204L

@ChangeId(
  bugId = 342448453L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = PROVISIONING_PROFILE_CHECKER_USE_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val PROVISIONING_PROFILE_CHECKER_RUNS_WITHOUT_UI_DEPENDENCY = 342448453L

@ChangeId(
  bugId = 342555863L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val NETWORK_ACTIVITY_USES_FRAGMENT_LOGIC = 342555863L

@ChangeId(
  bugId = 343135074L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val COMPLETE_IN_FLIGHT_UPDATES_USE_FRAGMENT_LOGIC = 343135074L

@ChangeId(
  bugId = 343134879L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = COMPLETE_IN_FLIGHT_UPDATES_USE_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val COMPLETE_IN_FLIGHT_UPDATES_RUNS_WITHOUT_UI_DEPENDENCY = 343134879L

@ChangeId(
  bugId = 343135237L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val CHECKIN_AND_UPDATE_USES_FRAGMENT_LOGIC = 343135237L

@ChangeId(
  bugId = 343135258L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = CHECKIN_AND_UPDATE_USES_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val CHECKIN_AND_UPDATE_RUNS_WITHOUT_UI_DEPENDENCY = 343135258L

@ChangeId(
  bugId = 344004346L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [ELIMINATE_LOADING],
  available = "2024-09-16",
)
const val WIZARD_MANAGER_USE_BACKGROUND_NEXT_ACTION_INLINE = 344004346L

@ChangeId(
  bugId = 345432388L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val POST_CHECKIN_AND_UPDATE_USES_FRAGMENT_LOGIC = 345432388L

@ChangeId(
  bugId = 356883024L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [ELIMINATE_LOADING],
  available = "2024-09-16",
)
const val AP_MONITOR_CAN_BE_USED_WITHOUT_UI = 356883024L

@ChangeId(
  bugId = 350934907L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val PRE_SYNC_ACTIVITY_USES_FRAGMENT_LOGIC = 350934907L

@ChangeId(
  bugId = 348627695L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val ESIM_SETUP_WRAPPER_USES_FRAGMENT_LOGIC = 348627695L

@ChangeId(
  bugId = 348625650L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = ESIM_SETUP_WRAPPER_USES_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val ESIM_SETUP_WRAPPER_RUNS_WITHOUT_UI_DEPENDENCY = 348625650L

@ChangeId(
  bugId = 349303956L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val CARRIER_SETUP_USES_FRAGMENT_LOGIC = 349303956L

@ChangeId(
  bugId = 349785002L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val NETWORK_CHECK_USES_FRAGMENT_LOGIC = 349785002L

@ChangeId(
  bugId = 349785000L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = NETWORK_CHECK_USES_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val NETWORK_CHECK_RUNS_WITHOUT_UI_DEPENDENCY = 349785000L

@ChangeId(
  bugId = 354932258L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val ZERO_TOUCH_USES_FRAGMENT_LOGIC = 354932258L

@ChangeId(
  bugId = 355158019L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val CHECK_FRP_USES_FRAGMENT_LOGIC = 355158019L

@ChangeId(
  bugId = 355158178L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = CHECK_FRP_USES_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val CHECK_FRP_RUNS_WITHOUT_UI_DEPENDENCY = 355158178L

@ChangeId(
  bugId = 355187780L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val LOAD_ADD_ACCOUNT_INTENT_USES_FRAGMENT_LOGIC = 355187780L

@ChangeId(
  bugId = 355188991L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = LOAD_ADD_ACCOUNT_INTENT_USES_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val LOAD_ADD_ACCOUNT_INTENT_RUNS_WITHOUT_UI_DEPENDENCY = 355188991L

@ChangeId(
  bugId = 355409716L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val ACCOUNT_SETUP_USES_FRAGMENT_LOGIC = 355409716L

@ChangeId(
  bugId = 355424870L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val GMS_CHECKIN_USES_FRAGMENT_LOGIC = 355424870L

@ChangeId(
  bugId = 355425460L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = GMS_CHECKIN_USES_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val GMS_CHECKIN_RUNS_WITHOUT_UI_DEPENDENCY = 355425460L

@ChangeId(
  bugId = 349359412L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val SLOTS_SELECTION_USES_FRAGMENT_LOGIC = 349359412L

@ChangeId(
  bugId = 355437887L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val CHECK_QUICK_START_USES_FRAGMENT_LOGIC = 355437887L

@ChangeId(
  bugId = 368378946L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val ACCOUNT_SETUP_WRAPPER_USES_FRAGMENT_LOGIC = 368378946L

@ChangeId(
  bugId = 368378446L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = ACCOUNT_SETUP_WRAPPER_USES_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2024-09-16",
)
const val ACCOUNT_SETUP_WRAPPER_RUNS_WITHOUT_UI_DEPENDENCY = 368378446L

@ChangeId(
  bugId = 377286779L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val TWEEN_SETUP_SELECTION_USES_FRAGMENT_LOGIC = 377286779L

@ChangeId(
  bugId = 382425611L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2024-09-16",
)
const val BASE_APP_COMPAT_ACTIVITY_METADATA = 382425611L

@ChangeId(
  bugId = 374049457L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [P10_DVT, "use_orchestrated_tasks"],
  available = "2025-02-16",
)
const val USE_ORCHESTRATED_TASKS = 374049457L

@ChangeId(
  bugId = 399865595L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  tags = [USE_FRAGMENT_LOGIC],
  available = "2025-02-16",
)
const val AUTH_EARLY_UPDATE_ROLLBACK_USES_FRAGMENT_LOGIC = 399865595L

@ChangeId(
  bugId = 368378446L,
  owner = "kholoudm",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = CHECK_FRP_USES_FRAGMENT_LOGIC,
  tags = [MOVE_TO_BACKGROUND],
  available = "2025-02-16",
)
const val AUTH_EARLY_UPDATE_ROLLBACK_RUNS_WITHOUT_UI_DEPENDENCY = 368378446L

@ChangeId(
  bugId = 401531972L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = TRANSFER_WIFI_INFO_RUNS_WITHOUT_UI_DEPENDENCY,
)
const val TRANSFER_WIFI_INFO_TASK_DOES_NOT_USE_LOGIC = 401531972L

@ChangeId(
  bugId = 470030883L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  dependsOn = CHECKIN_AND_UPDATE_RUNS_WITHOUT_UI_DEPENDENCY,
)
const val COMPAT_CHECKIN_AND_EARLY_UPDATE_DOES_NOT_USE_LOGIC = 470030883L

@ChangeId(
  bugId = 408134087L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [P11_EVT, NEXT],
)
const val UNIFIED_LOADING_EVERYWHERE = 408134087L

@ChangeId(
  bugId = 415074459L,
  owner = "petuska",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = "2025-02-26",
  tags = [P10_DVT],
)
const val SUW_TASK_FAILURE_CALLBACK_FOR_AUTH_HACK_CHANGE_ID = 415074459L

@ChangeId(
  bugId = 401476658L,
  owner = "iancheng",
  breaking = [],
  tags = [P11_EVT, NEXT],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = "2025-08-05",
)
const val PERSIST_ACCOUNTS_SNAPSHOT = 401476658L

@ChangeId(
  bugId = 436772442L,
  owner = "tmfang",
  breaking = [],
  tags = [P11_EVT, NEXT],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = ChangeId.NOT_AVAILABLE,
)
const val WIZARD_SCRIPT_SUPPORTS_TASK_ACTION = 436772442L

@ChangeId(
  bugId = 410554685L,
  owner = "eliselliott",
  breaking = [],
  tags = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = "2025-03-02",
)
const val PRE_PHENOTYPE_PRECALC_TASKS = 410554685L

@ChangeId(
  bugId = 438609409L,
  owner = "eliselliott",
  breaking = [],
  tags = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = "2025-03-02",
)
const val DSE_FLOW_CONTROL_PRECALC_TASK_DEFAULT = 438609409L

@ChangeId(
  bugId = 451533821L,
  owner = "eliselliott",
  breaking = [],
  tags = [P11_EVT],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
)
const val CONSENT_TRANSFER_PRECALC_TASKS_DEFAULT = 451533821L

@ChangeId(
  bugId = 434904270L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
)
const val EARLY_SIM_INFO = 434904270L

@ChangeId(
  bugId = 444632096L,
  owner = "atpugnayra",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [P11_EVT, NEXT],
  available = ChangeId.NOT_AVAILABLE,
)
const val ESIM_ORCHESTRATION_SUPPORT = 444632096L

/**
 * When enabled, this change allows [WizardResultParser] to parse *multiple* <NextAction> and
 * <YIELD> control tag in a script. If disabled, [WizardResultParser] will only parse the first
 * wizard:action or <NextAction> and ignore the yield control.
 */
@ChangeId(
  bugId = 448547273L,
  owner = "tmfang",
  breaking = [],
  tags = [P11_EVT, NEXT],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
)
const val SUPPORT_MULTI_NEXT_ACTIONS_AND_YIELD_IN_SCRIPT = 448547273L

@ChangeId(
  bugId = 450443090L,
  owner = "tmfang",
  breaking = [],
  tags = [P11_EVT, NEXT],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
)
const val ENABLE_SINGLE_WORKER_IN_WIZARD_MANAGER = 450443090L

/**
 * When enabled, this change allows [WizardManagerNextActionController] to manage multiple
 * background workers concurrently. Such as: Returns a next node given an idle worker id. If
 * disabled, [WizardManagerNextActionController] will do what it does now.
 */
@ChangeId(
  bugId = 450443584L,
  owner = "tmfang",
  breaking = [],
  tags = [P11_EVT, NEXT],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
)
const val ENABLE_MULTI_WORKER_IN_NEXT_ACTION_CONTROLLER = 450443584L
