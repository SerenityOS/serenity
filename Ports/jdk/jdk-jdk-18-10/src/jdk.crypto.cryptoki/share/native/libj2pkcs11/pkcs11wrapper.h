/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 */

/* Copyright  (c) 2002 Graz University of Technology. All rights reserved.
 *
 * Redistribution and use in  source and binary forms, with or without
 * modification, are permitted  provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in  binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment:
 *
 *    "This product includes software developed by IAIK of Graz University of
 *     Technology."
 *
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Graz University of Technology" and "IAIK of Graz University of
 *    Technology" must not be used to endorse or promote products derived from
 *    this software without prior written permission.
 *
 * 5. Products derived from this software may not be called
 *    "IAIK PKCS Wrapper", nor may "IAIK" appear in their name, without prior
 *    written permission of Graz University of Technology.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY  OF SUCH DAMAGE.
 */

/*
 * pkcs11wrapper.h
 * 18.05.2001
 *
 * declaration of all functions used by pkcs11wrapper.c
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */

#ifndef _PKCS11WRAPPER_H
#define _PKCS11WRAPPER_H 1

/* disable asserts in product mode */
#ifndef DEBUG
  #ifndef NDEBUG
    #define NDEBUG
  #endif
#endif

/* extra PKCS#11 constants not in the standard include files */

#define CKA_NETSCAPE_BASE                       (0x80000000 + 0x4E534350)
#define CKA_NETSCAPE_TRUST_BASE                 (CKA_NETSCAPE_BASE + 0x2000)
#define CKA_NETSCAPE_TRUST_SERVER_AUTH          (CKA_NETSCAPE_TRUST_BASE + 8)
#define CKA_NETSCAPE_TRUST_CLIENT_AUTH          (CKA_NETSCAPE_TRUST_BASE + 9)
#define CKA_NETSCAPE_TRUST_CODE_SIGNING (CKA_NETSCAPE_TRUST_BASE + 10)
#define CKA_NETSCAPE_TRUST_EMAIL_PROTECTION     (CKA_NETSCAPE_TRUST_BASE + 11)
#define CKA_NETSCAPE_DB                         0xD5A0DB00
#define CKM_NSS_TLS_PRF_GENERAL                 0x80000373

/*

 Define the PKCS#11 functions to include and exclude. Reduces the size
 of the binary somewhat.

 This list needs to be kept in sync with the mapfile and PKCS11.java

*/

