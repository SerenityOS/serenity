# Patches for CMake (and submodules) on SerenityOS

## `0001-cmnghttp2-check-for-HAVE_SIZEOF_SSIZE_T-and-not-HAVE.patch`

Fixes an issue where the first configure of cmake would generate an invalid config.h file somewhere, leading to the first run of `./package.sh` failing due to what `0016-conflicting-0.patch` and `0017-conflicting-1.patch` attempt to fix.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issues(s) with our side of things
- [ ] Hack

## `0010-don-t-use-siginfo.patch`

We don't support SIGINFO. This patch removes uses of SIGINFO.

### Status
- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0011-Fixed-your-code-rot-cmake.patch`

This purely fixes code issues with cmake. very funny patch.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0012-bin-bash.patch`

This patch swaps out `/bin/sh` for `/bin/bash` in two scripts that need it.

### Status
- [X] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0013-platform-serenityos.patch`
This patch adds the SerenityOS platform config file to CMake.

### Status
- [ ] Local?
- [X] Should be merged to upstream? If we want to have cmake support serenity out of the box.
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0014-cmcurl-include-unistd.patch`

Everyone gets this wrong. most platforms are very lax with these includes, but we're not one of them.

### Status
- [X] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0016-conflicting-0.patch` and `0017-conflicting-1.patch`

These two defines make GCC very sad. reasons are unknown at this time.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0026-curl-struct-stat.patch`

For unknown reasons, `curl_setup_once.h` does not include `sys/stat.h`. this patch includes `sys/stat.h`.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0028-cmake-disable-tests.patch`

We don't care about building tests for now, and it makes the compilation much faster.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack
