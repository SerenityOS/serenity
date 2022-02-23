#!/bin/sh

source $(dirname "$0")/test-commons.inc

if not internal:string_equal "$*" "" { fail "Argv list not empty" }
if not internal:number_equal "$#" 0 { fail "Argv list empty but count non-zero" }
if not internal:string_equal "$ARGV" "$*" { fail "\$ARGV not equal to \$*" }

ARGV=(1 2 3)
if not internal:number_equal "$#" 3 { fail "Assignment to ARGV does not affect \$#" }
if not internal:string_equal "$*" "1 2 3" { fail "Assignment to ARGV does not affect \$*" }

shift
if not internal:string_equal "$*" "2 3" { fail "'shift' does not work correctly" }

shift 2
if not internal:string_equal "$*" "" { fail "'shift 2' does not work correctly" }

echo PASS
