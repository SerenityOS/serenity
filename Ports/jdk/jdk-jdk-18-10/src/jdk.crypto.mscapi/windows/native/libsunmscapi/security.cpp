/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

//=--------------------------------------------------------------------------=
// security.cpp    by Stanley Man-Kit Ho
//=--------------------------------------------------------------------------=
//

#include <jni.h>
#include "jni_util.h"
#include <stdlib.h>
#include <string.h>
#include <windows.h>
#include <BaseTsd.h>
#include <wincrypt.h>
#include <stdio.h>
#include <memory>
#include "sun_security_mscapi_CKey.h"
#include "sun_security_mscapi_CKeyStore.h"
#include "sun_security_mscapi_PRNG.h"
#include "sun_security_mscapi_CRSACipher.h"
#include "sun_security_mscapi_CKeyPairGenerator_RSA.h"
#include "sun_security_mscapi_CPublicKey.h"
#include "sun_security_mscapi_CPublicKey_CRSAPublicKey.h"
#include "sun_security_mscapi_CSignature.h"
#include "sun_security_mscapi_CSignature_RSA.h"

#define OID_EKU_ANY         "2.5.29.37.0"

#define CERTIFICATE_PARSING_EXCEPTION \
                            "java/security/cert/CertificateParsingException"
#define INVALID_KEY_EXCEPTION \
                            "java/security/InvalidKeyException"
#define KEY_EXCEPTION       "java/security/KeyException"
#define KEYSTORE_EXCEPTION  "java/security/KeyStoreException"
#define PROVIDER_EXCEPTION  "java/security/ProviderException"
#define SIGNATURE_EXCEPTION "java/security/SignatureException"
#define OUT_OF_MEMORY_ERROR "java/lang/OutOfMemoryError"

#define SS_CHECK(Status) \
        if (Status != ERROR_SUCCESS) { \
            ThrowException(env, SIGNATURE_EXCEPTION, Status); \
            __leave; \
        }

#define PP(fmt, ...) \
        if (trace) { \
            fprintf(stdout, "MSCAPI (%ld): ", __LINE__); \
            fprintf(stdout, fmt, ##__VA_ARGS__); \
            fprintf(stdout, "\n"); \
            fflush(stdout); \
        }

extern "C" {

char* trace = getenv("CAPI_TRACE");

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

void showProperty(NCRYPT_HANDLE hKey);

void dump(LPSTR title, PBYTE data, DWORD len)
{
    if (trace) {
        printf("==== %s ====\n", title);
        for (DWORD i = 0; i < len; i+=16) {
            printf("%04x: ", i);
            for (int j = 0; j < 16; j++) {
                if (j == 8) {
                    printf("  ");
                }
                if (i + j < len) {
                    printf("%02X ", *(data + i + j) & 0xff);
                } else {
                    printf("   ");
                }
            }
            for (int j = 0; j < 16; j++) {
                if (i + j < len) {
                    int k = *(data + i + j) & 0xff;
                    if (k < 32 || k > 127) printf(".");
                    else printf("%c", (char)k);
                }
            }
            printf("\n");
        }
        fflush(stdout);
    }
}

/*
 * Throws an arbitrary Java exception with the given message.
 */
void ThrowExceptionWithMessage(JNIEnv *env, const char *exceptionName,
                               const char *szMessage)
{
    jclass exceptionClazz = env->FindClass(exceptionName);
    if (exceptionClazz != NULL) {
        env->ThrowNew(exceptionClazz, szMessage);
    }
}

/*
 * Throws an arbitrary Java exception.
 * The exception message is a Windows system error message.
 */
void ThrowException(JNIEnv *env, const char *exceptionName, DWORD dwError)
{
    char szMessage[1024];
    szMessage[0] = '\0';

    DWORD res = FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM, NULL, dwError,
        NULL, szMessage, sizeof(szMessage), NULL);
    if (res == 0) {
        strcpy(szMessage, "Unknown error");
    }

    ThrowExceptionWithMessage(env, exceptionName, szMessage);
}

/*
 * Overloaded 'operator new[]' variant, which will raise Java's
 * OutOfMemoryError in the case of a failure.
 */
void* operator new[](std::size_t size, JNIEnv *env)
{
    void* buf = ::operator new[](size, std::nothrow);
    if (buf == NULL) {
        ThrowExceptionWithMessage(env, OUT_OF_MEMORY_ERROR,
                "Native memory allocation failed");
    }
    return buf;
}

/*
 * Maps the name of a hash algorithm to an algorithm identifier.
 */
ALG_ID MapHashAlgorithm(JNIEnv *env, jstring jHashAlgorithm) {

    const char* pszHashAlgorithm = NULL;
    ALG_ID algId = 0;

    if ((pszHashAlgorithm = env->GetStringUTFChars(jHashAlgorithm, NULL))
        == NULL) {
        return algId;
    }

    if ((strcmp("SHA", pszHashAlgorithm) == 0) ||
        (strcmp("SHA1", pszHashAlgorithm) == 0) ||
        (strcmp("SHA-1", pszHashAlgorithm) == 0)) {

        algId = CALG_SHA1;
    } else if (strcmp("SHA1+MD5", pszHashAlgorithm) == 0) {
        algId = CALG_SSL3_SHAMD5; // a 36-byte concatenation of SHA-1 and MD5
    } else if (strcmp("SHA-256", pszHashAlgorithm) == 0) {
        algId = CALG_SHA_256;
    } else if (strcmp("SHA-384", pszHashAlgorithm) == 0) {
        algId = CALG_SHA_384;
    } else if (strcmp("SHA-512", pszHashAlgorithm) == 0) {
        algId = CALG_SHA_512;
    } else if (strcmp("MD5", pszHashAlgorithm) == 0) {
        algId = CALG_MD5;
    } else if (strcmp("MD2", pszHashAlgorithm) == 0) {
        algId = CALG_MD2;
    }

    if (pszHashAlgorithm)
        env->ReleaseStringUTFChars(jHashAlgorithm, pszHashAlgorithm);

   return algId;
}

/*
 * Maps the name of a hash algorithm to a CNG Algorithm Identifier.
 */
LPCWSTR MapHashIdentifier(JNIEnv *env, jstring jHashAlgorithm) {

    const char* pszHashAlgorithm = NULL;
    LPCWSTR id = NULL;

    if ((pszHashAlgorithm = env->GetStringUTFChars(jHashAlgorithm, NULL))
            == NULL) {
        return id;
    }

    if ((strcmp("SHA", pszHashAlgorithm) == 0) ||
        (strcmp("SHA1", pszHashAlgorithm) == 0) ||
        (strcmp("SHA-1", pszHashAlgorithm) == 0)) {

        id = BCRYPT_SHA1_ALGORITHM;
    } else if (strcmp("SHA-256", pszHashAlgorithm) == 0) {
        id = BCRYPT_SHA256_ALGORITHM;
    } else if (strcmp("SHA-384", pszHashAlgorithm) == 0) {
        id = BCRYPT_SHA384_ALGORITHM;
    } else if (strcmp("SHA-512", pszHashAlgorithm) == 0) {
        id = BCRYPT_SHA512_ALGORITHM;
    }

    if (pszHashAlgorithm)
        env->ReleaseStringUTFChars(jHashAlgorithm, pszHashAlgorithm);

    return id;
}

/*
 * Returns a certificate chain context given a certificate context and key
 * usage identifier.
 */
bool GetCertificateChain(LPSTR lpszKeyUsageIdentifier, PCCERT_CONTEXT pCertContext, PCCERT_CHAIN_CONTEXT* ppChainContext)
{
    CERT_ENHKEY_USAGE        EnhkeyUsage;
    CERT_USAGE_MATCH         CertUsage;
    CERT_CHAIN_PARA          ChainPara;
    DWORD                    dwFlags = 0;
    LPSTR                    szUsageIdentifierArray[1];

    szUsageIdentifierArray[0] = lpszKeyUsageIdentifier;
    EnhkeyUsage.cUsageIdentifier = 1;
    EnhkeyUsage.rgpszUsageIdentifier = szUsageIdentifierArray;
    CertUsage.dwType = USAGE_MATCH_TYPE_AND;
    CertUsage.Usage  = EnhkeyUsage;
    ChainPara.cbSize = sizeof(CERT_CHAIN_PARA);
    ChainPara.RequestedUsage=CertUsage;

    // Build a chain using CertGetCertificateChain
    // and the certificate retrieved.
    return (::CertGetCertificateChain(NULL,     // use the default chain engine
                pCertContext,   // pointer to the end certificate
                NULL,           // use the default time
                NULL,           // search no additional stores
                &ChainPara,     // use AND logic and enhanced key usage
                                //  as indicated in the ChainPara
                                //  data structure
                dwFlags,
                NULL,           // currently reserved
                ppChainContext) == TRUE);       // return a pointer to the chain created
}


/////////////////////////////////////////////////////////////////////////////
//

/*
 * Class:     sun_security_mscapi_PRNG
 * Method:    getContext
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_sun_security_mscapi_PRNG_getContext
        (JNIEnv *env, jclass clazz) {
    HCRYPTPROV hCryptProv = NULL;
    if(::CryptAcquireContext( //deprecated
       &hCryptProv,
       NULL,
       NULL,
       PROV_RSA_FULL,
       CRYPT_VERIFYCONTEXT) == FALSE)
    {
        ThrowException(env, PROVIDER_EXCEPTION, GetLastError());
    }
    return hCryptProv;
}


/*
 * Class:     sun_security_mscapi_PRNG
 * Method:    releaseContext
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_security_mscapi_PRNG_releaseContext
        (JNIEnv *env, jclass clazz, jlong ctxt) {
    if (ctxt) {
        ::CryptReleaseContext((HCRYPTPROV)ctxt, 0); //deprecated
    }
}


/*
 * Class:     sun_security_mscapi_PRNG
 * Method:    generateSeed
 * Signature: (JI[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_PRNG_generateSeed
  (JNIEnv *env, jclass clazz, jlong ctxt, jint length, jbyteArray seed)
{

    HCRYPTPROV hCryptProv = (HCRYPTPROV)ctxt;
    jbyte*     reseedBytes = NULL;
    jbyte*     seedBytes = NULL;
    jbyteArray result = NULL;

    __try
    {
        /*
         * If length is negative then use the supplied seed to re-seed the
         * generator and return null.
         * If length is non-zero then generate a new seed according to the
         * requested length and return the new seed.
         * If length is zero then overwrite the supplied seed with a new
         * seed of the same length and return the seed.
         */
        if (length < 0) {
            length = env->GetArrayLength(seed);
            if ((reseedBytes = env->GetByteArrayElements(seed, 0)) == NULL) {
                __leave;
            }

            if (::CryptGenRandom( //deprecated
                hCryptProv,
                length,
                (BYTE *) reseedBytes) == FALSE) {

                ThrowException(env, PROVIDER_EXCEPTION, GetLastError());
                __leave;
            }

            result = NULL;

        } else {

            if (length > 0) {
                seed = env->NewByteArray(length);
                if (seed == NULL) {
                    __leave;
                }
            } else {
                length = env->GetArrayLength(seed);
            }

            if ((seedBytes = env->GetByteArrayElements(seed, 0)) == NULL) {
                __leave;
            }

            if (::CryptGenRandom( //deprecated
                hCryptProv,
                length,
                (BYTE *) seedBytes) == FALSE) {

                ThrowException(env, PROVIDER_EXCEPTION, GetLastError());
                __leave;
            }

            result = seed; // seed will be updated when seedBytes gets released
        }
    }
    __finally
    {
        //--------------------------------------------------------------------
        // Clean up.

        if (reseedBytes)
            env->ReleaseByteArrayElements(seed, reseedBytes, JNI_ABORT);

        if (seedBytes)
            env->ReleaseByteArrayElements(seed, seedBytes, 0); // update orig
    }

    return result;
}


