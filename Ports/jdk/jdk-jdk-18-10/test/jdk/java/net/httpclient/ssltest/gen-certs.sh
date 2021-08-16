# Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

# Generate OpenSSL configuration file
echo "[req]" > openssl.conf
echo "distinguished_name = dn" >> openssl.conf
echo "x509_extensions = v3_ext" >> openssl.conf
echo "[dn]" >> openssl.conf
echo "[v3_ext]" >> openssl.conf
echo "subjectKeyIdentifier = hash" >> openssl.conf
echo "authorityKeyIdentifier = keyid" >> openssl.conf
echo "basicConstraints = critical,CA:FALSE" >> openssl.conf

# Generate X.509 version 3 extension file
echo "subjectKeyIdentifier = hash" > v3.ext
echo "authorityKeyIdentifier = keyid,issuer" >> v3.ext

# Generate good cert
openssl genpkey -algorithm rsa -pkeyopt rsa_keygen_bits:2048 -pkeyopt rsa_keygen_pubexp:65537 -out good.key
openssl req -config openssl.conf -new -key good.key -subj "/CN=localhost" -sha256 -out good.csr
openssl x509 -extfile v3.ext -req -CAcreateserial -days 3650 -in good.csr -sha256 -signkey good.key -out good.cer

# Generate bad cert
openssl genpkey -algorithm rsa -pkeyopt rsa_keygen_bits:2048 -pkeyopt rsa_keygen_pubexp:65537 -out bad.key
openssl req -config openssl.conf -new -key bad.key -subj "/CN=evil" -sha256 -out bad.csr
openssl x509 -extfile v3.ext -req -CAcreateserial -days 3650 -in bad.csr -sha256 -signkey bad.key -out bad.cer

# Generate loopback cert with subject alternative name
echo "subjectAltName = @alt_names" >> v3.ext
echo "[alt_names]" >> v3.ext
echo "IP.1 = 127.0.0.1" >> v3.ext

openssl genpkey -algorithm rsa -pkeyopt rsa_keygen_bits:2048 -pkeyopt rsa_keygen_pubexp:65537 -out loopback.key
openssl req -config openssl.conf -new -key loopback.key -subj "/CN=unknown" -sha256 -out loopback.csr
openssl x509 -extfile v3.ext -req -CAcreateserial -days 3650 -in loopback.csr -sha256 -signkey loopback.key -out loopback.cer
