#!/bin/Shell

source $(dirname "$0")/test-commons.inc

# can we use [0] as a bareword still?
if not test [0] = "[0]" {
    fail cannot use '[0]' as a bareword anymore
}

# can we use [0,2] as a bareword still?
if not test [0,2] = "[0,2]" {
    fail cannot use '[0,2]' as a bareword anymore
}

# can we use [0..2] as a bareword still?
if not test [0..2] = "[0..2]" {
    fail cannot use '[0..2]' as a bareword anymore
}

# Lists
x=(1 2 3)
if not test $x[0] -eq 1 {
    fail invalid first element
}

if not test $x[1] -eq 2 {
    fail invalid second element
}

if not test $x[-1] -eq 3 {
    fail invalid first-from-end element
}

if not test $x[-2] -eq 2 {
    fail invalid second-from-end element
}

## Multiple indices
if not test "$x[1,2]" = "2 3" {
    fail invalid multi-select '(1, 2)'
}

if not test "$x[0..2]" = "1 2 3" {
    fail invalid multi-select with range '[0..2]'
}

# Strings
x="Well Hello Friends!"
if not test $x[0] = W {
    fail invalid string first element
}

if not test $x[1] = e {
    fail invalid string second element
}

if not test $x[-1] = '!' {
    fail invalid string first-from-end element
}

if not test $x[-2] = 's' {
    fail invalid string second-from-end element
}

if not test $x[0,5,11,-1] = 'WHF!' {
    fail invalid string multi-select
}

if not test $x[5..9] = "Hello" {
    fail invalid string multi-select with range '[5..9]'
}

pass