#define P11_ENABLE_C_INITIALIZE
#define P11_ENABLE_C_FINALIZE
#define P11_ENABLE_C_GETINFO
#define P11_ENABLE_C_GETSLOTLIST
#define P11_ENABLE_C_GETSLOTINFO
#define P11_ENABLE_C_GETTOKENINFO
#define P11_ENABLE_C_GETMECHANISMLIST
#define P11_ENABLE_C_GETMECHANISMINFO
#undef  P11_ENABLE_C_INITTOKEN
#undef  P11_ENABLE_C_INITPIN
#undef  P11_ENABLE_C_SETPIN
#define P11_ENABLE_C_OPENSESSION
#define P11_ENABLE_C_CLOSESESSION
#undef  P11_ENABLE_C_CLOSEALLSESSIONS
#define P11_ENABLE_C_GETSESSIONINFO
#define P11_ENABLE_C_GETOPERATIONSTATE
#define P11_ENABLE_C_SETOPERATIONSTATE
#define P11_ENABLE_C_LOGIN
#define P11_ENABLE_C_LOGOUT
#define P11_ENABLE_C_CREATEOBJECT
#define P11_ENABLE_C_COPYOBJECT
#define P11_ENABLE_C_DESTROYOBJECT
#undef  P11_ENABLE_C_GETOBJECTSIZE
#define P11_ENABLE_C_GETATTRIBUTEVALUE
#define P11_ENABLE_C_SETATTRIBUTEVALUE
#define P11_ENABLE_C_FINDOBJECTSINIT
#define P11_ENABLE_C_FINDOBJECTS
#define P11_ENABLE_C_FINDOBJECTSFINAL
#define P11_ENABLE_C_ENCRYPTINIT
#define P11_ENABLE_C_ENCRYPT
#define P11_ENABLE_C_ENCRYPTUPDATE
#define P11_ENABLE_C_ENCRYPTFINAL
#define P11_ENABLE_C_DECRYPTINIT
#define P11_ENABLE_C_DECRYPT
#define P11_ENABLE_C_DECRYPTUPDATE
#define P11_ENABLE_C_DECRYPTFINAL
#define P11_ENABLE_C_DIGESTINIT
#define P11_ENABLE_C_DIGEST
#define P11_ENABLE_C_DIGESTUPDATE
#define P11_ENABLE_C_DIGESTKEY
#define P11_ENABLE_C_DIGESTFINAL
#define P11_ENABLE_C_SIGNINIT
#define P11_ENABLE_C_SIGN
#define P11_ENABLE_C_SIGNUPDATE
#define P11_ENABLE_C_SIGNFINAL
#define P11_ENABLE_C_SIGNRECOVERINIT
#define P11_ENABLE_C_SIGNRECOVER
#define P11_ENABLE_C_VERIFYINIT
#define P11_ENABLE_C_VERIFY
#define P11_ENABLE_C_VERIFYUPDATE
#define P11_ENABLE_C_VERIFYFINAL
#define P11_ENABLE_C_VERIFYRECOVERINIT
#define P11_ENABLE_C_VERIFYRECOVER
#undef  P11_ENABLE_C_DIGESTENCRYPTUPDATE
#undef  P11_ENABLE_C_DECRYPTDIGESTUPDATE
#undef  P11_ENABLE_C_SIGNENCRYPTUPDATE
#undef  P11_ENABLE_C_DECRYPTVERIFYUPDATE
#define P11_ENABLE_C_GENERATEKEY
#define P11_ENABLE_C_GENERATEKEYPAIR
#define P11_ENABLE_C_WRAPKEY
#define P11_ENABLE_C_UNWRAPKEY
#define P11_ENABLE_C_DERIVEKEY
#define P11_ENABLE_C_SEEDRANDOM
#define P11_ENABLE_C_GENERATERANDOM
#undef  P11_ENABLE_C_GETFUNCTIONSTATUS
#undef  P11_ENABLE_C_CANCELFUNCTION
#undef  P11_ENABLE_C_WAITFORSLOTEVENT
#define P11_ENABLE_GETNATIVEKEYINFO
#define P11_ENABLE_CREATENATIVEKEY


/* include the platform dependent part of the header */
#include "p11_md.h"

#include <jni.h>
#include <jni_util.h>
#include <stdarg.h>

#define MAX_STACK_BUFFER_LEN (4 * 1024)
#define MAX_HEAP_BUFFER_LEN (64 * 1024)

#define MAX_DIGEST_LEN (64)

#ifndef min
#define min(a, b)       (((a) < (b)) ? (a) : (b))
#endif

#define ckBBoolToJBoolean(x) ((x == TRUE) ? JNI_TRUE : JNI_FALSE);
#define jBooleanToCKBBool(x) ((x == JNI_TRUE) ? TRUE : FALSE);

#define ckByteToJByte(x) ((jbyte) x)
#define jByteToCKByte(x) ((CK_BYTE) x)

#define ckLongToJLong(x) ((jlong) x)
#define jLongToCKLong(x) ((CK_LONG) x)

#define ckULongToJLong(x) ((jlong) x)
#define jLongToCKULong(x) ((CK_ULONG) x)

// For CK_UNAVAILABLE_INFORMATION, always return -1 to avoid 32/64 bit problems.
#define ckULongSpecialToJLong(x) (((x) == CK_UNAVAILABLE_INFORMATION) \
    ? (jlong)-1 : ((jlong) x))

#define ckCharToJChar(x) ((jchar) x)
#define jCharToCKChar(x) ((CK_CHAR) x)

#define ckUTF8CharToJChar(x) ((jchar) x)
#define jCharToCKUTF8Char(x) ((CK_UTF8CHAR) x)

#define ckFlageToJLong(x) ((jlong) x)

#define ckVoidPtrToJObject(x) ((jobject) x)
#define jObjectToCKVoidPtr(x) ((CK_VOID_PTR) x)

