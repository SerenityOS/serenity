#!/usr/bin/env -S bash ../.port_include.sh
port=tcl
version=8.6.12
workdir=tcl${version}/unix
useconfigure=true
files="https://prdownloads.sourceforge.net/tcl/tcl${version}-src.tar.gz tcl${version}.tar.gz 26c995dd0f167e48b11961d891ee555f680c175f7173ff8cb829f4ebcde4c1a6"
auth_type=sha256