/*
 * Class:     sun_security_mscapi_CKeyStore
 * Method:    loadKeysOrCertificateChains
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_security_mscapi_CKeyStore_loadKeysOrCertificateChains
  (JNIEnv *env, jobject obj, jstring jCertStoreName)
{
    /**
     * Certificate in cert store has enhanced key usage extension
     * property (or EKU property) that is not part of the certificate itself. To determine
     * if the certificate should be returned, both the enhanced key usage in certificate
     * extension block and the extension property stored along with the certificate in
     * certificate store should be examined. Otherwise, we won't be able to determine
     * the proper key usage from the Java side because the information is not stored as
     * part of the encoded certificate.
     */

    const char* pszCertStoreName = NULL;
    HCERTSTORE hCertStore = NULL;
    PCCERT_CONTEXT pCertContext = NULL;
    char* pszNameString = NULL; // certificate's friendly name
    DWORD cchNameString = 0;


    __try
    {
        // Open a system certificate store.
        if ((pszCertStoreName = env->GetStringUTFChars(jCertStoreName, NULL))
            == NULL) {
            __leave;
        }
        if ((hCertStore = ::CertOpenSystemStore(NULL, pszCertStoreName))
            == NULL) {

            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

        // Determine clazz and method ID to generate certificate
        jclass clazzArrayList = env->FindClass("java/util/ArrayList");
        if (clazzArrayList == NULL) {
            __leave;
        }

        jmethodID mNewArrayList = env->GetMethodID(clazzArrayList, "<init>", "()V");
        if (mNewArrayList == NULL) {
            __leave;
        }

        jclass clazzOfThis = env->GetObjectClass(obj);
        if (clazzOfThis == NULL) {
            __leave;
        }

        jmethodID mGenCert = env->GetMethodID(clazzOfThis,
                                              "generateCertificate",
                                              "([BLjava/util/Collection;)V");
        if (mGenCert == NULL) {
            __leave;
        }

        // Determine method ID to generate certificate chain
        jmethodID mGenCertChain = env->GetMethodID(clazzOfThis,
                                                   "generateCertificateChain",
                                                   "(Ljava/lang/String;Ljava/util/Collection;)V");
        if (mGenCertChain == NULL) {
            __leave;
        }

        // Determine method ID to generate RSA certificate chain
        jmethodID mGenKeyAndCertChain = env->GetMethodID(clazzOfThis,
                                                   "generateKeyAndCertificateChain",
                                                   "(ZLjava/lang/String;JJILjava/util/Collection;)V");
        if (mGenKeyAndCertChain == NULL) {
            __leave;
        }

        // Use CertEnumCertificatesInStore to get the certificates
        // from the open store. pCertContext must be reset to
        // NULL to retrieve the first certificate in the store.
        while (pCertContext = ::CertEnumCertificatesInStore(hCertStore, pCertContext))
        {
            PP("--------------------------");
            // Check if private key available - client authentication certificate
            // must have private key available.
            HCRYPTPROV hCryptProv = NULL;
            DWORD dwKeySpec = 0;
            HCRYPTKEY hUserKey = NULL;
            BOOL bCallerFreeProv = FALSE;
            BOOL bHasNoPrivateKey = FALSE;
            DWORD dwPublicKeyLength = 0;

            // First, probe it silently
            if (::CryptAcquireCertificatePrivateKey(pCertContext,
                    CRYPT_ACQUIRE_ALLOW_NCRYPT_KEY_FLAG | CRYPT_ACQUIRE_SILENT_FLAG, NULL,
                    &hCryptProv, &dwKeySpec, &bCallerFreeProv) == FALSE
                && GetLastError() != NTE_SILENT_CONTEXT)
            {
                PP("bHasNoPrivateKey = TRUE!");
                bHasNoPrivateKey = TRUE;
            }
            else
            {
                if (bCallerFreeProv == TRUE) {
                    ::CryptReleaseContext(hCryptProv, NULL); // deprecated
                    bCallerFreeProv = FALSE;
                }

                // Second, acquire the key normally (not silently)
                if (::CryptAcquireCertificatePrivateKey(pCertContext, CRYPT_ACQUIRE_ALLOW_NCRYPT_KEY_FLAG, NULL,
                        &hCryptProv, &dwKeySpec, &bCallerFreeProv) == FALSE)
                {
                    PP("bHasNoPrivateKey = TRUE!!");
                    bHasNoPrivateKey = TRUE;
                }
                else
                {
                    if ((dwKeySpec & CERT_NCRYPT_KEY_SPEC) == CERT_NCRYPT_KEY_SPEC) {
                        PP("CNG %I64d", (__int64)hCryptProv);
                    } else {
                        // Private key is available
                        BOOL bGetUserKey = ::CryptGetUserKey(hCryptProv, dwKeySpec, &hUserKey); //deprecated

                        // Skip certificate if cannot find private key
                        if (bGetUserKey == FALSE) {
                            if (bCallerFreeProv)
                                ::CryptReleaseContext(hCryptProv, NULL); // deprecated
                            continue;
                        }

                        // Set cipher mode to ECB
                        DWORD dwCipherMode = CRYPT_MODE_ECB;
                        ::CryptSetKeyParam(hUserKey, KP_MODE, (BYTE*)&dwCipherMode, NULL); //deprecated
                        PP("CAPI %I64d %I64d", (__int64)hCryptProv, (__int64)hUserKey);
                    }
                    // If the private key is present in smart card, we may not be able to
                    // determine the key length by using the private key handle. However,
                    // since public/private key pairs must have the same length, we could
                    // determine the key length of the private key by using the public key
                    // in the certificate.
                    dwPublicKeyLength = ::CertGetPublicKeyLength(X509_ASN_ENCODING | PKCS_7_ASN_ENCODING,
                            &(pCertContext->pCertInfo->SubjectPublicKeyInfo));
                }
            }
            PCCERT_CHAIN_CONTEXT pCertChainContext = NULL;

            // Build certificate chain by using system certificate store.
            // Add cert chain into collection for any key usage.
            //
            if (GetCertificateChain(OID_EKU_ANY, pCertContext, &pCertChainContext))
            {
                for (DWORD i = 0; i < pCertChainContext->cChain; i++)
                {
                    // Found cert chain
                    PCERT_SIMPLE_CHAIN rgpChain =
                        pCertChainContext->rgpChain[i];

                    // Create ArrayList to store certs in each chain
                    jobject jArrayList =
                        env->NewObject(clazzArrayList, mNewArrayList);
                    if (jArrayList == NULL) {
                        __leave;
                    }

                    // Cleanup the previous allocated name
                    if (pszNameString) {
                        delete [] pszNameString;
                        pszNameString = NULL;
                    }

                    for (unsigned int j=0; j < rgpChain->cElement; j++)
                    {
                        PCERT_CHAIN_ELEMENT rgpElement =
                            rgpChain->rgpElement[j];
                        PCCERT_CONTEXT pc = rgpElement->pCertContext;

                        // Retrieve the friendly name of the first certificate
                        // in the chain
                        if (j == 0) {

                            // If the cert's name cannot be retrieved then
                            // pszNameString remains set to NULL.
                            // (An alias name will be generated automatically
                            // when storing this cert in the keystore.)

                            // Get length of friendly name
                            if ((cchNameString = CertGetNameString(pc,
                                CERT_NAME_FRIENDLY_DISPLAY_TYPE, 0, NULL,
                                NULL, 0)) > 1) {

                                // Found friendly name
                                pszNameString = new (env) char[cchNameString];
                                if (pszNameString == NULL) {
                                    __leave;
                                }

                                CertGetNameString(pc,
                                    CERT_NAME_FRIENDLY_DISPLAY_TYPE, 0, NULL,
                                    pszNameString, cchNameString);
                            }
                        }

                        BYTE* pbCertEncoded = pc->pbCertEncoded;
                        DWORD cbCertEncoded = pc->cbCertEncoded;

                        // Allocate and populate byte array
                        jbyteArray byteArray = env->NewByteArray(cbCertEncoded);
                        if (byteArray == NULL) {
                            __leave;
                        }
                        env->SetByteArrayRegion(byteArray, 0, cbCertEncoded,
                            (jbyte*) pbCertEncoded);

                        // Generate certificate from byte array and store into
                        // cert collection
                        env->CallVoidMethod(obj, mGenCert, byteArray, jArrayList);
                    }

                    // Usually pszNameString should be non-NULL. It's either
                    // the friendly name or an element from the subject name
                    // or SAN.
                    if (pszNameString)
                    {
                        PP("%s: %s", pszNameString, pCertContext->pCertInfo->SubjectPublicKeyInfo.Algorithm.pszObjId);
                        if (bHasNoPrivateKey)
                        {
                            // Generate certificate chain and store into cert chain
                            // collection
                            jstring name = env->NewStringUTF(pszNameString);
                            if (name == NULL) {
                                __leave;
                            }
                            env->CallVoidMethod(obj, mGenCertChain,
                                name,
                                jArrayList);
                        }
                        else
                        {
                            if (hUserKey) {
                                // Only accept RSA for CAPI
                                DWORD dwData = CALG_RSA_KEYX;
                                DWORD dwSize = sizeof(DWORD);
                                ::CryptGetKeyParam(hUserKey, KP_ALGID, (BYTE*)&dwData, //deprecated
                                        &dwSize, NULL);
                                if ((dwData & ALG_TYPE_RSA) == ALG_TYPE_RSA)
                                {
                                    // Generate RSA certificate chain and store into cert
                                    // chain collection
                                    jstring name = env->NewStringUTF(pszNameString);
                                    if (name == NULL) {
                                        __leave;
                                    }
                                    env->CallVoidMethod(obj, mGenKeyAndCertChain,
                                            1,
                                            name,
                                            (jlong) hCryptProv, (jlong) hUserKey,
                                            dwPublicKeyLength, jArrayList);
                                }
                            } else {
                                // Only accept EC for CNG
                                BYTE buffer[32];
                                DWORD len = 0;
                                if (::NCryptGetProperty(
                                        hCryptProv, NCRYPT_ALGORITHM_PROPERTY,
                                        (PBYTE)buffer, 32, &len, NCRYPT_SILENT_FLAG) == ERROR_SUCCESS) {
                                    jstring name = env->NewStringUTF(pszNameString);
                                    if (name == NULL) {
                                        __leave;
                                    }
                                    if (buffer[0] == 'E' && buffer[2] == 'C'
                                            && (dwPublicKeyLength == 256
                                                    || dwPublicKeyLength == 384
                                                    || dwPublicKeyLength == 521)) {
                                        env->CallVoidMethod(obj, mGenKeyAndCertChain,
                                            0,
                                            name,
                                            (jlong) hCryptProv, (jlong) 0,
                                            dwPublicKeyLength, jArrayList);
                                    } else if (buffer[0] == 'R' && buffer[2] == 'S'
                                            && buffer[4] == 'A') {
                                        env->CallVoidMethod(obj, mGenKeyAndCertChain,
                                            1,
                                            name,
                                            (jlong) hCryptProv, (jlong) 0,
                                            dwPublicKeyLength, jArrayList);
                                    } else {
                                        dump("Unknown NCRYPT_ALGORITHM_PROPERTY", buffer, len);
                                    }
                                }
                            }
                        }
                    }
                }

                // Free cert chain
                if (pCertChainContext)
                    ::CertFreeCertificateChain(pCertChainContext);
            } else {
                PP("GetCertificateChain failed %d", GetLastError());
            }
        }
    }
    __finally
    {
        if (hCertStore)
            ::CertCloseStore(hCertStore, 0);

        if (pszCertStoreName)
            env->ReleaseStringUTFChars(jCertStoreName, pszCertStoreName);

        if (pszNameString)
            delete [] pszNameString;
    }
}


