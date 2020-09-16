#!/bin/sh

# Are comments ignored?
# Sanity check: can we do && and || ?
true || exit 2
false

# Apply some useful aliases
fail() {
    echo $*
    exit 1
}

# Can we chain &&'s?
false && exit 2 && fail "can't chain &&'s"

# Proper precedence between &&'s and ||'s
false && exit 2 || true && false && fail Invalid precedence between '&&' and '||'


# Sanity check: can we pass arguments to 'test'?
test yes = yes || exit 2

# Sanity check: can we use $(command)?
test "$(echo yes)" = yes || exit 2
# Redirections.
test -z "$(echo foo > /dev/null)" || fail direct path redirection
test -z "$(echo foo 2> /dev/null 1>&2)" || fail indirect redirection
test -n "$(echo foo 2> /dev/null)" || fail fds interfere with each other

# Argument unpack
test "$(echo (yes))" = yes || fail arguments inside bare lists
test "$(echo (no)() yes)" = yes || fail arguments inside juxtaposition: empty
test "$(echo (y)(es))" = yes || fail arguments inside juxtaposition: list
test "$(echo "y"es)" = yes || fail arguments inside juxtaposition: string

# String substitution
foo=yes
test "$(echo $foo)" = yes || fail simple string var lookup
test "$(echo "$foo")" = yes || fail stringified string var lookup

# List substitution
foo=(yes)
# Static lookup, as list
test "$(echo $foo)" = yes || fail simple list var lookup
# Static lookup, stringified
test "$(echo "$foo")" = yes || fail stringified list var lookup
# Dynamic lookup through static expression
test "$(echo $'foo')" = yes || fail dynamic lookup through static exp
# Dynamic lookup through dynamic expression
ref_to_foo=foo
test "$(echo $"$ref_to_foo")" = yes || fail dynamic lookup through dynamic exp

# More redirections
echo test > /tmp/sh-test
test "$(cat /tmp/sh-test)" = test || fail simple path redirect
rm /tmp/sh-test

# 'brace' expansions
test "$(echo x(yes no))" = "xyes xno" || fail simple juxtaposition expansion
test "$(echo (y n)(es o))" = "yes yo nes no" || fail list-list juxtaposition expansion
test "$(echo ()(foo bar baz))" = "" || fail empty expansion

# Variables inside commands
to_devnull=(>/dev/null)
test "$(echo hewwo $to_devnull)" = "" || fail variable containing simple command

word_count=(() | wc -w)
test "$(echo well hello friends $word_count)" -eq 3 || fail variable containing pipeline

# Globs
mkdir sh-test
pushd sh-test
    touch (a b c)(d e f)
    test "$(echo a*)" = "ad ae af" || fail '*' glob expansion
    test "$(echo a?)" = "ad ae af" || fail '?' glob expansion

    glob_in_var='*'
    test "$(echo $glob_in_var)" = '*' || fail substituted string acts as glob

    test "$(echo (a*))" = "ad ae af" || fail globs in lists resolve wrong
    test "$(echo x(a*))" = "xad xae xaf" || fail globs in lists do not resolve to lists
    test "$(echo "foo"a*)" = "fooad fooae fooaf" || fail globs join to dquoted strings
popd
rm -fr sh-test

# Setopt
setopt --inline_exec_keep_empty_segments
test "$(echo -n "a\n\nb")" = "a  b" || fail inline_exec_keep_empty_segments has no effect

setopt --no_inline_exec_keep_empty_segments
test "$(echo -n "a\n\nb")" = "a b" || fail cannot unset inline_exec_keep_empty_segments
