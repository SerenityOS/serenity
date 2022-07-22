# Patches for cmake on SerenityOS

## `0001-kwsys-Don-t-use-siginfo.patch`

kwsys: Don't use siginfo

We don't support SIGINFO. This patch removes uses of SIGINFO.

- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0002-SerenityOS-Add-Platform-module.patch`

SerenityOS: Add Platform module

## `0003-cmcurl-Include-unistd.patch`

cmcurl: Include unistd

Everyone gets this wrong. Most platforms are very lax with these includes, but we're not one of them.

- [X] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

## `0004-cmcurl-Use-struct-stat-and-include-sys-stat.h.patch`

cmcurl: Use struct stat and include sys/stat.h

For unknown reasons, curl_setup_once.h does not include sys/stat.h. This patch includes sys/stat.h.

- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [X] Hack

## `0005-CMake-Disable-tests.patch`

CMake: Disable tests

We don't care about building tests for now, and it makes the compilation much faster.

- [ ] Local?
- [ ] Should be merged to upstream?
- [ ] Resolves issue(s) with our side of things
- [ ] Hack

