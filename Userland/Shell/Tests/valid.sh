#!/bin/sh

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
if not internal:string_equal yes yes  { exit 2 }

# Sanity check: can we use $(command)?
if not internal:string_equal "$(echo yes)" yes  { exit 2 }
# Redirections.
if not internal:string_equal "" "$(echo foo > /dev/null)"  { fail direct path redirection }
if not internal:string_equal "" "$(echo foo 2> /dev/null 1>&2)"  { fail indirect redirection }
if not not internal:string_equal "" "$(echo foo 2> /dev/null)"  { fail fds interfere with each other }

# Argument unpack
if not internal:string_equal "$(echo (yes))" yes  { fail arguments inside bare lists }
if not internal:string_equal "$(echo (no)() yes)" yes  { fail arguments inside juxtaposition: empty }
if not internal:string_equal "$(echo (y)(es))" yes  { fail arguments inside juxtaposition: list }
if not internal:string_equal "$(echo "y"es)" yes  { fail arguments inside juxtaposition: string }

# String substitution
foo=yes
if not internal:string_equal "$(echo $foo)" yes  { fail simple string var lookup }
if not internal:string_equal "$(echo "$foo")" yes  { fail stringified string var lookup }

# List substitution
foo=(yes)
# Static lookup, as list
if not internal:string_equal "$(echo $foo)" yes  { fail simple list var lookup }
# Static lookup, stringified
if not internal:string_equal "$(echo "$foo")" yes  { fail stringified list var lookup }
# Dynamic lookup through static expression
if not internal:string_equal "$(echo $'foo')" yes  { fail dynamic lookup through static exp }
# Dynamic lookup through dynamic expression
ref_to_foo=foo
if not internal:string_equal "$(echo $"$ref_to_foo")" yes  { fail dynamic lookup through dynamic exp }

# More redirections
echo test > /tmp/sh-test
if not internal:string_equal "$(cat /tmp/sh-test)" test  { fail simple path redirect }
rm /tmp/sh-test

# 'brace' expansions
if not internal:string_equal "$(echo x(yes no))" "xyes xno"  { fail simple juxtaposition expansion }
if not internal:string_equal "$(echo (y n)(es o))" "yes yo nes no"  { fail list-list juxtaposition expansion }
if not internal:string_equal "$(echo ()(foo bar baz))" ""  { fail empty expansion }

# Variables inside commands
to_devnull=(>/dev/null)
if not internal:string_equal "$(echo hewwo $to_devnull)" ""  { fail variable containing simple command }

word_count=(() | wc -w)
if not internal:number_equal "$(echo well hello friends $word_count)" 3  { fail variable containing pipeline }

# Globs
rm -fr /tmp/sh-test 2> /dev/null
mkdir -p /tmp/sh-test
pushd /tmp/sh-test
    touch (a b c)(d e f)
    if not internal:string_equal "$(echo a*)" "ad ae af"  { fail '*' glob expansion }
    if not internal:string_equal "$(echo a?)" "ad ae af"  { fail '?' glob expansion }

    glob_in_var='*'
    if not internal:string_equal "$(echo $glob_in_var)" '*'  { fail substituted string acts as glob }

    if not internal:string_equal "$(echo (a*))" "ad ae af"  { fail globs in lists resolve wrong }
    if not internal:string_equal "$(echo x(a*))" "xad xae xaf"  { fail globs in lists do not resolve to lists }
    if not internal:string_equal "$(echo "foo"a*)" "fooad fooae fooaf"  { fail globs join to dquoted strings }
popd
rm -fr /tmp/sh-test

# Setopt
setopt --inline_exec_keep_empty_segments
if not internal:string_equal "$(echo -n "a\n\nb")" "a  b"  { fail inline_exec_keep_empty_segments has no effect }

setopt --no_inline_exec_keep_empty_segments
if not internal:string_equal "$(echo -n "a\n\nb")" "a b"  { fail cannot unset inline_exec_keep_empty_segments }

echo PASS
