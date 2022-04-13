#!/usr/bin/env -S bash ../.port_include.sh

port=readline
version=8.1.2
useconfigure=true
config_sub_path=support/config.sub
use_fresh_config_sub=true
files="https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz readline-${version}.tar.gz 7589a2381a8419e68654a47623ce7dfcb756815c8fee726b98f90bf668af7bc6"
auth_type=sha256