#define jIntToCKLong(x)         ((CK_LONG) x)
#define jIntToCKULong(x)        ((CK_ULONG) x)
#define ckLongToJInt(x)         ((jint) x)
#define ckULongToJInt(x)        ((jint) x)
#define ckULongToJSize(x)       ((jsize) x)
#define unsignedIntToCKULong(x) ((CK_ULONG) x)

//#define TRACE0d(s) { printf(s); fflush(stdout); }
//#define TRACE1d(s, p1) { printf(s, p1); fflush(stdout); }
//#define TRACE2d(s, p1, p2) { printf(s, p1, p2); fflush(stdout); }

#ifdef P11_DEBUG
#define TRACE0(s) { printf(s); fflush(stdout); }
#define TRACE1(s, p1) { printf(s, p1); fflush(stdout); }
#define TRACE2(s, p1, p2) { printf(s, p1, p2); fflush(stdout); }
#define TRACE3(s, p1, p2, p3) { printf(s, p1, p2, p3); fflush(stdout); }
#else
#define TRACE0(s)
#define TRACE1(s, p1)
#define TRACE2(s, p1, p2)
#define TRACE3(s, p1, p2, p3)
#define TRACE_INTEND
#define TRACE_UNINTEND
#endif

/* debug output */
extern jboolean debug;
void printDebug(const char *format, ...);

#define CK_ASSERT_OK 0L

#define CLASS_P11PSSSIGNATURE "sun/security/pkcs11/P11PSSSignature"

#define CLASS_INFO "sun/security/pkcs11/wrapper/CK_INFO"
#define CLASS_VERSION "sun/security/pkcs11/wrapper/CK_VERSION"
#define CLASS_SLOT_INFO "sun/security/pkcs11/wrapper/CK_SLOT_INFO"
#define CLASS_TOKEN_INFO "sun/security/pkcs11/wrapper/CK_TOKEN_INFO"
#define CLASS_MECHANISM "sun/security/pkcs11/wrapper/CK_MECHANISM"
#define CLASS_MECHANISM_INFO "sun/security/pkcs11/wrapper/CK_MECHANISM_INFO"
#define CLASS_SESSION_INFO "sun/security/pkcs11/wrapper/CK_SESSION_INFO"
#define CLASS_ATTRIBUTE "sun/security/pkcs11/wrapper/CK_ATTRIBUTE"
#define CLASS_DATE "sun/security/pkcs11/wrapper/CK_DATE"
#define CLASS_PKCS11EXCEPTION "sun/security/pkcs11/wrapper/PKCS11Exception"
#define CLASS_PKCS11RUNTIMEEXCEPTION "sun/security/pkcs11/wrapper/PKCS11RuntimeException"
#define CLASS_FILE_NOT_FOUND_EXCEPTION "java/io/FileNotFoundException"
#define CLASS_C_INITIALIZE_ARGS "sun/security/pkcs11/wrapper/CK_C_INITIALIZE_ARGS"
#define CLASS_CREATEMUTEX "sun/security/pkcs11/wrapper/CK_CREATEMUTEX"
#define CLASS_DESTROYMUTEX "sun/security/pkcs11/wrapper/CK_DESTROYMUTEX"
#define CLASS_LOCKMUTEX "sun/security/pkcs11/wrapper/CK_LOCKMUTEX"
#define CLASS_UNLOCKMUTEX "sun/security/pkcs11/wrapper/CK_UNLOCKMUTEX"
#define CLASS_NOTIFY "sun/security/pkcs11/wrapper/CK_NOTIFY"


/* mechanism parameter classes */
#define CLASS_AES_CTR_PARAMS "sun/security/pkcs11/wrapper/CK_AES_CTR_PARAMS"
#define CLASS_GCM_PARAMS "sun/security/pkcs11/wrapper/CK_GCM_PARAMS"
#define CLASS_CCM_PARAMS "sun/security/pkcs11/wrapper/CK_CCM_PARAMS"
#define CLASS_SALSA20_CHACHA20_POLY1305_PARAMS \
        "sun/security/pkcs11/wrapper/CK_SALSA20_CHACHA20_POLY1305_PARAMS"
#define CLASS_RSA_PKCS_PSS_PARAMS "sun/security/pkcs11/wrapper/CK_RSA_PKCS_PSS_PARAMS"
#define CLASS_RSA_PKCS_OAEP_PARAMS "sun/security/pkcs11/wrapper/CK_RSA_PKCS_OAEP_PARAMS"

