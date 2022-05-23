# Patches for cmake on SerenityOS

## `0001-kwsys-Don-t-use-siginfo.patch`

kwsys: Don't use siginfo

We don't support SIGINFO. This patch removes uses of SIGINFO.

- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0002-bin-bash.patch`

/bin/bash

This patch swaps out /bin/sh for /bin/bash in two scripts that need it.

- [X] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0003-Platform-SerenityOS.patch`

Platform/SerenityOS

This patch adds the SerenityOS platform config file to CMake.

- [ ] Local?
- [X] Should be merged to upstream? If we want to have cmake support serenity out of the box.
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0004-cmcurl-Include-unistd.patch`

cmcurl: Include unistd

Everyone gets this wrong. Most platforms are very lax with these includes, but we're not one of them.

- [X] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0005-cmcurl-Use-struct-stat-and-include-sys-stat.h.patch`

cmcurl: Use struct stat and include sys/stat.h

For unknown reasons, curl_setup_once.h does not include sys/stat.h. This patch includes sys/stat.h.

- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0006-CMake-Disable-tests.patch`

CMake: Disable tests

We don't care about building tests for now, and it makes the compilation much faster.

- [ ] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

