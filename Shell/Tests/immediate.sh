fail() {
    echo FAIL: $*
    exit 1
}

a=(1 2 3)
b=(3 4 5)
test ${count $a} -eq 3 || fail \'count\' with single arg returned invalid value
test "${count $a $b}" = "3 3" || fail \'count\' with multiple args returned invalid value\(s\)

test ${length foo} -eq 3 || fail \'length\' of string invalid
a=(foo bar)
# length(list) -> length(String-Join(list))
test ${length $a} -eq 7 || fail \'length\' of list invalid

a=(1 2 3)
b=(3 4 5)
test "${nth 1 $a $b}" = "2 4" || fail \'nth\' invalid
# out of bounds FIXME: should this somehow fail or just be ()?
test "${nth 4 $a}" = "" || fail \'nth\' out of bounds invalid

test "${substring 1 3 'xwhf!'}" = "whf" || fail \'substring\' invalid
# out of bounds FIXME: should this instead somehow fail or just be ''?
test "${substring 1 5 'xwhf!'}" = "whf!" || fail \'substring\' out of bounds invalid

test "${slice 1 3 (x w h f !)}" = "w h f" || fail \'slice\' invalid
# out of bounds FIXME: should this instead somehow fail or just be ()?
test "${slice 1 5 (x w h f !)}" = "w h f !" || fail \'slice\' out of bounds invalid

test "${remove_prefix foo foo.txt foo.sh bar.foo}" = ".txt .sh bar.foo" || fail \'remove_prefix\' invalid

test "${remove_suffix .txt foo.txt bar.txt .txt.sh}" = "foo bar .txt.sh" || fail \'remove_suffix\' invalid

test "${regex_replace 'f(.)\1' '\1\1f' foo fxx bar afoo fyyx}" = "oof xxf bar aoof yyfx" || fail \'regex_replace\' invalid

test "${regex_replace 'f(.)\1' '\1\1f' (foo fxx bar afoo fyyx)}" = "oof xxf bar aoof yyfx" || fail \'regex_replace\' on list invalid

# FIXME: should this skip out on evaluating a literal glob?
test "${filter_glob '*.sh' (a.sh bsh c.txt .sh)}" = "a.sh .sh" || fail \'filter_glob\' invalid
