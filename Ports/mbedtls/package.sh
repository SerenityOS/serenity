#!/bin/sh ../.port_include.sh
port=mbedtls
version=2.16.2
files="https://tls.mbed.org/download/mbedtls-2.16.2-apache.tgz mbedtls-2.16.2-apache.tgz"
makeopts="CFLAGS=-DPLATFORM_UTIL_USE_GMTIME"
