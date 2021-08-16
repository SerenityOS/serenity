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

package sun.security.pkcs11.wrapper;

/**
 * This interface holds constants of the PKCS#11 v3.00 standard.
 * This is mainly the content of the 'pkcs11t.h' header file.
 *
 * Mapping of primitiv data types to Java types:
 * <pre>
 *   TRUE .......................................... true
 *   FALSE ......................................... false
 *   CK_BYTE ....................................... byte
 *   CK_CHAR ....................................... char
 *   CK_UTF8CHAR ................................... char
 *   CK_BBOOL ...................................... boolean
 *   CK_ULONG ...................................... long
 *   CK_LONG ....................................... long
 *   CK_FLAGS ...................................... long
 *   CK_BYTE_PTR ................................... byte[]
 *   CK_CHAR_PTR ................................... char[]
 *   CK_UTF8CHAR_PTR ............................... char[]
 *   CK_ULONG_PTR .................................. long[]
 *   CK_VOID_PTR ................................... Object[]
 *   CK_NOTIFICATION ............................... long
 *   CK_SLOT_ID .................................... long
 *   CK_SESSION_HANDLE ............................. long
 *   CK_USER_TYPE .................................. long
 *   CK_STATE ...................................... long
 *   CK_OBJECT_HANDLE .............................. long
 *   CK_OBJECT_CLASS ............................... long
 *   CK_HW_FEATURE_TYPE ............................ long
 *   CK_KEY_TYPE ................................... long
 *   CK_CERTIFICATE_TYPE ........................... long
 *   CK_ATTRIBUTE_TYPE ............................. long
 *   CK_MECHANISM_TYPE ............................. long
 *   CK_RV ......................................... long
 *   CK_RSA_PKCS_MGF_TYPE .......................... long
 *   CK_RSA_PKCS_OAEP_SOURCE_TYPE .................. long
 *   CK_EC_KDF_TYPE ................................ long
 *   CK_X9_42_DH_KDF_TYPE .......................... long
 *   CK_RC2_PARAMS ................................. long
 *   CK_MAC_GENERAL_PARAMS ......................... long
 *   CK_EXTRACT_PARAMS ............................. long
 *   CK_PKCS5_PBKD2_PSEUDO_RANDOM_FUNCTION_TYPE .... long
 *   CK_PKCS5_PBKDF2_SALT_SOURCE_TYPE .............. long
 *   CK_OTP_PARAM_TYPE / CK_PARAM_TYPE ............. long
 *   CK_GENERATOR_FUNCTION ......................... long
 *   CK_JAVA_MIDP_SECURITY_DOMAIN .................. long
 *   CK_CERTIFICATE_CATEGORY ....................... long
 *   CK_PROFILE_ID ................................. long
 *   CK_PRF_DATA_TYPE .............................. long
 *   CK_SP800_108_DKM_LENGTH_METHOD ................ long
 *   CK_X3DH_KDF_TYPE .............................. long
 *   CK_X2RATCHET_KDF_TYPE ......................... long
 *   CK_XEDDSA_HASH_TYPE ........................... long
 * </pre>
 *
 * @author <a href="mailto:Karl.Scheibelhofer@iaik.at"> Karl Scheibelhofer </a>
 * @invariants
 */
public interface PKCS11Constants {

    public static final boolean TRUE = true;

    public static final boolean FALSE = false;

    public static final Object NULL_PTR = null;

    /* some special values for certain CK_ULONG variables */

    // Cryptoki defines CK_UNAVAILABLE_INFORMATION as (~0UL)
    // This means it is 0xffffffff in ILP32/LLP64 but 0xffffffffffffffff in LP64.
    // To avoid these differences on the Java side, the native code treats
    // CK_UNAVAILABLE_INFORMATION specially and always returns (long)-1 for it.
    // See ckULongSpecialToJLong() in pkcs11wrapper.h
    public static final long CK_UNAVAILABLE_INFORMATION = -1;
    public static final long CK_EFFECTIVELY_INFINITE = 0L;

    /* The following value is always invalid if used as a session */
    /* handle or object handle */
    public static final long CK_INVALID_HANDLE = 0L;

    /* CK_NOTIFICATION enumerates the types of notifications that
     * Cryptoki provides to an application */
    /* CK_NOTIFICATION has been changed from an enum to a CK_ULONG
     * for v2.0 */
    public static final long CKN_SURRENDER = 0L;

    /* flags: bit flags that provide capabilities of the slot
     *      Bit Flag              Mask        Meaning
     */
    public static final long CKF_TOKEN_PRESENT = 0x00000001L;
    public static final long CKF_REMOVABLE_DEVICE = 0x00000002L;
    public static final long CKF_HW_SLOT = 0x00000004L;

    /* The flags parameter is defined as follows:
     *      Bit Flag                    Mask        Meaning
     */
    /* has random # generator */
    public static final long  CKF_RNG                     = 0x00000001L;

    /* token is write-protected */
    public static final long  CKF_WRITE_PROTECTED         = 0x00000002L;

    /* user must login */
    public static final long  CKF_LOGIN_REQUIRED          = 0x00000004L;

    /* normal user's PIN is set */
    public static final long  CKF_USER_PIN_INITIALIZED    = 0x00000008L;

    public static final long  CKF_RESTORE_KEY_NOT_NEEDED  = 0x00000020L;

    public static final long  CKF_CLOCK_ON_TOKEN          = 0x00000040L;

    public static final long  CKF_PROTECTED_AUTHENTICATION_PATH = 0x00000100L;

    public static final long  CKF_DUAL_CRYPTO_OPERATIONS  = 0x00000200L;

    public static final long  CKF_TOKEN_INITIALIZED       = 0x00000400L;

    public static final long  CKF_SECONDARY_AUTHENTICATION  = 0x00000800L;

    public static final long  CKF_USER_PIN_COUNT_LOW       = 0x00010000L;

    public static final long  CKF_USER_PIN_FINAL_TRY       = 0x00020000L;

    public static final long  CKF_USER_PIN_LOCKED          = 0x00040000L;

    public static final long  CKF_USER_PIN_TO_BE_CHANGED   = 0x00080000L;

    public static final long  CKF_SO_PIN_COUNT_LOW         = 0x00100000L;

    public static final long  CKF_SO_PIN_FINAL_TRY         = 0x00200000L;

    public static final long  CKF_SO_PIN_LOCKED            = 0x00400000L;

    public static final long  CKF_SO_PIN_TO_BE_CHANGED     = 0x00800000L;

    /* Security Officer */
    public static final long CKU_SO = 0L;
    /* Normal user */
    public static final long CKU_USER = 1L;

    /* CK_STATE enumerates the session states */
    public static final long  CKS_RO_PUBLIC_SESSION = 0L;
    public static final long  CKS_RO_USER_FUNCTIONS = 1L;
    public static final long  CKS_RW_PUBLIC_SESSION = 2L;
    public static final long  CKS_RW_USER_FUNCTIONS = 3L;
    public static final long  CKS_RW_SO_FUNCTIONS   = 4L;

    /* The flags are defined in the following table:
     *      Bit Flag                Mask        Meaning
     */
    /* session is r/w */
    public static final long  CKF_RW_SESSION        = 0x00000002L;
    /* no parallel */
    public static final long  CKF_SERIAL_SESSION    = 0x00000004L;


    /* The following classes of objects are defined: */
    public static final long  CKO_DATA              = 0x00000000L;
    public static final long  CKO_CERTIFICATE       = 0x00000001L;
    public static final long  CKO_PUBLIC_KEY        = 0x00000002L;
    public static final long  CKO_PRIVATE_KEY       = 0x00000003L;
    public static final long  CKO_SECRET_KEY        = 0x00000004L;
    public static final long  CKO_HW_FEATURE        = 0x00000005L;
    public static final long  CKO_DOMAIN_PARAMETERS = 0x00000006L;
    public static final long  CKO_MECHANISM         = 0x00000007L;
    public static final long  CKO_OTP_KEY           = 0x00000008L;
    public static final long  CKO_PROFILE           = 0x00000009L;

    public static final long  CKO_VENDOR_DEFINED    = 0x80000000L;

    // pseudo object class ANY (for template manager)
    public static final long  PCKO_ANY              = 0x7FFFFF23L;

    /* Uncomment when actually used
    // Profile ID's
    public static final long  CKP_INVALID_ID                = 0x00000000L;
    public static final long  CKP_BASELINE_PROVIDER         = 0x00000001L;
    public static final long  CKP_EXTENDED_PROVIDER         = 0x00000002L;
    public static final long  CKP_AUTHENTICATION_TOKEN      = 0x00000003L;
    public static final long  CKP_PUBLIC_CERTIFICATES_TOKEN = 0x00000004L;
    public static final long  CKP_VENDOR_DEFINED            = 0x80000000L;

    // The following hardware feature types are defined
    public static final long  CKH_MONOTONIC_COUNTER = 0x00000001L;
    public static final long  CKH_CLOCK             = 0x00000002L;
    public static final long  CKH_USER_INTERFACE    = 0x00000003L;
    public static final long  CKH_VENDOR_DEFINED    = 0x80000000L;
    */

