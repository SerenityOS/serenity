#!/usr/bin/env bash

echo "include_dir=$SERENITY_BUILD_DIR/Root/usr/include" > libc_installation.txt
echo "sys_include_dir=$SERENITY_BUILD_DIR/Root/usr/include" >> libc_installation.txt
echo "crt_dir=$SERENITY_BUILD_DIR/Root/usr/lib" >> libc_installation.txt
echo "msvc_lib_dir=" >> libc_installation.txt
echo "kernel32_lib_dir=" >> libc_installation.txt
echo "gcc_dir=" >> libc_installation.txt
