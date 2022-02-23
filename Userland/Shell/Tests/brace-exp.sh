#!/bin/sh

source $(dirname "$0")/test-commons.inc

setopt --verbose

if not internal:string_equal "$(echo {a,b,})" "a b " {  fail normal brace expansion with one empty slot }
if not internal:string_equal "$(echo {a,,b})" "a  b" {  fail normal brace expansion with one empty slot }
if not internal:string_equal "$(echo {a,,,b})" "a   b" {  fail normal brace expansion with two empty slots }
if not internal:string_equal "$(echo {a,b,,})" "a b  " {  fail normal brace expansion with two empty slots }

if not internal:string_equal "$(echo {a..c})" "a b c" {  fail range brace expansion, alpha }
if not internal:string_equal "$(echo {0..3})" "0 1 2 3" {  fail range brace expansion, number }
if not internal:string_equal "$(echo {😂..😄})" "😂 😃 😄" {  fail range brace expansion, unicode code point }

# Make sure that didn't mess with dots and commas in normal barewords
if not internal:string_equal .. ".." {  fail range brace expansion delimiter affects normal barewords }
if not internal:string_equal , "," {  fail normal brace expansion delimiter affects normal barewords }

echo PASS
