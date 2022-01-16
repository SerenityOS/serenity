# Patches for cmake on SerenityOS

## `0001-cmnghttp2-check-for-HAVE_SIZEOF_SSIZE_T-and-not-HAVE.patch`

cmnghttp2: check for HAVE_SIZEOF_SSIZE_T and not HAVE_SSIZE_T

The `check_size_type(ssize_t SIZEOF_SSIZE_T` call in cmcurl (referenced
by the comment above, which also references some other variables that
no longer seem to be used) defines HAVE_SIZEOF_SSIZE_T and not
HAVE_SSIZE_T.
The HAVE_SSIZE_T variable *does* get defined, but via the
`CHECK_SIZE_TYPE(ssize_t SSIZE_T)` call in cmlibarchive, which gets
configured *after* cmnghttp2, and so the first configure leads to an
invalid cmnghttp2/config.h file.

- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issues(s) with our side of things
- [ ] Hack

## `0002-kwsys-Don-t-use-siginfo.patch`

kwsys: Don't use siginfo

We don't support SIGINFO. This patch removes uses of SIGINFO.

- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0003-bin-bash.patch`

/bin/bash

This patch swaps out /bin/sh for /bin/bash in two scripts that need it.

- [X] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0004-Platform-SerenityOS.patch`

Platform/SerenityOS

This patch adds the SerenityOS platform config file to CMake.

- [ ] Local?
- [X] Should be merged to upstream? If we want to have cmake support serenity out of the box.
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0005-cmcurl-Include-unistd.patch`

cmcurl: Include unistd

Everyone gets this wrong. most platforms are very lax with these includes, but we're not one of them.

- [X] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0006-cmcurl-Use-struct-stat-and-include-sys-stat.h.patch`

cmcurl: Use struct stat and include sys/stat.h

For unknown reasons, curl_setup_once.h does not include sys/stat.h. this patch includes sys/stat.h.

- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0007-CMake-Disable-tests.patch`

CMake: Disable tests

We don't care about building tests for now, and it makes the compilation much faster.

- [ ] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

