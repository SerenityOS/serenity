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

# generate a self-signed root certificate
if [ ! -f root/root_cert.pem ]; then
    if [ ! -d root ]; then
        mkdir root
    fi

    openssl req -x509 -newkey rsa:1024 -keyout root/root_key.pem \
        -out root/root_cert.pem -subj "/C=US/O=Example" \
        -config openssl.cnf -reqexts cert_issuer -days 7650 \
        -passin pass:passphrase -passout pass:passphrase
fi

# generate a sele-issued root crl issuer certificate
if [ ! -f root/top_crlissuer_cert.pem ]; then
    if [ ! -d root ]; then
        mkdir root
    fi

    openssl req -newkey rsa:1024 -keyout root/top_crlissuer_key.pem \
        -out root/top_crlissuer_req.pem -subj "/C=US/O=Example" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in root/top_crlissuer_req.pem -extfile openssl.cnf \
        -extensions crl_issuer -CA root/root_cert.pem \
        -CAkey root/root_key.pem -out root/top_crlissuer_cert.pem \
        -CAcreateserial -CAserial root/root_cert.srl -days 7200 \
        -passin pass:passphrase
fi

# generate subca cert issuer and crl iuuser certificates
if [ ! -f subca/subca_cert.pem ]; then
    if [ ! -d subca ]; then
        mkdir subca
    fi

    openssl req -newkey rsa:1024 -keyout subca/subca_key.pem \
        -out subca/subca_req.pem -subj "/C=US/O=Example/OU=Class-1" \
        -days 7650 -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in subca/subca_req.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert.pem \
        -CAkey root/root_key.pem -out subca/subca_cert.pem -CAcreateserial \
        -CAserial root/root_cert.srl -days 7200 -passin pass:passphrase

    openssl req -newkey rsa:1024 -keyout subca/subca_crlissuer_key.pem \
        -out subca/subca_crlissuer_req.pem -subj "/C=US/O=Example/OU=Class-1" \
        -days 7650 -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in subca/subca_crlissuer_req.pem -extfile openssl.cnf \
        -extensions crl_issuer -CA root/root_cert.pem \
        -CAkey root/root_key.pem -out subca/subca_crlissuer_cert.pem \
        -CAcreateserial -CAserial root/root_cert.srl -days 7200 \
        -passin pass:passphrase
fi

# generate dumca cert issuer and crl iuuser certificates
if [ ! -f dumca/dumca_cert.pem ]; then
    if [ ! -d sumca ]; then
        mkdir dumca
    fi

    openssl req -newkey rsa:1024 -keyout dumca/dumca_key.pem \
        -out dumca/dumca_req.pem -subj "/C=US/O=Example/OU=Class-D" \
        -days 7650 -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in dumca/dumca_req.pem -extfile openssl.cnf \
        -extensions cert_issuer -CA root/root_cert.pem \
        -CAkey root/root_key.pem -out dumca/dumca_cert.pem \
        -CAcreateserial -CAserial root/root_cert.srl -days 7200 \
        -passin pass:passphrase

    openssl req -newkey rsa:1024 -keyout dumca/dumca_crlissuer_key.pem \
        -out dumca/dumca_crlissuer_req.pem -subj "/C=US/O=Example/OU=Class-D" \
        -days 7650 -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in dumca/dumca_crlissuer_req.pem \
        -extfile openssl.cnf -extensions crl_issuer -CA root/root_cert.pem \
        -CAkey root/root_key.pem -out dumca/dumca_crlissuer_cert.pem \
        -CAcreateserial -CAserial root/root_cert.srl -days 7200 \
        -passin pass:passphrase
fi

# generate certifiacte for Alice
if [ ! -f subca/alice/alice_cert.pem ]; then
    if [ ! -d subca/alice ]; then
        mkdir -p subca/alice
    fi

    openssl req -newkey rsa:1024 -keyout subca/alice/alice_key.pem \
        -out subca/alice/alice_req.pem \
        -subj "/C=US/O=Example/OU=Class-1/CN=Alice" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in subca/alice/alice_req.pem \
        -extfile openssl.cnf -extensions ee_of_subca \
        -CA subca/subca_cert.pem -CAkey subca/subca_key.pem \
        -out subca/alice/alice_cert.pem -CAcreateserial \
        -CAserial subca/subca_cert.srl -days 7200 -passin pass:passphrase
fi

# generate certifiacte for Bob
if [ ! -f subca/bob/bob_cert.pem ]; then
    if [ ! -d subca/bob ]; then
        mkdir -p subca/bob
    fi

    openssl req -newkey rsa:1024 -keyout subca/bob/bob_key.pem \
        -out subca/bob/bob_req.pem \
        -subj "/C=US/O=Example/OU=Class-1/CN=Bob" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in subca/bob/bob_req.pem \
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

    openssl req -newkey rsa:1024 -keyout subca/susan/susan_key.pem \
        -out subca/susan/susan_req.pem \
        -subj "/C=US/O=Example/OU=Class-1/CN=Susan" -days 7650 \
        -passin pass:passphrase -passout pass:passphrase

    openssl x509 -req -in subca/susan/susan_req.pem -extfile openssl.cnf \
        -extensions ee_of_subca -CA subca/subca_cert.pem \
        -CAkey subca/subca_key.pem -out subca/susan/susan_cert.pem \
        -CAcreateserial -CAserial subca/subca_cert.srl -days 7200 \
        -passin pass:passphrase
fi


# generate the top CRL
if [ ! -f root/top_crl.pem ]; then
    if [ ! -d root ]; then
        mkdir root
    fi

    if [ ! -f root/index.txt ]; then
        touch root/index.txt
        echo 00 > root/crlnumber
    fi

    openssl ca -gencrl -config openssl.cnf -name ca_top -crldays 7000 \
        -crl_reason superseded -keyfile root/top_crlissuer_key.pem \
        -cert root/top_crlissuer_cert.pem -out root/top_crl.pem \
        -passin pass:passphrase
fi

# revoke dumca
openssl ca -revoke dumca/dumca_cert.pem -config openssl.cnf \
        -name ca_top -crl_reason superseded \
        -keyfile root/top_crlissuer_key.pem -cert root/top_crlissuer_cert.pem \
        -passin pass:passphrase

openssl ca -gencrl -config openssl.cnf -name ca_top -crldays 7000 \
        -crl_reason superseded -keyfile root/top_crlissuer_key.pem \
        -cert root/top_crlissuer_cert.pem -out root/top_crl.pem \
        -passin pass:passphrase

# revoke for subca
if [ ! -f subca/subca_crl.pem ]; then
    if [ ! -d subca ]; then
        mkdir subca
    fi

    if [ ! -f subca/index.txt ]; then
        touch subca/index.txt
        echo 00 > subca/crlnumber
    fi

    openssl ca -gencrl -config openssl.cnf -name ca_subca -crldays 7000 \
        -crl_reason superseded -keyfile subca/subca_crlissuer_key.pem \
        -cert subca/subca_crlissuer_cert.pem -out subca/subca_crl.pem \
        -passin pass:passphrase
fi

# revoke susan
openssl ca -revoke subca/susan/susan_cert.pem -config openssl.cnf \
        -name ca_subca -crl_reason superseded \
        -keyfile subca/subca_crlissuer_key.pem \
        -cert subca/subca_crlissuer_cert.pem -passin pass:passphrase

openssl ca -gencrl -config openssl.cnf -name ca_subca -crldays 7000 \
        -crl_reason superseded -keyfile subca/subca_crlissuer_key.pem \
        -cert subca/subca_crlissuer_cert.pem -out subca/subca_crl.pem \
        -passin pass:passphrase
