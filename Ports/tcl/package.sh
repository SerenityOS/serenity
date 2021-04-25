#!/usr/bin/env -S bash ../.port_include.sh
port=tcl
version=8.6.11
workdir=tcl${version}/unix
useconfigure=true
files="https://prdownloads.sourceforge.net/tcl/tcl${version}-src.tar.gz tcl${version}.tar.gz 8c0486668586672c5693d7d95817cb05a18c5ecca2f40e2836b9578064088258"
auth_type=sha256
