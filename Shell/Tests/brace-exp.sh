#!/bin/sh

setopt --verbose

fail() {
    echo $*
    exit 1
}

test "$(echo {a,b,})" = "a b " || fail normal brace expansion with one empty slot
test "$(echo {a,,b})" = "a  b" || fail normal brace expansion with one empty slot
test "$(echo {a,,,b})" = "a   b" || fail normal brace expansion with two empty slots
test "$(echo {a,b,,})" = "a b  " || fail normal brace expansion with two empty slots

test "$(echo {a..c})" = "a b c" || fail range brace expansion, alpha
test "$(echo {0..3})" = "0 1 2 3" || fail range brace expansion, number
test "$(echo {😂..😄})" = "😂 😃 😄" || fail range brace expansion, unicode codepoint

# Make sure that didn't mess with dots and commas in normal barewords
test .. = ".." || fail range brace expansion delimiter affects normal barewords
test , = "," || fail normal brace expansion delimiter affects normal barewords
