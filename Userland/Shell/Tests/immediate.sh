#!/bin/sh

source $(dirname "$0")/test-commons.inc

# Length

if not internal:number_equal "${length foo}" 3 {
  fail invalid length for bareword
}

if not internal:number_equal "${length "foo"}" 3 {
  fail invalid length for literal string
}

if not internal:number_equal "${length foobar}" 6 {
  fail length string always returns 3...\?
}
if not internal:number_equal "${length string foo}" 3 {
  fail invalid length for bareword with explicit string mode
}

if not internal:number_equal "${length list foo}" 1 {
  fail invalid length for bareword with explicit list mode
}

if not internal:number_equal "${length (1 2 3 4)}" 4 {
  fail invalid length for list
}

if not internal:number_equal "${length list (1 2 3 4)}" 4 {
  fail invalid length for list with explicit list mode
}

if not internal:string_equal "${length_across (1 2 3 4)}" "1 1 1 1" {
  fail invalid length_across for list
}

if not internal:string_equal "${length_across list ((1 2 3) (4 5))}" "3 2" {
  fail invalid length_across for list with explicit list mode
}

if not internal:string_equal "${length_across string (foo test)}" "3 4" {
  fail invalid length_across for list of strings
}

# remove_suffix and remove_prefix
if not internal:string_equal "${remove_suffix .txt foo.txt}" "foo" {
  fail remove_suffix did not remove suffix from a single entry
}

if not internal:string_equal "${remove_suffix .txt (foo.txt bar.txt)}" "foo bar" {
  fail remove_suffix did not remove suffix from a list
}

if not internal:string_equal "${remove_prefix fo foo.txt}" "o.txt" {
  fail remove_prefix did not remove prefix from a single entry
}

if not internal:string_equal "${remove_prefix x (xfoo.txt xbar.txt)}" "foo.txt bar.txt" {
  fail remove_prefix did not remove prefix from a list
}

# regex_replace
if not internal:string_equal "${regex_replace a e wall}" "well" {
  fail regex_replace did not replace single character
}

if not internal:string_equal "${regex_replace a e waaaall}" "weeeell" {
  fail regex_replace did not replace multiple characters
}

if not internal:string_equal "${regex_replace '(.)l' 'e\1' hall}" "heal" {
  fail regex_replace did not replace with pattern
}

# split

if not internal:string_equal "${split 'x' "fooxbarxbaz"}" "foo bar baz" {
  fail split could not split correctly
}

if not internal:string_equal "${split '' "abc"}" "a b c" {
  fail split count not split to all characters
}
pass