    /* the following key types are defined: */
    public static final long  CKK_RSA                  = 0x00000000L;
    public static final long  CKK_DSA                  = 0x00000001L;
    public static final long  CKK_DH                   = 0x00000002L;
    public static final long  CKK_ECDSA /*deprecated*/ = 0x00000003L;
    public static final long  CKK_EC                   = 0x00000003L;
    public static final long  CKK_X9_42_DH             = 0x00000004L;
    public static final long  CKK_KEA                  = 0x00000005L;
    public static final long  CKK_GENERIC_SECRET       = 0x00000010L;
    public static final long  CKK_RC2                  = 0x00000011L;
    public static final long  CKK_RC4                  = 0x00000012L;
    public static final long  CKK_DES                  = 0x00000013L;
    public static final long  CKK_DES2                 = 0x00000014L;
    public static final long  CKK_DES3                 = 0x00000015L;

    public static final long  CKK_CAST                 = 0x00000016L;
    public static final long  CKK_CAST3                = 0x00000017L;
    public static final long  CKK_CAST5 /*deprecated*/ = 0x00000018L;
    public static final long  CKK_CAST128              = 0x00000018L;
    public static final long  CKK_RC5                  = 0x00000019L;
    public static final long  CKK_IDEA                 = 0x0000001AL;
    public static final long  CKK_SKIPJACK             = 0x0000001BL;
    public static final long  CKK_BATON                = 0x0000001CL;
    public static final long  CKK_JUNIPER              = 0x0000001DL;
    public static final long  CKK_CDMF                 = 0x0000001EL;
    public static final long  CKK_AES                  = 0x0000001FL;
    public static final long  CKK_BLOWFISH             = 0x00000020L;
    public static final long  CKK_TWOFISH              = 0x00000021L;
    public static final long  CKK_SECURID              = 0x00000022L;
    public static final long  CKK_HOTP                 = 0x00000023L;
    public static final long  CKK_ACTI                 = 0x00000024L;
    public static final long  CKK_CAMELLIA             = 0x00000025L;
    public static final long  CKK_ARIA                 = 0x00000026L;

    public static final long  CKK_MD5_HMAC             = 0x00000027L;
    public static final long  CKK_SHA_1_HMAC           = 0x00000028L;
    public static final long  CKK_RIPEMD128_HMAC       = 0x00000029L;
    public static final long  CKK_RIPEMD160_HMAC       = 0x0000002AL;
    public static final long  CKK_SHA256_HMAC          = 0x0000002BL;
    public static final long  CKK_SHA384_HMAC          = 0x0000002CL;
    public static final long  CKK_SHA512_HMAC          = 0x0000002DL;
    public static final long  CKK_SHA224_HMAC          = 0x0000002EL;

    public static final long  CKK_SEED                 = 0x0000002FL;
    public static final long  CKK_GOSTR3410            = 0x00000030L;
    public static final long  CKK_GOSTR3411            = 0x00000031L;
    public static final long  CKK_GOST28147            = 0x00000032L;
    public static final long  CKK_CHACHA20             = 0x00000033L;
    public static final long  CKK_POLY1305             = 0x00000034L;
    public static final long  CKK_AES_XTS              = 0x00000035L;

    public static final long  CKK_SHA3_224_HMAC        = 0x00000036L;
    public static final long  CKK_SHA3_256_HMAC        = 0x00000037L;
    public static final long  CKK_SHA3_384_HMAC        = 0x00000038L;
    public static final long  CKK_SHA3_512_HMAC        = 0x00000039L;

    public static final long  CKK_BLAKE2B_160_HMAC     = 0x0000003aL;
    public static final long  CKK_BLAKE2B_256_HMAC     = 0x0000003bL;
    public static final long  CKK_BLAKE2B_384_HMAC     = 0x0000003cL;
    public static final long  CKK_BLAKE2B_512_HMAC     = 0x0000003dL;
    public static final long  CKK_SALSA20              = 0x0000003eL;
    public static final long  CKK_X2RATCHET            = 0x0000003fL;
    public static final long  CKK_EC_EDWARDS           = 0x00000040L;
    public static final long  CKK_EC_MONTGOMERY        = 0x00000041L;
    public static final long  CKK_HKDF                 = 0x00000042L;

    public static final long  CKK_SHA512_224_HMAC      = 0x00000043L;
    public static final long  CKK_SHA512_256_HMAC      = 0x00000044L;
    public static final long  CKK_SHA512_T_HMAC        = 0x00000045L;

    public static final long  CKK_VENDOR_DEFINED       = 0x80000000L;

    // pseudo key type ANY (for template manager)
    public static final long  PCKK_ANY                 = 0x7FFFFF22L;

    public static final long  PCKK_HMAC                = 0x7FFFFF23L;
    public static final long  PCKK_SSLMAC              = 0x7FFFFF24L;
    public static final long  PCKK_TLSPREMASTER        = 0x7FFFFF25L;
    public static final long  PCKK_TLSRSAPREMASTER     = 0x7FFFFF26L;
    public static final long  PCKK_TLSMASTER           = 0x7FFFFF27L;

    /* Uncomment when actually used
    public static final long  CK_CERTIFICATE_CATEGORY_UNSPECIFIED   = 0L;
    public static final long  CK_CERTIFICATE_CATEGORY_TOKEN_USER    = 1L;
    public static final long  CK_CERTIFICATE_CATEGORY_AUTHORITY     = 2L;
    public static final long  CK_CERTIFICATE_CATEGORY_OTHER_ENTITY  = 3L;

    public static final long  CK_SECURITY_DOMAIN_UNSPECIFIED   = 0L;
    public static final long  CK_SECURITY_DOMAIN_MANUFACTURER  = 1L;
    public static final long  CK_SECURITY_DOMAIN_OPERATOR      = 2L;
    public static final long  CK_SECURITY_DOMAIN_THIRD_PARTY   = 3L;
    */

    /* The following certificate types are defined: */
    public static final long  CKC_X_509                = 0x00000000L;
    public static final long  CKC_X_509_ATTR_CERT      = 0x00000001L;
    public static final long  CKC_WTLS                 = 0x00000002L;
    public static final long  CKC_VENDOR_DEFINED       = 0x80000000L;

    /* The CKF_ARRAY_ATTRIBUTE flag identifies an attribute which
     * consists of an array of values.
     */
    public static final long  CKF_ARRAY_ATTRIBUTE      = 0x40000000L;

    /* Uncomment when actually used
    public static final long  CK_OTP_FORMAT_DECIMAL         = 0L;
    public static final long  CK_OTP_FORMAT_HEXADECIMAL     = 1L;
    public static final long  CK_OTP_FORMAT_ALPHANUMERIC    = 2L;
    public static final long  CK_OTP_FORMAT_BINARY          = 3L;

    public static final long  CK_OTP_PARAM_IGNORED          = 0L;
    public static final long  CK_OTP_PARAM_OPTIONAL         = 1L;
    public static final long  CK_OTP_PARAM_MANDATORY        = 2L;
    */

    /* The following attribute types are defined: */
    public static final long  CKA_CLASS              = 0x00000000L;
    public static final long  CKA_TOKEN              = 0x00000001L;
    public static final long  CKA_PRIVATE            = 0x00000002L;
    public static final long  CKA_LABEL              = 0x00000003L;
    public static final long  CKA_UNIQUE_ID          = 0x00000004L;
    public static final long  CKA_APPLICATION        = 0x00000010L;
    public static final long  CKA_VALUE              = 0x00000011L;
    public static final long  CKA_OBJECT_ID          = 0x00000012L;
    public static final long  CKA_CERTIFICATE_TYPE   = 0x00000080L;
    public static final long  CKA_ISSUER             = 0x00000081L;
    public static final long  CKA_SERIAL_NUMBER      = 0x00000082L;
    public static final long  CKA_AC_ISSUER          = 0x00000083L;
    public static final long  CKA_OWNER              = 0x00000084L;
    public static final long  CKA_ATTR_TYPES         = 0x00000085L;
    public static final long  CKA_TRUSTED            = 0x00000086L;
    public static final long  CKA_CERTIFICATE_CATEGORY
                                                     = 0x00000087L;
    public static final long  CKA_JAVA_MIDP_SECURITY_DOMAIN
                                                     = 0x00000088L;
    public static final long  CKA_URL                = 0x00000089L;
    public static final long  CKA_HASH_OF_SUBJECT_PUBLIC_KEY
                                                     = 0x0000008AL;
    public static final long  CKA_HASH_OF_ISSUER_PUBLIC_KEY
                                                     = 0x0000008BL;
    public static final long  CKA_NAME_HASH_ALGORITHM
                                                     = 0x0000008CL;
    public static final long  CKA_CHECK_VALUE        = 0x00000090L;

