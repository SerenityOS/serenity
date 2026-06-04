# Patches for fuse-ext2 on macOS

## `0001-Build-with-latest-macFUSE-on-macOS.patch`

Build with latest macFUSE on macOS

Adjusts the macOS build for current macFUSE headers and skips installing
the legacy preference pane.

The `op_getxattr` signature gains a `position` parameter required by the
macFUSE 4.x FUSE API. The preference pane install step is removed because
the prebuilt `.prefPane` bundle is no longer shipped in the source tree.

https://github.com/alperakcan/fuse-ext2/issues/149
