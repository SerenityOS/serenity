#!/bin/sh

singlecommand_ok=yes
multicommand_ok=yes
inlineexec_ok=yes
implicit_ok=yes

# Full form
 # Empty
for x in () { }

 # Empty block but nonempty list
for x in (1 2 3) { }

 # Single command in block
for cmd in ((test 1 = 1) (test 2 = 2)) {
    $cmd || unset singlecommand_ok
}

 # Multiple commands in block
for cmd in ((test 1 = 1) (test 2 = 2)) {
    test -z "$cmd"
    test -z "$cmd" && unset multicommand_ok

}

 # $(...) as iterable expression
test_file=sh-test-1
echo 1 > $test_file
echo 2 >> $test_file
echo 3 >> $test_file
echo 4 >> $test_file
lst=()
for line in $(cat $test_file) {
    lst=($lst $line)
}
test "$lst" = "1 2 3 4" || unset inlineexec_ok
rm $test_file

# Implicit var
for ((test 1 = 1) (test 2 = 2)) {
    $it || unset implicit_ok
}

test $singlecommand_ok || echo Fail: Single command inside for body
test $multicommand_ok || echo Fail: Multiple commands inside for body
test $inlineexec_ok || echo Fail: Inline Exec
test $implicit_ok || echo Fail: implicit iter variable

test "$singlecommand_ok $multicommand_ok $inlineexec_ok $implicit_ok" = "yes yes yes yes" || exit 1