    public static final long  CKA_KEY_TYPE           = 0x00000100L;
    public static final long  CKA_SUBJECT            = 0x00000101L;
    public static final long  CKA_ID                 = 0x00000102L;
    public static final long  CKA_SENSITIVE          = 0x00000103L;
    public static final long  CKA_ENCRYPT            = 0x00000104L;
    public static final long  CKA_DECRYPT            = 0x00000105L;
    public static final long  CKA_WRAP               = 0x00000106L;
    public static final long  CKA_UNWRAP             = 0x00000107L;
    public static final long  CKA_SIGN               = 0x00000108L;
    public static final long  CKA_SIGN_RECOVER       = 0x00000109L;
    public static final long  CKA_VERIFY             = 0x0000010AL;
    public static final long  CKA_VERIFY_RECOVER     = 0x0000010BL;
    public static final long  CKA_DERIVE             = 0x0000010CL;
    public static final long  CKA_START_DATE         = 0x00000110L;
    public static final long  CKA_END_DATE           = 0x00000111L;
    public static final long  CKA_MODULUS            = 0x00000120L;
    public static final long  CKA_MODULUS_BITS       = 0x00000121L;
    public static final long  CKA_PUBLIC_EXPONENT    = 0x00000122L;
    public static final long  CKA_PRIVATE_EXPONENT   = 0x00000123L;
    public static final long  CKA_PRIME_1            = 0x00000124L;
    public static final long  CKA_PRIME_2            = 0x00000125L;
    public static final long  CKA_EXPONENT_1         = 0x00000126L;
    public static final long  CKA_EXPONENT_2         = 0x00000127L;
    public static final long  CKA_COEFFICIENT        = 0x00000128L;
    public static final long  CKA_PUBLIC_KEY_INFO    = 0x00000129L;
    public static final long  CKA_PRIME              = 0x00000130L;
    public static final long  CKA_SUBPRIME           = 0x00000131L;
    public static final long  CKA_BASE               = 0x00000132L;

    public static final long  CKA_PRIME_BITS         = 0x00000133L;
    public static final long  CKA_SUB_PRIME_BITS     = 0x00000134L;

    public static final long  CKA_VALUE_BITS         = 0x00000160L;
    public static final long  CKA_VALUE_LEN          = 0x00000161L;
    public static final long  CKA_EXTRACTABLE        = 0x00000162L;
    public static final long  CKA_LOCAL              = 0x00000163L;
    public static final long  CKA_NEVER_EXTRACTABLE  = 0x00000164L;
    public static final long  CKA_ALWAYS_SENSITIVE   = 0x00000165L;
    public static final long  CKA_KEY_GEN_MECHANISM  = 0x00000166L;

    public static final long  CKA_MODIFIABLE         = 0x00000170L;
    public static final long  CKA_COPYABLE           = 0x00000171L;
    public static final long  CKA_DESTROYABLE        = 0x00000172L;

    public static final long  CKA_ECDSA_PARAMS /*deprecated*/  = 0x00000180L;
    public static final long  CKA_EC_PARAMS                    = 0x00000180L;
    public static final long  CKA_EC_POINT                     = 0x00000181L;

    public static final long  CKA_SECONDARY_AUTH /*deprecated*/= 0x00000200L;
    public static final long  CKA_AUTH_PIN_FLAGS /*deprecated*/= 0x00000201L;
    public static final long  CKA_ALWAYS_AUTHENTICATE          = 0x00000202L;
    public static final long  CKA_WRAP_WITH_TRUSTED  = 0x00000210L;
    public static final long  CKA_WRAP_TEMPLATE      = (CKF_ARRAY_ATTRIBUTE|0x00000211L);
    public static final long  CKA_UNWRAP_TEMPLATE    = (CKF_ARRAY_ATTRIBUTE|0x00000212L);
    public static final long  CKA_DERIVE_TEMPLATE    = (CKF_ARRAY_ATTRIBUTE|0x00000213L);

    public static final long  CKA_OTP_FORMAT         = 0x00000220L;
    public static final long  CKA_OTP_LENGTH         = 0x00000221L;
    public static final long  CKA_OTP_TIME_INTERVAL  = 0x00000222L;
    public static final long  CKA_OTP_USER_FRIENDLY_MODE       = 0x00000223L;
    public static final long  CKA_OTP_CHALLENGE_REQUIREMENT    = 0x00000224L;
    public static final long  CKA_OTP_TIME_REQUIREMENT         = 0x00000225L;
    public static final long  CKA_OTP_COUNTER_REQUIREMENT      = 0x00000226L;
    public static final long  CKA_OTP_PIN_REQUIREMENT          = 0x00000227L;
    public static final long  CKA_OTP_COUNTER        = 0x0000022EL;
    public static final long  CKA_OTP_TIME           = 0x0000022FL;
    public static final long  CKA_OTP_USER_IDENTIFIER          = 0x0000022AL;
    public static final long  CKA_OTP_SERVICE_IDENTIFIER       = 0x0000022BL;
    public static final long  CKA_OTP_SERVICE_LOGO   = 0x0000022CL;
    public static final long  CKA_OTP_SERVICE_LOGO_TYPE        = 0x0000022DL;

    public static final long  CKA_GOSTR3410_PARAMS   = 0x00000250L;
    public static final long  CKA_GOSTR3411_PARAMS   = 0x00000251L;
    public static final long  CKA_GOST28147_PARAMS   = 0x00000252L;

    public static final long  CKA_HW_FEATURE_TYPE    = 0x00000300L;
    public static final long  CKA_RESET_ON_INIT      = 0x00000301L;
    public static final long  CKA_HAS_RESET          = 0x00000302L;

    public static final long  CKA_PIXEL_X            = 0x00000400L;
    public static final long  CKA_PIXEL_Y            = 0x00000401L;
    public static final long  CKA_RESOLUTION         = 0x00000402L;
    public static final long  CKA_CHAR_ROWS          = 0x00000403L;
    public static final long  CKA_CHAR_COLUMNS       = 0x00000404L;
    public static final long  CKA_COLOR              = 0x00000405L;
    public static final long  CKA_BITS_PER_PIXEL     = 0x00000406L;
    public static final long  CKA_CHAR_SETS          = 0x00000480L;
    public static final long  CKA_ENCODING_METHODS   = 0x00000481L;
    public static final long  CKA_MIME_TYPES         = 0x00000482L;
    public static final long  CKA_MECHANISM_TYPE     = 0x00000500L;
    public static final long  CKA_REQUIRED_CMS_ATTRIBUTES      = 0x00000501L;
    public static final long  CKA_DEFAULT_CMS_ATTRIBUTES       = 0x00000502L;
    public static final long  CKA_SUPPORTED_CMS_ATTRIBUTES     = 0x00000503L;
    public static final long  CKA_ALLOWED_MECHANISMS = (CKF_ARRAY_ATTRIBUTE|0x00000600L);

    public static final long  CKA_PROFILE_ID                   = 0x00000601L;
    public static final long  CKA_X2RATCHET_BAG                = 0x00000602L;
    public static final long  CKA_X2RATCHET_BAGSIZE            = 0x00000603L;
    public static final long  CKA_X2RATCHET_BOBS1STMSG         = 0x00000604L;
    public static final long  CKA_X2RATCHET_CKR                = 0x00000605L;
    public static final long  CKA_X2RATCHET_CKS                = 0x00000606L;
    public static final long  CKA_X2RATCHET_DHP                = 0x00000607L;
    public static final long  CKA_X2RATCHET_DHR                = 0x00000608L;
    public static final long  CKA_X2RATCHET_DHS                = 0x00000609L;
    public static final long  CKA_X2RATCHET_HKR                = 0x0000060aL;
    public static final long  CKA_X2RATCHET_HKS                = 0x0000060bL;
    public static final long  CKA_X2RATCHET_ISALICE            = 0x0000060cL;
    public static final long  CKA_X2RATCHET_NHKR               = 0x0000060dL;
    public static final long  CKA_X2RATCHET_NHKS               = 0x0000060eL;
    public static final long  CKA_X2RATCHET_NR                 = 0x0000060fL;
    public static final long  CKA_X2RATCHET_NS                 = 0x00000610L;
    public static final long  CKA_X2RATCHET_PNS                = 0x00000611L;
    public static final long  CKA_X2RATCHET_RK                 = 0x00000612L;

    public static final long  CKA_VENDOR_DEFINED     = 0x80000000L;

    /* the following mechanism types are defined: */
    public static final long  CKM_RSA_PKCS_KEY_PAIR_GEN      = 0x00000000L;
    public static final long  CKM_RSA_PKCS                   = 0x00000001L;
    public static final long  CKM_RSA_9796                   = 0x00000002L;
    public static final long  CKM_RSA_X_509                  = 0x00000003L;

    public static final long  CKM_MD2_RSA_PKCS               = 0x00000004L;
    public static final long  CKM_MD5_RSA_PKCS               = 0x00000005L;
    public static final long  CKM_SHA1_RSA_PKCS              = 0x00000006L;

    public static final long  CKM_RIPEMD128_RSA_PKCS         = 0x00000007L;
    public static final long  CKM_RIPEMD160_RSA_PKCS         = 0x00000008L;
    public static final long  CKM_RSA_PKCS_OAEP              = 0x00000009L;

    public static final long  CKM_RSA_X9_31_KEY_PAIR_GEN     = 0x0000000AL;
    public static final long  CKM_RSA_X9_31                  = 0x0000000BL;
    public static final long  CKM_SHA1_RSA_X9_31             = 0x0000000CL;
    public static final long  CKM_RSA_PKCS_PSS               = 0x0000000DL;
    public static final long  CKM_SHA1_RSA_PKCS_PSS          = 0x0000000EL;