/*
 * Class:     sun_security_mscapi_CKey
 * Method:    cleanUp
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_security_mscapi_CKey_cleanUp
  (JNIEnv *env, jclass clazz, jlong hCryptProv, jlong hCryptKey)
{
    if (hCryptKey == NULL && hCryptProv != NULL) {
        NCryptFreeObject((NCRYPT_HANDLE)hCryptProv);
    } else {
        if (hCryptKey != NULL)
            ::CryptDestroyKey((HCRYPTKEY) hCryptKey); // deprecated

        if (hCryptProv != NULL)
            ::CryptReleaseContext((HCRYPTPROV) hCryptProv, NULL); // deprecated
    }
}

/*
 * Class:     sun_security_mscapi_CSignature
 * Method:    signHash
 * Signature: (Z[BILjava/lang/String;JJ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_CSignature_signHash
  (JNIEnv *env, jclass clazz, jboolean noHashOID, jbyteArray jHash,
        jint jHashSize, jstring jHashAlgorithm, jlong hCryptProv,
        jlong hCryptKey)
{
    HCRYPTHASH hHash = NULL;
    jbyte* pHashBuffer = NULL;
    jbyte* pSignedHashBuffer = NULL;
    jbyteArray jSignedHash = NULL;
    HCRYPTPROV hCryptProvAlt = NULL;

    __try
    {
        // Map hash algorithm
        ALG_ID algId = MapHashAlgorithm(env, jHashAlgorithm);

        // Acquire a hash object handle.
        if (::CryptCreateHash(HCRYPTPROV(hCryptProv), algId, 0, 0, &hHash) == FALSE) //deprecated
        {
            // Failover to using the PROV_RSA_AES CSP

            DWORD cbData = 256;
            BYTE pbData[256];
            pbData[0] = '\0';

            // Get name of the key container
            ::CryptGetProvParam((HCRYPTPROV)hCryptProv, PP_CONTAINER, //deprecated
                (BYTE *)pbData, &cbData, 0);

            // Acquire an alternative CSP handle
            if (::CryptAcquireContext(&hCryptProvAlt, LPCSTR(pbData), NULL, //deprecated
                PROV_RSA_AES, 0) == FALSE)
            {

                ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
                __leave;
            }

            // Acquire a hash object handle.
            if (::CryptCreateHash(HCRYPTPROV(hCryptProvAlt), algId, 0, 0, //deprecated
                &hHash) == FALSE)
            {
                ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
                __leave;
            }
        }

        // Copy hash from Java to native buffer
        pHashBuffer = new (env) jbyte[jHashSize];
        if (pHashBuffer == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jHash, 0, jHashSize, pHashBuffer);

        // Set hash value in the hash object
        if (::CryptSetHashParam(hHash, HP_HASHVAL, (BYTE*)pHashBuffer, NULL) == FALSE) //deprecated
        {
            ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
            __leave;
        }

        // Determine key spec.
        DWORD dwKeySpec = AT_SIGNATURE;
        ALG_ID dwAlgId;
        DWORD dwAlgIdLen = sizeof(ALG_ID);

        if (! ::CryptGetKeyParam((HCRYPTKEY) hCryptKey, KP_ALGID, (BYTE*)&dwAlgId, &dwAlgIdLen, 0)) { //deprecated
            ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
            __leave;

        }
        if (CALG_RSA_KEYX == dwAlgId) {
            dwKeySpec = AT_KEYEXCHANGE;
        }

        // Determine size of buffer
        DWORD dwBufLen = 0;
        DWORD dwFlags = 0;

        if (noHashOID == JNI_TRUE) {
            dwFlags = CRYPT_NOHASHOID; // omit hash OID in NONEwithRSA signature
        }

        if (::CryptSignHash(hHash, dwKeySpec, NULL, dwFlags, NULL, &dwBufLen) == FALSE) //deprecated
        {
            ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
            __leave;
        }

        pSignedHashBuffer = new (env) jbyte[dwBufLen];
        if (pSignedHashBuffer == NULL) {
            __leave;
        }
        if (::CryptSignHash(hHash, dwKeySpec, NULL, dwFlags, (BYTE*)pSignedHashBuffer, &dwBufLen) == FALSE) //deprecated
        {
            ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
            __leave;
        }

        // Create new byte array
        jbyteArray temp = env->NewByteArray(dwBufLen);
        if (temp == NULL) {
            __leave;
        }

        // Copy data from native buffer
        env->SetByteArrayRegion(temp, 0, dwBufLen, pSignedHashBuffer);

        jSignedHash = temp;
    }
    __finally
    {
        if (pSignedHashBuffer)
            delete [] pSignedHashBuffer;

        if (pHashBuffer)
            delete [] pHashBuffer;

        if (hHash)
            ::CryptDestroyHash(hHash); //deprecated

        if (hCryptProvAlt)
            ::CryptReleaseContext(hCryptProvAlt, 0); // deprecated
    }

    return jSignedHash;
}

/*
 * Class:     sun_security_mscapi_CSignature
 * Method:    signCngHash
 * Signature: (I[BIILjava/lang/String;JJ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_CSignature_signCngHash
  (JNIEnv *env, jclass clazz, jint type, jbyteArray jHash,
        jint jHashSize, jint saltLen, jstring jHashAlgorithm, jlong hCryptProv,
        jlong hCryptKey)
{
    jbyteArray jSignedHash = NULL;

    jbyte* pHashBuffer = NULL;
    jbyte* pSignedHashBuffer = NULL;
    NCRYPT_KEY_HANDLE hk = NULL;

    __try
    {
        if (hCryptKey == 0) {
            hk = (NCRYPT_KEY_HANDLE)hCryptProv;
        } else {
            SS_CHECK(::NCryptTranslateHandle(
                NULL,
                &hk,
                (HCRYPTPROV)hCryptProv,
                (HCRYPTKEY)hCryptKey,
                NULL,
                0));
        }

        // Copy hash from Java to native buffer
        pHashBuffer = new (env) jbyte[jHashSize];
        if (pHashBuffer == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jHash, 0, jHashSize, pHashBuffer);

        VOID* param;
        DWORD dwFlags;

        switch (type) {
        case 0:
            param = NULL;
            dwFlags = 0;
            break;
        case 1:
            BCRYPT_PKCS1_PADDING_INFO pkcs1Info;
            if (jHashAlgorithm) {
                pkcs1Info.pszAlgId = MapHashIdentifier(env, jHashAlgorithm);
                if (pkcs1Info.pszAlgId == NULL) {
                    ThrowExceptionWithMessage(env, SIGNATURE_EXCEPTION,
                            "Unrecognised hash algorithm");
                    __leave;
                }
            } else {
                pkcs1Info.pszAlgId = NULL;
            }
            param = &pkcs1Info;
            dwFlags = BCRYPT_PAD_PKCS1;
            break;
        case 2:
            BCRYPT_PSS_PADDING_INFO pssInfo;
            pssInfo.pszAlgId = MapHashIdentifier(env, jHashAlgorithm);
            pssInfo.cbSalt = saltLen;
            if (pssInfo.pszAlgId == NULL) {
                ThrowExceptionWithMessage(env, SIGNATURE_EXCEPTION,
                        "Unrecognised hash algorithm");
                __leave;
            }
            param = &pssInfo;
            dwFlags = BCRYPT_PAD_PSS;
            break;
        }

        DWORD jSignedHashSize = 0;
        SS_CHECK(::NCryptSignHash(
                hk,
                param,
                (BYTE*)pHashBuffer, jHashSize,
                NULL, 0, &jSignedHashSize,
                dwFlags
                ));

        pSignedHashBuffer = new (env) jbyte[jSignedHashSize];
        if (pSignedHashBuffer == NULL) {
            __leave;
        }

        SS_CHECK(::NCryptSignHash(
                hk,
                param,
                (BYTE*)pHashBuffer, jHashSize,
                (BYTE*)pSignedHashBuffer, jSignedHashSize, &jSignedHashSize,
                dwFlags
                ));

        // Create new byte array
        jbyteArray temp = env->NewByteArray(jSignedHashSize);
        if (temp == NULL) {
            __leave;
        }

        // Copy data from native buffer
        env->SetByteArrayRegion(temp, 0, jSignedHashSize, pSignedHashBuffer);

        jSignedHash = temp;
    }
    __finally
    {
        if (pSignedHashBuffer)
            delete [] pSignedHashBuffer;

        if (pHashBuffer)
            delete [] pHashBuffer;

        if (hCryptKey != 0 && hk != NULL)
            ::NCryptFreeObject(hk);
    }

    return jSignedHash;
}

/*
 * Class:     sun_security_mscapi_CSignature
 * Method:    verifySignedHash
 * Signature: ([BIL/java/lang/String;[BIJJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_security_mscapi_CSignature_verifySignedHash
  (JNIEnv *env, jclass clazz, jbyteArray jHash, jint jHashSize,
        jstring jHashAlgorithm, jbyteArray jSignedHash, jint jSignedHashSize,
        jlong hCryptProv, jlong hCryptKey)
{
    HCRYPTHASH hHash = NULL;
    jbyte* pHashBuffer = NULL;
    jbyte* pSignedHashBuffer = NULL;
    DWORD dwSignedHashBufferLen = jSignedHashSize;
    jboolean result = JNI_FALSE;
    HCRYPTPROV hCryptProvAlt = NULL;

    __try
    {
        // Map hash algorithm
        ALG_ID algId = MapHashAlgorithm(env, jHashAlgorithm);

        // Acquire a hash object handle.
        if (::CryptCreateHash(HCRYPTPROV(hCryptProv), algId, 0, 0, &hHash)
            == FALSE)
        {
            // Failover to using the PROV_RSA_AES CSP

            DWORD cbData = 256;
            BYTE pbData[256];
            pbData[0] = '\0';

            // Get name of the key container
            ::CryptGetProvParam((HCRYPTPROV)hCryptProv, PP_CONTAINER, //deprecated
                (BYTE *)pbData, &cbData, 0);

            // Acquire an alternative CSP handle
            if (::CryptAcquireContext(&hCryptProvAlt, LPCSTR(pbData), NULL, //deprecated
                PROV_RSA_AES, 0) == FALSE)
            {

                ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
                __leave;
            }

            // Acquire a hash object handle.
            if (::CryptCreateHash(HCRYPTPROV(hCryptProvAlt), algId, 0, 0,
                &hHash) == FALSE)
            {
                ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
                __leave;
            }
        }

        // Copy hash and signedHash from Java to native buffer
        pHashBuffer = new (env) jbyte[jHashSize];
        if (pHashBuffer == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jHash, 0, jHashSize, pHashBuffer);

        pSignedHashBuffer = new (env) jbyte[jSignedHashSize];
        if (pSignedHashBuffer == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jSignedHash, 0, jSignedHashSize,
            pSignedHashBuffer);

        // Set hash value in the hash object
        if (::CryptSetHashParam(hHash, HP_HASHVAL, (BYTE*) pHashBuffer, NULL) //deprecated
            == FALSE)
        {
            ThrowException(env, SIGNATURE_EXCEPTION, GetLastError());
            __leave;
        }

        // For RSA, the hash encryption algorithm is normally the same as the
        // public key algorithm, so AT_SIGNATURE is used.

        // Verify the signature
        if (::CryptVerifySignatureA(hHash, (BYTE *) pSignedHashBuffer, //deprecated
            dwSignedHashBufferLen, (HCRYPTKEY) hCryptKey, NULL, 0) == TRUE)
        {
            result = JNI_TRUE;
        }
    }

    __finally
    {
        if (pSignedHashBuffer)
            delete [] pSignedHashBuffer;

        if (pHashBuffer)
            delete [] pHashBuffer;

        if (hHash)
            ::CryptDestroyHash(hHash); //deprecated

        if (hCryptProvAlt)
            ::CryptReleaseContext(hCryptProvAlt, 0); // deprecated
    }

    return result;
}

/*
 * Class:     sun_security_mscapi_CSignature
 * Method:    verifyCngSignedHash
 * Signature: (I[BI[BIILjava/lang/String;JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_security_mscapi_CSignature_verifyCngSignedHash
  (JNIEnv *env, jclass clazz, jint type,
        jbyteArray jHash, jint jHashSize,
        jbyteArray jSignedHash, jint jSignedHashSize,
        jint saltLen, jstring jHashAlgorithm,
        jlong hCryptProv, jlong hCryptKey)
{
    jbyte* pHashBuffer = NULL;
    jbyte* pSignedHashBuffer = NULL;
    jboolean result = JNI_FALSE;
    NCRYPT_KEY_HANDLE hk = NULL;

    __try
    {
        if (hCryptKey == 0) {
            hk = (NCRYPT_KEY_HANDLE)hCryptProv;
        } else {
            SS_CHECK(::NCryptTranslateHandle(
                NULL,
                &hk,
                (HCRYPTPROV)hCryptProv,
                (HCRYPTKEY)hCryptKey,
                NULL,
                0));
        }

        // Copy hash and signedHash from Java to native buffer
        pHashBuffer = new (env) jbyte[jHashSize];
        if (pHashBuffer == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jHash, 0, jHashSize, pHashBuffer);

        pSignedHashBuffer = new (env) jbyte[jSignedHashSize];
        if (pSignedHashBuffer == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jSignedHash, 0, jSignedHashSize,
            pSignedHashBuffer);

        VOID* param;
        DWORD dwFlags;

        switch (type) {
        case 0:
            param = NULL;
            dwFlags = 0;
            break;
        case 1:
            BCRYPT_PKCS1_PADDING_INFO pkcs1Info;
            if (jHashAlgorithm) {
                pkcs1Info.pszAlgId = MapHashIdentifier(env, jHashAlgorithm);
                if (pkcs1Info.pszAlgId == NULL) {
                    ThrowExceptionWithMessage(env, SIGNATURE_EXCEPTION,
                            "Unrecognised hash algorithm");
                    __leave;
                }
            } else {
                pkcs1Info.pszAlgId = NULL;
            }
            param = &pkcs1Info;
            dwFlags = NCRYPT_PAD_PKCS1_FLAG;
            break;
        case 2:
            BCRYPT_PSS_PADDING_INFO pssInfo;
            pssInfo.pszAlgId = MapHashIdentifier(env, jHashAlgorithm);
            pssInfo.cbSalt = saltLen;
            if (pssInfo.pszAlgId == NULL) {
                ThrowExceptionWithMessage(env, SIGNATURE_EXCEPTION,
                        "Unrecognised hash algorithm");
                __leave;
            }
            param = &pssInfo;
            dwFlags = NCRYPT_PAD_PSS_FLAG;
            break;
        }

        if (::NCryptVerifySignature(hk, param,
                (BYTE *) pHashBuffer, jHashSize,
                (BYTE *) pSignedHashBuffer, jSignedHashSize,
                dwFlags) == ERROR_SUCCESS)
        {
            result = JNI_TRUE;
        }
    }

    __finally
    {
        if (pSignedHashBuffer)
            delete [] pSignedHashBuffer;

        if (pHashBuffer)
            delete [] pHashBuffer;

        if (hCryptKey != 0 && hk != NULL)
            ::NCryptFreeObject(hk);
    }

    return result;
}

#define DUMP_PROP(p) \
    if (::NCryptGetProperty(hKey, p, (PBYTE)buffer, 8192, &len, NCRYPT_SILENT_FLAG) == ERROR_SUCCESS) { \
        sprintf(header, "%s %ls", #p, p); \
        dump(header, buffer, len); \
    }

#define EXPORT_BLOB(p) \
    desc.cBuffers = 0; \
    if (::NCryptExportKey(hKey, NULL, p, &desc, (PBYTE)buffer, 8192, &len, NCRYPT_SILENT_FLAG) == ERROR_SUCCESS) { \
        sprintf(header, "%s %ls (%ld)", #p, p, desc.cBuffers); \
        dump(header, buffer, len); \
        for (int i = 0; i < (int)desc.cBuffers; i++) { \
            sprintf(header, "desc %ld", desc.pBuffers[i].BufferType); \
            dump(header, (PBYTE)desc.pBuffers[i].pvBuffer, desc.pBuffers[i].cbBuffer); \
        } \
    }

void showProperty(NCRYPT_HANDLE hKey) {
    char header[100];
    BYTE buffer[8192];
    DWORD len = 9;
    NCryptBufferDesc desc;
    DUMP_PROP(NCRYPT_ALGORITHM_GROUP_PROPERTY);
    DUMP_PROP(NCRYPT_ALGORITHM_PROPERTY);
    DUMP_PROP(NCRYPT_ASSOCIATED_ECDH_KEY);
    DUMP_PROP(NCRYPT_BLOCK_LENGTH_PROPERTY);
    DUMP_PROP(NCRYPT_CERTIFICATE_PROPERTY);
    DUMP_PROP(NCRYPT_DH_PARAMETERS_PROPERTY);
    DUMP_PROP(NCRYPT_EXPORT_POLICY_PROPERTY);
    DUMP_PROP(NCRYPT_IMPL_TYPE_PROPERTY);
    DUMP_PROP(NCRYPT_KEY_TYPE_PROPERTY);
    DUMP_PROP(NCRYPT_KEY_USAGE_PROPERTY);
    DUMP_PROP(NCRYPT_LAST_MODIFIED_PROPERTY);
    DUMP_PROP(NCRYPT_LENGTH_PROPERTY);
    DUMP_PROP(NCRYPT_LENGTHS_PROPERTY);
    DUMP_PROP(NCRYPT_MAX_NAME_LENGTH_PROPERTY);
    DUMP_PROP(NCRYPT_NAME_PROPERTY);
    DUMP_PROP(NCRYPT_PIN_PROMPT_PROPERTY);
    DUMP_PROP(NCRYPT_PIN_PROPERTY);
    DUMP_PROP(NCRYPT_PROVIDER_HANDLE_PROPERTY);
    DUMP_PROP(NCRYPT_READER_PROPERTY);
    DUMP_PROP(NCRYPT_ROOT_CERTSTORE_PROPERTY);
    DUMP_PROP(NCRYPT_SCARD_PIN_ID);
    DUMP_PROP(NCRYPT_SCARD_PIN_INFO);
    DUMP_PROP(NCRYPT_SECURE_PIN_PROPERTY);
    DUMP_PROP(NCRYPT_SECURITY_DESCR_PROPERTY);
    DUMP_PROP(NCRYPT_SECURITY_DESCR_SUPPORT_PROPERTY);
    DUMP_PROP(NCRYPT_SMARTCARD_GUID_PROPERTY);
    DUMP_PROP(NCRYPT_UI_POLICY_PROPERTY);
    DUMP_PROP(NCRYPT_UNIQUE_NAME_PROPERTY);
    DUMP_PROP(NCRYPT_USE_CONTEXT_PROPERTY);
    DUMP_PROP(NCRYPT_USE_COUNT_ENABLED_PROPERTY);
    DUMP_PROP(NCRYPT_USE_COUNT_PROPERTY);
    DUMP_PROP(NCRYPT_USER_CERTSTORE_PROPERTY);
    DUMP_PROP(NCRYPT_VERSION_PROPERTY);
    DUMP_PROP(NCRYPT_WINDOW_HANDLE_PROPERTY);

    EXPORT_BLOB(BCRYPT_DH_PRIVATE_BLOB);
    EXPORT_BLOB(BCRYPT_DH_PUBLIC_BLOB);
    EXPORT_BLOB(BCRYPT_DSA_PRIVATE_BLOB);
    EXPORT_BLOB(BCRYPT_DSA_PUBLIC_BLOB);
    EXPORT_BLOB(BCRYPT_ECCPRIVATE_BLOB);
    EXPORT_BLOB(BCRYPT_ECCPUBLIC_BLOB);
    EXPORT_BLOB(BCRYPT_PUBLIC_KEY_BLOB);
    EXPORT_BLOB(BCRYPT_PRIVATE_KEY_BLOB);
    EXPORT_BLOB(BCRYPT_RSAFULLPRIVATE_BLOB);
    EXPORT_BLOB(BCRYPT_RSAPRIVATE_BLOB);
    EXPORT_BLOB(BCRYPT_RSAPUBLIC_BLOB);
    EXPORT_BLOB(LEGACY_DH_PRIVATE_BLOB);
    EXPORT_BLOB(LEGACY_DH_PUBLIC_BLOB);
    EXPORT_BLOB(LEGACY_DSA_PRIVATE_BLOB);
    EXPORT_BLOB(LEGACY_DSA_PUBLIC_BLOB);
    EXPORT_BLOB(LEGACY_RSAPRIVATE_BLOB);
    EXPORT_BLOB(LEGACY_RSAPUBLIC_BLOB);
    EXPORT_BLOB(NCRYPT_CIPHER_KEY_BLOB);
    EXPORT_BLOB(NCRYPT_OPAQUETRANSPORT_BLOB);
    EXPORT_BLOB(NCRYPT_PKCS7_ENVELOPE_BLOB);
    //EXPORT_BLOB(NCRYPTBUFFER_CERT_BLOB);
    //EXPORT_BLOB(NCRYPT_PKCS8_PRIVATE_KEY_BLOB);
    BCryptBuffer bb;
    bb.BufferType = NCRYPTBUFFER_PKCS_SECRET;
    bb.cbBuffer = 18;
    bb.pvBuffer = L"changeit";
    BCryptBufferDesc bbd;
    bbd.ulVersion = 0;
    bbd.cBuffers = 1;
    bbd.pBuffers = &bb;
    if(::NCryptExportKey(hKey, NULL, NCRYPT_PKCS8_PRIVATE_KEY_BLOB, NULL,
            (PBYTE)buffer, 8192, &len, NCRYPT_SILENT_FLAG) == ERROR_SUCCESS) {
        sprintf(header, "NCRYPT_PKCS8_PRIVATE_KEY_BLOB %ls", NCRYPT_PKCS8_PRIVATE_KEY_BLOB);
        dump(header, buffer, len);
    }
    EXPORT_BLOB(NCRYPT_PROTECTED_KEY_BLOB);
}

/*
 * Class:     sun_security_mscapi_CKeyPairGenerator_RSA
 * Method:    generateCKeyPair
 * Signature: (Ljava/lang/String;ILjava/lang/String;)Lsun/security/mscapi/CKeyPair;
 */
