#!/bin/sh

singlecommand_ok=yes
multicommand_ok=yes
inlineexec_ok=yes
implicit_ok=yes
infinite_ok=''
break_ok=yes
continue_ok=yes
break_in_infinite_ok=''

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

# Infinite loop
loop {
    infinite_ok=yes
    break
    unset break_ok
}

# 'Continue'
for (1 2 3) {
    continue
    unset continue_ok
}

# 'break' in infinite external loop
for $(yes) {
    break_in_infinite_ok=yes
    break
}

test $singlecommand_ok || echo Fail: Single command inside for body
test $multicommand_ok || echo Fail: Multiple commands inside for body
test $inlineexec_ok || echo Fail: Inline Exec
test $implicit_ok || echo Fail: implicit iter variable
test $infinite_ok || echo Fail: infinite loop
test $break_ok || echo Fail: break
test $continue_ok || echo Fail: continue
test $break_in_infinite_ok || echo Fail: break from external infinite loop

test "$singlecommand_ok $multicommand_ok $inlineexec_ok $implicit_ok $infinite_ok $break_ok $continue_ok $break_in_infinite_ok" = "yes yes yes yes yes yes yes yes" || exit 1
