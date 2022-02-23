#!/bin/sh

source $(dirname "$0")/test-commons.inc

setopt --verbose
executed=no
rm -rf /tmp/shell-test 2> /dev/null
mkdir -p /tmp/shell-test
pushd /tmp/shell-test
    # Can we do logical stuff with control structures?
    true && for $(seq 1) { executed=yes }
    if not internal:string_equal "$executed" "yes" { fail for cannot appear as second part of '&&' }

    # FIXME: These should work!

    # executed=no
    # for $(seq 1) { executed=yes } && echo HELLO!
    # if not internal:string_equal "$executed" "yes" { echo for cannot appear as first part of '&&' }
    # rm listing

    # Can we pipe things into and from control structures?
    # ls | if true { cat > listing }
    # if not internal:string_equal "$(cat listing)" "a b c" { fail if cannot be correctly redirected to }
    # rm listing

    # ls | for $(seq 1) { cat > listing }
    # if not internal:string_equal "$(cat listing)" "a b c" { fail for cannot be correctly redirected to }
    # rm listing

    for $(seq 4) { echo $it } | cat > listing
    if not internal:string_equal "$(cat listing)" "1 2 3 4" { fail for cannot be correctly redirected from }
    rm listing

    if true { echo TRUE! } | cat > listing
    if not internal:string_equal "$(cat listing)" "TRUE!" { fail if cannot be correctly redirected from }
    rm listing
popd
rm -fr /tmp/shell-test 2> /dev/null

echo PASS
