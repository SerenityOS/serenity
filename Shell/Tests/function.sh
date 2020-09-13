#!/bin/sh

# Syntax ok?
fn() { echo $* }

# Can we invoke that?
test "$(fn 1)" = 1 || echo cannot invoke "'fn 1'" && exit 1
test "$(fn 1 2)" = "1 2" || echo cannot invoke "'fn 1 2'" && exit 1

# With explicit argument names?
fn(a) { echo $a }

# Can we invoke that?
test "$(fn 1)" = 1 || echo cannot invoke "'fn 1'" with explicit names && exit 1
test "$(fn 1 2)" = 1 || echo cannot invoke "'fn 1 2'" with explicit names and extra arguments && exit 1

# Can it fail?
if fn 2>/dev/null {
    echo "'fn'" with an explicit argument is not failing with not enough args
    exit 1
}

# $0 in function should be its name
fn() { echo $0 }

test "$(fn)" = fn || echo '$0' in function not equal to its name && exit 1
