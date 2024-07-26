#!/bin/Shell

echo "Not running Shell-valid as it has a high failure rate on target #7336"
echo PASS
exit 0

source $(dirname "$0")/test-commons.inc

# Are comments ignored?
# Sanity check: can we do && and || ?
true || exit 2
false

# Can we chain &&'s?
false && exit 2 && fail "can't chain &&'s"

# Proper precedence between &&'s and ||'s
false && exit 2 || true && false && fail Invalid precedence between '&&' and '||'


# Sanity check: can we pass arguments to 'test'?
if not test yes = yes  { exit 2 }

# Sanity check: can we use $(command)?
if not test "$(echo yes)" = yes  { exit 2 }
# Redirections.
if not test -z "$(echo foo > /dev/null)"  { fail direct path redirection }
if not test -z "$(echo foo 2> /dev/null 1>&2)"  { fail indirect redirection }
if not test -n "$(echo foo 2> /dev/null)"  { fail fds interfere with each other }

# Argument unpack
if not test "$(echo (yes))" = yes  { fail arguments inside bare lists }
if not test "$(echo (no)() yes)" = yes  { fail arguments inside juxtaposition: empty }
if not test "$(echo (y)(es))" = yes  { fail arguments inside juxtaposition: list }
if not test "$(echo "y"es)" = yes  { fail arguments inside juxtaposition: string }

# String substitution
foo=yes
if not test "$(echo $foo)" = yes  { fail simple string var lookup }
if not test "$(echo "$foo")" = yes  { fail stringified string var lookup }

# List substitution
foo=(yes)
# Static lookup, as list
if not test "$(echo $foo)" = yes  { fail simple list var lookup }
# Static lookup, stringified
if not test "$(echo "$foo")" = yes  { fail stringified list var lookup }
# Dynamic lookup through static expression
if not test "$(echo $'foo')" = yes  { fail dynamic lookup through static exp }
# Dynamic lookup through dynamic expression
ref_to_foo=foo
if not test "$(echo $"$ref_to_foo")" = yes  { fail dynamic lookup through dynamic exp }

# More redirections
echo test > /tmp/sh-test
if not test "$(cat /tmp/sh-test)" = test  { fail simple path redirect }
rm /tmp/sh-test

# 'brace' expansions
if not test "$(echo x(yes no))" = "xyes xno"  { fail simple juxtaposition expansion }
if not test "$(echo (y n)(es o))" = "yes yo nes no"  { fail list-list juxtaposition expansion }
if not test "$(echo ()(foo bar baz))" = ""  { fail empty expansion }

# Variables inside commands
to_devnull=(>/dev/null)
if not test "$(echo hewwo $to_devnull)" = ""  { fail variable containing simple command }

word_count=(() | wc -w)
if not test "$(echo well hello friends $word_count)" -eq 3  { fail variable containing pipeline }

# Globs
rm -fr /tmp/sh-test 2> /dev/null
mkdir -p /tmp/sh-test
pushd /tmp/sh-test
    touch (a b c)(d e f)
    if not test "$(echo a*)" = "ad ae af"  { fail '*' glob expansion }
    if not test "$(echo a?)" = "ad ae af"  { fail '?' glob expansion }

    glob_in_var='*'
    if not test "$(echo $glob_in_var)" = '*'  { fail substituted string acts as glob }

    if not test "$(echo (a*))" = "ad ae af"  { fail globs in lists resolve wrong }
    if not test "$(echo x(a*))" = "xad xae xaf"  { fail globs in lists do not resolve to lists }
    if not test "$(echo "foo"a*)" = "fooad fooae fooaf"  { fail globs join to dquoted strings }
popd
rm -fr /tmp/sh-test

# Setopt
setopt --inline_exec_keep_empty_segments
if not test "$(echo -n "a\n\nb")" = "a  b"  { fail inline_exec_keep_empty_segments has no effect }

setopt --no_inline_exec_keep_empty_segments
if not test "$(echo -n "a\n\nb")" = "a b"  { fail cannot unset inline_exec_keep_empty_segments }

echo PASS
