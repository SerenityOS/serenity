#!/bin/Shell

source $(dirname "$0")/test-commons.inc

result=no
match hello {
    he* { result=yes }
    * { result=fail }
};

if not test "$result" = yes { fail invalid result $result for normal string match, single option }

result=no
match hello {
    he* | f* { result=yes }
    * { result=fail }
};

if not test "$result" = yes { fail invalid result $result for normal string match, multiple options }

result=no
match (well hello friends) {
    (* *) { result=fail }
    (* * *) { result=yes }
    * { result=fail }
};

if not test "$result" = yes { fail invalid result $result for list match }

result=no
match yes as v {
    () { result=fail }
    (*) { result=yes }
    * { result=$v }
};

if not test "$result" = yes { fail invalid result $result for match with name }

result=no
# $(...) is a list, $(echo) should be an empty list, not an empty string
match $(echo) {
    * { result=fail }
    () { result=yes }
};

if not test "$result" = yes { fail invalid result $result for list subst match }

result=no
# "$(...)" is a string, "$(echo)" should be an empty string, not an empty list
match "$(echo)" {
    * { result=yes }
    () { result=fail }
};

if not test "$result" = yes { fail invalid result $result for string subst match }

match (foo bar) {
    (f? *) as (x y) {
        result=fail
    }
    (f* b*) as (x y) {
        if [ "$x" = oo -a "$y" = ar ] {
            result=yes
        } else {
            result=fail
        }
    }
}

if not test "$result" = yes { fail invalid result $result for subst match with name }

match (foo bar baz) {
    (f? * *z) as (x y z) {
        result=fail
    }
    (f* b* *z) as (x y z) {
        if [ "$x" = oo -a "$y" = ar -a "$z" = ba ] {
            result=yes
        } else {
            result=fail
        }
    }
}

if not test "$result" = yes { fail invalid result $result for subst match with name 2 }

echo PASS
