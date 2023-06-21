#!/bin/Shell

source $(dirname "$0")/test-commons.inc

setopt --verbose

rm -rf /tmp/shell-test 2> /dev/null
mkdir -p /tmp/shell-test
pushd /tmp/shell-test

    touch a b c

    # Can we do logical stuff with control structures?
    ls && for $(seq 1) { echo yes > listing }
    if not test "$(cat listing)" = "yes" { fail for cannot appear as second part of '&&' }
    rm listing

    # FIXME: These should work!

    # for $(seq 1) { echo yes > listing } && echo HELLO!
    # if not test "$(cat listing)" = "yes" { echo for cannot appear as first part of '&&' }
    # rm listing

    # Can we pipe things into and from control structures?
    # ls | if true { cat > listing }
    # if not test "$(cat listing)" = "a b c" { fail if cannot be correctly redirected to }
    # rm listing

    # ls | for $(seq 1) { cat > listing }
    # if not test "$(cat listing)" = "a b c" { fail for cannot be correctly redirected to }
    # rm listing

    for $(seq 4) { echo $it } | cat > listing
    if not test "$(cat listing)" = "1 2 3 4" { fail for cannot be correctly redirected from }
    rm listing

    if true { echo TRUE! } | cat > listing
    if not test "$(cat listing)" = "TRUE!" { fail if cannot be correctly redirected from }
    rm listing

popd
rm -rf /tmp/shell-test

echo PASS