    public static final long  CKM_DSA_KEY_PAIR_GEN           = 0x00000010L;
    public static final long  CKM_DSA                        = 0x00000011L;
    public static final long  CKM_DSA_SHA1                   = 0x00000012L;
    public static final long  CKM_DSA_SHA224                 = 0x00000013L;
    public static final long  CKM_DSA_SHA256                 = 0x00000014L;
    public static final long  CKM_DSA_SHA384                 = 0x00000015L;
    public static final long  CKM_DSA_SHA512                 = 0x00000016L;
    public static final long  CKM_DSA_SHA3_224               = 0x00000018L;
    public static final long  CKM_DSA_SHA3_256               = 0x00000019L;
    public static final long  CKM_DSA_SHA3_384               = 0x0000001AL;
    public static final long  CKM_DSA_SHA3_512               = 0x0000001BL;

    public static final long  CKM_DH_PKCS_KEY_PAIR_GEN       = 0x00000020L;
    public static final long  CKM_DH_PKCS_DERIVE             = 0x00000021L;

    public static final long  CKM_X9_42_DH_KEY_PAIR_GEN      = 0x00000030L;
    public static final long  CKM_X9_42_DH_DERIVE            = 0x00000031L;
    public static final long  CKM_X9_42_DH_HYBRID_DERIVE     = 0x00000032L;
    public static final long  CKM_X9_42_MQV_DERIVE           = 0x00000033L;

    public static final long  CKM_SHA256_RSA_PKCS            = 0x00000040L;
    public static final long  CKM_SHA384_RSA_PKCS            = 0x00000041L;
    public static final long  CKM_SHA512_RSA_PKCS            = 0x00000042L;
    public static final long  CKM_SHA256_RSA_PKCS_PSS        = 0x00000043L;
    public static final long  CKM_SHA384_RSA_PKCS_PSS        = 0x00000044L;
    public static final long  CKM_SHA512_RSA_PKCS_PSS        = 0x00000045L;

    public static final long  CKM_SHA224_RSA_PKCS            = 0x00000046L;
    public static final long  CKM_SHA224_RSA_PKCS_PSS        = 0x00000047L;

    public static final long  CKM_SHA512_224                 = 0x00000048L;
    public static final long  CKM_SHA512_224_HMAC            = 0x00000049L;
    public static final long  CKM_SHA512_224_HMAC_GENERAL    = 0x0000004AL;
    public static final long  CKM_SHA512_224_KEY_DERIVATION  = 0x0000004BL;
    public static final long  CKM_SHA512_256                 = 0x0000004CL;
    public static final long  CKM_SHA512_256_HMAC            = 0x0000004DL;
    public static final long  CKM_SHA512_256_HMAC_GENERAL    = 0x0000004EL;
    public static final long  CKM_SHA512_256_KEY_DERIVATION  = 0x0000004FL;

    public static final long  CKM_SHA512_T                   = 0x00000050L;
    public static final long  CKM_SHA512_T_HMAC              = 0x00000051L;
    public static final long  CKM_SHA512_T_HMAC_GENERAL      = 0x00000052L;
    public static final long  CKM_SHA512_T_KEY_DERIVATION    = 0x00000053L;

    public static final long  CKM_SHA3_256_RSA_PKCS          = 0x00000060L;
    public static final long  CKM_SHA3_384_RSA_PKCS          = 0x00000061L;
    public static final long  CKM_SHA3_512_RSA_PKCS          = 0x00000062L;
    public static final long  CKM_SHA3_256_RSA_PKCS_PSS      = 0x00000063L;
    public static final long  CKM_SHA3_384_RSA_PKCS_PSS      = 0x00000064L;
    public static final long  CKM_SHA3_512_RSA_PKCS_PSS      = 0x00000065L;
    public static final long  CKM_SHA3_224_RSA_PKCS          = 0x00000066L;
    public static final long  CKM_SHA3_224_RSA_PKCS_PSS      = 0x00000067L;

    public static final long  CKM_RC2_KEY_GEN                = 0x00000100L;
    public static final long  CKM_RC2_ECB                    = 0x00000101L;
    public static final long  CKM_RC2_CBC                    = 0x00000102L;
    public static final long  CKM_RC2_MAC                    = 0x00000103L;

    public static final long  CKM_RC2_MAC_GENERAL            = 0x00000104L;
    public static final long  CKM_RC2_CBC_PAD                = 0x00000105L;

    public static final long  CKM_RC4_KEY_GEN                = 0x00000110L;
    public static final long  CKM_RC4                        = 0x00000111L;
    public static final long  CKM_DES_KEY_GEN                = 0x00000120L;
    public static final long  CKM_DES_ECB                    = 0x00000121L;
    public static final long  CKM_DES_CBC                    = 0x00000122L;
    public static final long  CKM_DES_MAC                    = 0x00000123L;

    public static final long  CKM_DES_MAC_GENERAL            = 0x00000124L;
    public static final long  CKM_DES_CBC_PAD                = 0x00000125L;

    public static final long  CKM_DES2_KEY_GEN               = 0x00000130L;
    public static final long  CKM_DES3_KEY_GEN               = 0x00000131L;
    public static final long  CKM_DES3_ECB                   = 0x00000132L;
    public static final long  CKM_DES3_CBC                   = 0x00000133L;
    public static final long  CKM_DES3_MAC                   = 0x00000134L;

    public static final long  CKM_DES3_MAC_GENERAL           = 0x00000135L;
    public static final long  CKM_DES3_CBC_PAD               = 0x00000136L;
    public static final long  CKM_DES3_CMAC_GENERAL          = 0x00000137L;
    public static final long  CKM_DES3_CMAC                  = 0x00000138L;
    public static final long  CKM_CDMF_KEY_GEN               = 0x00000140L;
    public static final long  CKM_CDMF_ECB                   = 0x00000141L;
    public static final long  CKM_CDMF_CBC                   = 0x00000142L;
    public static final long  CKM_CDMF_MAC                   = 0x00000143L;
    public static final long  CKM_CDMF_MAC_GENERAL           = 0x00000144L;
    public static final long  CKM_CDMF_CBC_PAD               = 0x00000145L;

    public static final long  CKM_DES_OFB64                  = 0x00000150L;
    public static final long  CKM_DES_OFB8                   = 0x00000151L;
    public static final long  CKM_DES_CFB64                  = 0x00000152L;
    public static final long  CKM_DES_CFB8                   = 0x00000153L;

    public static final long  CKM_MD2                        = 0x00000200L;

    public static final long  CKM_MD2_HMAC                   = 0x00000201L;
    public static final long  CKM_MD2_HMAC_GENERAL           = 0x00000202L;

    public static final long  CKM_MD5                        = 0x00000210L;

    public static final long  CKM_MD5_HMAC                   = 0x00000211L;
    public static final long  CKM_MD5_HMAC_GENERAL           = 0x00000212L;

    public static final long  CKM_SHA_1                      = 0x00000220L;

    public static final long  CKM_SHA_1_HMAC                 = 0x00000221L;
    public static final long  CKM_SHA_1_HMAC_GENERAL         = 0x00000222L;

    public static final long  CKM_RIPEMD128                  = 0x00000230L;
    public static final long  CKM_RIPEMD128_HMAC             = 0x00000231L;
    public static final long  CKM_RIPEMD128_HMAC_GENERAL     = 0x00000232L;
    public static final long  CKM_RIPEMD160                  = 0x00000240L;
    public static final long  CKM_RIPEMD160_HMAC             = 0x00000241L;
    public static final long  CKM_RIPEMD160_HMAC_GENERAL     = 0x00000242L;

    public static final long  CKM_SHA256                     = 0x00000250L;
    public static final long  CKM_SHA256_HMAC                = 0x00000251L;
    public static final long  CKM_SHA256_HMAC_GENERAL        = 0x00000252L;
    public static final long  CKM_SHA224                     = 0x00000255L;
    public static final long  CKM_SHA224_HMAC                = 0x00000256L;
    public static final long  CKM_SHA224_HMAC_GENERAL        = 0x00000257L;
    public static final long  CKM_SHA384                     = 0x00000260L;
    public static final long  CKM_SHA384_HMAC                = 0x00000261L;
    public static final long  CKM_SHA384_HMAC_GENERAL        = 0x00000262L;

    public static final long  CKM_SHA512                     = 0x00000270L;
    public static final long  CKM_SHA512_HMAC                = 0x00000271L;
    public static final long  CKM_SHA512_HMAC_GENERAL        = 0x00000272L;

    public static final long  CKM_SECURID_KEY_GEN            = 0x00000280L;
    public static final long  CKM_SECURID                    = 0x00000282L;
    public static final long  CKM_HOTP_KEY_GEN               = 0x00000290L;
    public static final long  CKM_HOTP                       = 0x00000291L;
    public static final long  CKM_ACTI                       = 0x000002A0L;
    public static final long  CKM_ACTI_KEY_GEN               = 0x000002A1L;

    public static final long  CKM_SHA3_256                   = 0x000002B0L;
    public static final long  CKM_SHA3_256_HMAC              = 0x000002B1L;
    public static final long  CKM_SHA3_256_HMAC_GENERAL      = 0x000002B2L;
    public static final long  CKM_SHA3_256_KEY_GEN           = 0x000002B3L;
    public static final long  CKM_SHA3_224                   = 0x000002B5L;
    public static final long  CKM_SHA3_224_HMAC              = 0x000002B6L;
    public static final long  CKM_SHA3_224_HMAC_GENERAL      = 0x000002B7L;
    public static final long  CKM_SHA3_224_KEY_GEN           = 0x000002B8L;
    public static final long  CKM_SHA3_384                   = 0x000002C0L;
    public static final long  CKM_SHA3_384_HMAC              = 0x000002C1L;
    public static final long  CKM_SHA3_384_HMAC_GENERAL      = 0x000002C2L;
    public static final long  CKM_SHA3_384_KEY_GEN           = 0x000002C3L;
    public static final long  CKM_SHA3_512                   = 0x000002D0L;
    public static final long  CKM_SHA3_512_HMAC              = 0x000002D1L;
    public static final long  CKM_SHA3_512_HMAC_GENERAL      = 0x000002D2L;
    public static final long  CKM_SHA3_512_KEY_GEN           = 0x000002D3L;