JNIEXPORT jobject JNICALL Java_sun_security_mscapi_CKeyPairGenerator_00024RSA_generateCKeyPair
  (JNIEnv *env, jclass clazz, jstring alg, jint keySize, jstring keyContainerName)
{
    HCRYPTPROV hCryptProv = NULL;
    HCRYPTKEY hKeyPair;
    DWORD dwFlags = (keySize << 16) | CRYPT_EXPORTABLE;
    jobject keypair = NULL;
    const char* pszKeyContainerName = NULL; // UUID

    __try
    {
        if ((pszKeyContainerName =
            env->GetStringUTFChars(keyContainerName, NULL)) == NULL) {
            __leave;
        }

        // Acquire a CSP context (create a new key container).
        // Prefer a PROV_RSA_AES CSP, when available, due to its support
        // for SHA-2-based signatures.
        if (::CryptAcquireContext( //deprecated
            &hCryptProv,
            pszKeyContainerName,
            NULL,
            PROV_RSA_AES,
            CRYPT_NEWKEYSET) == FALSE)
        {
            // Failover to using the default CSP (PROV_RSA_FULL)

            if (::CryptAcquireContext( //deprecated
                &hCryptProv,
                pszKeyContainerName,
                NULL,
                PROV_RSA_FULL,
                CRYPT_NEWKEYSET) == FALSE)
            {
                ThrowException(env, KEY_EXCEPTION, GetLastError());
                __leave;
            }
        }

        // Generate an keypair
        if(::CryptGenKey( //deprecated
           hCryptProv,
           AT_KEYEXCHANGE,
           dwFlags,
           &hKeyPair) == FALSE)
        {
            ThrowException(env, KEY_EXCEPTION, GetLastError());
            __leave;
        }

        // Get the method ID for the CKeyPair constructor
        jclass clazzCKeyPair =
            env->FindClass("sun/security/mscapi/CKeyPair");
        if (clazzCKeyPair == NULL) {
            __leave;
        }

        jmethodID mNewCKeyPair =
            env->GetMethodID(clazzCKeyPair, "<init>", "(Ljava/lang/String;JJI)V");
        if (mNewCKeyPair == NULL) {
            __leave;
        }

        // Create a new keypair
        keypair = env->NewObject(clazzCKeyPair, mNewCKeyPair,
            alg, (jlong) hCryptProv, (jlong) hKeyPair, keySize);

    }
    __finally
    {
        //--------------------------------------------------------------------
        // Clean up.

        if (pszKeyContainerName)
            env->ReleaseStringUTFChars(keyContainerName, pszKeyContainerName);
    }

    return keypair;
}

