/*	$NetBSD: pkcs7.c,v 1.7 2018/04/05 21:19:32 sevan Exp $	*/
#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif

__RCSID("$NetBSD: pkcs7.c,v 1.7 2018/04/05 21:19:32 sevan Exp $");

/*-
 * Copyright (c) 2004, 2008 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Love Hörnquist Åstrand <lha@it.su.se>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#if HAVE_ERR_H
#include <err.h>
#endif

#include <openssl/pkcs7.h>
#include <openssl/evp.h>
#include <openssl/x509.h>
#include <openssl/x509v3.h>
#include <openssl/pem.h>
#include <openssl/err.h>

#include "lib.h"

#ifndef NS_ANY_CA
#define NS_ANY_CA		(NS_SSL_CA|NS_SMIME_CA|NS_OBJSIGN_CA)
#endif

#if !defined(OPENSSL_VERSION_NUMBER) || (OPENSSL_VERSION_NUMBER < 0x10100000L) || \
    defined(LIBRESSL_VERSION_NUMBER)
#define X509_get_extended_key_usage(x) x->ex_xkusage
#define X509_get_extension_flags(x) x->ex_flags
#endif

static const unsigned int pkg_key_usage = XKU_CODE_SIGN | XKU_SMIME;

static STACK_OF(X509) *
file_to_certs(const char *file)
{
	unsigned long ret;
	STACK_OF(X509) *certs;
	FILE *f;

	if ((f = fopen(file, "r")) == NULL) {
		warn("open failed %s", file);
		return NULL;
	}

	certs = sk_X509_new_null();
	for (;;) {
		X509 *cert;

		cert = PEM_read_X509(f, NULL, NULL, NULL);
		if (cert == NULL) {
			ret = ERR_GET_REASON(ERR_peek_error());
			if (ret == PEM_R_NO_START_LINE) {
				/* End of file reached. no error */
				ERR_clear_error();
				break;
			}
			sk_X509_free(certs);
			warnx("Can't read certificate in file: %s", file);
			fclose(f);
			return NULL;
		}
		sk_X509_insert(certs, cert, sk_X509_num(certs));
	}

	fclose(f);

	if (sk_X509_num(certs) == 0) {
		sk_X509_free(certs);
		certs = NULL;
		warnx("No certificate found in file %s", file);
	}

	return certs;
}

int
easy_pkcs7_verify(const char *content, size_t len,
    const char *signature, size_t signature_len,
    const char *anchor, int is_pkg)
{
	STACK_OF(X509) *cert_chain, *signers;
	X509_STORE *store;
	BIO *sig, *in;
	PKCS7 *p7;
	int i, status;
	X509_NAME *name;
	char *subject;

	OpenSSL_add_all_algorithms();
	ERR_load_crypto_strings();

	status = -1;

	if (cert_chain_file)
		cert_chain = file_to_certs(cert_chain_file);
	else
		cert_chain = NULL;

	store = X509_STORE_new();
	if (store == NULL) {
		sk_X509_free(cert_chain);
		warnx("Failed to create certificate store");
		return -1;
	}

	X509_STORE_load_locations(store, anchor, NULL);

	in = BIO_new_mem_buf(__UNCONST(content), len);
	sig = BIO_new_mem_buf(__UNCONST(signature), signature_len);
	signers = NULL;

	p7 = PEM_read_bio_PKCS7(sig, NULL, NULL, NULL);
	if (p7 == NULL) {
		warnx("Failed to parse the signature");
		goto cleanup;
	}

	if (PKCS7_verify(p7, cert_chain, store, in, NULL, 0) != 1) {
		warnx("Failed to verify signature");
		goto cleanup;
	}

	signers = PKCS7_get0_signers(p7, NULL, 0);
	if (signers == NULL) {
		warnx("Failed to get signers");
		goto cleanup;
	}
    
	if (sk_X509_num(signers) == 0) {
		warnx("No signers found");
		goto cleanup;
	}

	for (i = 0; i < sk_X509_num(signers); i++) {
		/* Compute ex_xkusage */
		X509_check_purpose(sk_X509_value(signers, i), -1, -1);

		if (X509_check_ca(sk_X509_value(signers, i))) {
			warnx("CA keys are not valid for signatures");
			goto cleanup;
		}
		if (is_pkg) {
			if (X509_get_extended_key_usage(sk_X509_value(signers, i)) != pkg_key_usage) {
				warnx("Certificate must have CODE SIGNING "
				    "and EMAIL PROTECTION property");
				goto cleanup;
			}
		} else {
			if (X509_get_extension_flags(sk_X509_value(signers, i)) & EXFLAG_XKUSAGE) {
				warnx("Certificate must not have any property");
				goto cleanup;
			}
		}
	}

	printf("Sigature ok, signed by:\n");

	for (i = 0; i < sk_X509_num(signers); i++) {
		name = X509_get_subject_name(sk_X509_value(signers, i));
		subject = X509_NAME_oneline(name, NULL, 0);

		printf("\t%s\n", subject);

		OPENSSL_free(subject);
	}

	status = 0;

cleanup:
	sk_X509_free(cert_chain);
	sk_X509_free(signers);
	X509_STORE_free(store);

	PKCS7_free(p7);
	BIO_free(in);
	BIO_free(sig);

	return status;
}

