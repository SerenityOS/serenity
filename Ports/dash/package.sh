#!/bin/bash ../.port_include.sh
port=dash
version=0.5.10.2
useconfigure=true
configopts="--enable-static"
files="http://gondor.apana.org.au/~herbert/dash/files/dash-${version}.tar.gz dash-${version}.tar.gz
http://gondor.apana.org.au/~herbert/dash/files/dash-${version}.tar.gz.sha256sum dash-${version}.tar.gz.sha256sum"
auth_type="sha256"
