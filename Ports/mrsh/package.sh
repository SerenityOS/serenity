#!/usr/bin/env -S bash ../.port_include.sh
port=mrsh
version=d9763a32e7da572677d1681bb1fc67f117d641f3
files="https://codeload.github.com/emersion/mrsh/legacy.tar.gz/${version} emersion-mrsh-d9763a3.tar.gz 80d268ebf0fca32293605b6e91bc64d0"
auth_type=md5
useconfigure=true
makeopts=
workdir=emersion-mrsh-d9763a3

export CFLAGS=-Wno-deprecated-declarations
