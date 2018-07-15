package kr.co.plasticcity.kotlmata

/* log in KotlmataImpl */
val KOTLMATA_INITIALIZED = "** Kotlmata has been initialized"
val KOTLMATA_RELEASED = "** Kotlmata has been released"
val KOTLMATA_ERROR_IN_NOT_INIT = "** Kotlmata initialization error : Call Kotlmata.initialize() first"
val KOTLMATA_ERROR_IN_RUNNING = "** Kotlmata unknown error : Kotlmata is in RUNNIG state, but instance == null"
val KOTLMATA_ERROR_IN_RELEASED = "** Kotlmata already released : Kotlmata is released, but Kotlmata command is called"
val KOTLMATA_ERROR_IN_UNDEFINED = "** Kotlmata undefined state : %s"
val KOTLMATA_REJECTED_EXECUTION_EXCEPTION = "** Kotlmata RejectedExecutionException occurred"

/* log in JMBuilderImpl */
val MACHINE_BUILD_STARTED = "[%s] machine build started"
val IGNORE_MACHINE_BUILD = "[%s] machine already exists, ignoring build"
val REPLACE_MACHINE = "[%s] machine already exists and will be replaced with a new machine"
val STATE_DEFINITION_DUPLICATED = "[%s] definition of state [%s] duplicated"
val MACHINE_BUILT = "[%s] machine has been built"

/* log in JMMachineImpl */
val MACHINE_STATE_CHANGED = "[%s] machine state changed : [%s] -> [%s]"
val STATE_SWITCHED_BY_CLASS = "[%s] switched from [%s] to [%s] by [%s]"
val STATE_SWITCHED_BY_STRING = "[%s] switched from [%s] to [%s] by [\"%s\"]"
val MACHINE_SHUTDOWN_FAILED_AS_TIMEOUT = "[%s] machine shutdown failed because the last work is too long (over 5 second)"
val MACHINE_SHUTDOWN_FAILED_AS_INTERRUPT = "[%s] machine shutdown failed because unknown interrupt"

/* log in JMStateImpl */
val ENTER_FUNC_DUPLICATED = "[%s] definition of default entry function duplicated in state [%s]"
val ENTER_BY_CLASS_FUNC_DUPLICATED = "[%s] definition of entry function for input [%s] duplicated in state [%s]"
val ENTER_BY_STRING_FUNC_DUPLICATED = "[%s] definition of entry function for input [\"%s\"] duplicated in state [%s]"
val EXIT_FUNC_DUPLICATED = "[%s] definition of default exit function duplicated in state [%s]"
val EXIT_BY_CLASS_FUNC_DUPLICATED = "[%s] definition of exit function for input [%s] duplicated in state [%s]"
val EXIT_BY_STRING_FUNC_DUPLICATED = "[%s] definition of exit function for input [\"%s\"] duplicated in state [%s]"
val SWITCH_RULE_BY_CLASS_DUPLICATED = "[%s] definition of switch rule for input [%s] duplicated in state [%s]"
val SWITCH_RULE_BY_STRING_DUPLICATED = "[%s] definition of switch rule for input [\"%s\"] duplicated in state [%s]"
val SWITCH_TO_UNDEFINED_STATE_BY_CLASS = "[%s] tried to switch from [%s] to [%s] by [%s], but machine has no definition for [%s]"
val SWITCH_TO_UNDEFINED_STATE_BY_STRING = "[%s] tried to switch from [%s] to [%s] by [\"%s\"], but machine has no definition for [%s]"