    public static final long  CKM_CAST_KEY_GEN               = 0x00000300L;
    public static final long  CKM_CAST_ECB                   = 0x00000301L;
    public static final long  CKM_CAST_CBC                   = 0x00000302L;
    public static final long  CKM_CAST_MAC                   = 0x00000303L;
    public static final long  CKM_CAST_MAC_GENERAL           = 0x00000304L;
    public static final long  CKM_CAST_CBC_PAD               = 0x00000305L;
    public static final long  CKM_CAST3_KEY_GEN              = 0x00000310L;
    public static final long  CKM_CAST3_ECB                  = 0x00000311L;
    public static final long  CKM_CAST3_CBC                  = 0x00000312L;
    public static final long  CKM_CAST3_MAC                  = 0x00000313L;
    public static final long  CKM_CAST3_MAC_GENERAL          = 0x00000314L;
    public static final long  CKM_CAST3_CBC_PAD              = 0x00000315L;
    /* Note that CAST128 and CAST5 are the same algorithm */
    public static final long  CKM_CAST5_KEY_GEN                = 0x00000320L;
    public static final long  CKM_CAST128_KEY_GEN              = 0x00000320L;
    public static final long  CKM_CAST5_ECB                    = 0x00000321L;
    public static final long  CKM_CAST128_ECB                  = 0x00000321L;
    public static final long  CKM_CAST5_CBC /*deprecated*/     = 0x00000322L;
    public static final long  CKM_CAST128_CBC                  = 0x00000322L;
    public static final long  CKM_CAST5_MAC /*deprecated*/     = 0x00000323L;
    public static final long  CKM_CAST128_MAC                  = 0x00000323L;
    public static final long  CKM_CAST5_MAC_GENERAL /*deprecated*/
                                                               = 0x00000324L;
    public static final long  CKM_CAST128_MAC_GENERAL          = 0x00000324L;
    public static final long  CKM_CAST5_CBC_PAD /*deprecated*/ = 0x00000325L;
    public static final long  CKM_CAST128_CBC_PAD              = 0x00000325L;
    public static final long  CKM_RC5_KEY_GEN                = 0x00000330L;
    public static final long  CKM_RC5_ECB                    = 0x00000331L;
    public static final long  CKM_RC5_CBC                    = 0x00000332L;
    public static final long  CKM_RC5_MAC                    = 0x00000333L;
    public static final long  CKM_RC5_MAC_GENERAL            = 0x00000334L;
    public static final long  CKM_RC5_CBC_PAD                = 0x00000335L;
    public static final long  CKM_IDEA_KEY_GEN               = 0x00000340L;
    public static final long  CKM_IDEA_ECB                   = 0x00000341L;
    public static final long  CKM_IDEA_CBC                   = 0x00000342L;
    public static final long  CKM_IDEA_MAC                   = 0x00000343L;
    public static final long  CKM_IDEA_MAC_GENERAL           = 0x00000344L;
    public static final long  CKM_IDEA_CBC_PAD               = 0x00000345L;
    public static final long  CKM_GENERIC_SECRET_KEY_GEN     = 0x00000350L;
    public static final long  CKM_CONCATENATE_BASE_AND_KEY   = 0x00000360L;
    public static final long  CKM_CONCATENATE_BASE_AND_DATA  = 0x00000362L;
    public static final long  CKM_CONCATENATE_DATA_AND_BASE  = 0x00000363L;
    public static final long  CKM_XOR_BASE_AND_DATA          = 0x00000364L;
    public static final long  CKM_EXTRACT_KEY_FROM_KEY       = 0x00000365L;
    public static final long  CKM_SSL3_PRE_MASTER_KEY_GEN    = 0x00000370L;
    public static final long  CKM_SSL3_MASTER_KEY_DERIVE     = 0x00000371L;
    public static final long  CKM_SSL3_KEY_AND_MAC_DERIVE    = 0x00000372L;

    public static final long  CKM_SSL3_MASTER_KEY_DERIVE_DH  = 0x00000373L;
    public static final long  CKM_TLS_PRE_MASTER_KEY_GEN     = 0x00000374L;
    public static final long  CKM_TLS_MASTER_KEY_DERIVE      = 0x00000375L;
    public static final long  CKM_TLS_KEY_AND_MAC_DERIVE     = 0x00000376L;
    public static final long  CKM_TLS_MASTER_KEY_DERIVE_DH   = 0x00000377L;
    public static final long  CKM_TLS_PRF                    = 0x00000378L;

    public static final long  CKM_SSL3_MD5_MAC               = 0x00000380L;
    public static final long  CKM_SSL3_SHA1_MAC              = 0x00000381L;
    public static final long  CKM_MD5_KEY_DERIVATION         = 0x00000390L;
    public static final long  CKM_MD2_KEY_DERIVATION         = 0x00000391L;
    public static final long  CKM_SHA1_KEY_DERIVATION        = 0x00000392L;
    public static final long  CKM_SHA256_KEY_DERIVATION      = 0x00000393L;
    public static final long  CKM_SHA384_KEY_DERIVATION      = 0x00000394L;
    public static final long  CKM_SHA512_KEY_DERIVATION      = 0x00000395L;
    public static final long  CKM_SHA224_KEY_DERIVATION      = 0x00000396L;
    public static final long  CKM_SHA3_256_KEY_DERIVATION    = 0x00000397L;
    public static final long  CKM_SHA3_224_KEY_DERIVATION    = 0x00000398L;
    public static final long  CKM_SHA3_384_KEY_DERIVATION    = 0x00000399L;
    public static final long  CKM_SHA3_512_KEY_DERIVATION    = 0x0000039AL;
    public static final long  CKM_SHAKE_128_KEY_DERIVATION   = 0x0000039BL;
    public static final long  CKM_SHAKE_256_KEY_DERIVATION   = 0x0000039CL;

    public static final long  CKM_PBE_MD2_DES_CBC            = 0x000003A0L;
    public static final long  CKM_PBE_MD5_DES_CBC            = 0x000003A1L;
    public static final long  CKM_PBE_MD5_CAST_CBC           = 0x000003A2L;
    public static final long  CKM_PBE_MD5_CAST3_CBC          = 0x000003A3L;
    public static final long  CKM_PBE_MD5_CAST5_CBC /*deprecated*/
                                                             = 0x000003A4L;
    public static final long  CKM_PBE_MD5_CAST128_CBC        = 0x000003A4L;
    public static final long  CKM_PBE_SHA1_CAST5_CBC /*deprecated*/
                                                             = 0x000003A5L;
    public static final long  CKM_PBE_SHA1_CAST128_CBC       = 0x000003A5L;
    public static final long  CKM_PBE_SHA1_RC4_128           = 0x000003A6L;
    public static final long  CKM_PBE_SHA1_RC4_40            = 0x000003A7L;
    public static final long  CKM_PBE_SHA1_DES3_EDE_CBC      = 0x000003A8L;
    public static final long  CKM_PBE_SHA1_DES2_EDE_CBC      = 0x000003A9L;
    public static final long  CKM_PBE_SHA1_RC2_128_CBC       = 0x000003AAL;
    public static final long  CKM_PBE_SHA1_RC2_40_CBC        = 0x000003ABL;

    public static final long  CKM_PKCS5_PBKD2                = 0x000003B0L;

    public static final long  CKM_PBA_SHA1_WITH_SHA1_HMAC    = 0x000003C0L;

    public static final long  CKM_WTLS_PRE_MASTER_KEY_GEN         = 0x000003D0L;
    public static final long  CKM_WTLS_MASTER_KEY_DERIVE          = 0x000003D1L;
    public static final long  CKM_WTLS_MASTER_KEY_DERIVE_DH_ECC   = 0x000003D2L;
    public static final long  CKM_WTLS_PRF                        = 0x000003D3L;
    public static final long  CKM_WTLS_SERVER_KEY_AND_MAC_DERIVE  = 0x000003D4L;
    public static final long  CKM_WTLS_CLIENT_KEY_AND_MAC_DERIVE  = 0x000003D5L;

    public static final long  CKM_TLS10_MAC_SERVER /*removed in 3.00*/
                                                             = 0x000003D6L;
    public static final long  CKM_TLS10_MAC_CLIENT /*removed in 3.00*/
                                                             = 0x000003D7L;
    public static final long  CKM_TLS12_MAC                  = 0x000003D8L;
    public static final long  CKM_TLS12_KDF                  = 0x000003D9L;
    public static final long  CKM_TLS12_MASTER_KEY_DERIVE    = 0x000003E0L;
    public static final long  CKM_TLS12_KEY_AND_MAC_DERIVE   = 0x000003E1L;
    public static final long  CKM_TLS12_MASTER_KEY_DERIVE_DH = 0x000003E2L;
    public static final long  CKM_TLS12_KEY_SAFE_DERIVE      = 0x000003E3L;
    public static final long  CKM_TLS_MAC                    = 0x000003E4L;
    public static final long  CKM_TLS_KDF                    = 0x000003E5L;