/*
 * Class:     sun_security_mscapi_CKey
 * Method:    getContainerName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_security_mscapi_CKey_getContainerName
  (JNIEnv *env, jclass jclazz, jlong hCryptProv)
{
    DWORD cbData = 256;
    BYTE pbData[256];
    pbData[0] = '\0';

    ::CryptGetProvParam( //deprecated
        (HCRYPTPROV)hCryptProv,
        PP_CONTAINER,
        (BYTE *)pbData,
        &cbData,
        0);

    return env->NewStringUTF((const char*)pbData);
}

/*
 * Class:     sun_security_mscapi_CKey
 * Method:    getKeyType
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_security_mscapi_CKey_getKeyType
  (JNIEnv *env, jclass jclazz, jlong hCryptKey)
{
    ALG_ID dwAlgId;
    DWORD dwAlgIdLen = sizeof(ALG_ID);

    if (::CryptGetKeyParam((HCRYPTKEY) hCryptKey, KP_ALGID, (BYTE*)&dwAlgId, &dwAlgIdLen, 0)) { //deprecated

        if (CALG_RSA_SIGN == dwAlgId) {
            return env->NewStringUTF("Signature");

        } else if (CALG_RSA_KEYX == dwAlgId) {
            return env->NewStringUTF("Exchange");

        } else {
            char buffer[64];
            if (sprintf(buffer, "%lu", dwAlgId)) {
                return env->NewStringUTF(buffer);
            }
        }
    }

    return env->NewStringUTF("<Unknown>");
}

/*
 * Class:     sun_security_mscapi_CKeyStore
 * Method:    storeCertificate
 * Signature: (Ljava/lang/String;Ljava/lang/String;[BIJJ)V
 */
