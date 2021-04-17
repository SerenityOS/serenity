#!/bin/sh

### IF STATEMENT

if false
then
    fail "if false runs false branch"
fi

# then on other line
if true
then
else
    fail "if true runs false branch"
fi


if false

then
    fail "if false runs true branch"
else
fi

# stuff on the same line
if true; then
else fail "if true runs false branch"; fi

if false; then fail "if false runs true branch";
else
fi

# elif

if false; then
    fail "if false runs true branch"
elif true
then
else
    fail "elif true runs false branch"
fi

if false; then
    fail "if false runs true branch"
elif false
then
    fail "elif true runs false branch"
else
fi

echo PASS