    public static final long  CKM_KEY_WRAP_LYNKS             = 0x00000400L;
    public static final long  CKM_KEY_WRAP_SET_OAEP          = 0x00000401L;

    public static final long  CKM_CMS_SIG                    = 0x00000500L;
    public static final long  CKM_KIP_DERIVE                 = 0x00000510L;
    public static final long  CKM_KIP_WRAP                   = 0x00000511L;
    public static final long  CKM_KIP_MAC                    = 0x00000512L;

    public static final long  CKM_CAMELLIA_KEY_GEN           = 0x00000550L;
    public static final long  CKM_CAMELLIA_ECB               = 0x00000551L;
    public static final long  CKM_CAMELLIA_CBC               = 0x00000552L;
    public static final long  CKM_CAMELLIA_MAC               = 0x00000553L;
    public static final long  CKM_CAMELLIA_MAC_GENERAL       = 0x00000554L;
    public static final long  CKM_CAMELLIA_CBC_PAD           = 0x00000555L;
    public static final long  CKM_CAMELLIA_ECB_ENCRYPT_DATA  = 0x00000556L;
    public static final long  CKM_CAMELLIA_CBC_ENCRYPT_DATA  = 0x00000557L;
    public static final long  CKM_CAMELLIA_CTR               = 0x00000558L;

    public static final long  CKM_ARIA_KEY_GEN               = 0x00000560L;
    public static final long  CKM_ARIA_ECB                   = 0x00000561L;
    public static final long  CKM_ARIA_CBC                   = 0x00000562L;
    public static final long  CKM_ARIA_MAC                   = 0x00000563L;
    public static final long  CKM_ARIA_MAC_GENERAL           = 0x00000564L;
    public static final long  CKM_ARIA_CBC_PAD               = 0x00000565L;
    public static final long  CKM_ARIA_ECB_ENCRYPT_DATA      = 0x00000566L;
    public static final long  CKM_ARIA_CBC_ENCRYPT_DATA      = 0x00000567L;

    public static final long  CKM_SEED_KEY_GEN               = 0x00000650L;
    public static final long  CKM_SEED_ECB                   = 0x00000651L;
    public static final long  CKM_SEED_CBC                   = 0x00000652L;
    public static final long  CKM_SEED_MAC                   = 0x00000653L;
    public static final long  CKM_SEED_MAC_GENERAL           = 0x00000654L;
    public static final long  CKM_SEED_CBC_PAD               = 0x00000655L;
    public static final long  CKM_SEED_ECB_ENCRYPT_DATA      = 0x00000656L;
    public static final long  CKM_SEED_CBC_ENCRYPT_DATA      = 0x00000657L;

    public static final long  CKM_SKIPJACK_KEY_GEN           = 0x00001000L;
    public static final long  CKM_SKIPJACK_ECB64             = 0x00001001L;
    public static final long  CKM_SKIPJACK_CBC64             = 0x00001002L;
    public static final long  CKM_SKIPJACK_OFB64             = 0x00001003L;
    public static final long  CKM_SKIPJACK_CFB64             = 0x00001004L;
    public static final long  CKM_SKIPJACK_CFB32             = 0x00001005L;
    public static final long  CKM_SKIPJACK_CFB16             = 0x00001006L;
    public static final long  CKM_SKIPJACK_CFB8              = 0x00001007L;
    public static final long  CKM_SKIPJACK_WRAP              = 0x00001008L;
    public static final long  CKM_SKIPJACK_PRIVATE_WRAP      = 0x00001009L;
    public static final long  CKM_SKIPJACK_RELAYX            = 0x0000100AL;
    public static final long  CKM_KEA_KEY_PAIR_GEN           = 0x00001010L;
    public static final long  CKM_KEA_KEY_DERIVE             = 0x00001011L;
    public static final long  CKM_KEA_DERIVE                 = 0x00001012L;
    public static final long  CKM_FORTEZZA_TIMESTAMP         = 0x00001020L;
    public static final long  CKM_BATON_KEY_GEN              = 0x00001030L;
    public static final long  CKM_BATON_ECB128               = 0x00001031L;
    public static final long  CKM_BATON_ECB96                = 0x00001032L;
    public static final long  CKM_BATON_CBC128               = 0x00001033L;
    public static final long  CKM_BATON_COUNTER              = 0x00001034L;
    public static final long  CKM_BATON_SHUFFLE              = 0x00001035L;
    public static final long  CKM_BATON_WRAP                 = 0x00001036L;

    public static final long  CKM_ECDSA_KEY_PAIR_GEN /*deprecated*/
                                                             = 0x00001040L;
    public static final long  CKM_EC_KEY_PAIR_GEN            = 0x00001040L;
    public static final long  CKM_EC_KEY_PAIR_GEN_W_EXTRA_BITS = 0x0000140BL;

    public static final long  CKM_ECDSA                      = 0x00001041L;
    public static final long  CKM_ECDSA_SHA1                 = 0x00001042L;
    public static final long  CKM_ECDSA_SHA224               = 0x00001043L;
    public static final long  CKM_ECDSA_SHA256               = 0x00001044L;
    public static final long  CKM_ECDSA_SHA384               = 0x00001045L;
    public static final long  CKM_ECDSA_SHA512               = 0x00001046L;
    public static final long  CKM_ECDSA_SHA3_224             = 0x00001047L;
    public static final long  CKM_ECDSA_SHA3_256             = 0x00001048L;
    public static final long  CKM_ECDSA_SHA3_384             = 0x00001049L;
    public static final long  CKM_ECDSA_SHA3_512             = 0x0000104AL;

    public static final long  CKM_ECDH1_DERIVE               = 0x00001050L;
    public static final long  CKM_ECDH1_COFACTOR_DERIVE      = 0x00001051L;
    public static final long  CKM_ECMQV_DERIVE               = 0x00001052L;

    public static final long  CKM_ECDH_AES_KEY_WRAP          = 0x00001053L;
    public static final long  CKM_RSA_AES_KEY_WRAP           = 0x00001054L;

    public static final long  CKM_EC_EDWARDS_KEY_PAIR_GEN    = 0x00001055L;
    public static final long  CKM_EC_MONTGOMERY_KEY_PAIR_GEN = 0x00001056L;
    public static final long  CKM_EDDSA                      = 0x00001057L;

    public static final long  CKM_JUNIPER_KEY_GEN            = 0x00001060L;
    public static final long  CKM_JUNIPER_ECB128             = 0x00001061L;
    public static final long  CKM_JUNIPER_CBC128             = 0x00001062L;
    public static final long  CKM_JUNIPER_COUNTER            = 0x00001063L;
    public static final long  CKM_JUNIPER_SHUFFLE            = 0x00001064L;
    public static final long  CKM_JUNIPER_WRAP               = 0x00001065L;
    public static final long  CKM_FASTHASH                   = 0x00001070L;

    public static final long  CKM_AES_XTS                    = 0x00001071L;
    public static final long  CKM_AES_XTS_KEY_GEN            = 0x00001072L;
    public static final long  CKM_AES_KEY_GEN                = 0x00001080L;
    public static final long  CKM_AES_ECB                    = 0x00001081L;
    public static final long  CKM_AES_CBC                    = 0x00001082L;
    public static final long  CKM_AES_MAC                    = 0x00001083L;
    public static final long  CKM_AES_MAC_GENERAL            = 0x00001084L;
    public static final long  CKM_AES_CBC_PAD                = 0x00001085L;
    public static final long  CKM_AES_CTR                    = 0x00001086L;
    public static final long  CKM_AES_GCM                    = 0x00001087L;
    public static final long  CKM_AES_CCM                    = 0x00001088L;
    public static final long  CKM_AES_CTS                    = 0x00001089L;
    public static final long  CKM_AES_CMAC                   = 0x0000108AL;
    public static final long  CKM_AES_CMAC_GENERAL           = 0x0000108BL;

    public static final long  CKM_AES_XCBC_MAC               = 0x0000108CL;
    public static final long  CKM_AES_XCBC_MAC_96            = 0x0000108DL;
    public static final long  CKM_AES_GMAC                   = 0x0000108EL;

    public static final long  CKM_BLOWFISH_KEY_GEN           = 0x00001090L;
    public static final long  CKM_BLOWFISH_CBC               = 0x00001091L;
    public static final long  CKM_TWOFISH_KEY_GEN            = 0x00001092L;
    public static final long  CKM_TWOFISH_CBC                = 0x00001093L;
    public static final long  CKM_BLOWFISH_CBC_PAD           = 0x00001094L;
    public static final long  CKM_TWOFISH_CBC_PAD            = 0x00001095L;

    public static final long  CKM_DES_ECB_ENCRYPT_DATA       = 0x00001100L;
    public static final long  CKM_DES_CBC_ENCRYPT_DATA       = 0x00001101L;
    public static final long  CKM_DES3_ECB_ENCRYPT_DATA      = 0x00001102L;
    public static final long  CKM_DES3_CBC_ENCRYPT_DATA      = 0x00001103L;
    public static final long  CKM_AES_ECB_ENCRYPT_DATA       = 0x00001104L;
    public static final long  CKM_AES_CBC_ENCRYPT_DATA       = 0x00001105L;

