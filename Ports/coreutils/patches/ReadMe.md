# Patches for coreutils on SerenityOS

## `0001-gnulib-Port-getlocalename_l_unsafe-to-SerenityOS.patch`

gnulib: Port getlocalename_l_unsafe to SerenityOS


## `0002-Disable-unsupported-prctl-call.patch`

Disable unsupported prctl call

Serenity has prctl but doesn't define PR_SET_PDEATHSIG.

