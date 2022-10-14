# Patches for acpica-tools on SerenityOS

## `0001-Stop-compiler-warnings-on-dangling-pointer.patch`

Stop compiler warnings on dangling pointer

Use static variable to prevent using a dangling pointer from a previous
stack trace.

## `0002-Disable-sprintf-debug-message-with-formatting-issues.patch`

Disable sprintf debug message with formatting issues


## `0003-Fix-printf-bad-specifier-formatting.patch`

Fix printf bad specifier formatting

Fix sprintf specifier being written in a bad format leading to iASL to
crash.

