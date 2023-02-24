#!/bin/Shell

source $(dirname "$0")/test-commons.inc
# go to a writable directory
cd /tmp

# Simple usage, single doc
echo <<-test > sh.doc.test
this is a test
test
if test "$(cat sh.doc.test)" != "this is a test" {
    fail "Could not use normal interpolated heredoc"
}

echo <<-'test' > sh.doc.test
this is a test
test
if test "$(cat sh.doc.test)" != "this is a test" {
    fail "Could not use normal non-interpolated heredoc"
}

echo <<~test > sh.doc.test
    this is a test
test
if test "$(cat sh.doc.test)" != "this is a test" {
    fail "Could not use normal dedented heredoc"
}

echo <<~'test' > sh.doc.test
    this is a test
test
if test "$(cat sh.doc.test)" != "this is a test" {
    fail "Could not use normal non-interpolated dedented heredoc"
}

var=test
echo <<-test > sh.doc.test
this is a $var
test
if test "$(cat sh.doc.test)" != "this is a test" {
    fail "Could not use interpolated heredoc with interpolation"
}

echo <<~test > sh.doc.test
    this is a $var
test
if test "$(cat sh.doc.test)" != "this is a test" {
    fail "Could not use dedented interpolated heredoc with interpolation"
}

# Multiple heredocs
echo <<-test <<-test2 > sh.doc.test
contents for test
test
contents for test2
test2
if test "$(cat sh.doc.test)" != "contents for test contents for test2" {
    fail "Could not use two heredocs"
}

# Why would you do this you crazy person?
if test "$(echo <<~text)" != "test" {
                test
                text
    fail "Could not use heredocs in a weird place"
}

# Now let's try something _really_ weird!
if test "$(echo <<~test1)" != "$(echo <<~test2)" { fail "The parser forgot about heredocs after a block, oops" }
test
test1
test
test2

rm -f sh.doc.test
# return to original directory
cd -
pass
