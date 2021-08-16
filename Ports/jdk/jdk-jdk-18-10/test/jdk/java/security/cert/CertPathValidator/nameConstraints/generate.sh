#
# Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
#

#!/bin/ksh
#
# needs ksh to run the script.
OPENSSL=openssl

# generate a self-signed root certificate
if [ ! -f root/root_cert.pem ]; then
    if [ ! -d root ]; then
        mkdir root
    fi

    ${OPENSSL} req -x509 -newkey rsa:1024 -keyout root/root_key.pem \
        -out root/root_cert.pem -subj "/C=US/O=Example" \
        -config openssl.cnf -reqexts cert_issuer -days 7650 \
        -passin pass:passphrase -passout pass:passphrase
fi

# generate subca cert issuer
if [ ! -f subca/subca_cert.pem ]; then
    if [ ! -d subca ]; then
        mkdir subca
    fi

    ${OPENSSL} req -newkey rsa:1024 -keyout subca/subca_key.pem \
        -out subca/subca_req.pem -subj "/C=US/O=Example/OU=Class-1" \
        -days 7650 -passin pass:passphrase -passout pass:passphrase

    ${OPENSSL} x509 -req -in subca/subca_req.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert.pem \
        -CAkey root/root_key.pem -out subca/subca_cert.pem -CAcreateserial \
        -CAserial root/root_cert.srl -days 7200 -passin pass:passphrase
fi

# generate certifiacte for Alice
if [ ! -f subca/alice/alice_cert.pem ]; then
    if [ ! -d subca/alice ]; then
        mkdir -p subca/alice
    fi

    ${OPENSSL} req -newkey rsa:1024 -keyout subca/alice/alice_key.pem \
        -out subca/alice/alice_req.pem \
        -subj "/C=US/O=Example/OU=Class-1/CN=Alice" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    ${OPENSSL} x509 -req -in subca/alice/alice_req.pem \
        -extfile openssl.cnf -extensions alice_of_subca \
        -CA subca/subca_cert.pem -CAkey subca/subca_key.pem \
        -out subca/alice/alice_cert.pem -CAcreateserial \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase
fi

# generate certifiacte for Bob
if [ ! -f subca/bob/bob.pem ]; then
    if [ ! -d subca/bob ]; then
        mkdir -p subca/bob
    fi

    ${OPENSSL} req -newkey rsa:1024 -keyout subca/bob/bob_key.pem \
        -out subca/bob/bob_req.pem \
        -subj "/C=US/O=Example/OU=Class-1/CN=Bob" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    ${OPENSSL} x509 -req -in subca/bob/bob_req.pem \
        -extfile openssl.cnf -extensions ee_of_subca \
        -CA subca/subca_cert.pem -CAkey subca/subca_key.pem \
        -out subca/bob/bob_cert.pem -CAcreateserial \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase
fi

# generate certifiacte for Susan
if [ ! -f subca/susan/susan_cert.pem ]; then
    if [ ! -d subca/susan ]; then
        mkdir -p subca/susan
    fi

    ${OPENSSL} req -newkey rsa:1024 -keyout subca/susan/susan_key.pem \
        -out subca/susan/susan_req.pem \
        -subj "/C=US/O=Example/OU=Class-1/CN=Susan" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    ${OPENSSL} x509 -req -in subca/susan/susan_req.pem \
        -extfile openssl.cnf -extensions susan_of_subca \
        -CA subca/subca_cert.pem -CAkey subca/subca_key.pem \
        -out subca/susan/susan_cert.pem -CAcreateserial \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase
fi

