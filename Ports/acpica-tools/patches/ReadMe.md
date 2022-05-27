# Patches for nyancat on SerenityOS

## `0001--Stop-compiler-warnings-on-dangling-pointer.patch`

Use static variable to prevent using a dangling pointer from a previous stack trace.


## `0002-Disable-sprintf-debug-message-due-to-formatting-erro.patch`

Disable sprintf debug message with formatting issues.

## `0003-Fix-printf-bad-specifier-formatting.patch`

Fix sprintf specifier being written in a bad format leading to iASL to crash.
