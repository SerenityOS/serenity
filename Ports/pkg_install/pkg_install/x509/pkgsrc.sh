#!/bin/sh
#
# $NetBSD: pkgsrc.sh,v 1.2 2009/02/02 12:49:16 joerg Exp $
#

CA="openssl ca -config pkgsrc.cnf"
REQ="openssl req -config pkgsrc.cnf"

set -e

new_ca() {
	if [ -f $1/serial ]; then
		echo "CA already exists, exiting" >& 2
		exit 1
	fi

	mkdir -p $1/certs $1/crl $1/newcerts $1/private
	echo "00" > $1/serial
	touch $1/index.txt

	echo "Making CA certificate ..."
	$REQ -new -keyout $1/private/cakey.pem \
		   -out $1/careq.pem
	$CA -out $1/cacert.pem -batch \
		   -keyfile $1/private/cakey.pem -selfsign \
		   -infiles $1/careq.pem
}

new_pkgkey() {
	$REQ -new -keyout pkgkey_key.pem -out pkgkey_req.pem
	$CA -extensions pkgkey -policy policy_match -out pkgkey_cert.pem -infiles pkgkey_req.pem
	rm pkgkey_req.pem
	echo "Signed certificate is in pkgkey_cert.pem, key in pkgkey_key.pem"
}

new_pkgsec() {
	$REQ -new -keyout pkgsec_key.pem -out pkgsec_req.pem
	$CA -extensions pkgsec -policy policy_match -out pkgsec_cert.pem -infiles pkgsec_req.pem
	rm pkgsec_req.pem
	echo "Signed certificate is in pkgsec_cert.pem, key in pkgsec_key.pem"
}

usage() {
	echo "$0:"
	echo "setup - create new CA in ./pkgsrc for use by pkg_install"
	echo "pkgkey - create and sign a certificate for binary packages"
	echo "pkgsec - create and sign a certificate for pkg-vulnerabilities"
}

case "$1" in
setup)
	new_ca ./pkgsrc
	;;
pkgkey)
	new_pkgkey
	;;
pkgsec)
	new_pkgsec
	;;
*)
	usage
	;;
esac