#define CLASS_MAC_GENERAL_PARAMS "sun/security/pkcs11/wrapper/CK_MAC_GENERAL_PARAMS"
#define CLASS_PBE_PARAMS "sun/security/pkcs11/wrapper/CK_PBE_PARAMS"
#define PBE_INIT_VECTOR_SIZE 8
#define CLASS_PKCS5_PBKD2_PARAMS "sun/security/pkcs11/wrapper/CK_PKCS5_PBKD2_PARAMS"
#define CLASS_EXTRACT_PARAMS "sun/security/pkcs11/wrapper/CK_EXTRACT_PARAMS"

#define CLASS_ECDH1_DERIVE_PARAMS "sun/security/pkcs11/wrapper/CK_ECDH1_DERIVE_PARAMS"
#define CLASS_ECDH2_DERIVE_PARAMS "sun/security/pkcs11/wrapper/CK_ECDH2_DERIVE_PARAMS"
#define CLASS_X9_42_DH1_DERIVE_PARAMS "sun/security/pkcs11/wrapper/CK_X9_42_DH1_DERIVE_PARAMS"
#define CLASS_X9_42_DH2_DERIVE_PARAMS "sun/security/pkcs11/wrapper/CK_X9_42_DH2_DERIVE_PARAMS"

/*
#define CLASS_KEA_DERIVE_PARAMS "sun/security/pkcs11/wrapper/CK_KEA_DERIVE_PARAMS"
#define CLASS_RC2_PARAMS "sun/security/pkcs11/wrapper/CK_RC2_PARAMS"
#define CLASS_RC2_CBC_PARAMS "sun/security/pkcs11/wrapper/CK_RC2_CBC_PARAMS"
#define CLASS_RC2_MAC_GENERAL_PARAMS "sun/security/pkcs11/wrapper/CK_RC2_MAC_GENERAL_PARAMS"
#define CLASS_RC5_PARAMS "sun/security/pkcs11/wrapper/CK_RC5_PARAMS"
#define CLASS_RC5_CBC_PARAMS "sun/security/pkcs11/wrapper/CK_RC5_CBC_PARAMS"
#define CLASS_RC5_MAC_GENERAL_PARAMS "sun/security/pkcs11/wrapper/CK_RC5_MAC_GENERAL_PARAMS"
#define CLASS_SKIPJACK_PRIVATE_WRAP_PARAMS "sun/security/pkcs11/wrapper/CK_SKIPJACK_PRIVATE_WRAP_PARAMS"
#define CLASS_SKIPJACK_RELAYX_PARAMS "sun/security/pkcs11/wrapper/CK_SKIPJACK_RELAYX_PARAMS"
#define CLASS_KEY_WRAP_SET_OAEP_PARAMS "sun/security/pkcs11/wrapper/CK_KEY_WRAP_SET_OAEP_PARAMS"
#define CLASS_KEY_DERIVATION_STRING_DATA "sun/security/pkcs11/wrapper/CK_KEY_DERIVATION_STRING_DATA"
*/

#define CLASS_SSL3_RANDOM_DATA "sun/security/pkcs11/wrapper/CK_SSL3_RANDOM_DATA"
// CLASS_SSL3_RANDOM_DATA is used by CLASS_SSL3_MASTER_KEY_DERIVE_PARAMS
#define CLASS_SSL3_KEY_MAT_OUT "sun/security/pkcs11/wrapper/CK_SSL3_KEY_MAT_OUT"
// CLASS_SSL3_KEY_MAT_OUT is used by CLASS_SSL3_KEY_MAT_PARAMS and CK_TLS12_KEY_MAT_PARAMS
#define CLASS_SSL3_MASTER_KEY_DERIVE_PARAMS "sun/security/pkcs11/wrapper/CK_SSL3_MASTER_KEY_DERIVE_PARAMS"
#define CLASS_TLS12_MASTER_KEY_DERIVE_PARAMS "sun/security/pkcs11/wrapper/CK_TLS12_MASTER_KEY_DERIVE_PARAMS"
#define CLASS_SSL3_KEY_MAT_PARAMS "sun/security/pkcs11/wrapper/CK_SSL3_KEY_MAT_PARAMS"
#define CLASS_TLS12_KEY_MAT_PARAMS "sun/security/pkcs11/wrapper/CK_TLS12_KEY_MAT_PARAMS"
#define CLASS_TLS_PRF_PARAMS "sun/security/pkcs11/wrapper/CK_TLS_PRF_PARAMS"
#define CLASS_TLS_MAC_PARAMS "sun/security/pkcs11/wrapper/CK_TLS_MAC_PARAMS"

