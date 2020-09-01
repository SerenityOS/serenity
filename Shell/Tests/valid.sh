#!/bin/sh

# Are comments ignored?
# Sanity check: can we do && and || ?
true || exit 2
false && exit 2

# Can we chain &&'s?
false && exit 2 && echo "can't chain &&'s" && exit 2

# Proper precedence between &&'s and ||'s
false && exit 2 || true && false && exit 2


# Sanity check: can we pass arguments to 'test'?
test yes = yes || exit 2

# Sanity check: can we use $(command)?
test "$(echo yes)" = yes || exit 2

# Apply some useful aliases
# FIXME: Convert these to functions when we have them
alias fail="echo Failure: "

# Redirections.
test -z "$(echo foo > /dev/null)" || fail direct path redirection && exit 2
test -z "$(echo foo 2> /dev/null 1>&2)" || fail indirect redirection && exit 2
test -n "$(echo foo 2> /dev/null)" || fail fds interfere with each other && exit 2

# Argument unpack
test "$(echo (yes))" = yes || fail arguments inside bare lists && exit 2
test "$(echo (no)() yes)" = yes || fail arguments inside juxtaposition: empty && exit 2
test "$(echo (y)(es))" = yes || fail arguments inside juxtaposition: list && exit 2
test "$(echo "y"es)" = yes || fail arguments inside juxtaposition: string && exit 2

# String substitution
foo=yes
test "$(echo $foo)" = yes || fail simple string var lookup && exit 2
test "$(echo "$foo")" = yes || fail stringified string var lookup && exit 2

# List substitution
foo=(yes)
# Static lookup, as list
test "$(echo $foo)" = yes || fail simple list var lookup && exit 2
# Static lookup, stringified
test "$(echo "$foo")" = yes || fail stringified list var lookup && exit 2
# Dynamic lookup through static expression
test "$(echo $'foo')" = yes || fail dynamic lookup through static exp && exit 2
# Dynamic lookup through dynamic expression
ref_to_foo=foo
test "$(echo $"$ref_to_foo")" = yes || fail dynamic lookup through dynamic exp && exit 2

# More redirections
echo test > /tmp/sh-test
test "$(cat /tmp/sh-test)" = test || fail simple path redirect && exit 2
rm /tmp/sh-test

# 'brace' expansions
test "$(echo x(yes no))" = "xyes xno" || fail simple juxtaposition expansion && exit 2
test "$(echo (y n)(es o))" = "yes yo nes no" || fail list-list juxtaposition expansion && exit 2
test "$(echo ()(foo bar baz))" = "" || fail empty expansion && exit 2

# Variables inside commands
to_devnull=(>/dev/null)
test "$(echo hewwo $to_devnull)" = "" || fail variable containing simple command && exit 2

word_count=(() | wc -w)
test "$(echo well hello friends $word_count)" -eq 3 || fail variable containing pipeline && exit 2

# Globs
mkdir sh-test
pushd sh-test
    touch (a b c)(d e f)
    test "$(echo a*)" = "ad ae af" || fail '*' glob expansion && exit 2
    test "$(echo a?)" = "ad ae af" || fail '?' glob expansion && exit 2

    glob_in_var='*'
    test "$(echo $glob_in_var)" = '*' || fail substituted string acts as glob && exit 2

    test "$(echo (a*))" = "ad ae af" || fail globs in lists resolve wrong && exit 2
    test "$(echo x(a*))" = "xad xae xaf" || fail globs in lists do not resolve to lists && exit 2
    test "$(echo "foo"a*)" = "fooad fooae fooaf" || fail globs join to dquoted strings && exit 2
popd
rm -fr sh-test

# Setopt
setopt --inline_exec_keep_empty_segments
test "$(echo -n "a\n\nb")" = "a  b" || fail inline_exec_keep_empty_segments has no effect && exit 2

setopt --no_inline_exec_keep_empty_segments
test "$(echo -n "a\n\nb")" = "a b" || fail cannot unset inline_exec_keep_empty_segments && exit 2
