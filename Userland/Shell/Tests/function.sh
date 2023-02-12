#!/bin/Shell

source $(dirname "$0")/test-commons.inc

# Syntax ok?
fn() { echo $* }

# Can we invoke that?
if not test "$(fn 1)" = 1 { fail cannot invoke "'fn 1'" }
if not test "$(fn 1 2)" = "1 2" { fail cannot invoke "'fn 1 2'" }

# With explicit argument names?
fn(a) { echo $a }

# Can we invoke that?
if not test "$(fn 1)" = 1 { fail cannot invoke "'fn 1'" with explicit names }
if not test "$(fn 1 2)" = 1 { fail cannot invoke "'fn 1 2'" with explicit names and extra arguments }

# FIXME: Re-enable this when we have something akin to 'try'
#        or when not-enough-args isn't a hard failure.
# Can it fail?
# if fn 2>/dev/null {
#     fail "'fn'" with an explicit argument is not failing with not enough args
#     exit 1
# }

# $0 in function should be its name
fn() { echo $0 }

if not test "$(fn)" = fn { fail '$0' in function not equal to its name }

# Ensure ARGV does not leak from inner frames.
fn() {
    fn2 1 2 3
    echo $*
}

fn2() { }

if not test "$(fn foobar)" = "foobar" { fail 'Frames are somehow messed up in nested functions' }

fn(xfoo) { }
xfoo=1
fn 2
if not test $xfoo -eq 1 { fail 'Functions overwrite parent scopes' }

echo PASS
