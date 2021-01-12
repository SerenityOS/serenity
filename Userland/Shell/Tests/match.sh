#!/bin/Shell

result=no
match hello {
    he* { result=yes }
    * { result=fail }
};

test "$result" = yes || echo invalid result $result for normal string match, single option && exit 1

result=no
match hello {
    he* | f* { result=yes }
    * { result=fail }
};

test "$result" = yes || echo invalid result $result for normal string match, multiple options && exit 1

result=no
match (well hello friends) {
    (* *) { result=fail }
    (* * *) { result=yes }
    * { result=fail }
};

test "$result" = yes || echo invalid result $result for list match && exit 1

result=no
match yes as v {
    () { result=fail }
    (*) { result=yes }
    * { result=$v }
};

test "$result" = yes || echo invalid result $result for match with name && exit 1

result=no
# $(...) is a list, $(echo) should be an empty list, not an empty string
match $(echo) {
    * { result=fail }
    () { result=yes }
};

test "$result" = yes || echo invalid result $result for list subst match && exit 1

result=no
# "$(...)" is a string, "$(echo)" should be an empty string, not an empty list
match "$(echo)" {
    * { result=yes }
    () { result=fail }
};

test "$result" = yes || echo invalid result $result for string subst match && exit 1

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

test "$result" = yes || echo invalid result $result for subst match with name && exit 1

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

test "$result" = yes || echo invalid result $result for subst match with name 2 && exit 1
