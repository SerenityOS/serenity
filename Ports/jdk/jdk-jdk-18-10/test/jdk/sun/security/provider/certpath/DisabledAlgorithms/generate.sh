#
# Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
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
set -e

OPENSSL=openssl

# generate a self-signed root certificate
if [ ! -f root/finished ]; then
    if [ ! -d root ]; then
        mkdir root
    fi

    # SHA1withRSA 1024
    ${OPENSSL} req -x509 -newkey rsa:1024 -keyout root/root_key_1024.pem \
        -out root/root_cert_sha1_1024.pem -subj "/C=US/O=Example" \
        -config openssl.cnf -reqexts cert_issuer -days 7650 -sha1 \
        -passin pass:passphrase -passout pass:passphrase

    # SHA1withRSA 512
    ${OPENSSL} req -x509 -newkey rsa:512 -keyout root/root_key_512.pem \
        -out root/root_cert_sha1_512.pem -subj "/C=US/O=Example" \
        -config openssl.cnf -reqexts cert_issuer -days 7650 -sha1 \
        -passin pass:passphrase -passout pass:passphrase

    # MD2withRSA 2048
    ${OPENSSL} req -x509 -newkey rsa:2048 -keyout root/root_key_2048.pem \
        -out root/root_cert_md2_2048.pem -subj "/C=US/O=Example" \
        -config openssl.cnf -reqexts cert_issuer -days 7650 -md2 \
        -passin pass:passphrase -passout pass:passphrase

    openssl req -newkey rsa:1024 -keyout root/root_crlissuer_key.pem \
        -out root/root_crlissuer_req.pem -subj "/C=US/O=Example" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in root/root_crlissuer_req.pem -extfile openssl.cnf \
        -extensions crl_issuer -CA root/root_cert_sha1_1024.pem \
        -CAkey root/root_key_1024.pem -out root/root_crlissuer_cert.pem \
        -CAcreateserial -CAserial root/root_cert.srl -days 7200 \
        -passin pass:passphrase

    touch root/finished
fi


# generate subca cert issuer
if [ ! -f subca/finished ]; then
    if [ ! -d subca ]; then
        mkdir subca
    fi

    # RSA 1024
    ${OPENSSL} req -newkey rsa:1024 -keyout subca/subca_key_1024.pem \
        -out subca/subca_req_1024.pem -subj "/C=US/O=Example/OU=Class-1" \
        -days 7650 -passin pass:passphrase -passout pass:passphrase

    # RSA 512
    ${OPENSSL} req -newkey rsa:512 -keyout subca/subca_key_512.pem \
        -out subca/subca_req_512.pem -subj "/C=US/O=Example/OU=Class-1" \
        -days 7650 -passin pass:passphrase -passout pass:passphrase

    # SHA1withRSA 1024 signed with RSA 1024
    ${OPENSSL} x509 -req -in subca/subca_req_1024.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert_sha1_1024.pem \
        -CAkey root/root_key_1024.pem -out subca/subca_cert_sha1_1024_1024.pem \
        -CAcreateserial -sha1 \
        -CAserial root/root_cert.srl -days 7200 -passin pass:passphrase

    # SHA1withRSA 1024 signed with RSA 512
    ${OPENSSL} x509 -req -in subca/subca_req_1024.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert_sha1_512.pem \
        -CAkey root/root_key_512.pem -out subca/subca_cert_sha1_1024_512.pem \
        -CAcreateserial -sha1 \
        -CAserial root/root_cert.srl -days 7200 -passin pass:passphrase

    # SHA1withRSA 512 signed with RSA 1024
    ${OPENSSL} x509 -req -in subca/subca_req_512.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert_sha1_1024.pem \
        -CAkey root/root_key_1024.pem -out subca/subca_cert_sha1_512_1024.pem \
        -CAcreateserial -sha1 \
        -CAserial root/root_cert.srl -days 7200 -passin pass:passphrase

    # SHA1withRSA 512 signed with RSA 512
    ${OPENSSL} x509 -req -in subca/subca_req_512.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert_sha1_512.pem \
        -CAkey root/root_key_512.pem -out subca/subca_cert_sha1_512_512.pem \
        -CAcreateserial -sha1 \
        -CAserial root/root_cert.srl -days 7200 -passin pass:passphrase

    # MD2withRSA 1024 signed with RSA 1024
    ${OPENSSL} x509 -req -in subca/subca_req_1024.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert_sha1_1024.pem \
        -CAkey root/root_key_1024.pem -out subca/subca_cert_md2_1024_1024.pem \
        -CAcreateserial -md2 \
        -CAserial root/root_cert.srl -days 7200 -passin pass:passphrase

    # MD2withRSA 1024 signed with RSA 512
    ${OPENSSL} x509 -req -in subca/subca_req_1024.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert_sha1_512.pem \
        -CAkey root/root_key_512.pem -out subca/subca_cert_md2_1024_512.pem \
        -CAcreateserial -md2 \
        -CAserial root/root_cert.srl -days 7200 -passin pass:passphrase

    openssl req -newkey rsa:1024 -keyout subca/subca_crlissuer_key.pem \
        -out subca/subca_crlissuer_req.pem -subj "/C=US/O=Example/OU=Class-1" \
        -days 7650 -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in subca/subca_crlissuer_req.pem -extfile openssl.cnf \
        -extensions crl_issuer -CA root/root_cert_sha1_1024.pem \
        -CAkey root/root_key_1024.pem -out subca/subca_crlissuer_cert.pem \
        -CAcreateserial -CAserial root/root_cert.srl -days 7200 \
        -passin pass:passphrase

    touch subca/finished
