# Patches for VVVVVV on SerenityOS

## `0001-Change-C-standard-to-C99.patch`

Change C standard to C99

The originally-set C90 doesn't support the `inline` keyword, causing
weird compile errors because isalnum() and other functions under
ctype.h are actually #define'd as inline.

## `0002-Add-SerenityOS-in-platform-specific-defines.patch`

Add SerenityOS in platform-specific defines


## `0003-Defer-sound-system-initialization-until-main.patch`

Defer sound system initialization until main()

The original VVVVVV source code initializes the sound subsystem
during global variable initialization. Technically speaking, the
order of initialization of global variables is unspecified. This
results in SerenityOS's dynamic linker to call Mix_OpenAudio()
(which uses EventLoop under the hood) before all global variable
initializers finish executing, resulting in an inconsistent state
that causes assertion failure.

This patch moves initialization of the culprit sound system to
a separate method, which is called first thing inside main(), thus
preventing the setup from happening during musicclass's initialization
in the global scope, which causes the aforementioned crash.