/* function to update the CK_NSS_GCM_PARAMS in mechanism pointer with
 * CK_GCM_PARAMS
 */
CK_MECHANISM_PTR updateGCMParams(JNIEnv *env, CK_MECHANISM_PTR mechPtr);

/* function to convert a PKCS#11 return value other than CK_OK into a Java Exception
 * or to throw a PKCS11RuntimeException
 */

jlong ckAssertReturnValueOK(JNIEnv *env, CK_RV returnValue);
jlong ckAssertReturnValueOK2(JNIEnv *env, CK_RV returnValue, const char *msg);
void throwOutOfMemoryError(JNIEnv *env, const char *message);
void throwNullPointerException(JNIEnv *env, const char *message);
void throwIOException(JNIEnv *env, const char *message);
void throwPKCS11RuntimeException(JNIEnv *env, const char *message);
void throwDisconnectedRuntimeException(JNIEnv *env);

/* functions to free CK structures and pointers
 */
void freeCKAttributeArray(CK_ATTRIBUTE_PTR attrPtr, int len);
void freeCKMechanismPtr(CK_MECHANISM_PTR mechPtr);

/* functions to convert Java arrays to a CK-type array and the array length */

void jBooleanArrayToCKBBoolArray(JNIEnv *env, const jbooleanArray jArray, CK_BBOOL **ckpArray, CK_ULONG_PTR ckLength);
void jByteArrayToCKByteArray(JNIEnv *env, const jbyteArray jArray, CK_BYTE_PTR *ckpArray, CK_ULONG_PTR ckLength);
void jLongArrayToCKULongArray(JNIEnv *env, const jlongArray jArray, CK_ULONG_PTR *ckpArray, CK_ULONG_PTR ckLength);
void jCharArrayToCKCharArray(JNIEnv *env, const jcharArray jArray, CK_CHAR_PTR *ckpArray, CK_ULONG_PTR ckLength);
void jCharArrayToCKUTF8CharArray(JNIEnv *env, const jcharArray jArray, CK_UTF8CHAR_PTR *ckpArray, CK_ULONG_PTR ckLength);
void jStringToCKUTF8CharArray(JNIEnv *env, const jstring jArray, CK_UTF8CHAR_PTR *ckpArray, CK_ULONG_PTR ckpLength);
void jAttributeArrayToCKAttributeArray(JNIEnv *env, jobjectArray jAArray, CK_ATTRIBUTE_PTR *ckpArray, CK_ULONG_PTR ckpLength);
/*void jObjectArrayToCKVoidPtrArray(JNIEnv *env, const jobjectArray jArray, CK_VOID_PTR_PTR ckpArray, CK_ULONG_PTR ckpLength); */


/* functions to convert a CK-type array and the array length to a Java array */

jbyteArray ckByteArrayToJByteArray(JNIEnv *env, const CK_BYTE_PTR ckpArray, CK_ULONG ckLength);
jlongArray ckULongArrayToJLongArray(JNIEnv *env, const CK_ULONG_PTR ckpArray, CK_ULONG ckLength);
jcharArray ckCharArrayToJCharArray(JNIEnv *env, const CK_CHAR_PTR ckpArray, CK_ULONG length);
jcharArray ckUTF8CharArrayToJCharArray(JNIEnv *env, const CK_UTF8CHAR_PTR ckpArray, CK_ULONG ckLength);


/* functions to convert a CK-type structure or a pointer to a CK-value to a Java object */

