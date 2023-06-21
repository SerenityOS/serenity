#!/bin/Shell

source $(dirname "$0")/test-commons.inc

setopt --verbose

if not test "$(echo {a,b,})" = "a b " {  fail normal brace expansion with one empty slot }
if not test "$(echo {a,,b})" = "a  b" {  fail normal brace expansion with one empty slot }
if not test "$(echo {a,,,b})" = "a   b" {  fail normal brace expansion with two empty slots }
if not test "$(echo {a,b,,})" = "a b  " {  fail normal brace expansion with two empty slots }

if not test "$(echo {a..c})" = "a b c" {  fail range brace expansion, alpha }
if not test "$(echo {0..3})" = "0 1 2 3" {  fail range brace expansion, number }
if not test "$(echo {😂..😄})" = "😂 😃 😄" {  fail range brace expansion, unicode code point }

# Make sure that didn't mess with dots and commas in normal barewords
if not test .. = ".." {  fail range brace expansion delimiter affects normal barewords }
if not test , = "," {  fail normal brace expansion delimiter affects normal barewords }

echo PASS
