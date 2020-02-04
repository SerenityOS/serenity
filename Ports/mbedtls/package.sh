#!/bin/bash ../.port_include.sh
port=mbedtls
version=2.16.2
files="https://tls.mbed.org/download/mbedtls-${version}-apache.tgz mbedtls-${version}-apache.tgz ba809acfd4b41b86895b92e98d936695b5b62b73"
makeopts="CFLAGS=-DPLATFORM_UTIL_USE_GMTIME"
auth_type="sha1"