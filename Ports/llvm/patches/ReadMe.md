# Patches for LLVM on SerenityOS

## `insert-ifdef-serenity.patch`

This patch adds several defines in order to omit things not supported by SerenityOS.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [x] Hack

## `remove-version-script.patch`

Instructs the linker to not build LLVM shared libraries (`libclang.so`, `libLTO.so`, etc.) with
symbol versioning, which our dynamic linker does not support.

### Status
- [ ] Local?
- [x] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `toolchain.patch`

Adds support for the `$arch-pc-serenity` target to the Clang front end. This makes the compiler
look for libraries and headers in the right places, and enables some security mitigations, like
stack-smashing protection and building position-independent executables by default.

### Status
- [ ] Local?
- [x] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `llvm-backport-objcopy-update-section.patch`

Backports support for `llvm-objcopy --update-section` used by our toolchain from reviews.llvm.org/D112116.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issues(s) with our side of things
- [ ] Hack

