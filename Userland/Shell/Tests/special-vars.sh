#!/bin/sh

test "$*" = "" || echo "Fail: Argv list not empty" && exit 1
test "$#" -eq 0 || echo "Fail: Argv list empty but count non-zero" && exit 1
test "$ARGV" = "$*" || echo "Fail: \$ARGV not equal to \$*" && exit 1

ARGV=(1 2 3)
test "$#" -eq 3 || echo "Fail: Assignment to ARGV does not affect \$#" && exit 1
test "$*" = "1 2 3" || echo "Fail: Assignment to ARGV does not affect \$*" && exit 1

shift
test "$*" = "2 3" || echo "Fail: 'shift' does not work correctly" && exit 1

shift 2
test "$*" = "" || echo "Fail: 'shift 2' does not work correctly" && exit 1
