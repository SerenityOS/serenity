#!/usr/bin/env -S bash ../.port_include.sh
port=mbedtls
version=2.16.2
files="https://tls.mbed.org/download/mbedtls-${version}-apache.tgz mbedtls-${version}-apache.tgz a6834fcd7b7e64b83dfaaa6ee695198cb5019a929b2806cb0162e049f98206a4"
makeopts="CFLAGS=-DPLATFORM_UTIL_USE_GMTIME"
auth_type=sha256