static int
ssl_pass_cb(char *buf, int size, int rwflag, void *u)
{

	if (EVP_read_pw_string(buf, size, "Passphrase :", 0)) {
#if OPENSSL_VERSION >= 0x0090608fL
		OPENSSL_cleanse(buf, size);
#else
		memset(buf, 0, size);
#endif
		return 0;
	}
	return strlen(buf);
}

int
easy_pkcs7_sign(const char *content, size_t len,
    char **signature, size_t *signature_len,
    const char *key_file, const char *cert_file)
{
	FILE *f;
	X509 *certificate;
	STACK_OF(X509) *c, *cert_chain;
	EVP_PKEY *private_key;
	char *tmp_sig;
	BIO *out, *in;
	PKCS7 *p7;
	int status;

	OpenSSL_add_all_algorithms();
	ERR_load_crypto_strings();

	status = -1;
	private_key = NULL;
	cert_chain = NULL;
	in = NULL;

	c = file_to_certs(cert_file);

	if (sk_X509_num(c) != 1) {
		warnx("More then one certificate in the certificate file");
		goto cleanup;
	}
	certificate = sk_X509_value(c, 0);

	/* Compute ex_kusage */
	X509_check_purpose(certificate, -1, 0);

	if (X509_check_ca(certificate)) {
		warnx("CA keys are not valid for signatures");
		goto cleanup;
	}

	if (X509_get_extended_key_usage(certificate) != pkg_key_usage) {
		warnx("Certificate must have CODE SIGNING "
		    "and EMAIL PROTECTION property");
		goto cleanup;
	}

	if (cert_chain_file)
		cert_chain = file_to_certs(cert_chain_file);

	if ((f = fopen(key_file, "r")) == NULL) {
		warn("Failed to open private key file %s", key_file);
		goto cleanup;
	}
	private_key = PEM_read_PrivateKey(f, NULL, ssl_pass_cb, NULL);
	fclose(f);
	if (private_key == NULL) {
		warnx("Can't read private key: %s", key_file);
		goto cleanup;
	}

	if (X509_check_private_key(certificate, private_key) != 1) {
		warnx("The private key %s doesn't match the certificate %s",
		    key_file, cert_file);
		goto cleanup;
	}

	in = BIO_new_mem_buf(__UNCONST(content), len);

	p7 = PKCS7_sign(certificate, private_key, cert_chain, in, 
	    PKCS7_DETACHED|PKCS7_NOATTR|PKCS7_BINARY);
	if (p7 == NULL) {
		warnx("Failed to create signature structure");
		goto cleanup;
	}

	out = BIO_new(BIO_s_mem());
	PEM_write_bio_PKCS7(out, p7);
	*signature_len = BIO_get_mem_data(out, &tmp_sig);
	*signature = xmalloc(*signature_len);
	memcpy(*signature, tmp_sig, *signature_len);
	BIO_free_all(out);

	PKCS7_free(p7);

	status = 0;

cleanup:
	sk_X509_free(c);
	sk_X509_free(cert_chain);
	EVP_PKEY_free(private_key);
	BIO_free(in);

	return status;
}
