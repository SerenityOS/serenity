#!/bin/Shell

source $(dirname "$0")/test-commons.inc


if not [ "$(type ls)" = "ls is $(which ls)" ] { fail "'type' on a binary not working" }

if not [ "$(type pwd)" = "pwd is a shell builtin" ] { fail "'type' on a builtin not working" }

f() { ls }

if not [ "$(type f)" = "f is a function f() { ls }" ] { fail "'type' on a function not working" }


if not [ "$(type -f f)" = "f is a function" ] { fail "'type' on a function not working with -f" }

alias l=ls

if not [ "$(type l)" = "l is aliased to `ls`" ] { fail "'type' on a alias not working" }


if not [ "$(type l ls)" = "l is aliased to `ls` ls is $(which ls)" ] { fail "'type' on multiple commands not working" }

echo PASS
