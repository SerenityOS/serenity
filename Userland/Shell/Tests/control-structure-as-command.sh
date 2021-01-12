#!/bin/sh

setopt --verbose

rm -rf shell-test 2> /dev/null
mkdir shell-test
cd shell-test

    touch a b c

    # Can we do logical stuff with control structures?
    ls && for $(seq 1) { echo yes > listing }
    test "$(cat listing)" = "yes" || echo for cannot appear as second part of '&&' && exit 1
    rm listing

    # FIXME: This should work!
    # for $(seq 1) { echo yes > listing } && echo HELLO!
    # test "$(cat listing)" = "yes" || echo for cannot appear as first part of '&&' && exit 1
    # rm listing

    # Can we pipe things into and from control structures?
    ls | if true { cat > listing }
    test "$(cat listing)" = "a b c" || echo if cannot be correctly redirected to && exit 1
    rm listing

    ls | for $(seq 1) { cat > listing }
    test "$(cat listing)" = "a b c" || echo for cannot be correctly redirected to && exit 1
    rm listing

    for $(seq 4) { echo $it } | cat > listing
    test "$(cat listing)" = "1 2 3 4" || echo for cannot be correctly redirected from && exit 1
    rm listing

    if true { echo TRUE! } | cat > listing
    test "$(cat listing)" = "TRUE!" || echo if cannot be correctly redirected from && exit 1
    rm listing

cd ..
rm -rf shell-test