    public static final long  CKM_GOSTR3410_KEY_PAIR_GEN     = 0x00001200L;
    public static final long  CKM_GOSTR3410                  = 0x00001201L;
    public static final long  CKM_GOSTR3410_WITH_GOSTR3411   = 0x00001202L;
    public static final long  CKM_GOSTR3410_KEY_WRAP         = 0x00001203L;
    public static final long  CKM_GOSTR3410_DERIVE           = 0x00001204L;
    public static final long  CKM_GOSTR3411                  = 0x00001210L;
    public static final long  CKM_GOSTR3411_HMAC             = 0x00001211L;
    public static final long  CKM_GOST28147_KEY_GEN          = 0x00001220L;
    public static final long  CKM_GOST28147_ECB              = 0x00001221L;
    public static final long  CKM_GOST28147                  = 0x00001222L;
    public static final long  CKM_GOST28147_MAC              = 0x00001223L;
    public static final long  CKM_GOST28147_KEY_WRAP         = 0x00001224L;

    public static final long  CKM_CHACHA20_KEY_GEN           = 0x00001225L;
    public static final long  CKM_CHACHA20                   = 0x00001226L;
    public static final long  CKM_POLY1305_KEY_GEN           = 0x00001227L;
    public static final long  CKM_POLY1305                   = 0x00001228L;

    public static final long  CKM_DSA_PARAMETER_GEN          = 0x00002000L;
    public static final long  CKM_DH_PKCS_PARAMETER_GEN      = 0x00002001L;
    public static final long  CKM_X9_42_DH_PARAMETER_GEN     = 0x00002002L;
    public static final long  CKM_DSA_PROBABLISTIC_PARAMETER_GEN = 0x00002003L;
    public static final long  CKM_DSA_SHAWE_TAYLOR_PARAMETER_GEN = 0x00002004L;
    public static final long  CKM_DSA_FIPS_G_GEN             = 0x00002005L;

    public static final long  CKM_AES_OFB                    = 0x00002104L;
    public static final long  CKM_AES_CFB64                  = 0x00002105L;
    public static final long  CKM_AES_CFB8                   = 0x00002106L;
    public static final long  CKM_AES_CFB128                 = 0x00002107L;
    public static final long  CKM_AES_CFB1                   = 0x00002108L;
    public static final long  CKM_AES_KEY_WRAP /* WAS: 0x00001090 */
                                                             = 0x00002109L;
    public static final long  CKM_AES_KEY_WRAP_PAD /* WAS: 0x00001091 */
                                                             = 0x0000210AL;
    public static final long  CKM_AES_KEY_WRAP_KWP           = 0x0000210BL;

    public static final long  CKM_RSA_PKCS_TPM_1_1           = 0x00004001L;
    public static final long  CKM_RSA_PKCS_OAEP_TPM_1_1      = 0x00004002L;

    public static final long  CKM_SHA_1_KEY_GEN              = 0x00004003L;
    public static final long  CKM_SHA224_KEY_GEN             = 0x00004004L;
    public static final long  CKM_SHA256_KEY_GEN             = 0x00004005L;
    public static final long  CKM_SHA384_KEY_GEN             = 0x00004006L;
    public static final long  CKM_SHA512_KEY_GEN             = 0x00004007L;
    public static final long  CKM_SHA512_224_KEY_GEN         = 0x00004008L;
    public static final long  CKM_SHA512_256_KEY_GEN         = 0x00004009L;
    public static final long  CKM_SHA512_T_KEY_GEN           = 0x0000400aL;
    public static final long  CKM_NULL                       = 0x0000400bL;
    public static final long  CKM_BLAKE2B_160                = 0x0000400cL;
    public static final long  CKM_BLAKE2B_160_HMAC           = 0x0000400dL;
    public static final long  CKM_BLAKE2B_160_HMAC_GENERAL   = 0x0000400eL;
    public static final long  CKM_BLAKE2B_160_KEY_DERIVE     = 0x0000400fL;
    public static final long  CKM_BLAKE2B_160_KEY_GEN        = 0x00004010L;
    public static final long  CKM_BLAKE2B_256                = 0x00004011L;
    public static final long  CKM_BLAKE2B_256_HMAC           = 0x00004012L;
    public static final long  CKM_BLAKE2B_256_HMAC_GENERAL   = 0x00004013L;
    public static final long  CKM_BLAKE2B_256_KEY_DERIVE     = 0x00004014L;
    public static final long  CKM_BLAKE2B_256_KEY_GEN        = 0x00004015L;
    public static final long  CKM_BLAKE2B_384                = 0x00004016L;
    public static final long  CKM_BLAKE2B_384_HMAC           = 0x00004017L;
    public static final long  CKM_BLAKE2B_384_HMAC_GENERAL   = 0x00004018L;
    public static final long  CKM_BLAKE2B_384_KEY_DERIVE     = 0x00004019L;
    public static final long  CKM_BLAKE2B_384_KEY_GEN        = 0x0000401aL;
    public static final long  CKM_BLAKE2B_512                = 0x0000401bL;
    public static final long  CKM_BLAKE2B_512_HMAC           = 0x0000401cL;
    public static final long  CKM_BLAKE2B_512_HMAC_GENERAL   = 0x0000401dL;
    public static final long  CKM_BLAKE2B_512_KEY_DERIVE     = 0x0000401eL;
    public static final long  CKM_BLAKE2B_512_KEY_GEN        = 0x0000401fL;
    public static final long  CKM_SALSA20                    = 0x00004020L;
    public static final long  CKM_CHACHA20_POLY1305          = 0x00004021L;
    public static final long  CKM_SALSA20_POLY1305           = 0x00004022L;
    public static final long  CKM_X3DH_INITIALIZE            = 0x00004023L;
    public static final long  CKM_X3DH_RESPOND               = 0x00004024L;
    public static final long  CKM_X2RATCHET_INITIALIZE       = 0x00004025L;
    public static final long  CKM_X2RATCHET_RESPOND          = 0x00004026L;
    public static final long  CKM_X2RATCHET_ENCRYPT          = 0x00004027L;
    public static final long  CKM_X2RATCHET_DECRYPT          = 0x00004028L;
    public static final long  CKM_XEDDSA                     = 0x00004029L;
    public static final long  CKM_HKDF_DERIVE                = 0x0000402aL;
    public static final long  CKM_HKDF_DATA                  = 0x0000402bL;
    public static final long  CKM_HKDF_KEY_GEN               = 0x0000402cL;
    public static final long  CKM_SALSA20_KEY_GEN            = 0x0000402dL;

    public static final long  CKM_SP800_108_COUNTER_KDF      = 0x000003acL;
    public static final long  CKM_SP800_108_FEEDBACK_KDF     = 0x000003adL;
    public static final long  CKM_SP800_108_DOUBLE_PIPELINE_KDF = 0x000003aeL;

    public static final long  CKM_VENDOR_DEFINED             = 0x80000000L;

    // NSS private
    public static final long  CKM_NSS_TLS_PRF_GENERAL        = 0x80000373L;

    // internal ids for our pseudo mechanisms SecureRandom and KeyStore
    public static final long  PCKM_SECURERANDOM              = 0x7FFFFF20L;
    public static final long  PCKM_KEYSTORE                  = 0x7FFFFF21L;

    /* The flags specify whether or not a mechanism can be used for a
     * particular task */
    public static final long  CKF_HW                 = 0x00000001L;
    public static final long  CKF_MESSAGE_ENCRYPT    = 0x00000002L;
    public static final long  CKF_MESSAGE_DECRYPT    = 0x00000004L;
    public static final long  CKF_MESSAGE_SIGN       = 0x00000008L;
    public static final long  CKF_MESSAGE_VERIFY     = 0x00000010L;
    public static final long  CKF_MULTI_MESSAGE      = 0x00000020L;
    public static final long  CKF_FIND_OBJECTS       = 0x00000040L;

    public static final long  CKF_ENCRYPT            = 0x00000100L;
    public static final long  CKF_DECRYPT            = 0x00000200L;
    public static final long  CKF_DIGEST             = 0x00000400L;
    public static final long  CKF_SIGN               = 0x00000800L;
    public static final long  CKF_SIGN_RECOVER       = 0x00001000L;
    public static final long  CKF_VERIFY             = 0x00002000L;
    public static final long  CKF_VERIFY_RECOVER     = 0x00004000L;
    public static final long  CKF_GENERATE           = 0x00008000L;
    public static final long  CKF_GENERATE_KEY_PAIR  = 0x00010000L;
    public static final long  CKF_WRAP               = 0x00020000L;
    public static final long  CKF_UNWRAP             = 0x00040000L;
    public static final long  CKF_DERIVE             = 0x00080000L;

    /* Describe a token's EC capabilities not available in mechanism
     * information.
     */
    public static final long  CKF_EC_F_P             = 0x00100000L;
    public static final long  CKF_EC_F_2M            = 0x00200000L;
    public static final long  CKF_EC_ECPARAMETERS    = 0x00400000L;
    public static final long  CKF_EC_OID             = 0x00400000L;
    public static final long  CKF_EC_NAMEDCURVE /*deprecated since 3.00*/
                                                     = CKF_EC_OID;
    public static final long  CKF_EC_UNCOMPRESS      = 0x01000000L;
    public static final long  CKF_EC_COMPRESS        = 0x02000000L;
    public static final long  CKF_EC_CURVENAME       = 0x04000000L;