jobject ckBBoolPtrToJBooleanObject(JNIEnv *env, const CK_BBOOL* ckpValue);
jobject ckULongPtrToJLongObject(JNIEnv *env, const CK_ULONG_PTR ckpValue);
jobject ckDatePtrToJDateObject(JNIEnv *env, const CK_DATE *ckpValue);
jobject ckVersionPtrToJVersion(JNIEnv *env, const CK_VERSION_PTR ckpVersion);
jobject ckSessionInfoPtrToJSessionInfo(JNIEnv *env, const CK_SESSION_INFO_PTR ckpSessionInfo);
jobject ckAttributePtrToJAttribute(JNIEnv *env, const CK_ATTRIBUTE_PTR ckpAttribute);


/* function to convert the CK-value used by the CK_ATTRIBUTE structure to a Java object */

jobject ckAttributeValueToJObject(JNIEnv *env, const CK_ATTRIBUTE_PTR ckpAttribute);


/* functions to convert a Java object to a CK-type structure or a pointer to a CK-value */

CK_BBOOL* jBooleanObjectToCKBBoolPtr(JNIEnv *env, jobject jObject);
CK_BYTE_PTR jByteObjectToCKBytePtr(JNIEnv *env, jobject jObject);
CK_ULONG* jIntegerObjectToCKULongPtr(JNIEnv *env, jobject jObject);
CK_ULONG* jLongObjectToCKULongPtr(JNIEnv *env, jobject jObject);
CK_CHAR_PTR jCharObjectToCKCharPtr(JNIEnv *env, jobject jObject);
CK_VERSION_PTR jVersionToCKVersionPtr(JNIEnv *env, jobject jVersion);
CK_DATE * jDateObjectToCKDatePtr(JNIEnv *env, jobject jDate);
CK_ATTRIBUTE jAttributeToCKAttribute(JNIEnv *env, jobject jAttribute);
CK_MECHANISM_PTR jMechanismToCKMechanismPtr(JNIEnv *env, jobject jMechanism);


/* functions to convert Java objects used by the Mechanism and Attribute class to a CK-type structure */
CK_VOID_PTR jObjectToPrimitiveCKObjectPtr(JNIEnv *env, jobject jObject, CK_ULONG *ckpLength);
CK_VOID_PTR jMechParamToCKMechParamPtr(JNIEnv *env, jobject jParam, CK_MECHANISM_TYPE, CK_ULONG
*ckpLength);


/* functions to convert a specific Java mechanism parameter object to a CK-mechanism parameter pointer */

CK_RSA_PKCS_OAEP_PARAMS_PTR jRsaPkcsOaepParamToCKRsaPkcsOaepParamPtr(JNIEnv *env,
    jobject jParam, CK_ULONG* pLength);
CK_PBE_PARAMS_PTR jPbeParamToCKPbeParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);
CK_PKCS5_PBKD2_PARAMS_PTR jPkcs5Pbkd2ParamToCKPkcs5Pbkd2ParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);
CK_SSL3_MASTER_KEY_DERIVE_PARAMS_PTR jSsl3MasterKeyDeriveParamToCKSsl3MasterKeyDeriveParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);
CK_SSL3_KEY_MAT_PARAMS_PTR jSsl3KeyMatParamToCKSsl3KeyMatParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);
CK_KEY_DERIVATION_STRING_DATA jKeyDerivationStringDataToCKKeyDerivationStringData(JNIEnv *env, jobject jParam);
CK_RSA_PKCS_PSS_PARAMS_PTR jRsaPkcsPssParamToCKRsaPkcsPssParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);
CK_ECDH1_DERIVE_PARAMS_PTR jEcdh1DeriveParamToCKEcdh1DeriveParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);
CK_ECDH2_DERIVE_PARAMS_PTR jEcdh2DeriveParamToCKEcdh2DeriveParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);
CK_X9_42_DH1_DERIVE_PARAMS_PTR jX942Dh1DeriveParamToCKX942Dh1DeriveParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);
CK_X9_42_DH2_DERIVE_PARAMS_PTR jX942Dh2DeriveParamToCKX942Dh2DeriveParamPtr(JNIEnv *env, jobject jParam, CK_ULONG* pLength);

/* functions to copy the returned values inside CK-mechanism back to Java object */

