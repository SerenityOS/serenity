#!/bin/sh

# `head -n 1` should close stdout of the `Shell -c` command, which means the
# second echo should exit unsuccessfully and sigpipe.sh.out should not be
# created.
rm -f sigpipe.sh.out

{ echo foo && echo bar && echo baz > sigpipe.sh.out } | head -n 1 > /dev/null

# Failing commands don't make the test fail, just an explicit `exit 1` does.
# So the test only fails if sigpipe.sh.out exists (since then `exit 1` runs),
# not if the `test` statement returns false.
test -e sigpipe.sh.out && exit 1
