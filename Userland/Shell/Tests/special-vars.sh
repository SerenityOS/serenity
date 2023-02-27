#!/bin/Shell

source $(dirname "$0")/test-commons.inc

if not test "$*" = "" { fail "Argv list not empty" }
if not test "$#" -eq 0 { fail "Argv list empty but count non-zero" }
if not test "$ARGV" = "$*" { fail "\$ARGV not equal to \$*" }

ARGV=(1 2 3)
if not test "$#" -eq 3 { fail "Assignment to ARGV does not affect \$#" }
if not test "$*" = "1 2 3" { fail "Assignment to ARGV does not affect \$*" }

shift
if not test "$*" = "2 3" { fail "'shift' does not work correctly" }

shift 2
if not test "$*" = "" { fail "'shift 2' does not work correctly" }

echo PASS
