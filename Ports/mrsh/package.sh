#!/usr/bin/env -S bash ../.port_include.sh
port=mrsh
version=cd3c3a48055ab4085d83f149ff4b4feba40b40cb
files="https://codeload.github.com/emersion/mrsh/legacy.tar.gz/${version} emersion-mrsh-d9763a3.tar.gz 6896493a1020774715ccca28e8d8f4ec722af63a93543fb6dd2762f7b1de9c8a"
auth_type=sha256
useconfigure=true
workdir=emersion-mrsh-d9763a3

export CFLAGS=-Wno-deprecated-declarations
