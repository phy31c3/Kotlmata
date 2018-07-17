package kr.co.plasticcity.kotlmata

/* KotlmataImpl */

const val KOTLMATA_INITIALIZED = "** Kotlmata has been initialized"
const val KOTLMATA_RELEASED = "** Kotlmata has been released"
const val KOTLMATA_ERROR_IN_NOT_INIT = "** Kotlmata initialization error : Call Kotlmata.initialize() first"
const val KOTLMATA_ERROR_IN_RUNNING = "** Kotlmata unknown error : Kotlmata is in RUNNIG state, but instance == null"
const val KOTLMATA_ERROR_IN_RELEASED = "** Kotlmata already released : Kotlmata is released, but Kotlmata command is called"
const val KOTLMATA_ERROR_IN_UNDEFINED = "** Kotlmata undefined state : %s"
const val KOTLMATA_REJECTED_EXECUTION_EXCEPTION = "** Kotlmata RejectedExecutionException occurred"

/* JMBuilderImpl */

const val MACHINE_BUILD_STARTED = "[%s] machine build started"
const val IGNORE_MACHINE_BUILD = "[%s] machine already exists, ignoring build"
const val REPLACE_MACHINE = "[%s] machine already exists and will be replaced with a new machine"
const val STATE_DEFINITION_DUPLICATED = "[%s] definition of state [%s] duplicated"
const val MACHINE_BUILT = "[%s] machine has been built"

/* JMMachineImpl */

const val MACHINE_STATE_CHANGED = "[%s] machine state changed : [%s] -> [%s]"
const val STATE_SWITCHED_BY_CLASS = "[%s] switched from [%s] to [%s] by [%s]"
const val STATE_SWITCHED_BY_STRING = "[%s] switched from [%s] to [%s] by [\"%s\"]"
const val MACHINE_SHUTDOWN_FAILED_AS_TIMEOUT = "[%s] machine shutdown failed because the last work is too long (over 5 second)"
const val MACHINE_SHUTDOWN_FAILED_AS_INTERRUPT = "[%s] machine shutdown failed because unknown interrupt"

/* JMStateImpl */

const val ENTER_FUNC_DUPLICATED = "[%s] definition of default entry function duplicated in state [%s]"
const val ENTER_BY_CLASS_FUNC_DUPLICATED = "[%s] definition of entry function for input [%s] duplicated in state [%s]"
const val ENTER_BY_STRING_FUNC_DUPLICATED = "[%s] definition of entry function for input [\"%s\"] duplicated in state [%s]"
const val EXIT_FUNC_DUPLICATED = "[%s] definition of default exit function duplicated in state [%s]"
const val EXIT_BY_CLASS_FUNC_DUPLICATED = "[%s] definition of exit function for input [%s] duplicated in state [%s]"
const val EXIT_BY_STRING_FUNC_DUPLICATED = "[%s] definition of exit function for input [\"%s\"] duplicated in state [%s]"
const val SWITCH_RULE_BY_CLASS_DUPLICATED = "[%s] definition of switch rule for input [%s] duplicated in state [%s]"
const val SWITCH_RULE_BY_STRING_DUPLICATED = "[%s] definition of switch rule for input [\"%s\"] duplicated in state [%s]"
const val SWITCH_TO_UNDEFINED_STATE_BY_CLASS = "[%s] tried to switch from [%s] to [%s] by [%s], but machine has no definition for [%s]"
const val SWITCH_TO_UNDEFINED_STATE_BY_STRING = "[%s] tried to switch from [%s] to [%s] by [\"%s\"], but machine has no definition for [%s]"

/* Config */
const val INVALID_CONFIG = "Use invalid Config object. Config is only available within the 'Kotlmata.init' function"