JNIEXPORT void JNICALL Java_sun_security_mscapi_CKeyStore_storeCertificate
  (JNIEnv *env, jobject obj, jstring jCertStoreName, jstring jCertAliasName,
        jbyteArray jCertEncoding, jint jCertEncodingSize, jlong hCryptProv,
        jlong hCryptKey)
{
    const char* pszCertStoreName = NULL;
    HCERTSTORE hCertStore = NULL;
    PCCERT_CONTEXT pCertContext = NULL;
    PWCHAR pszCertAliasName = NULL;
    jbyte* pbCertEncoding = NULL;
    const jchar* jCertAliasChars = NULL;
    const char* pszContainerName = NULL;
    const char* pszProviderName = NULL;
    WCHAR * pwszContainerName = NULL;
    WCHAR * pwszProviderName = NULL;

    __try
    {
        // Open a system certificate store.
        if ((pszCertStoreName = env->GetStringUTFChars(jCertStoreName, NULL))
            == NULL) {
            __leave;
        }
        if ((hCertStore = ::CertOpenSystemStore(NULL, pszCertStoreName)) == NULL) {
            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

        // Copy encoding from Java to native buffer
        pbCertEncoding = new (env) jbyte[jCertEncodingSize];
        if (pbCertEncoding == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jCertEncoding, 0, jCertEncodingSize, pbCertEncoding);

        // Create a certificate context from the encoded cert
        if (!(pCertContext = ::CertCreateCertificateContext(X509_ASN_ENCODING,
            (BYTE*) pbCertEncoding, jCertEncodingSize))) {

            ThrowException(env, CERTIFICATE_PARSING_EXCEPTION, GetLastError());
            __leave;
        }

        // Set the certificate's friendly name
        int size = env->GetStringLength(jCertAliasName);
        pszCertAliasName = new (env) WCHAR[size + 1];
        if (pszCertAliasName == NULL) {
            __leave;
        }

        jCertAliasChars = env->GetStringChars(jCertAliasName, NULL);
        if (jCertAliasChars == NULL) {
            __leave;
        }
        memcpy(pszCertAliasName, jCertAliasChars, size * sizeof(WCHAR));
        pszCertAliasName[size] = 0; // append the string terminator

        CRYPT_DATA_BLOB friendlyName = {
            sizeof(WCHAR) * (size + 1),
            (BYTE *) pszCertAliasName
        };

        env->ReleaseStringChars(jCertAliasName, jCertAliasChars);

        if (! ::CertSetCertificateContextProperty(pCertContext,
            CERT_FRIENDLY_NAME_PROP_ID, 0, &friendlyName)) {

            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

        // Attach the certificate's private key (if supplied)
        if (hCryptProv != 0 && hCryptKey != 0) {

            CRYPT_KEY_PROV_INFO keyProviderInfo;
            DWORD dwDataLen;

            // Get the name of the key container
            if (! ::CryptGetProvParam( //deprecated
                (HCRYPTPROV) hCryptProv,
                PP_CONTAINER,
                NULL,
                &dwDataLen,
                0)) {

                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }

            pszContainerName = new (env) char[dwDataLen];
            if (pszContainerName == NULL) {
                __leave;
            }

            if (! ::CryptGetProvParam( //deprecated
                (HCRYPTPROV) hCryptProv,
                PP_CONTAINER,
                (BYTE *) pszContainerName,
                &dwDataLen,
                0)) {

                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }

            // Convert to a wide char string
            pwszContainerName = new (env) WCHAR[dwDataLen];
            if (pwszContainerName == NULL) {
                __leave;
            }

            if (mbstowcs(pwszContainerName, pszContainerName, dwDataLen) == 0) {
                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }

            // Set the name of the key container
            keyProviderInfo.pwszContainerName = pwszContainerName;


            // Get the name of the provider
            if (! ::CryptGetProvParam( //deprecated
                (HCRYPTPROV) hCryptProv,
                PP_NAME,
                NULL,
                &dwDataLen,
                0)) {

                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }

            pszProviderName = new (env) char[dwDataLen];
            if (pszProviderName == NULL) {
                __leave;
            }

            if (! ::CryptGetProvParam( //deprecated
                (HCRYPTPROV) hCryptProv,
                PP_NAME,
                (BYTE *) pszProviderName,
                &dwDataLen,
                0)) {

                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }

            // Convert to a wide char string
            pwszProviderName = new (env) WCHAR[dwDataLen];
            if (pwszProviderName == NULL) {
                __leave;
            }

            if (mbstowcs(pwszProviderName, pszProviderName, dwDataLen) == 0) {
                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }

            // Set the name of the provider
            keyProviderInfo.pwszProvName = pwszProviderName;

            // Get and set the type of the provider
            if (! ::CryptGetProvParam( //deprecated
                (HCRYPTPROV) hCryptProv,
                PP_PROVTYPE,
                (LPBYTE) &keyProviderInfo.dwProvType,
                &dwDataLen,
                0)) {

                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }

            // Set no provider flags
            keyProviderInfo.dwFlags = 0;

            // Set no provider parameters
            keyProviderInfo.cProvParam = 0;
            keyProviderInfo.rgProvParam = NULL;

            // Get the key's algorithm ID
            if (! ::CryptGetKeyParam( //deprecated
                (HCRYPTKEY) hCryptKey,
                KP_ALGID,
                (LPBYTE) &keyProviderInfo.dwKeySpec,
                &dwDataLen,
                0)) {

                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }
            // Set the key spec (using the algorithm ID).
            switch (keyProviderInfo.dwKeySpec) {
            case CALG_RSA_KEYX:
            case CALG_DH_SF:
                keyProviderInfo.dwKeySpec = AT_KEYEXCHANGE;
                break;

            case CALG_RSA_SIGN:
            case CALG_DSS_SIGN:
                keyProviderInfo.dwKeySpec = AT_SIGNATURE;
                break;

            default:
                ThrowException(env, KEYSTORE_EXCEPTION, NTE_BAD_ALGID);
                __leave;
            }

            if (! ::CertSetCertificateContextProperty(pCertContext,
                CERT_KEY_PROV_INFO_PROP_ID, 0, &keyProviderInfo)) {

                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }
        }

        // Import encoded certificate
        if (!::CertAddCertificateContextToStore(hCertStore, pCertContext,
            CERT_STORE_ADD_REPLACE_EXISTING, NULL))
        {
            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

    }
    __finally
    {
        //--------------------------------------------------------------------
        // Clean up.

        if (hCertStore)
            ::CertCloseStore(hCertStore, 0);

        if (pszCertStoreName)
            env->ReleaseStringUTFChars(jCertStoreName, pszCertStoreName);

        if (pbCertEncoding)
            delete [] pbCertEncoding;

        if (pszCertAliasName)
            delete [] pszCertAliasName;

        if (pszContainerName)
            delete [] pszContainerName;

        if (pwszContainerName)
            delete [] pwszContainerName;

        if (pszProviderName)
            delete [] pszProviderName;

        if (pwszProviderName)
            delete [] pwszProviderName;

        if (pCertContext)
            ::CertFreeCertificateContext(pCertContext);
    }
}

/*
 * Class:     sun_security_mscapi_CKeyStore
 * Method:    removeCertificate
 * Signature: (Ljava/lang/String;Ljava/lang/String;[BI)V
 */
JNIEXPORT void JNICALL Java_sun_security_mscapi_CKeyStore_removeCertificate
  (JNIEnv *env, jobject obj, jstring jCertStoreName, jstring jCertAliasName,
  jbyteArray jCertEncoding, jint jCertEncodingSize) {

    const char* pszCertStoreName = NULL;
    const char* pszCertAliasName = NULL;
    HCERTSTORE hCertStore = NULL;
    PCCERT_CONTEXT pCertContext = NULL;
    PCCERT_CONTEXT pTBDCertContext = NULL;
    jbyte* pbCertEncoding = NULL;
    DWORD cchNameString = 0;
    char* pszNameString = NULL; // certificate's friendly name
    BOOL bDeleteAttempted = FALSE;

    __try
    {
        // Open a system certificate store.
        if ((pszCertStoreName = env->GetStringUTFChars(jCertStoreName, NULL))
            == NULL) {
            __leave;
        }
        if ((hCertStore = ::CertOpenSystemStore(NULL, pszCertStoreName)) == NULL) {
            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

        // Copy encoding from Java to native buffer
        pbCertEncoding = new (env) jbyte[jCertEncodingSize];
        if (pbCertEncoding == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jCertEncoding, 0, jCertEncodingSize, pbCertEncoding);

        // Create a certificate context from the encoded cert
        if (!(pCertContext = ::CertCreateCertificateContext(X509_ASN_ENCODING,
            (BYTE*) pbCertEncoding, jCertEncodingSize))) {

            ThrowException(env, CERTIFICATE_PARSING_EXCEPTION, GetLastError());
            __leave;
        }

        // Find the certificate to be deleted
        if (!(pTBDCertContext = ::CertFindCertificateInStore(hCertStore,
            X509_ASN_ENCODING, 0, CERT_FIND_EXISTING, pCertContext, NULL))) {

            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

        // Check that its friendly name matches the supplied alias
        if ((cchNameString = ::CertGetNameString(pTBDCertContext,
                CERT_NAME_FRIENDLY_DISPLAY_TYPE, 0, NULL, NULL, 0)) > 1) {

            pszNameString = new (env) char[cchNameString];
            if (pszNameString == NULL) {
                __leave;
            }

            ::CertGetNameString(pTBDCertContext,
                CERT_NAME_FRIENDLY_DISPLAY_TYPE, 0, NULL, pszNameString,
                cchNameString);

            // Compare the certificate's friendly name with supplied alias name
            if ((pszCertAliasName = env->GetStringUTFChars(jCertAliasName, NULL))
                == NULL) {
                __leave;
            }
            if (strcmp(pszCertAliasName, pszNameString) == 0) {

                // Only delete the certificate if the alias names matches
                if (! ::CertDeleteCertificateFromStore(pTBDCertContext)) {

                    // pTBDCertContext is always freed by the
                    //  CertDeleteCertificateFromStore method
                    bDeleteAttempted = TRUE;

                    ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                    __leave;
                }
            }
        }

    }
    __finally
    {
        //--------------------------------------------------------------------
        // Clean up.

        if (hCertStore)
            ::CertCloseStore(hCertStore, 0);

        if (pszCertStoreName)
            env->ReleaseStringUTFChars(jCertStoreName, pszCertStoreName);

        if (pszCertAliasName)
            env->ReleaseStringUTFChars(jCertAliasName, pszCertAliasName);

        if (pbCertEncoding)
            delete [] pbCertEncoding;

        if (pszNameString)
            delete [] pszNameString;

        if (pCertContext)
            ::CertFreeCertificateContext(pCertContext);

        if (bDeleteAttempted && pTBDCertContext)
            ::CertFreeCertificateContext(pTBDCertContext);
    }
}

/*
 * Class:     sun_security_mscapi_CKeyStore
 * Method:    destroyKeyContainer
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_security_mscapi_CKeyStore_destroyKeyContainer
  (JNIEnv *env, jobject clazz, jstring keyContainerName)
{
    HCRYPTPROV hCryptProv = NULL;
    const char* pszKeyContainerName = NULL;

    __try
    {
        if ((pszKeyContainerName =
            env->GetStringUTFChars(keyContainerName, NULL)) == NULL) {
            __leave;
        }

        // Destroying the default key container is not permitted
        // (because it may contain more one keypair).
        if (pszKeyContainerName == NULL) {

            ThrowException(env, KEYSTORE_EXCEPTION, NTE_BAD_KEYSET_PARAM);
            __leave;
        }

        // Acquire a CSP context (to the key container).
        if (::CryptAcquireContext( //deprecated
            &hCryptProv,
            pszKeyContainerName,
            NULL,
            PROV_RSA_FULL,
            CRYPT_DELETEKEYSET) == FALSE)
        {
            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

    }
    __finally
    {
        //--------------------------------------------------------------------
        // Clean up.

        if (pszKeyContainerName)
            env->ReleaseStringUTFChars(keyContainerName, pszKeyContainerName);
    }
}

/*
 * Class:     sun_security_mscapi_CRSACipher
 * Method:    encryptDecrypt
 * Signature: ([BIJZ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_CRSACipher_encryptDecrypt
  (JNIEnv *env, jclass clazz, jbyteArray jData, jint jDataSize, jlong hKey,
   jboolean doEncrypt)
{
    jbyteArray result = NULL;
    jbyte* pData = NULL;
    DWORD dwDataLen = jDataSize;
    DWORD dwBufLen = env->GetArrayLength(jData);
    DWORD i;
    BYTE tmp;

    __try
    {
        // Copy data from Java buffer to native buffer
        pData = new (env) jbyte[dwBufLen];
        if (pData == NULL) {
            __leave;
        }
        env->GetByteArrayRegion(jData, 0, dwBufLen, pData);

        if (doEncrypt == JNI_TRUE) {
            // encrypt
            if (! ::CryptEncrypt((HCRYPTKEY) hKey, 0, TRUE, 0, (BYTE *)pData, //deprecated
                &dwDataLen, dwBufLen)) {

                ThrowException(env, KEY_EXCEPTION, GetLastError());
                __leave;
            }
            dwBufLen = dwDataLen;

            // convert from little-endian
            for (i = 0; i < dwBufLen / 2; i++) {
                tmp = pData[i];
                pData[i] = pData[dwBufLen - i -1];
                pData[dwBufLen - i - 1] = tmp;
            }
        } else {
            // convert to little-endian
            for (i = 0; i < dwBufLen / 2; i++) {
                tmp = pData[i];
                pData[i] = pData[dwBufLen - i -1];
                pData[dwBufLen - i - 1] = tmp;
            }

            // decrypt
            if (! ::CryptDecrypt((HCRYPTKEY) hKey, 0, TRUE, 0, (BYTE *)pData, //deprecated
                &dwBufLen)) {

                ThrowException(env, KEY_EXCEPTION, GetLastError());
                __leave;
            }
        }

        // Create new byte array
        if ((result = env->NewByteArray(dwBufLen)) == NULL) {
            __leave;
        }

        // Copy data from native buffer to Java buffer
        env->SetByteArrayRegion(result, 0, dwBufLen, (jbyte*) pData);
    }
    __finally
    {
        if (pData)
            delete [] pData;
    }

    return result;
}

/*
 * Class:     sun_security_mscapi_CPublicKey
 * Method:    getPublicKeyBlob
 * Signature: (JJ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_CPublicKey_getPublicKeyBlob
    (JNIEnv *env, jobject clazz, jlong hCryptProv, jlong hCryptKey) {

    jbyteArray blob = NULL;
    DWORD dwBlobLen;
    BYTE* pbKeyBlob = NULL;

    __try
    {

        // Determine the size of the blob
        if (hCryptKey == 0) {
            SS_CHECK(::NCryptExportKey(
                (NCRYPT_KEY_HANDLE)hCryptProv, NULL, BCRYPT_ECCPUBLIC_BLOB,
                NULL, NULL, 0, &dwBlobLen, NCRYPT_SILENT_FLAG));
        } else {
            if (! ::CryptExportKey((HCRYPTKEY) hCryptKey, 0, PUBLICKEYBLOB, 0, NULL, //deprecated
                &dwBlobLen)) {

                ThrowException(env, KEY_EXCEPTION, GetLastError());
                __leave;
            }
        }

        pbKeyBlob = new (env) BYTE[dwBlobLen];
        if (pbKeyBlob == NULL) {
            __leave;
        }

        // Generate key blob
        if (hCryptKey == 0) {
            SS_CHECK(::NCryptExportKey(
                (NCRYPT_KEY_HANDLE)hCryptProv, NULL, BCRYPT_ECCPUBLIC_BLOB,
                NULL, pbKeyBlob, dwBlobLen, &dwBlobLen, NCRYPT_SILENT_FLAG));
        } else {
            if (! ::CryptExportKey((HCRYPTKEY) hCryptKey, 0, PUBLICKEYBLOB, 0, //deprecated
                pbKeyBlob, &dwBlobLen)) {

                ThrowException(env, KEY_EXCEPTION, GetLastError());
                __leave;
            }
        }

        // Create new byte array
        if ((blob = env->NewByteArray(dwBlobLen)) == NULL) {
            __leave;
        }

        // Copy data from native buffer to Java buffer
        env->SetByteArrayRegion(blob, 0, dwBlobLen, (jbyte*) pbKeyBlob);
    }
    __finally
    {
        if (pbKeyBlob)
            delete [] pbKeyBlob;
    }

    return blob;
}

/*
 * Class:     sun_security_mscapi_CPublicKey_CRSAPublicKey
 * Method:    getExponent
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_CPublicKey_00024CRSAPublicKey_getExponent
    (JNIEnv *env, jobject clazz, jbyteArray jKeyBlob) {

    jbyteArray exponent = NULL;
    jbyte*     exponentBytes = NULL;
    jbyte*     keyBlob = NULL;

    __try {

        jsize length = env->GetArrayLength(jKeyBlob);
        jsize headerLength = sizeof(PUBLICKEYSTRUC) + sizeof(RSAPUBKEY);

        if (length < headerLength) {
            ThrowExceptionWithMessage(env, KEY_EXCEPTION, "Invalid BLOB");
            __leave;
        }

        if ((keyBlob = env->GetByteArrayElements(jKeyBlob, 0)) == NULL) {
            __leave;
        }

        PUBLICKEYSTRUC* pPublicKeyStruc = (PUBLICKEYSTRUC *) keyBlob;

        // Check BLOB type
        if (pPublicKeyStruc->bType != PUBLICKEYBLOB) {
            ThrowException(env, KEY_EXCEPTION, NTE_BAD_TYPE);
            __leave;
        }

        RSAPUBKEY* pRsaPubKey =
            (RSAPUBKEY *) (keyBlob + sizeof(PUBLICKEYSTRUC));

        int len = sizeof(pRsaPubKey->pubexp);
        exponentBytes = new (env) jbyte[len];
        if (exponentBytes == NULL) {
            __leave;
        }

        // convert from little-endian while copying from blob
        for (int i = 0, j = len - 1; i < len; i++, j--) {
            exponentBytes[i] = ((BYTE*) &pRsaPubKey->pubexp)[j];
        }

        if ((exponent = env->NewByteArray(len)) == NULL) {
            __leave;
        }
        env->SetByteArrayRegion(exponent, 0, len, exponentBytes);
    }
    __finally
    {
        if (keyBlob)
            env->ReleaseByteArrayElements(jKeyBlob, keyBlob, JNI_ABORT);

        if (exponentBytes)
            delete [] exponentBytes;
    }

    return exponent;
}

/*
 * Class:     sun_security_mscapi_CPublicKey_CRSAPublicKey
 * Method:    getModulus
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_CPublicKey_00024CRSAPublicKey_getModulus
    (JNIEnv *env, jobject clazz, jbyteArray jKeyBlob) {

    jbyteArray modulus = NULL;
    jbyte*     modulusBytes = NULL;
    jbyte*     keyBlob = NULL;

    __try {

        jsize length = env->GetArrayLength(jKeyBlob);
        jsize headerLength = sizeof(PUBLICKEYSTRUC) + sizeof(RSAPUBKEY);

        if (length < headerLength) {
            ThrowExceptionWithMessage(env, KEY_EXCEPTION, "Invalid BLOB");
            __leave;
        }

        if ((keyBlob = env->GetByteArrayElements(jKeyBlob, 0)) == NULL) {
            __leave;
        }

        PUBLICKEYSTRUC* pPublicKeyStruc = (PUBLICKEYSTRUC *) keyBlob;

        // Check BLOB type
        if (pPublicKeyStruc->bType != PUBLICKEYBLOB) {
            ThrowException(env, KEY_EXCEPTION, NTE_BAD_TYPE);
            __leave;
        }

        RSAPUBKEY* pRsaPubKey =
            (RSAPUBKEY *) (keyBlob + sizeof(PUBLICKEYSTRUC));

        int len = pRsaPubKey->bitlen / 8;
        if (len < 0 || len > length - headerLength) {
            ThrowExceptionWithMessage(env, KEY_EXCEPTION, "Invalid key length");
            __leave;
        }

        modulusBytes = new (env) jbyte[len];
        if (modulusBytes == NULL) {
            __leave;
        }
        BYTE * pbModulus = (BYTE *) (keyBlob + headerLength);

        // convert from little-endian while copying from blob
        for (int i = 0, j = len - 1; i < len; i++, j--) {
            modulusBytes[i] = pbModulus[j];
        }

        if ((modulus = env->NewByteArray(len)) == NULL) {
            __leave;
        }
        env->SetByteArrayRegion(modulus, 0, len, modulusBytes);
    }
    __finally
    {
        if (keyBlob)
            env->ReleaseByteArrayElements(jKeyBlob, keyBlob, JNI_ABORT);

        if (modulusBytes)
            delete [] modulusBytes;
    }

    return modulus;
}

/*
 * Convert an array in big-endian byte order into little-endian byte order.
 */
int convertToLittleEndian(JNIEnv *env, jbyteArray source, jbyte* destination,
        int destinationLength) {

    int result = -1;
    jbyte* sourceBytes = NULL;

    __try {
        int sourceLength = env->GetArrayLength(source);

        sourceBytes = env->GetByteArrayElements(source, 0);
        if (sourceBytes == NULL) {
            __leave;
        }

        int copyLen = sourceLength;
        if (sourceLength > destinationLength) {
            // source might include an extra sign byte
            if (sourceLength == destinationLength + 1 && sourceBytes[0] == 0) {
                copyLen--;
            } else {
                __leave;
            }
        }

        // Copy bytes from the end of the source array to the beginning of the
        // destination array (until the destination array is full).
        // This ensures that the sign byte from the source array will be excluded.
        for (int i = 0; i < copyLen; i++) {
            destination[i] = sourceBytes[sourceLength - 1 - i];
        }
        if (copyLen < destinationLength) {
            memset(destination + copyLen, 0, destinationLength - copyLen);
        }
        result = destinationLength;
    } __finally {
        // Clean up.
        if (sourceBytes) {
            env->ReleaseByteArrayElements(source, sourceBytes, JNI_ABORT);
        }
    }

    return result;
}

/*
 * The Microsoft Base Cryptographic Provider supports public-key BLOBs
 * that have the following format:
 *
 *     PUBLICKEYSTRUC publickeystruc;
 *     RSAPUBKEY rsapubkey;
 *     BYTE modulus[rsapubkey.bitlen/8];
 *
 * and private-key BLOBs that have the following format:
 *
 *     PUBLICKEYSTRUC publickeystruc;
 *     RSAPUBKEY rsapubkey;
 *     BYTE modulus[rsapubkey.bitlen/8];
 *     BYTE prime1[rsapubkey.bitlen/16];
 *     BYTE prime2[rsapubkey.bitlen/16];
 *     BYTE exponent1[rsapubkey.bitlen/16];
 *     BYTE exponent2[rsapubkey.bitlen/16];
 *     BYTE coefficient[rsapubkey.bitlen/16];
 *     BYTE privateExponent[rsapubkey.bitlen/8];
 *
 * This method generates such BLOBs from the key elements supplied.
 */
jbyteArray generateKeyBlob(
        JNIEnv *env,
        jint jKeyBitLength,
        jbyteArray jModulus,
        jbyteArray jPublicExponent,
        jbyteArray jPrivateExponent,
        jbyteArray jPrimeP,
        jbyteArray jPrimeQ,
        jbyteArray jExponentP,
        jbyteArray jExponentQ,
        jbyteArray jCrtCoefficient)
{
    jsize jKeyByteLength = jKeyBitLength / 8;
    jsize jBlobLength;
    BOOL bGeneratePrivateKeyBlob;

    // Determine whether to generate a public-key or a private-key BLOB
    if (jPrivateExponent != NULL &&
        jPrimeP != NULL &&
        jPrimeQ != NULL &&
        jExponentP != NULL &&
        jExponentQ != NULL &&
        jCrtCoefficient != NULL) {

        bGeneratePrivateKeyBlob = TRUE;
        jBlobLength = sizeof(BLOBHEADER) +
                        sizeof(RSAPUBKEY) +
                        ((jKeyBitLength / 8) * 4) +
                        (jKeyBitLength / 16);

    } else {
        bGeneratePrivateKeyBlob = FALSE;
        jBlobLength = sizeof(BLOBHEADER) +
                        sizeof(RSAPUBKEY) +
                        (jKeyBitLength / 8);
    }

    jbyte* jBlobBytes = NULL;
    jbyte* jBlobElement;
    jbyteArray jBlob = NULL;
    jsize  jElementLength;

    __try {
        jBlobBytes = new (env) jbyte[jBlobLength];
        if (jBlobBytes == NULL) {
            __leave;
        }

        BLOBHEADER *pBlobHeader = (BLOBHEADER *) jBlobBytes;
        if (bGeneratePrivateKeyBlob) {
            pBlobHeader->bType = PRIVATEKEYBLOB;  // 0x07
        } else {
            pBlobHeader->bType = PUBLICKEYBLOB;   // 0x06
        }
        pBlobHeader->bVersion = CUR_BLOB_VERSION; // 0x02
        pBlobHeader->reserved = 0;                // 0x0000
        pBlobHeader->aiKeyAlg = CALG_RSA_KEYX;    // 0x0000a400

        RSAPUBKEY *pRsaPubKey =
            (RSAPUBKEY *) (jBlobBytes + sizeof(PUBLICKEYSTRUC));
        if (bGeneratePrivateKeyBlob) {
            pRsaPubKey->magic = 0x32415352;       // "RSA2"
        } else {
            pRsaPubKey->magic = 0x31415352;       // "RSA1"
        }
        pRsaPubKey->bitlen = jKeyBitLength;
        pRsaPubKey->pubexp = 0; // init

        // Sanity check
        jsize jPublicExponentLength = env->GetArrayLength(jPublicExponent);
        if (jPublicExponentLength > sizeof(pRsaPubKey->pubexp)) {
            ThrowException(env, INVALID_KEY_EXCEPTION, NTE_BAD_TYPE);
            __leave;
        }
        // The length argument must be the smaller of jPublicExponentLength
        // and sizeof(pRsaPubKey->pubkey)
        if ((jElementLength = convertToLittleEndian(env, jPublicExponent,
            (jbyte *) &(pRsaPubKey->pubexp), jPublicExponentLength)) < 0) {
            __leave;
        }

        // Modulus n
        jBlobElement =
            (jbyte *) (jBlobBytes + sizeof(PUBLICKEYSTRUC) + sizeof(RSAPUBKEY));
        if ((jElementLength = convertToLittleEndian(env, jModulus, jBlobElement,
            jKeyByteLength)) < 0) {
            __leave;
        }

        if (bGeneratePrivateKeyBlob) {
            // Prime p
            jBlobElement += jElementLength;
            if ((jElementLength = convertToLittleEndian(env, jPrimeP,
                jBlobElement, jKeyByteLength / 2)) < 0) {
                __leave;
            }

            // Prime q
            jBlobElement += jElementLength;
            if ((jElementLength = convertToLittleEndian(env, jPrimeQ,
                jBlobElement, jKeyByteLength / 2)) < 0) {
                __leave;
            }

            // Prime exponent p
            jBlobElement += jElementLength;
            if ((jElementLength = convertToLittleEndian(env, jExponentP,
                jBlobElement, jKeyByteLength / 2)) < 0) {
                __leave;
            }

            // Prime exponent q
            jBlobElement += jElementLength;
            if ((jElementLength = convertToLittleEndian(env, jExponentQ,
                jBlobElement, jKeyByteLength / 2)) < 0) {
                __leave;
            }

            // CRT coefficient
            jBlobElement += jElementLength;
            if ((jElementLength = convertToLittleEndian(env, jCrtCoefficient,
                jBlobElement, jKeyByteLength / 2)) < 0) {
                __leave;
            }

            // Private exponent
            jBlobElement += jElementLength;
            if ((jElementLength = convertToLittleEndian(env, jPrivateExponent,
                jBlobElement, jKeyByteLength)) < 0) {
                __leave;
            }
        }

        if ((jBlob = env->NewByteArray(jBlobLength)) == NULL) {
            __leave;
        }
        env->SetByteArrayRegion(jBlob, 0, jBlobLength, jBlobBytes);

    }
    __finally
    {
        if (jBlobBytes)
            delete [] jBlobBytes;
    }

    return jBlob;
}

/*
 * Class:     sun_security_mscapi_CKeyStore
 * Method:    generateRSAPrivateKeyBlob
 * Signature: (I[B[B[B[B[B[B[B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_CKeyStore_generateRSAPrivateKeyBlob
    (JNIEnv *env, jobject clazz,
        jint jKeyBitLength,
        jbyteArray jModulus,
        jbyteArray jPublicExponent,
        jbyteArray jPrivateExponent,
        jbyteArray jPrimeP,
        jbyteArray jPrimeQ,
        jbyteArray jExponentP,
        jbyteArray jExponentQ,
        jbyteArray jCrtCoefficient)
{
    return generateKeyBlob(env, jKeyBitLength, jModulus, jPublicExponent,
        jPrivateExponent, jPrimeP, jPrimeQ, jExponentP, jExponentQ,
        jCrtCoefficient);
}

/*
 * Class:     sun_security_mscapi_CSignature_RSA
 * Method:    generatePublicKeyBlob
 * Signature: (I[B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_security_mscapi_CSignature_00024RSA_generatePublicKeyBlob
    (JNIEnv *env, jclass clazz,
        jint jKeyBitLength,
        jbyteArray jModulus,
        jbyteArray jPublicExponent)
{
    return generateKeyBlob(env, jKeyBitLength, jModulus, jPublicExponent,
        NULL, NULL, NULL, NULL, NULL, NULL);
}

/*
 * Class:     sun_security_mscapi_CKeyStore
 * Method:    storePrivateKey
 * Signature: (Ljava/lang/String;[BLjava/lang/String;I)Lsun/security/mscapi/CPrivateKey;
 */
JNIEXPORT jobject JNICALL Java_sun_security_mscapi_CKeyStore_storePrivateKey
    (JNIEnv *env, jobject clazz, jstring alg, jbyteArray keyBlob,
     jstring keyContainerName, jint keySize)
{
    HCRYPTPROV hCryptProv = NULL;
    HCRYPTKEY hKey = NULL;
    DWORD dwBlobLen;
    BYTE * pbKeyBlob = NULL;
    const char* pszKeyContainerName = NULL; // UUID
    jobject privateKey = NULL;

    __try
    {
        if ((pszKeyContainerName =
            env->GetStringUTFChars(keyContainerName, NULL)) == NULL) {
            __leave;
        }
        dwBlobLen = env->GetArrayLength(keyBlob);
        if ((pbKeyBlob = (BYTE *) env->GetByteArrayElements(keyBlob, 0))
            == NULL) {
            __leave;
        }

        // Acquire a CSP context (create a new key container).
        if (::CryptAcquireContext( //deprecated
            &hCryptProv,
            pszKeyContainerName,
            NULL,
            PROV_RSA_FULL,
            CRYPT_NEWKEYSET) == FALSE)
        {
            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

        // Import the private key
        if (::CryptImportKey( //deprecated
            hCryptProv,
            pbKeyBlob,
            dwBlobLen,
            0,
            CRYPT_EXPORTABLE,
            &hKey) == FALSE)
        {
            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

        // Get the method ID for the CPrivateKey constructor
        jclass clazzCPrivateKey =
            env->FindClass("sun/security/mscapi/CPrivateKey");
        if (clazzCPrivateKey == NULL) {
            __leave;
        }

        jmethodID mNewCPrivateKey =
            env->GetStaticMethodID(clazzCPrivateKey, "of",
            "(Ljava/lang/String;JJI)Lsun/security/mscapi/CPrivateKey;");
        if (mNewCPrivateKey == NULL) {
            __leave;
        }

        // Create a new private key
        privateKey = env->CallStaticObjectMethod(clazzCPrivateKey, mNewCPrivateKey,
            alg, (jlong) hCryptProv, (jlong) hKey, keySize);

    }
    __finally
    {
        //--------------------------------------------------------------------
        // Clean up.

        if (pszKeyContainerName)
            env->ReleaseStringUTFChars(keyContainerName, pszKeyContainerName);

        if (pbKeyBlob)
            env->ReleaseByteArrayElements(keyBlob, (jbyte *) pbKeyBlob,
                JNI_ABORT);
    }

    return privateKey;
}

/*
 * Class:     sun_security_mscapi_CSignature
 * Method:    importECPublicKey
 * Signature: (Ljava/lang/String;[BI)Lsun/security/mscapi/CPublicKey;
 */
JNIEXPORT jobject JNICALL Java_sun_security_mscapi_CSignature_importECPublicKey
    (JNIEnv *env, jclass clazz, jstring alg, jbyteArray keyBlob, jint keySize)
{
    BCRYPT_ALG_HANDLE hSignAlg = NULL;
    NCRYPT_KEY_HANDLE       hTmpKey         = NULL;
    DWORD dwBlobLen;
    BYTE * pbKeyBlob = NULL;
    jobject publicKey = NULL;

    __try
    {
        dwBlobLen = env->GetArrayLength(keyBlob);
        if ((pbKeyBlob = (BYTE *) env->GetByteArrayElements(keyBlob, 0))
            == NULL) {
            __leave;
        }
        dump("NCryptImportKey", pbKeyBlob, dwBlobLen);
        NCRYPT_PROV_HANDLE hProv;
        SS_CHECK(NCryptOpenStorageProvider(
                &hProv, L"Microsoft Software Key Storage Provider", 0 ));
        SS_CHECK(NCryptImportKey(
                                                    hProv,
                                                    NULL,
                                                    BCRYPT_ECCPUBLIC_BLOB,
                                                    NULL,
                                                    &hTmpKey,
                                                    pbKeyBlob,
                                                    dwBlobLen,
                                                    0));
        NCryptFreeObject( hProv );
        // Get the method ID for the CPublicKey constructor
        jclass clazzCPublicKey =
            env->FindClass("sun/security/mscapi/CPublicKey");
        if (clazzCPublicKey == NULL) {
            __leave;
        }

        jmethodID mNewCPublicKey =
            env->GetStaticMethodID(clazzCPublicKey, "of",
            "(Ljava/lang/String;JJI)Lsun/security/mscapi/CPublicKey;");
        if (mNewCPublicKey == NULL) {
            __leave;
        }

        // Create a new public key
        publicKey = env->CallStaticObjectMethod(clazzCPublicKey, mNewCPublicKey,
            alg, (jlong) hTmpKey, (jlong) 0, keySize);
    }
    __finally
    {
    }

    return publicKey;
}

/*
 * Class:     sun_security_mscapi_CSignature
 * Method:    importPublicKey
 * Signature: (Ljava/lang/String;[BI)Lsun/security/mscapi/CPublicKey;
 */
JNIEXPORT jobject JNICALL Java_sun_security_mscapi_CSignature_importPublicKey
    (JNIEnv *env, jclass clazz, jstring alg, jbyteArray keyBlob, jint keySize)
{
    HCRYPTPROV hCryptProv = NULL;
    HCRYPTKEY hKey = NULL;
    DWORD dwBlobLen;
    BYTE * pbKeyBlob = NULL;
    jobject publicKey = NULL;

    __try
    {
        dwBlobLen = env->GetArrayLength(keyBlob);
        if ((pbKeyBlob = (BYTE *) env->GetByteArrayElements(keyBlob, 0))
            == NULL) {
            __leave;
        }

        // Acquire a CSP context (create a new key container).
        // Prefer a PROV_RSA_AES CSP, when available, due to its support
        // for SHA-2-based signatures.
        if (::CryptAcquireContext( //deprecated
            &hCryptProv,
            NULL,
            NULL,
            PROV_RSA_AES,
            CRYPT_VERIFYCONTEXT) == FALSE)
        {
            // Failover to using the default CSP (PROV_RSA_FULL)

            if (::CryptAcquireContext( //deprecated
                &hCryptProv,
                NULL,
                NULL,
                PROV_RSA_FULL,
                CRYPT_VERIFYCONTEXT) == FALSE)
            {
                ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
                __leave;
            }
        }

        // Import the public key
        if (::CryptImportKey( //deprecated
            hCryptProv,
            pbKeyBlob,
            dwBlobLen,
            0,
            CRYPT_EXPORTABLE,
            &hKey) == FALSE)
        {
            ThrowException(env, KEYSTORE_EXCEPTION, GetLastError());
            __leave;
        }

        // Get the method ID for the CPublicKey constructor
        jclass clazzCPublicKey =
            env->FindClass("sun/security/mscapi/CPublicKey");
        if (clazzCPublicKey == NULL) {
            __leave;
        }

        jmethodID mNewCPublicKey =
            env->GetStaticMethodID(clazzCPublicKey, "of",
            "(Ljava/lang/String;JJI)Lsun/security/mscapi/CPublicKey;");
        if (mNewCPublicKey == NULL) {
            __leave;
        }

        // Create a new public key
        publicKey = env->CallStaticObjectMethod(clazzCPublicKey, mNewCPublicKey,
            alg, (jlong) hCryptProv, (jlong) hKey, keySize);

    }
    __finally
    {
        //--------------------------------------------------------------------
        // Clean up.

        if (pbKeyBlob)
            env->ReleaseByteArrayElements(keyBlob, (jbyte *) pbKeyBlob,
                JNI_ABORT);
    }

    return publicKey;
}

} /* extern "C" */
