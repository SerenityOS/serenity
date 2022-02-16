# Patches for mesa on SerenityOS

## `0001-Add-Serenity-detection.patch`

Adds "build for SerenityOS?" detection to the meson build file and source code.

- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0002-Add-Serenity-in-configuration-checks.patch`

Based on the detection in the previous commit, adds "is Serenity?" code/build configuration checks where appropriate.

- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0003-Don-t-use-ELF-TLS.patch`

Currently, we don't support ELF TLS in the DynamicLoader, so disable its usage when building for Serenity.

- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0004-Use-C-compiler-for-libdl-detection.patch`

For some reason C compiler can't detect our dl library but C++ compiler can, so use it when resolving the dependency.

- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0005-Expose-LibGL-interface-from-libOSMesa.patch`

This allows us to expose a LibGL interface implementation from libOSMesa. And combined with creating a symlink
named 'libgl.so' and placing its path in LD_LIBRARY_PATH it'll allow substituting GL backend for apps built for LibGL.

- [X] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [X] Hack