void copyBackPBEInitializationVector(JNIEnv *env, CK_MECHANISM *ckMechanism, jobject jMechanism);
void copyBackSetUnwrappedKey(JNIEnv *env, CK_MECHANISM *ckMechanism, jobject jMechanism);
void ssl3CopyBackClientVersion(JNIEnv *env, CK_MECHANISM *ckMechanism, jobject jMechanism);
void tls12CopyBackClientVersion(JNIEnv *env, CK_MECHANISM *ckMechanism, jobject jMechanism);
void ssl3CopyBackKeyMatParams(JNIEnv *env, CK_MECHANISM *ckMechanism, jobject jMechanism);
void tls12CopyBackKeyMatParams(JNIEnv *env, CK_MECHANISM *ckMechanism, jobject jMechanism);


/* functions to convert the InitArgs object for calling the right Java mutex functions */

CK_C_INITIALIZE_ARGS_PTR makeCKInitArgsAdapter(JNIEnv *env, jobject pInitArgs);

#ifndef NO_CALLBACKS /* if the library should not make callbacks; e.g. no javai.lib or jvm.lib available */
CK_RV callJCreateMutex(CK_VOID_PTR_PTR ppMutex);
CK_RV callJDestroyMutex(CK_VOID_PTR pMutex);
CK_RV callJLockMutex(CK_VOID_PTR pMutex);
CK_RV callJUnlockMutex(CK_VOID_PTR pMutex);
#endif /* NO_CALLBACKS */

void putModuleEntry(JNIEnv *env, jobject pkcs11Implementation, ModuleData *moduleData);
ModuleData * removeModuleEntry(JNIEnv *env, jobject pkcs11Implementation);
CK_FUNCTION_LIST_PTR getFunctionList(JNIEnv *env, jobject pkcs11Implementation);

/* A structure to encapsulate the required data for a Notify callback */
struct NotifyEncapsulation {

    /* The object that implements the CK_NOTIFY interface and which should be
     * notified.
     */
    jobject jNotifyObject;

    /* The data object to pass back to the Notify object upon callback. */
    jobject jApplicationData;
};
typedef struct NotifyEncapsulation NotifyEncapsulation;

/* The function for handling notify callbacks. */
CK_RV notifyCallback(
    CK_SESSION_HANDLE hSession,     /* the session's handle */
    CK_NOTIFICATION   event,
    CK_VOID_PTR       pApplication  /* passed to C_OpenSession */
);


/* A node of the list of notify callbacks. To be able to free the resources after use. */
struct NotifyListNode {

    /* The handle of the session this notify object is attached to*/
    CK_SESSION_HANDLE hSession;

    /* Reference to the Notify encapsulation object that was passed to C_OpenSession. */
    NotifyEncapsulation *notifyEncapsulation;

    /* Pointer to the next node in the list. */
    struct NotifyListNode *next;

};
typedef struct NotifyListNode NotifyListNode;

void putNotifyEntry(JNIEnv *env, CK_SESSION_HANDLE hSession, NotifyEncapsulation *notifyEncapsulation);
NotifyEncapsulation * removeNotifyEntry(JNIEnv *env, CK_SESSION_HANDLE hSession);
NotifyEncapsulation * removeFirstNotifyEntry(JNIEnv *env);

jobject createLockObject(JNIEnv *env);
void destroyLockObject(JNIEnv *env, jobject jLockObject);

extern jfieldID pNativeDataID;
extern jfieldID mech_mechanismID;
extern jfieldID mech_pParameterID;
extern jfieldID mech_pHandleID;

extern jclass jByteArrayClass;
extern jclass jLongClass;

#ifndef NO_CALLBACKS
extern NotifyListNode *notifyListHead;
extern jobject notifyListLock;

extern jobject jInitArgsObject;
extern CK_C_INITIALIZE_ARGS_PTR ckpGlobalInitArgs;
#endif /* NO_CALLBACKS */

#ifdef P11_MEMORYDEBUG
#include <stdlib.h>

/* Simple malloc/calloc/free dumper */
void *p11malloc(size_t c, char *file, int line);
void *p11calloc(size_t c, size_t s, char *file, int line);
void p11free(void *p, char *file, int line);

#define malloc(c)       (p11malloc((c), __FILE__, __LINE__))
#define calloc(c, s)    (p11calloc((c), (s), __FILE__, __LINE__))
#define free(c)         (p11free((c), __FILE__, __LINE__))

#endif

#endif /* _PKCS11WRAPPER_H */