fi


# generate certifiacte for Alice
if [ ! -f subca/alice/finished ]; then
    if [ ! -d subca/alice ]; then
        mkdir -p subca/alice
    fi

    # RSA 1024
    ${OPENSSL} req -newkey rsa:1024 -keyout subca/alice/alice_key_1024.pem \
        -out subca/alice/alice_req_1024.pem \
        -subj "/C=US/O=Example/OU=Class-1/CN=Alice" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    # RSA 512
    ${OPENSSL} req -newkey rsa:512 -keyout subca/alice/alice_key_512.pem \
        -out subca/alice/alice_req_512.pem \
        -subj "/C=US/O=Example/OU=Class-1/CN=Alice" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    # SHA1withRSA 1024 signed with RSA 1024
    ${OPENSSL} x509 -req -in subca/alice/alice_req_1024.pem \
        -extfile openssl.cnf -extensions ee_of_subca \
        -CA subca/subca_cert_sha1_1024_1024.pem \
        -CAkey subca/subca_key_1024.pem \
        -out subca/alice/alice_cert_sha1_1024_1024.pem -CAcreateserial -sha1 \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase

    # SHA1withRSA 1024 signed with RSA 512
    ${OPENSSL} x509 -req -in subca/alice/alice_req_1024.pem \
        -extfile openssl.cnf -extensions ee_of_subca \
        -CA subca/subca_cert_sha1_512_1024.pem \
        -CAkey subca/subca_key_512.pem \
        -out subca/alice/alice_cert_sha1_1024_512.pem -CAcreateserial -sha1 \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase

    # SHA1withRSA 512 signed with RSA 1024
    ${OPENSSL} x509 -req -in subca/alice/alice_req_512.pem \
        -extfile openssl.cnf -extensions ee_of_subca \
        -CA subca/subca_cert_sha1_1024_1024.pem \
        -CAkey subca/subca_key_1024.pem \
        -out subca/alice/alice_cert_sha1_512_1024.pem -CAcreateserial -sha1 \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase

    # SHA1withRSA 512 signed with RSA 512
    ${OPENSSL} x509 -req -in subca/alice/alice_req_512.pem \
        -extfile openssl.cnf -extensions ee_of_subca \
        -CA subca/subca_cert_sha1_512_1024.pem \
        -CAkey subca/subca_key_512.pem \
        -out subca/alice/alice_cert_sha1_512_512.pem -CAcreateserial -sha1 \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase

    # MD2withRSA 1024 signed with RSA 1024
    ${OPENSSL} x509 -req -in subca/alice/alice_req_1024.pem \
        -extfile openssl.cnf -extensions ee_of_subca \
        -CA subca/subca_cert_sha1_1024_1024.pem \
        -CAkey subca/subca_key_1024.pem \
        -out subca/alice/alice_cert_md2_1024_1024.pem -CAcreateserial -md2 \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase

    # MD2withRSA 1024 signed with RSA 512
    ${OPENSSL} x509 -req -in subca/alice/alice_req_1024.pem \
        -extfile openssl.cnf -extensions ee_of_subca \
        -CA subca/subca_cert_sha1_512_1024.pem \
        -CAkey subca/subca_key_512.pem \
        -out subca/alice/alice_cert_md2_1024_512.pem -CAcreateserial -md2 \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase

    touch subca/alice/finished
fi

if [ ! -f root/revoked ]; then
    if [ ! -d root ]; then
        mkdir root
    fi

    if [ ! -f root/index.txt ]; then
        touch root/index.txt
        echo 00 > root/crlnumber
    fi

    openssl ca -gencrl -config openssl.cnf -name ca_top -crldays 7000 -md sha1 \
        -crl_reason superseded -keyfile root/root_crlissuer_key.pem \
        -cert root/root_crlissuer_cert.pem -out root/top_crl.pem \
        -passin pass:passphrase

    touch root/revoked
fi

if [ ! -f subca/revoked ]; then
    if [ ! -d subca ]; then
        mkdir subca
    fi

    if [ ! -f subca/index.txt ]; then
        touch subca/index.txt
        echo 00 > subca/crlnumber
    fi

    # revoke alice's SHA1withRSA 1024 signed with RSA 1024
    openssl ca -revoke subca/alice/alice_cert_sha1_1024_1024.pem \
        -config openssl.cnf \
        -name ca_subca -crl_reason superseded \
        -keyfile subca/subca_crlissuer_key.pem \
        -cert subca/subca_crlissuer_cert.pem -passin pass:passphrase

    openssl ca -gencrl -config openssl.cnf \
        -name ca_subca -crldays 7000 -md md2 \
        -crl_reason superseded -keyfile subca/subca_crlissuer_key.pem \
        -cert subca/subca_crlissuer_cert.pem \
        -out subca/subca_crl.pem \
        -passin pass:passphrase

    touch subca/revoked
fi