    public static final long  CKF_EXTENSION          = 0x80000000L;

    /* Identifies the return value of a Cryptoki function */
    // These CKR_xxx constants are stored/defined in PKCS11Exception class

    /* Uncomment when actually used
    public static final long  CKF_END_OF_MESSAGE      = 0x00000001L;
    public static final long  CKF_INTERFACE_FORK_SAFE = 0x00000001L;
    */

    /* flags: bit flags that provide capabilities of the slot
     *        Bit Flag = Mask
     */
    public static final long  CKF_LIBRARY_CANT_CREATE_OS_THREADS = 0x00000001L;
    public static final long  CKF_OS_LOCKING_OK                  = 0x00000002L;

    /* CKF_DONT_BLOCK is for the function C_WaitForSlotEvent */
    public static final long  CKF_DONT_BLOCK =    1L;

    /* The following MGFs are defined */
    public static final long  CKG_MGF1_SHA1       = 0x00000001L;
    public static final long  CKG_MGF1_SHA256     = 0x00000002L;
    public static final long  CKG_MGF1_SHA384     = 0x00000003L;
    public static final long  CKG_MGF1_SHA512     = 0x00000004L;
    public static final long  CKG_MGF1_SHA224     = 0x00000005L;
    public static final long  CKG_MGF1_SHA3_224   = 0x00000006L;
    public static final long  CKG_MGF1_SHA3_256   = 0x00000007L;
    public static final long  CKG_MGF1_SHA3_384   = 0x00000008L;
    public static final long  CKG_MGF1_SHA3_512   = 0x00000009L;

    /* The following encoding parameter sources are defined */
    public static final long  CKZ_DATA_SPECIFIED   = 0x00000001L;

    // the following EC Key Derivation Functions are defined
    public static final long  CKD_NULL                 = 0x00000001L;
    public static final long  CKD_SHA1_KDF             = 0x00000002L;

    /* Uncomment when actually used
    // the following X9.42 Diffie-Hellman Key Derivation Functions are defined
    public static final long  CKD_SHA1_KDF_ASN1        = 0x00000003L;
    public static final long  CKD_SHA1_KDF_CONCATENATE = 0x00000004L;
    public static final long  CKD_SHA224_KDF           = 0x00000005L;
    public static final long  CKD_SHA256_KDF           = 0x00000006L;
    public static final long  CKD_SHA384_KDF           = 0x00000007L;
    public static final long  CKD_SHA512_KDF           = 0x00000008L;
    public static final long  CKD_CPDIVERSIFY_KDF      = 0x00000009L;
    public static final long  CKD_SHA3_224_KDF         = 0x0000000AL;
    public static final long  CKD_SHA3_256_KDF         = 0x0000000BL;
    public static final long  CKD_SHA3_384_KDF         = 0x0000000CL;
    public static final long  CKD_SHA3_512_KDF         = 0x0000000DL;
    public static final long  CKD_SHA1_KDF_SP800       = 0x0000000EL;
    public static final long  CKD_SHA224_KDF_SP800     = 0x0000000FL;
    public static final long  CKD_SHA256_KDF_SP800     = 0x00000010L;
    public static final long  CKD_SHA384_KDF_SP800     = 0x00000011L;
    public static final long  CKD_SHA512_KDF_SP800     = 0x00000012L;
    public static final long  CKD_SHA3_224_KDF_SP800   = 0x00000013L;
    public static final long  CKD_SHA3_256_KDF_SP800   = 0x00000014L;
    public static final long  CKD_SHA3_384_KDF_SP800   = 0x00000015L;
    public static final long  CKD_SHA3_512_KDF_SP800   = 0x00000016L;
    public static final long  CKD_BLAKE2B_160_KDF      = 0x00000017L;
    public static final long  CKD_BLAKE2B_256_KDF      = 0x00000018L;
    public static final long  CKD_BLAKE2B_384_KDF      = 0x00000019L;
    public static final long  CKD_BLAKE2B_512_KDF      = 0x0000001aL;

    public static final long  CKP_PKCS5_PBKD2_HMAC_SHA1        = 0x00000001L;
    public static final long  CKP_PKCS5_PBKD2_HMAC_GOSTR3411   = 0x00000002L;
    public static final long  CKP_PKCS5_PBKD2_HMAC_SHA224      = 0x00000003L;
    public static final long  CKP_PKCS5_PBKD2_HMAC_SHA256      = 0x00000004L;
    public static final long  CKP_PKCS5_PBKD2_HMAC_SHA384      = 0x00000005L;
    public static final long  CKP_PKCS5_PBKD2_HMAC_SHA512      = 0x00000006L;
    public static final long  CKP_PKCS5_PBKD2_HMAC_SHA512_224  = 0x00000007L;
    public static final long  CKP_PKCS5_PBKD2_HMAC_SHA512_256  = 0x00000008L;

    public static final long  CKZ_SALT_SPECIFIED      = 0x00000001L;

    public static final long  CK_OTP_VALUE            = 0x00000000L;
    public static final long  CK_OTP_PIN              = 0x00000001L;
    public static final long  CK_OTP_CHALLENGE        = 0x00000002L;
    public static final long  CK_OTP_TIME             = 0x00000003L;
    public static final long  CK_OTP_COUNTER          = 0x00000004L;
    public static final long  CK_OTP_FLAGS            = 0x00000005L;
    public static final long  CK_OTP_OUTPUT_LENGTH    = 0x00000006L;
    public static final long  CK_OTP_OUTPUT_FORMAT    = 0x00000007L;

    public static final long  CKF_NEXT_OTP            = 0x00000001L;
    public static final long  CKF_EXCLUDE_TIME        = 0x00000002L;
    public static final long  CKF_EXCLUDE_COUNTER     = 0x00000004L;
    public static final long  CKF_EXCLUDE_CHALLENGE   = 0x00000008L;
    public static final long  CKF_EXCLUDE_PIN         = 0x00000010L;
    public static final long  CKF_USER_FRIENDLY_OTP   = 0x00000020L;

    public static final long  CKG_NO_GENERATE      = 0x00000000L;
    public static final long  CKG_GENERATE         = 0x00000001L;
    public static final long  CKG_GENERATE_COUNTER = 0x00000002L;
    public static final long  CKG_GENERATE_RANDOM  = 0x00000003L;

    public static final long  CK_SP800_108_ITERATION_VARIABLE = 0x00000001L;
    public static final long  CK_SP800_108_OPTIONAL_COUNTER   = 0x00000002L;
    public static final long  CK_SP800_108_DKM_LENGTH         = 0x00000003L;
    public static final long  CK_SP800_108_BYTE_ARRAY         = 0x00000004L;
    public static final long  CK_SP800_108_DKM_LENGTH_SUM_OF_KEYS
                                                              = 0x00000001L;
    public static final long  CK_SP800_108_DKM_LENGTH_SUM_OF_SEGMENTS
                                                              = 0x00000002L;

    public static final long  CKF_HKDF_SALT_NULL   = 0x00000001L;
    public static final long  CKF_HKDF_SALT_DATA   = 0x00000002L;
    public static final long  CKF_HKDF_SALT_KEY    = 0x00000004L;
    */

    // private NSS attribute (for DSA and DH private keys)
    public static final long  CKA_NETSCAPE_DB         = 0xD5A0DB00L;

    // base number of NSS private attributes
    public static final long  CKA_NETSCAPE_BASE /*0x80000000L + 0x4E534350L*/
                                                      = 0xCE534350L;

    // object type for NSS trust
    public static final long  CKO_NETSCAPE_TRUST      = 0xCE534353L;

    // base number for NSS trust attributes
    public static final long  CKA_NETSCAPE_TRUST_BASE = 0xCE536350L;

    // attributes for NSS trust
    public static final long  CKA_NETSCAPE_TRUST_SERVER_AUTH      = 0xCE536358L;
    public static final long  CKA_NETSCAPE_TRUST_CLIENT_AUTH      = 0xCE536359L;
    public static final long  CKA_NETSCAPE_TRUST_CODE_SIGNING     = 0xCE53635AL;
    public static final long  CKA_NETSCAPE_TRUST_EMAIL_PROTECTION = 0xCE53635BL;
    public static final long  CKA_NETSCAPE_CERT_SHA1_HASH         = 0xCE5363B4L;
    public static final long  CKA_NETSCAPE_CERT_MD5_HASH          = 0xCE5363B5L;

    // trust values for each of the NSS trust attributes
    public static final long  CKT_NETSCAPE_TRUSTED           = 0xCE534351L;
    public static final long  CKT_NETSCAPE_TRUSTED_DELEGATOR = 0xCE534352L;
    public static final long  CKT_NETSCAPE_UNTRUSTED         = 0xCE534353L;
    public static final long  CKT_NETSCAPE_MUST_VERIFY       = 0xCE534354L;
    public static final long  CKT_NETSCAPE_TRUST_UNKNOWN /* default */
                                                             = 0xCE534355L;
    public static final long  CKT_NETSCAPE_VALID             = 0xCE53435AL;
    public static final long  CKT_NETSCAPE_VALID_DELEGATOR   = 0xCE53435BL;
}
