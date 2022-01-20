#!/usr/bin/env -S bash ../.port_include.sh

port=readline
version=8.1
useconfigure=true
config_sub_path=support/config.sub
use_fresh_config_sub=true
files="https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz readline-${version}.tar.gz f8ceb4ee131e3232226a17f51b164afc46cd0b9e6cef344be87c65962cb82b02"
auth_type=sha256
