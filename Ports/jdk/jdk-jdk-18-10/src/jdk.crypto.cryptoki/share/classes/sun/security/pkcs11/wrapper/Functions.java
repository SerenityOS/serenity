/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;

import java.util.*;

import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * This class contains only static methods. It is the place for all functions
 * that are used by several classes in this package.
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */
public class Functions {

    // maps between ids and their names, forward and reverse
    // ids are stored as Integers to save space
    // since only the lower 32 bits are ever used anyway

    // mechanisms (CKM_*)
    private static final Map<Integer,String> mechNames =
        new HashMap<Integer,String>();

    private static final Map<String,Integer> mechIds =
        new HashMap<String,Integer>();

    private static final Map<String, Long> hashMechIds =
            new HashMap<String, Long>();

    // key types (CKK_*)
    private static final Map<Integer,String> keyNames =
        new HashMap<Integer,String>();

    private static final Map<String,Integer> keyIds =
        new HashMap<String,Integer>();

    // attributes (CKA_*)
    private static final Map<Integer,String> attributeNames =
        new HashMap<Integer,String>();

    private static final Map<String,Integer> attributeIds =
        new HashMap<String,Integer>();

    // object classes (CKO_*)
    private static final Map<Integer,String> objectClassNames =
        new HashMap<Integer,String>();

    private static final Map<String,Integer> objectClassIds =
        new HashMap<String,Integer>();

    // MGFs (CKG_*)
    private static final Map<Integer,String> mgfNames =
        new HashMap<Integer,String>();

    private static final Map<String,Integer> mgfIds =
        new HashMap<String,Integer>();

    /**
     * For converting numbers to their hex presentation.
     */
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    /**
     * Converts a long value to a hexadecimal String of length 16. Includes
     * leading zeros if necessary.
     *
     * @param value The long value to be converted.
     * @return The hexadecimal string representation of the long value.
     */
    public static String toFullHexString(long value) {
        long currentValue = value;
        StringBuilder sb = new StringBuilder(16);
        for(int j = 0; j < 16; j++) {
            int currentDigit = (int) currentValue & 0xf;
            sb.append(HEX_DIGITS[currentDigit]);
            currentValue >>>= 4;
        }

        return sb.reverse().toString();
    }

    /**
     * Converts a int value to a hexadecimal String of length 8. Includes
     * leading zeros if necessary.
     *
     * @param value The int value to be converted.
     * @return The hexadecimal string representation of the int value.
     */
    public static String toFullHexString(int value) {
        int currentValue = value;
        StringBuilder sb = new StringBuilder(8);
        for(int i = 0; i < 8; i++) {
            int currentDigit = currentValue & 0xf;
            sb.append(HEX_DIGITS[currentDigit]);
            currentValue >>>= 4;
        }

        return sb.reverse().toString();
    }

    /**
     * converts a long value to a hexadecimal String
     *
     * @param value the long value to be converted
     * @return the hexadecimal string representation of the long value
     */
    public static String toHexString(long value) {
        return Long.toHexString(value);
    }

    /**
     * Converts a byte array to a hexadecimal String. Each byte is presented by
     * its two digit hex-code; 0x0A -> "0a", 0x00 -> "00". No leading "0x" is
     * included in the result.
     *
     * @param value the byte array to be converted
     * @return the hexadecimal string representation of the byte array
     */
    public static String toHexString(byte[] value) {
        if (value == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(2 * value.length);
        int          single;

        for (int i = 0; i < value.length; i++) {
            single = value[i] & 0xFF;

            if (single < 0x10) {
                sb.append('0');
            }

            sb.append(Integer.toString(single, 16));
        }

        return sb.toString();
    }

    /**
     * converts a long value to a binary String
     *
     * @param value the long value to be converted
     * @return the binary string representation of the long value
     */
    public static String toBinaryString(long value) {
        return Long.toString(value, 2);
    }

    /**
     * converts a byte array to a binary String
     *
     * @param value the byte array to be converted
     * @return the binary string representation of the byte array
     */
    public static String toBinaryString(byte[] value) {
        BigInteger helpBigInteger = new BigInteger(1, value);

        return helpBigInteger.toString(2);
    }

    private static class Flags {
        private final long[] flagIds;
        private final String[] flagNames;
        Flags(long[] flagIds, String[] flagNames) {
            if (flagIds.length != flagNames.length) {
                throw new AssertionError("Array lengths do not match");
            }
            this.flagIds = flagIds;
            this.flagNames = flagNames;
        }
        String toString(long val) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int i = 0; i < flagIds.length; i++) {
                if ((val & flagIds[i]) != 0) {
                    if (first == false) {
                        sb.append(" | ");
                    }
                    sb.append(flagNames[i]);
                    first = false;
                }
            }
            return sb.toString();
        }
    }

    private static final Flags slotInfoFlags = new Flags(new long[] {
        CKF_TOKEN_PRESENT,
        CKF_REMOVABLE_DEVICE,
        CKF_HW_SLOT,
    }, new String[] {
        "CKF_TOKEN_PRESENT",
        "CKF_REMOVABLE_DEVICE",
        "CKF_HW_SLOT",
    });

    /**
     * converts the long value flags to a SlotInfoFlag string
     *
     * @param flags the flags to be converted
     * @return the SlotInfoFlag string representation of the flags
     */
    public static String slotInfoFlagsToString(long flags) {
        return slotInfoFlags.toString(flags);
    }

    private static final Flags tokenInfoFlags = new Flags(new long[] {
        CKF_RNG,
        CKF_WRITE_PROTECTED,
        CKF_LOGIN_REQUIRED,
        CKF_USER_PIN_INITIALIZED,
        CKF_RESTORE_KEY_NOT_NEEDED,
        CKF_CLOCK_ON_TOKEN,
        CKF_PROTECTED_AUTHENTICATION_PATH,
        CKF_DUAL_CRYPTO_OPERATIONS,
        CKF_TOKEN_INITIALIZED,
        CKF_SECONDARY_AUTHENTICATION,
        CKF_USER_PIN_COUNT_LOW,
        CKF_USER_PIN_FINAL_TRY,
        CKF_USER_PIN_LOCKED,
        CKF_USER_PIN_TO_BE_CHANGED,
        CKF_SO_PIN_COUNT_LOW,
        CKF_SO_PIN_FINAL_TRY,
        CKF_SO_PIN_LOCKED,
        CKF_SO_PIN_TO_BE_CHANGED,
    }, new String[] {
        "CKF_RNG",
        "CKF_WRITE_PROTECTED",
        "CKF_LOGIN_REQUIRED",
        "CKF_USER_PIN_INITIALIZED",
        "CKF_RESTORE_KEY_NOT_NEEDED",
        "CKF_CLOCK_ON_TOKEN",
        "CKF_PROTECTED_AUTHENTICATION_PATH",
        "CKF_DUAL_CRYPTO_OPERATIONS",
        "CKF_TOKEN_INITIALIZED",
        "CKF_SECONDARY_AUTHENTICATION",
        "CKF_USER_PIN_COUNT_LOW",
        "CKF_USER_PIN_FINAL_TRY",
        "CKF_USER_PIN_LOCKED",
        "CKF_USER_PIN_TO_BE_CHANGED",
        "CKF_SO_PIN_COUNT_LOW",
        "CKF_SO_PIN_FINAL_TRY",
        "CKF_SO_PIN_LOCKED",
        "CKF_SO_PIN_TO_BE_CHANGED",
    });

    /**
     * converts long value flags to a TokenInfoFlag string
     *
     * @param flags the flags to be converted
     * @return the TokenInfoFlag string representation of the flags
     */
    public static String tokenInfoFlagsToString(long flags) {
        return tokenInfoFlags.toString(flags);
    }

    private static final Flags sessionInfoFlags = new Flags(new long[] {
        CKF_RW_SESSION,
        CKF_SERIAL_SESSION,
    }, new String[] {
        "CKF_RW_SESSION",
        "CKF_SERIAL_SESSION",
    });

    /**
     * converts the long value flags to a SessionInfoFlag string
     *
     * @param flags the flags to be converted
     * @return the SessionInfoFlag string representation of the flags
     */
    public static String sessionInfoFlagsToString(long flags) {
        return sessionInfoFlags.toString(flags);
    }

    /**
     * converts the long value state to a SessionState string
     *
     * @param state the state to be converted
     * @return the SessionState string representation of the state
     */
    public static String sessionStateToString(long state) {
        String name;

        if (state == CKS_RO_PUBLIC_SESSION) {
            name = "CKS_RO_PUBLIC_SESSION";
        } else if (state == CKS_RO_USER_FUNCTIONS) {
            name = "CKS_RO_USER_FUNCTIONS";
        } else if (state == CKS_RW_PUBLIC_SESSION) {
            name = "CKS_RW_PUBLIC_SESSION";
        } else if (state == CKS_RW_USER_FUNCTIONS) {
            name = "CKS_RW_USER_FUNCTIONS";
        } else if (state == CKS_RW_SO_FUNCTIONS) {
            name = "CKS_RW_SO_FUNCTIONS";
        } else {
            name = "ERROR: unknown session state 0x" + toFullHexString(state);
        }

        return name;
    }

    private static final Flags mechanismInfoFlags = new Flags(new long[] {
        CKF_HW,
        CKF_MESSAGE_ENCRYPT,
        CKF_MESSAGE_DECRYPT,
        CKF_MESSAGE_SIGN,
        CKF_MESSAGE_VERIFY,
        CKF_MULTI_MESSAGE,
        CKF_FIND_OBJECTS,
        CKF_ENCRYPT,
        CKF_DECRYPT,
        CKF_DIGEST,
        CKF_SIGN,
        CKF_SIGN_RECOVER,
        CKF_VERIFY,
        CKF_VERIFY_RECOVER,
        CKF_GENERATE,
        CKF_GENERATE_KEY_PAIR,
        CKF_WRAP,
        CKF_UNWRAP,
        CKF_DERIVE,
        CKF_EC_F_P,
        CKF_EC_F_2M,
        CKF_EC_ECPARAMETERS,
        CKF_EC_OID,
        CKF_EC_UNCOMPRESS,
        CKF_EC_COMPRESS,
        CKF_EC_CURVENAME,
        CKF_EXTENSION,
    }, new String[] {
        "CKF_HW",
        "CKF_MESSAGE_ENCRYPT",
        "CKF_MESSAGE_DECRYPT",
        "CKF_MESSAGE_SIGN",
        "CKF_MESSAGE_VERIFY",
        "CKF_MULTI_MESSAGE",
        "CKF_FIND_OBJECTS",
        "CKF_ENCRYPT",
        "CKF_DECRYPT",
        "CKF_DIGEST",
        "CKF_SIGN",
        "CKF_SIGN_RECOVER",
        "CKF_VERIFY",
        "CKF_VERIFY_RECOVER",
        "CKF_GENERATE",
        "CKF_GENERATE_KEY_PAIR",
        "CKF_WRAP",
        "CKF_UNWRAP",
        "CKF_DERIVE",
        "CKF_EC_F_P",
        "CKF_EC_F_2M",
        "CKF_EC_ECPARAMETERS",
        "CKF_EC_OID",
        "CKF_EC_UNCOMPRESS",
        "CKF_EC_COMPRESS",
        "CKF_EC_CURVENAME",
        "CKF_EXTENSION",
    });

    /**
     * converts the long value flags to a MechanismInfoFlag string
     *
     * @param flags the flags to be converted
     * @return the MechanismInfoFlag string representation of the flags
     */
    public static String mechanismInfoFlagsToString(long flags) {
        return mechanismInfoFlags.toString(flags);
    }

    private static String getName(Map<Integer,String> nameMap, long id) {
        String name = null;
        if ((id >>> 32) == 0) {
            name = nameMap.get(Integer.valueOf((int)id));
        }
        if (name == null) {
            if ((id & CKM_VENDOR_DEFINED) != 0) {
                name = "(Vendor-Specific) 0x" + toFullHexString(id);
            } else {
                name = "(Unknown) 0x" + toFullHexString(id);
            }
        }
        return name;
    }

    public static long getId(Map<String,Integer> idMap, String name) {
        Integer mech = idMap.get(name);
        if (mech == null) {
            throw new IllegalArgumentException("Unknown name " + name);
        }
        return mech.intValue() & 0xffffffffL;
    }

    public static String getMechanismName(long id) {
        return getName(mechNames, id);
    }

    public static long getMechanismId(String name) {
        return getId(mechIds, name);
    }

    public static String getKeyName(long id) {
        return getName(keyNames, id);
    }

    public static long getKeyId(String name) {
        return getId(keyIds, name);
    }

    public static String getAttributeName(long id) {
        return getName(attributeNames, id);
    }

    public static long getAttributeId(String name) {
        return getId(attributeIds, name);
    }

    public static String getObjectClassName(long id) {
        return getName(objectClassNames, id);
    }

    public static long getObjectClassId(String name) {
        return getId(objectClassIds, name);
    }

    public static long getHashMechId(String name) {
        return hashMechIds.get(name);
    }

    public static String getMGFName(long id) {
        return getName(mgfNames, id);
    }

    public static long getMGFId(String name) {
        return getId(mgfIds, name);
    }

    /**
     * Check the given arrays for equalitiy. This method considers both arrays as
     * equal, if both are <code>null</code> or both have the same length and
     * contain exactly the same char values.
     *
     * @param array1 The first array.
     * @param array2 The second array.
     * @return True, if both arrays are <code>null</code> or both have the same
     *         length and contain exactly the same char values. False, otherwise.
     * @preconditions
     * @postconditions
     */
    private static boolean equals(char[] array1, char[] array2) {
        return Arrays.equals(array1, array2);
    }

    /**
     * Check the given dates for equalitiy. This method considers both dates as
     * equal, if both are <code>null</code> or both contain exactly the same char
     * values.
     *
     * @param date1 The first date.
     * @param date2 The second date.
     * @return True, if both dates are <code>null</code> or both contain the same
     *         char values. False, otherwise.
     * @preconditions
     * @postconditions
     */
    public static boolean equals(CK_DATE date1, CK_DATE date2) {
        boolean equal = false;

        if (date1 == date2) {
            equal = true;
        } else if ((date1 != null) && (date2 != null)) {
            equal = equals(date1.year, date2.year)
              && equals(date1.month, date2.month)
              && equals(date1.day, date2.day);
        } else {
            equal = false;
        }

        return equal ;
    }

    /**
     * Calculate a hash code for the given byte array.
     *
     * @param array The byte array.
     * @return A hash code for the given array.
     * @preconditions
     * @postconditions
     */
    public static int hashCode(byte[] array) {
        int hash = 0;

        if (array != null) {
            for (int i = 0; (i < 4) && (i < array.length); i++) {
                hash ^= (0xFF & array[i]) << ((i%4) << 3);
            }
        }

        return hash ;
    }

    /**
     * Calculate a hash code for the given char array.
     *
     * @param array The char array.
     * @return A hash code for the given array.
     * @preconditions
     * @postconditions
     */
    public static int hashCode(char[] array) {
        int hash = 0;

        if (array != null) {
            for (int i = 0; (i < 4) && (i < array.length); i++) {
                hash ^= (0xFFFF & array[i]) << ((i%2) << 4);
            }
        }

        return hash ;
    }

    /**
     * Calculate a hash code for the given date object.
     *
     * @param date The date object.
     * @return A hash code for the given date.
     * @preconditions
     * @postconditions
     */
    public static int hashCode(CK_DATE date) {
        int hash = 0;

        if (date != null) {
            if (date.year.length == 4) {
                hash ^= (0xFFFF & date.year[0]) << 16;
                hash ^= 0xFFFF & date.year[1];
                hash ^= (0xFFFF & date.year[2]) << 16;
                hash ^= 0xFFFF & date.year[3];
            }
            if (date.month.length == 2) {
                hash ^= (0xFFFF & date.month[0]) << 16;
                hash ^= 0xFFFF & date.month[1];
            }
            if (date.day.length == 2) {
                hash ^= (0xFFFF & date.day[0]) << 16;
                hash ^= 0xFFFF & date.day[1];
            }
        }

        return hash ;
    }

    private static void addMapping(Map<Integer,String> nameMap,
            Map<String,Integer> idMap, long id, String name) {
        if ((id >>> 32) != 0) {
            throw new AssertionError("Id has high bits set: " + id + ", " + name);
        }
        Integer intId = Integer.valueOf((int)id);
        if (nameMap.put(intId, name) != null) {
            throw new AssertionError("Duplicate id: " + id + ", " + name);
        }
        if (idMap.put(name, intId) != null) {
            throw new AssertionError("Duplicate name: " + id + ", " + name);
        }
    }

    private static void addMech(long id, String name) {
        addMapping(mechNames, mechIds, id, name);
    }

    private static void addKeyType(long id, String name) {
        addMapping(keyNames, keyIds, id, name);
    }

    private static void addAttribute(long id, String name) {
        addMapping(attributeNames, attributeIds, id, name);
    }

    private static void addObjectClass(long id, String name) {
        addMapping(objectClassNames, objectClassIds, id, name);
    }

    private static void addHashMech(long id, String... names) {
        for (String n : names) {
            hashMechIds.put(n, id);
        }
    }

    private static void addMGF(long id, String name) {
        addMapping(mgfNames, mgfIds, id, name);
    }

    // The ordering here follows the PKCS11Constants class
    static {
        addMech(CKM_RSA_PKCS_KEY_PAIR_GEN,      "CKM_RSA_PKCS_KEY_PAIR_GEN");
        addMech(CKM_RSA_PKCS,                   "CKM_RSA_PKCS");
        addMech(CKM_RSA_9796,                   "CKM_RSA_9796");
        addMech(CKM_RSA_X_509,                  "CKM_RSA_X_509");
        addMech(CKM_MD2_RSA_PKCS,               "CKM_MD2_RSA_PKCS");
        addMech(CKM_MD5_RSA_PKCS,               "CKM_MD5_RSA_PKCS");
        addMech(CKM_SHA1_RSA_PKCS,              "CKM_SHA1_RSA_PKCS");
        addMech(CKM_RIPEMD128_RSA_PKCS,         "CKM_RIPEMD128_RSA_PKCS");
        addMech(CKM_RIPEMD160_RSA_PKCS,         "CKM_RIPEMD160_RSA_PKCS");
        addMech(CKM_RSA_PKCS_OAEP,              "CKM_RSA_PKCS_OAEP");
        addMech(CKM_RSA_X9_31_KEY_PAIR_GEN,     "CKM_RSA_X9_31_KEY_PAIR_GEN");
        addMech(CKM_RSA_X9_31,                  "CKM_RSA_X9_31");
        addMech(CKM_SHA1_RSA_X9_31,             "CKM_SHA1_RSA_X9_31");
        addMech(CKM_RSA_PKCS_PSS,               "CKM_RSA_PKCS_PSS");
        addMech(CKM_SHA1_RSA_PKCS_PSS,          "CKM_SHA1_RSA_PKCS_PSS");
        addMech(CKM_DSA_KEY_PAIR_GEN,           "CKM_DSA_KEY_PAIR_GEN");
        addMech(CKM_DSA,                        "CKM_DSA");
        addMech(CKM_DSA_SHA1,                   "CKM_DSA_SHA1");
        addMech(CKM_DSA_SHA224,                 "CKM_DSA_SHA224");
        addMech(CKM_DSA_SHA256,                 "CKM_DSA_SHA256");
        addMech(CKM_DSA_SHA384,                 "CKM_DSA_SHA384");
        addMech(CKM_DSA_SHA512,                 "CKM_DSA_SHA512");
        addMech(CKM_DSA_SHA3_224,               "CKM_DSA_SHA3_224");
        addMech(CKM_DSA_SHA3_256,               "CKM_DSA_SHA3_256");
        addMech(CKM_DSA_SHA3_384,               "CKM_DSA_SHA3_384");
        addMech(CKM_DSA_SHA3_512,               "CKM_DSA_SHA3_512");

        addMech(CKM_DH_PKCS_KEY_PAIR_GEN,       "CKM_DH_PKCS_KEY_PAIR_GEN");
        addMech(CKM_DH_PKCS_DERIVE,             "CKM_DH_PKCS_DERIVE");
        addMech(CKM_X9_42_DH_KEY_PAIR_GEN,      "CKM_X9_42_DH_KEY_PAIR_GEN");
        addMech(CKM_X9_42_DH_DERIVE,            "CKM_X9_42_DH_DERIVE");
        addMech(CKM_X9_42_DH_HYBRID_DERIVE,     "CKM_X9_42_DH_HYBRID_DERIVE");
        addMech(CKM_X9_42_MQV_DERIVE,           "CKM_X9_42_MQV_DERIVE");

        addMech(CKM_SHA256_RSA_PKCS,            "CKM_SHA256_RSA_PKCS");
        addMech(CKM_SHA384_RSA_PKCS,            "CKM_SHA384_RSA_PKCS");
        addMech(CKM_SHA512_RSA_PKCS,            "CKM_SHA512_RSA_PKCS");
        addMech(CKM_SHA256_RSA_PKCS_PSS,        "CKM_SHA256_RSA_PKCS_PSS");
        addMech(CKM_SHA384_RSA_PKCS_PSS,        "CKM_SHA384_RSA_PKCS_PSS");
        addMech(CKM_SHA512_RSA_PKCS_PSS,        "CKM_SHA512_RSA_PKCS_PSS");
        addMech(CKM_SHA224_RSA_PKCS,            "CKM_SHA224_RSA_PKCS");
        addMech(CKM_SHA224_RSA_PKCS_PSS,        "CKM_SHA224_RSA_PKCS_PSS");

        addMech(CKM_SHA512_224,                 "CKM_SHA512_224");
        addMech(CKM_SHA512_224_HMAC,            "CKM_SHA512_224_HMAC");
        addMech(CKM_SHA512_224_HMAC_GENERAL,    "CKM_SHA512_224_HMAC_GENERAL");
        addMech(CKM_SHA512_224_KEY_DERIVATION,  "CKM_SHA512_224_KEY_DERIVATION");
        addMech(CKM_SHA512_256,                 "CKM_SHA512_256");
        addMech(CKM_SHA512_256_HMAC,            "CKM_SHA512_256_HMAC");
        addMech(CKM_SHA512_256_HMAC_GENERAL,    "CKM_SHA512_256_HMAC_GENERAL");
        addMech(CKM_SHA512_256_KEY_DERIVATION,  "CKM_SHA512_256_KEY_DERIVATION");
        addMech(CKM_SHA512_T,                   "CKM_SHA512_T");
        addMech(CKM_SHA512_T_HMAC,              "CKM_SHA512_T_HMAC");
        addMech(CKM_SHA512_T_HMAC_GENERAL,      "CKM_SHA512_T_HMAC_GENERAL");
        addMech(CKM_SHA512_T_KEY_DERIVATION,    "CKM_SHA512_T_KEY_DERIVATION");

        addMech(CKM_SHA3_256_RSA_PKCS,          "CKM_SHA3_256_RSA_PKCS");
        addMech(CKM_SHA3_384_RSA_PKCS,          "CKM_SHA3_384_RSA_PKCS");
        addMech(CKM_SHA3_512_RSA_PKCS,          "CKM_SHA3_512_RSA_PKCS");
        addMech(CKM_SHA3_256_RSA_PKCS_PSS,      "CKM_SHA3_256_RSA_PKCS_PSS");
        addMech(CKM_SHA3_384_RSA_PKCS_PSS,      "CKM_SHA3_384_RSA_PKCS_PSS");
        addMech(CKM_SHA3_512_RSA_PKCS_PSS,      "CKM_SHA3_512_RSA_PKCS_PSS");
        addMech(CKM_SHA3_224_RSA_PKCS,          "CKM_SHA3_224_RSA_PKCS");
        addMech(CKM_SHA3_224_RSA_PKCS_PSS,      "CKM_SHA3_224_RSA_PKCS_PSS");

        addMech(CKM_RC2_KEY_GEN,                "CKM_RC2_KEY_GEN");
        addMech(CKM_RC2_ECB,                    "CKM_RC2_ECB");
        addMech(CKM_RC2_CBC,                    "CKM_RC2_CBC");
        addMech(CKM_RC2_MAC,                    "CKM_RC2_MAC");
        addMech(CKM_RC2_MAC_GENERAL,            "CKM_RC2_MAC_GENERAL");
        addMech(CKM_RC2_CBC_PAD,                "CKM_RC2_CBC_PAD");
        addMech(CKM_RC4_KEY_GEN,                "CKM_RC4_KEY_GEN");
        addMech(CKM_RC4,                        "CKM_RC4");
        addMech(CKM_DES_KEY_GEN,                "CKM_DES_KEY_GEN");
        addMech(CKM_DES_ECB,                    "CKM_DES_ECB");
        addMech(CKM_DES_CBC,                    "CKM_DES_CBC");
        addMech(CKM_DES_MAC,                    "CKM_DES_MAC");
        addMech(CKM_DES_MAC_GENERAL,            "CKM_DES_MAC_GENERAL");
        addMech(CKM_DES_CBC_PAD,                "CKM_DES_CBC_PAD");
        addMech(CKM_DES2_KEY_GEN,               "CKM_DES2_KEY_GEN");
        addMech(CKM_DES3_KEY_GEN,               "CKM_DES3_KEY_GEN");
        addMech(CKM_DES3_ECB,                   "CKM_DES3_ECB");
        addMech(CKM_DES3_CBC,                   "CKM_DES3_CBC");
        addMech(CKM_DES3_MAC,                   "CKM_DES3_MAC");
        addMech(CKM_DES3_MAC_GENERAL,           "CKM_DES3_MAC_GENERAL");
        addMech(CKM_DES3_CBC_PAD,               "CKM_DES3_CBC_PAD");
        addMech(CKM_DES3_CMAC_GENERAL,          "CKM_DES3_CMAC_GENERAL");
        addMech(CKM_DES3_CMAC,                  "CKM_DES3_CMAC");

        addMech(CKM_CDMF_KEY_GEN,               "CKM_CDMF_KEY_GEN");
        addMech(CKM_CDMF_ECB,                   "CKM_CDMF_ECB");
        addMech(CKM_CDMF_CBC,                   "CKM_CDMF_CBC");
        addMech(CKM_CDMF_MAC,                   "CKM_CDMF_MAC");
        addMech(CKM_CDMF_MAC_GENERAL,           "CKM_CDMF_MAC_GENERAL");
        addMech(CKM_CDMF_CBC_PAD,               "CKM_CDMF_CBC_PAD");

        addMech(CKM_DES_OFB64,                  "CKM_DES_OFB64");
        addMech(CKM_DES_OFB8,                   "CKM_DES_OFB8");
        addMech(CKM_DES_CFB64,                  "CKM_DES_CFB64");
        addMech(CKM_DES_CFB8,                   "CKM_DES_CFB8");

        addMech(CKM_MD2,                        "CKM_MD2");
        addMech(CKM_MD2_HMAC,                   "CKM_MD2_HMAC");
        addMech(CKM_MD2_HMAC_GENERAL,           "CKM_MD2_HMAC_GENERAL");
        addMech(CKM_MD5,                        "CKM_MD5");
        addMech(CKM_MD5_HMAC,                   "CKM_MD5_HMAC");
        addMech(CKM_MD5_HMAC_GENERAL,           "CKM_MD5_HMAC_GENERAL");
        addMech(CKM_SHA_1,                      "CKM_SHA_1");
        addMech(CKM_SHA_1_HMAC,                 "CKM_SHA_1_HMAC");
        addMech(CKM_SHA_1_HMAC_GENERAL,         "CKM_SHA_1_HMAC_GENERAL");
        addMech(CKM_RIPEMD128,                  "CKM_RIPEMD128");
        addMech(CKM_RIPEMD128_HMAC,             "CKM_RIPEMD128_HMAC");
        addMech(CKM_RIPEMD128_HMAC_GENERAL,     "CKM_RIPEMD128_HMAC_GENERAL");
        addMech(CKM_RIPEMD160,                  "CKM_RIPEMD160");
        addMech(CKM_RIPEMD160_HMAC,             "CKM_RIPEMD160_HMAC");
        addMech(CKM_RIPEMD160_HMAC_GENERAL,     "CKM_RIPEMD160_HMAC_GENERAL");
        addMech(CKM_SHA256,                     "CKM_SHA256");
        addMech(CKM_SHA256_HMAC,                "CKM_SHA256_HMAC");
        addMech(CKM_SHA256_HMAC_GENERAL,        "CKM_SHA256_HMAC_GENERAL");
        addMech(CKM_SHA224,                     "CKM_SHA224");
        addMech(CKM_SHA224_HMAC,                "CKM_SHA224_HMAC");
        addMech(CKM_SHA224_HMAC_GENERAL,        "CKM_SHA224_HMAC_GENERAL");
        addMech(CKM_SHA384,                     "CKM_SHA384");
        addMech(CKM_SHA384_HMAC,                "CKM_SHA384_HMAC");
        addMech(CKM_SHA384_HMAC_GENERAL,        "CKM_SHA384_HMAC_GENERAL");
        addMech(CKM_SHA512,                     "CKM_SHA512");
        addMech(CKM_SHA512_HMAC,                "CKM_SHA512_HMAC");
        addMech(CKM_SHA512_HMAC_GENERAL,        "CKM_SHA512_HMAC_GENERAL");

        addMech(CKM_SECURID_KEY_GEN,            "CKM_SECURID_KEY_GEN");
        addMech(CKM_SECURID,                    "CKM_SECURID");
        addMech(CKM_HOTP_KEY_GEN,               "CKM_HOTP_KEY_GEN");
        addMech(CKM_HOTP,                       "CKM_HOTP");
        addMech(CKM_ACTI,                       "CKM_ACTI");
        addMech(CKM_ACTI_KEY_GEN,               "CKM_ACTI_KEY_GEN");

        addMech(CKM_SHA3_256,                   "CKM_SHA3_256");
        addMech(CKM_SHA3_256_HMAC,              "CKM_SHA3_256_HMAC");
        addMech(CKM_SHA3_256_HMAC_GENERAL,      "CKM_SHA3_256_HMAC_GENERAL");
        addMech(CKM_SHA3_256_KEY_GEN,           "CKM_SHA3_256_KEY_GEN");
        addMech(CKM_SHA3_224,                   "CKM_SHA3_224");
        addMech(CKM_SHA3_224_HMAC,              "CKM_SHA3_224_HMAC");
        addMech(CKM_SHA3_224_HMAC_GENERAL,      "CKM_SHA3_224_HMAC_GENERAL");
        addMech(CKM_SHA3_224_KEY_GEN,           "CKM_SHA3_224_KEY_GEN");
        addMech(CKM_SHA3_384,                   "CKM_SHA3_384");
        addMech(CKM_SHA3_384_HMAC,              "CKM_SHA3_384_HMAC");
        addMech(CKM_SHA3_384_HMAC_GENERAL,      "CKM_SHA3_384_HMAC_GENERAL");
        addMech(CKM_SHA3_384_KEY_GEN,           "CKM_SHA3_384_KEY_GEN");
        addMech(CKM_SHA3_512,                   "CKM_SHA3_512");
        addMech(CKM_SHA3_512_HMAC,              "CKM_SHA3_512_HMAC");
        addMech(CKM_SHA3_512_HMAC_GENERAL,      "CKM_SHA3_512_HMAC_GENERAL");
        addMech(CKM_SHA3_512_KEY_GEN,           "CKM_SHA3_512_KEY_GEN");

        addMech(CKM_CAST_KEY_GEN,               "CKM_CAST_KEY_GEN");
        addMech(CKM_CAST_ECB,                   "CKM_CAST_ECB");
        addMech(CKM_CAST_CBC,                   "CKM_CAST_CBC");
        addMech(CKM_CAST_MAC,                   "CKM_CAST_MAC");
        addMech(CKM_CAST_MAC_GENERAL,           "CKM_CAST_MAC_GENERAL");
        addMech(CKM_CAST_CBC_PAD,               "CKM_CAST_CBC_PAD");
        addMech(CKM_CAST3_KEY_GEN,              "CKM_CAST3_KEY_GEN");
        addMech(CKM_CAST3_ECB,                  "CKM_CAST3_ECB");
        addMech(CKM_CAST3_CBC,                  "CKM_CAST3_CBC");
        addMech(CKM_CAST3_MAC,                  "CKM_CAST3_MAC");
        addMech(CKM_CAST3_MAC_GENERAL,          "CKM_CAST3_MAC_GENERAL");
        addMech(CKM_CAST3_CBC_PAD,              "CKM_CAST3_CBC_PAD");
        addMech(CKM_CAST128_KEY_GEN,            "CKM_CAST128_KEY_GEN");
        addMech(CKM_CAST128_ECB,                "CKM_CAST128_ECB");
        addMech(CKM_CAST128_CBC,                "CKM_CAST128_CBC");
        addMech(CKM_CAST128_MAC,                "CKM_CAST128_MAC");
        addMech(CKM_CAST128_MAC_GENERAL,        "CKM_CAST128_MAC_GENERAL");
        addMech(CKM_CAST128_CBC_PAD,            "CKM_CAST128_CBC_PAD");
        addMech(CKM_RC5_KEY_GEN,                "CKM_RC5_KEY_GEN");
        addMech(CKM_RC5_ECB,                    "CKM_RC5_ECB");
        addMech(CKM_RC5_CBC,                    "CKM_RC5_CBC");
        addMech(CKM_RC5_MAC,                    "CKM_RC5_MAC");
        addMech(CKM_RC5_MAC_GENERAL,            "CKM_RC5_MAC_GENERAL");
        addMech(CKM_RC5_CBC_PAD,                "CKM_RC5_CBC_PAD");
        addMech(CKM_IDEA_KEY_GEN,               "CKM_IDEA_KEY_GEN");
        addMech(CKM_IDEA_ECB,                   "CKM_IDEA_ECB");
        addMech(CKM_IDEA_CBC,                   "CKM_IDEA_CBC");
        addMech(CKM_IDEA_MAC,                   "CKM_IDEA_MAC");
        addMech(CKM_IDEA_MAC_GENERAL,           "CKM_IDEA_MAC_GENERAL");
        addMech(CKM_IDEA_CBC_PAD,               "CKM_IDEA_CBC_PAD");
        addMech(CKM_GENERIC_SECRET_KEY_GEN,     "CKM_GENERIC_SECRET_KEY_GEN");
        addMech(CKM_CONCATENATE_BASE_AND_KEY,   "CKM_CONCATENATE_BASE_AND_KEY");
        addMech(CKM_CONCATENATE_BASE_AND_DATA,  "CKM_CONCATENATE_BASE_AND_DATA");
        addMech(CKM_CONCATENATE_DATA_AND_BASE,  "CKM_CONCATENATE_DATA_AND_BASE");
        addMech(CKM_XOR_BASE_AND_DATA,          "CKM_XOR_BASE_AND_DATA");
        addMech(CKM_EXTRACT_KEY_FROM_KEY,       "CKM_EXTRACT_KEY_FROM_KEY");
        addMech(CKM_SSL3_PRE_MASTER_KEY_GEN,    "CKM_SSL3_PRE_MASTER_KEY_GEN");
        addMech(CKM_SSL3_MASTER_KEY_DERIVE,     "CKM_SSL3_MASTER_KEY_DERIVE");
        addMech(CKM_SSL3_KEY_AND_MAC_DERIVE,    "CKM_SSL3_KEY_AND_MAC_DERIVE");
        addMech(CKM_SSL3_MASTER_KEY_DERIVE_DH,  "CKM_SSL3_MASTER_KEY_DERIVE_DH");
        addMech(CKM_TLS_PRE_MASTER_KEY_GEN,     "CKM_TLS_PRE_MASTER_KEY_GEN");
        addMech(CKM_TLS_MASTER_KEY_DERIVE,      "CKM_TLS_MASTER_KEY_DERIVE");
        addMech(CKM_TLS_KEY_AND_MAC_DERIVE,     "CKM_TLS_KEY_AND_MAC_DERIVE");
        addMech(CKM_TLS_MASTER_KEY_DERIVE_DH,   "CKM_TLS_MASTER_KEY_DERIVE_DH");
        addMech(CKM_TLS_PRF,                    "CKM_TLS_PRF");
        addMech(CKM_SSL3_MD5_MAC,               "CKM_SSL3_MD5_MAC");
        addMech(CKM_SSL3_SHA1_MAC,              "CKM_SSL3_SHA1_MAC");

        addMech(CKM_MD5_KEY_DERIVATION,         "CKM_MD5_KEY_DERIVATION");
        addMech(CKM_MD2_KEY_DERIVATION,         "CKM_MD2_KEY_DERIVATION");
        addMech(CKM_SHA1_KEY_DERIVATION,        "CKM_SHA1_KEY_DERIVATION");
        addMech(CKM_SHA256_KEY_DERIVATION,      "CKM_SHA256_KEY_DERIVATION");
        addMech(CKM_SHA384_KEY_DERIVATION,      "CKM_SHA384_KEY_DERIVATION");
        addMech(CKM_SHA512_KEY_DERIVATION,      "CKM_SHA512_KEY_DERIVATION");
        addMech(CKM_SHA224_KEY_DERIVATION,      "CKM_SHA224_KEY_DERIVATION");
        addMech(CKM_SHA3_256_KEY_DERIVATION,    "CKM_SHA3_256_KEY_DERIVATION");
        addMech(CKM_SHA3_224_KEY_DERIVATION,    "CKM_SHA3_224_KEY_DERIVATION");
        addMech(CKM_SHA3_384_KEY_DERIVATION,    "CKM_SHA3_384_KEY_DERIVATION");
        addMech(CKM_SHA3_512_KEY_DERIVATION,    "CKM_SHA3_512_KEY_DERIVATION");
        addMech(CKM_SHAKE_128_KEY_DERIVATION,   "CKM_SHAKE_128_KEY_DERIVATION");
        addMech(CKM_SHAKE_256_KEY_DERIVATION,   "CKM_SHAKE_256_KEY_DERIVATION");

        addMech(CKM_PBE_MD2_DES_CBC,            "CKM_PBE_MD2_DES_CBC");
        addMech(CKM_PBE_MD5_DES_CBC,            "CKM_PBE_MD5_DES_CBC");
        addMech(CKM_PBE_MD5_CAST_CBC,           "CKM_PBE_MD5_CAST_CBC");
        addMech(CKM_PBE_MD5_CAST3_CBC,          "CKM_PBE_MD5_CAST3_CBC");
        addMech(CKM_PBE_MD5_CAST128_CBC,        "CKM_PBE_MD5_CAST128_CBC");
        addMech(CKM_PBE_SHA1_CAST128_CBC,       "CKM_PBE_SHA1_CAST128_CBC");
        addMech(CKM_PBE_SHA1_RC4_128,           "CKM_PBE_SHA1_RC4_128");
        addMech(CKM_PBE_SHA1_RC4_40,            "CKM_PBE_SHA1_RC4_40");
        addMech(CKM_PBE_SHA1_DES3_EDE_CBC,      "CKM_PBE_SHA1_DES3_EDE_CBC");
        addMech(CKM_PBE_SHA1_DES2_EDE_CBC,      "CKM_PBE_SHA1_DES2_EDE_CBC");
        addMech(CKM_PBE_SHA1_RC2_128_CBC,       "CKM_PBE_SHA1_RC2_128_CBC");
        addMech(CKM_PBE_SHA1_RC2_40_CBC,        "CKM_PBE_SHA1_RC2_40_CBC");
        addMech(CKM_PKCS5_PBKD2,                "CKM_PKCS5_PBKD2");
        addMech(CKM_PBA_SHA1_WITH_SHA1_HMAC,    "CKM_PBA_SHA1_WITH_SHA1_HMAC");

        addMech(CKM_WTLS_PRE_MASTER_KEY_GEN,    "CKM_WTLS_PRE_MASTER_KEY_GEN");
        addMech(CKM_WTLS_MASTER_KEY_DERIVE,     "CKM_WTLS_MASTER_KEY_DERIVE");
        addMech(CKM_WTLS_MASTER_KEY_DERIVE_DH_ECC,
                                                "CKM_WTLS_MASTER_KEY_DERIVE_DH_ECC");
        addMech(CKM_WTLS_PRF,                   "CKM_WTLS_PRF");
        addMech(CKM_WTLS_SERVER_KEY_AND_MAC_DERIVE,
                                                "CKM_WTLS_SERVER_KEY_AND_MAC_DERIVE");
        addMech(CKM_WTLS_CLIENT_KEY_AND_MAC_DERIVE,
                                                "CKM_WTLS_CLIENT_KEY_AND_MAC_DERIVE");
        addMech(CKM_TLS10_MAC_SERVER,           "CKM_TLS10_MAC_SERVER");
        addMech(CKM_TLS10_MAC_CLIENT,           "CKM_TLS10_MAC_CLIENT");
        addMech(CKM_TLS12_MAC,                  "CKM_TLS12_MAC");
        addMech(CKM_TLS12_KDF,                  "CKM_TLS12_KDF");
        addMech(CKM_TLS12_MASTER_KEY_DERIVE,    "CKM_TLS12_MASTER_KEY_DERIVE");
        addMech(CKM_TLS12_KEY_AND_MAC_DERIVE,   "CKM_TLS12_KEY_AND_MAC_DERIVE");
        addMech(CKM_TLS12_MASTER_KEY_DERIVE_DH, "CKM_TLS12_MASTER_KEY_DERIVE_DH");
        addMech(CKM_TLS12_KEY_SAFE_DERIVE,      "CKM_TLS12_KEY_SAFE_DERIVE");
        addMech(CKM_TLS_MAC,                    "CKM_TLS_MAC");
        addMech(CKM_TLS_KDF,                    "CKM_TLS_KDF");

        addMech(CKM_KEY_WRAP_LYNKS,             "CKM_KEY_WRAP_LYNKS");
        addMech(CKM_KEY_WRAP_SET_OAEP,          "CKM_KEY_WRAP_SET_OAEP");

        addMech(CKM_CMS_SIG,                    "CKM_CMS_SIG");
        addMech(CKM_KIP_DERIVE,                 "CKM_KIP_DERIVE");
        addMech(CKM_KIP_WRAP,                   "CKM_KIP_WRAP");
        addMech(CKM_KIP_MAC,                    "CKM_KIP_MAC");
        addMech(CKM_CAMELLIA_KEY_GEN,           "CKM_CAMELLIA_KEY_GEN");
        addMech(CKM_CAMELLIA_ECB,               "CKM_CAMELLIA_ECB");
        addMech(CKM_CAMELLIA_CBC,               "CKM_CAMELLIA_CBC");
        addMech(CKM_CAMELLIA_MAC,               "CKM_CAMELLIA_MAC");
        addMech(CKM_CAMELLIA_MAC_GENERAL,       "CKM_CAMELLIA_MAC_GENERAL");
        addMech(CKM_CAMELLIA_CBC_PAD,           "CKM_CAMELLIA_CBC_PAD");
        addMech(CKM_CAMELLIA_ECB_ENCRYPT_DATA,  "CKM_CAMELLIA_ECB_ENCRYPT_DATA");
        addMech(CKM_CAMELLIA_CBC_ENCRYPT_DATA,  "CKM_CAMELLIA_CBC_ENCRYPT_DATA");
        addMech(CKM_CAMELLIA_CTR,               "CKM_CAMELLIA_CTR");

        addMech(CKM_ARIA_KEY_GEN,               "CKM_ARIA_KEY_GEN");
        addMech(CKM_ARIA_ECB,                   "CKM_ARIA_ECB");
        addMech(CKM_ARIA_CBC,                   "CKM_ARIA_CBC");
        addMech(CKM_ARIA_MAC,                   "CKM_ARIA_MAC");
        addMech(CKM_ARIA_MAC_GENERAL,           "CKM_ARIA_MAC_GENERAL");
        addMech(CKM_ARIA_CBC_PAD,               "CKM_ARIA_CBC_PAD");
        addMech(CKM_ARIA_ECB_ENCRYPT_DATA,      "CKM_ARIA_ECB_ENCRYPT_DATA");
        addMech(CKM_ARIA_CBC_ENCRYPT_DATA,      "CKM_ARIA_CBC_ENCRYPT_DATA");

        addMech(CKM_SEED_KEY_GEN,               "CKM_SEED_KEY_GEN");
        addMech(CKM_SEED_ECB,                   "CKM_SEED_ECB");
        addMech(CKM_SEED_CBC,                   "CKM_SEED_CBC");
        addMech(CKM_SEED_MAC,                   "CKM_SEED_MAC");
        addMech(CKM_SEED_MAC_GENERAL,           "CKM_SEED_MAC_GENERAL");
        addMech(CKM_SEED_CBC_PAD,               "CKM_SEED_CBC_PAD");
        addMech(CKM_SEED_ECB_ENCRYPT_DATA,      "CKM_SEED_ECB_ENCRYPT_DATA");
        addMech(CKM_SEED_CBC_ENCRYPT_DATA,      "CKM_SEED_CBC_ENCRYPT_DATA");

        addMech(CKM_SKIPJACK_KEY_GEN,           "CKM_SKIPJACK_KEY_GEN");
        addMech(CKM_SKIPJACK_ECB64,             "CKM_SKIPJACK_ECB64");
        addMech(CKM_SKIPJACK_CBC64,             "CKM_SKIPJACK_CBC64");
        addMech(CKM_SKIPJACK_OFB64,             "CKM_SKIPJACK_OFB64");
        addMech(CKM_SKIPJACK_CFB64,             "CKM_SKIPJACK_CFB64");
        addMech(CKM_SKIPJACK_CFB32,             "CKM_SKIPJACK_CFB32");
        addMech(CKM_SKIPJACK_CFB16,             "CKM_SKIPJACK_CFB16");
        addMech(CKM_SKIPJACK_CFB8,              "CKM_SKIPJACK_CFB8");
        addMech(CKM_SKIPJACK_WRAP,              "CKM_SKIPJACK_WRAP");
        addMech(CKM_SKIPJACK_PRIVATE_WRAP,      "CKM_SKIPJACK_PRIVATE_WRAP");
        addMech(CKM_SKIPJACK_RELAYX,            "CKM_SKIPJACK_RELAYX");
        addMech(CKM_KEA_KEY_PAIR_GEN,           "CKM_KEA_KEY_PAIR_GEN");
        addMech(CKM_KEA_KEY_DERIVE,             "CKM_KEA_KEY_DERIVE");
        addMech(CKM_FORTEZZA_TIMESTAMP,         "CKM_FORTEZZA_TIMESTAMP");
        addMech(CKM_BATON_KEY_GEN,              "CKM_BATON_KEY_GEN");
        addMech(CKM_BATON_ECB128,               "CKM_BATON_ECB128");
        addMech(CKM_BATON_ECB96,                "CKM_BATON_ECB96");
        addMech(CKM_BATON_CBC128,               "CKM_BATON_CBC128");
        addMech(CKM_BATON_COUNTER,              "CKM_BATON_COUNTER");
        addMech(CKM_BATON_SHUFFLE,              "CKM_BATON_SHUFFLE");
        addMech(CKM_BATON_WRAP,                 "CKM_BATON_WRAP");
        addMech(CKM_EC_KEY_PAIR_GEN,            "CKM_EC_KEY_PAIR_GEN");
        addMech(CKM_EC_KEY_PAIR_GEN_W_EXTRA_BITS,
                                            "CKM_EC_KEY_PAIR_GEN_W_EXTRA_BITS");

        addMech(CKM_ECDSA,                      "CKM_ECDSA");
        addMech(CKM_ECDSA_SHA1,                 "CKM_ECDSA_SHA1");
        addMech(CKM_ECDSA_SHA224,               "CKM_ECDSA_SHA224");
        addMech(CKM_ECDSA_SHA256,               "CKM_ECDSA_SHA256");
        addMech(CKM_ECDSA_SHA384,               "CKM_ECDSA_SHA384");
        addMech(CKM_ECDSA_SHA512,               "CKM_ECDSA_SHA512");
        addMech(CKM_ECDSA_SHA3_224,             "CKM_ECDSA_SHA3_224");
        addMech(CKM_ECDSA_SHA3_256,             "CKM_ECDSA_SHA3_256");
        addMech(CKM_ECDSA_SHA3_384,             "CKM_ECDSA_SHA3_384");
        addMech(CKM_ECDSA_SHA3_512,             "CKM_ECDSA_SHA3_512");

        addMech(CKM_ECDH1_DERIVE,               "CKM_ECDH1_DERIVE");
        addMech(CKM_ECDH1_COFACTOR_DERIVE,      "CKM_ECDH1_COFACTOR_DERIVE");
        addMech(CKM_ECMQV_DERIVE,               "CKM_ECMQV_DERIVE");
        addMech(CKM_ECDH_AES_KEY_WRAP,          "CKM_ECDH_AES_KEY_WRAP");
        addMech(CKM_RSA_AES_KEY_WRAP,           "CKM_RSA_AES_KEY_WRAP");
        addMech(CKM_EC_EDWARDS_KEY_PAIR_GEN,    "CKM_EC_EDWARDS_KEY_PAIR_GEN");
        addMech(CKM_EC_MONTGOMERY_KEY_PAIR_GEN,
                                            "CKM_EC_MONTGOMERY_KEY_PAIR_GEN");
        addMech(CKM_EDDSA,                      "CKM_EDDSA");

        addMech(CKM_JUNIPER_KEY_GEN,            "CKM_JUNIPER_KEY_GEN");
        addMech(CKM_JUNIPER_ECB128,             "CKM_JUNIPER_ECB128");
        addMech(CKM_JUNIPER_CBC128,             "CKM_JUNIPER_CBC128");
        addMech(CKM_JUNIPER_COUNTER,            "CKM_JUNIPER_COUNTER");
        addMech(CKM_JUNIPER_SHUFFLE,            "CKM_JUNIPER_SHUFFLE");
        addMech(CKM_JUNIPER_WRAP,               "CKM_JUNIPER_WRAP");
        addMech(CKM_FASTHASH,                   "CKM_FASTHASH");
        addMech(CKM_AES_XTS,                    "CKM_AES_XTS");
        addMech(CKM_AES_XTS_KEY_GEN,            "CKM_AES_XTS_KEY_GEN");

        addMech(CKM_AES_KEY_GEN,                "CKM_AES_KEY_GEN");
        addMech(CKM_AES_ECB,                    "CKM_AES_ECB");
        addMech(CKM_AES_CBC,                    "CKM_AES_CBC");
        addMech(CKM_AES_MAC,                    "CKM_AES_MAC");
        addMech(CKM_AES_MAC_GENERAL,            "CKM_AES_MAC_GENERAL");
        addMech(CKM_AES_CBC_PAD,                "CKM_AES_CBC_PAD");
        addMech(CKM_AES_CTR,                    "CKM_AES_CTR");
        addMech(CKM_AES_GCM,                    "CKM_AES_GCM");
        addMech(CKM_AES_CCM,                    "CKM_AES_CCM");
        addMech(CKM_AES_CTS,                    "CKM_AES_CTS");
        addMech(CKM_AES_CMAC,                   "CKM_AES_CMAC");
        addMech(CKM_AES_CMAC_GENERAL,           "CKM_AES_CMAC_GENERAL");
        addMech(CKM_AES_XCBC_MAC,               "CKM_AES_XCBC_MAC");
        addMech(CKM_AES_XCBC_MAC_96,            "CKM_AES_XCBC_MAC_96");
        addMech(CKM_AES_GMAC,                   "CKM_AES_GMAC");

        addMech(CKM_BLOWFISH_KEY_GEN,           "CKM_BLOWFISH_KEY_GEN");
        addMech(CKM_BLOWFISH_CBC,               "CKM_BLOWFISH_CBC");
        addMech(CKM_TWOFISH_KEY_GEN,            "CKM_TWOFISH_KEY_GEN");
        addMech(CKM_TWOFISH_CBC,                "CKM_TWOFISH_CBC");
        addMech(CKM_BLOWFISH_CBC_PAD,           "CKM_BLOWFISH_CBC_PAD");
        addMech(CKM_TWOFISH_CBC_PAD,            "CKM_TWOFISH_CBC_PAD");

        addMech(CKM_DES_ECB_ENCRYPT_DATA,       "CKM_DES_ECB_ENCRYPT_DATA");
        addMech(CKM_DES_CBC_ENCRYPT_DATA,       "CKM_DES_CBC_ENCRYPT_DATA");
        addMech(CKM_DES3_ECB_ENCRYPT_DATA,      "CKM_DES3_ECB_ENCRYPT_DATA");
        addMech(CKM_DES3_CBC_ENCRYPT_DATA,      "CKM_DES3_CBC_ENCRYPT_DATA");
        addMech(CKM_AES_ECB_ENCRYPT_DATA,       "CKM_AES_ECB_ENCRYPT_DATA");
        addMech(CKM_AES_CBC_ENCRYPT_DATA,       "CKM_AES_CBC_ENCRYPT_DATA");

        addMech(CKM_GOSTR3410_KEY_PAIR_GEN,     "CKM_GOSTR3410_KEY_PAIR_GEN");
        addMech(CKM_GOSTR3410,                  "CKM_GOSTR3410");
        addMech(CKM_GOSTR3410_WITH_GOSTR3411,   "CKM_GOSTR3410_WITH_GOSTR3411");
        addMech(CKM_GOSTR3410_KEY_WRAP,         "CKM_GOSTR3410_KEY_WRAP");
        addMech(CKM_GOSTR3410_DERIVE,           "CKM_GOSTR3410_DERIVE");
        addMech(CKM_GOSTR3411,                  "CKM_GOSTR3411");
        addMech(CKM_GOSTR3411_HMAC,             "CKM_GOSTR3411_HMAC");
        addMech(CKM_GOST28147_KEY_GEN,          "CKM_GOST28147_KEY_GEN");
        addMech(CKM_GOST28147_ECB,              "CKM_GOST28147_ECB");
        addMech(CKM_GOST28147,                  "CKM_GOST28147");
        addMech(CKM_GOST28147_MAC,              "CKM_GOST28147_MAC");
        addMech(CKM_GOST28147_KEY_WRAP,         "CKM_GOST28147_KEY_WRAP");
        addMech(CKM_CHACHA20_KEY_GEN,           "CKM_CHACHA20_KEY_GEN");
        addMech(CKM_CHACHA20,                   "CKM_CHACHA20");
        addMech(CKM_POLY1305_KEY_GEN,           "CKM_POLY1305_KEY_GEN");
        addMech(CKM_POLY1305,                   "CKM_POLY1305");

        addMech(CKM_DSA_PARAMETER_GEN,          "CKM_DSA_PARAMETER_GEN");
        addMech(CKM_DH_PKCS_PARAMETER_GEN,      "CKM_DH_PKCS_PARAMETER_GEN");
        addMech(CKM_X9_42_DH_PARAMETER_GEN,     "CKM_X9_42_DH_PARAMETER_GEN");
        addMech(CKM_DSA_PROBABLISTIC_PARAMETER_GEN,
                                                "CKM_DSA_PROBABLISTIC_PARAMETER_GEN");
        addMech(CKM_DSA_SHAWE_TAYLOR_PARAMETER_GEN,
                                                "CKM_DSA_SHAWE_TAYLOR_PARAMETER_GEN");
        addMech(CKM_DSA_FIPS_G_GEN,             "CKM_DSA_FIPS_G_GEN");

        addMech(CKM_AES_OFB,                    "CKM_AES_OFB");
        addMech(CKM_AES_CFB64,                  "CKM_AES_CFB64");
        addMech(CKM_AES_CFB8,                   "CKM_AES_CFB8");
        addMech(CKM_AES_CFB128,                 "CKM_AES_CFB128");
        addMech(CKM_AES_CFB1,                   "CKM_AES_CFB1");
        addMech(CKM_AES_KEY_WRAP,               "CKM_AES_KEY_WRAP");
        addMech(CKM_AES_KEY_WRAP_PAD,           "CKM_AES_KEY_WRAP_PAD");
        addMech(CKM_AES_KEY_WRAP_KWP,           "CKM_AES_KEY_WRAP_KWP");
        addMech(CKM_RSA_PKCS_TPM_1_1,           "CKM_RSA_PKCS_TPM_1_1");
        addMech(CKM_RSA_PKCS_OAEP_TPM_1_1,      "CKM_RSA_PKCS_OAEP_TPM_1_1");
        addMech(CKM_SHA_1_KEY_GEN,              "CKM_SHA_1_KEY_GEN");
        addMech(CKM_SHA224_KEY_GEN,             "CKM_SHA224_KEY_GEN");
        addMech(CKM_SHA256_KEY_GEN,             "CKM_SHA256_KEY_GEN");
        addMech(CKM_SHA384_KEY_GEN,             "CKM_SHA384_KEY_GEN");
        addMech(CKM_SHA512_KEY_GEN,             "CKM_SHA512_KEY_GEN");
        addMech(CKM_SHA512_224_KEY_GEN,         "CKM_SHA512_224_KEY_GEN");
        addMech(CKM_SHA512_256_KEY_GEN,         "CKM_SHA512_256_KEY_GEN");
        addMech(CKM_SHA512_T_KEY_GEN,           "CKM_SHA512_T_KEY_GEN");
        addMech(CKM_NULL,                       "CKM_NULL");
        addMech(CKM_BLAKE2B_160,                "CKM_BLAKE2B_160");
        addMech(CKM_BLAKE2B_160_HMAC,           "CKM_BLAKE2B_160_HMAC");
        addMech(CKM_BLAKE2B_160_HMAC_GENERAL,   "CKM_BLAKE2B_160_HMAC_GENERAL");
        addMech(CKM_BLAKE2B_160_KEY_DERIVE,     "CKM_BLAKE2B_160_KEY_DERIVE");
        addMech(CKM_BLAKE2B_160_KEY_GEN,        "CKM_BLAKE2B_160_KEY_GEN");
        addMech(CKM_BLAKE2B_256,                "CKM_BLAKE2B_256");
        addMech(CKM_BLAKE2B_256_HMAC,           "CKM_BLAKE2B_256_HMAC");
        addMech(CKM_BLAKE2B_256_HMAC_GENERAL,   "CKM_BLAKE2B_256_HMAC_GENERAL");
        addMech(CKM_BLAKE2B_256_KEY_DERIVE,     "CKM_BLAKE2B_256_KEY_DERIVE");
        addMech(CKM_BLAKE2B_256_KEY_GEN,        "CKM_BLAKE2B_256_KEY_GEN");
        addMech(CKM_BLAKE2B_384,                "CKM_BLAKE2B_384");
        addMech(CKM_BLAKE2B_384_HMAC,           "CKM_BLAKE2B_384_HMAC");
        addMech(CKM_BLAKE2B_384_HMAC_GENERAL,   "CKM_BLAKE2B_384_HMAC_GENERAL");
        addMech(CKM_BLAKE2B_384_KEY_DERIVE,     "CKM_BLAKE2B_384_KEY_DERIVE");
        addMech(CKM_BLAKE2B_384_KEY_GEN,        "CKM_BLAKE2B_384_KEY_GEN");
        addMech(CKM_BLAKE2B_512,                "CKM_BLAKE2B_512");
        addMech(CKM_BLAKE2B_512_HMAC,           "CKM_BLAKE2B_512_HMAC");
        addMech(CKM_BLAKE2B_512_HMAC_GENERAL,   "CKM_BLAKE2B_512_HMAC_GENERAL");
        addMech(CKM_BLAKE2B_512_KEY_DERIVE,     "CKM_BLAKE2B_512_KEY_DERIVE");
        addMech(CKM_BLAKE2B_512_KEY_GEN,        "CKM_BLAKE2B_512_KEY_GEN");
        addMech(CKM_SALSA20,                    "CKM_SALSA20");
        addMech(CKM_CHACHA20_POLY1305,          "CKM_CHACHA20_POLY1305");
        addMech(CKM_SALSA20_POLY1305,           "CKM_SALSA20_POLY1305");
        addMech(CKM_X3DH_INITIALIZE,            "CKM_X3DH_INITIALIZE");
        addMech(CKM_X3DH_RESPOND,               "CKM_X3DH_RESPOND");
        addMech(CKM_X2RATCHET_INITIALIZE,       "CKM_X2RATCHET_INITIALIZE");
        addMech(CKM_X2RATCHET_RESPOND,          "CKM_X2RATCHET_RESPOND");
        addMech(CKM_X2RATCHET_ENCRYPT,          "CKM_X2RATCHET_ENCRYPT");
        addMech(CKM_X2RATCHET_DECRYPT,          "CKM_X2RATCHET_DECRYPT");
        addMech(CKM_XEDDSA,                     "CKM_XEDDSA");
        addMech(CKM_HKDF_DERIVE,                "CKM_HKDF_DERIVE");
        addMech(CKM_HKDF_DATA,                  "CKM_HKDF_DATA");
        addMech(CKM_HKDF_KEY_GEN,               "CKM_HKDF_KEY_GEN");
        addMech(CKM_SALSA20_KEY_GEN,            "CKM_SALSA20_KEY_GEN");
        addMech(CKM_SP800_108_COUNTER_KDF,      "CKM_SP800_108_COUNTER_KDF");
        addMech(CKM_SP800_108_FEEDBACK_KDF,     "CKM_SP800_108_FEEDBACK_KDF");
        addMech(CKM_SP800_108_DOUBLE_PIPELINE_KDF,
                                         "CKM_SP800_108_DOUBLE_PIPELINE_KDF");

        addMech(CKM_VENDOR_DEFINED,             "CKM_VENDOR_DEFINED");

        addMech(CKM_NSS_TLS_PRF_GENERAL,        "CKM_NSS_TLS_PRF_GENERAL");

        addMech(PCKM_SECURERANDOM,              "SecureRandom");
        addMech(PCKM_KEYSTORE,                  "KeyStore");

        addHashMech(CKM_SHA_1,                  "SHA-1", "SHA", "SHA1");
        addHashMech(CKM_SHA224,                 "SHA-224", "SHA224");
        addHashMech(CKM_SHA256,                 "SHA-256", "SHA256");
        addHashMech(CKM_SHA384,                 "SHA-384", "SHA384");
        addHashMech(CKM_SHA512,                 "SHA-512", "SHA512");
        addHashMech(CKM_SHA512_224,             "SHA-512/224", "SHA512/224");
        addHashMech(CKM_SHA512_256,             "SHA-512/256", "SHA512/256");
        addHashMech(CKM_SHA3_224,               "SHA3-224");
        addHashMech(CKM_SHA3_256,               "SHA3-256");
        addHashMech(CKM_SHA3_384,               "SHA3-384");
        addHashMech(CKM_SHA3_512,               "SHA3-512");

        addKeyType(CKK_RSA,                     "CKK_RSA");
        addKeyType(CKK_DSA,                     "CKK_DSA");
        addKeyType(CKK_DH,                      "CKK_DH");
        addKeyType(CKK_EC,                      "CKK_EC");
        addKeyType(CKK_X9_42_DH,                "CKK_X9_42_DH");
        addKeyType(CKK_KEA,                     "CKK_KEA");
        addKeyType(CKK_GENERIC_SECRET,          "CKK_GENERIC_SECRET");
        addKeyType(CKK_RC2,                     "CKK_RC2");
        addKeyType(CKK_RC4,                     "CKK_RC4");
        addKeyType(CKK_DES,                     "CKK_DES");
        addKeyType(CKK_DES2,                    "CKK_DES2");
        addKeyType(CKK_DES3,                    "CKK_DES3");
        addKeyType(CKK_CAST,                    "CKK_CAST");
        addKeyType(CKK_CAST3,                   "CKK_CAST3");
        addKeyType(CKK_CAST128,                 "CKK_CAST128");
        addKeyType(CKK_RC5,                     "CKK_RC5");
        addKeyType(CKK_IDEA,                    "CKK_IDEA");
        addKeyType(CKK_SKIPJACK,                "CKK_SKIPJACK");
        addKeyType(CKK_BATON,                   "CKK_BATON");
        addKeyType(CKK_JUNIPER,                 "CKK_JUNIPER");
        addKeyType(CKK_CDMF,                    "CKK_CDMF");
        addKeyType(CKK_AES,                     "CKK_AES");
        addKeyType(CKK_BLOWFISH,                "CKK_BLOWFISH");
        addKeyType(CKK_TWOFISH,                 "CKK_TWOFISH");
        addKeyType(CKK_SECURID,                 "CKK_SECURID");
        addKeyType(CKK_HOTP,                    "CKK_HOTP");
        addKeyType(CKK_ACTI,                    "CKK_ACTI");
        addKeyType(CKK_CAMELLIA,                "CKK_CAMELLIA");
        addKeyType(CKK_ARIA,                    "CKK_ARIA");
        addKeyType(CKK_MD5_HMAC,                "CKK_MD5_HMAC");
        addKeyType(CKK_SHA_1_HMAC,              "CKK_SHA_1_HMAC");
        addKeyType(CKK_RIPEMD128_HMAC,          "CKK_RIPEMD128_HMAC");
        addKeyType(CKK_RIPEMD160_HMAC,          "CKK_RIPEMD160_HMAC");
        addKeyType(CKK_SHA256_HMAC,             "CKK_SHA256_HMAC");
        addKeyType(CKK_SHA384_HMAC,             "CKK_SHA384_HMAC");
        addKeyType(CKK_SHA512_HMAC,             "CKK_SHA512_HMAC");
        addKeyType(CKK_SHA224_HMAC,             "CKK_SHA224_HMAC");
        addKeyType(CKK_SEED,                    "CKK_SEED");
        addKeyType(CKK_GOSTR3410,               "CKK_GOSTR3410");
        addKeyType(CKK_GOSTR3411,               "CKK_GOSTR3411");
        addKeyType(CKK_GOST28147,               "CKK_GOST28147");
        addKeyType(CKK_CHACHA20,                "CKK_CHACHA20");
        addKeyType(CKK_POLY1305,                "CKK_POLY1305");
        addKeyType(CKK_AES_XTS,                 "CKK_AES_XTS");

        addKeyType(CKK_SHA3_224_HMAC,           "CKK_SHA3_224_HMAC");
        addKeyType(CKK_SHA3_256_HMAC,           "CKK_SHA3_256_HMAC");
        addKeyType(CKK_SHA3_384_HMAC,           "CKK_SHA3_384_HMAC");
        addKeyType(CKK_SHA3_512_HMAC,           "CKK_SHA3_512_HMAC");
        addKeyType(CKK_BLAKE2B_160_HMAC,        "CKK_BLAKE2B_160_HMAC");
        addKeyType(CKK_BLAKE2B_256_HMAC,        "CKK_BLAKE2B_256_HMAC");
        addKeyType(CKK_BLAKE2B_384_HMAC,        "CKK_BLAKE2B_384_HMAC");
        addKeyType(CKK_BLAKE2B_512_HMAC,        "CKK_BLAKE2B_512_HMAC");
        addKeyType(CKK_SALSA20,                 "CKK_SALSA20");
        addKeyType(CKK_X2RATCHET,               "CKK_X2RATCHET");
        addKeyType(CKK_EC_EDWARDS,              "CKK_EC_EDWARDS");
        addKeyType(CKK_EC_MONTGOMERY,           "CKK_EC_MONTGOMERY");
        addKeyType(CKK_HKDF,                    "CKK_HKDF");

        addKeyType(CKK_SHA512_224_HMAC,         "CKK_SHA512_224_HMAC");
        addKeyType(CKK_SHA512_256_HMAC,         "CKK_SHA512_256_HMAC");
        addKeyType(CKK_SHA512_T_HMAC,           "CKK_SHA512_T_HMAC");

        addKeyType(CKK_VENDOR_DEFINED,          "CKK_VENDOR_DEFINED");

        addKeyType(PCKK_ANY,                    "*");

        addAttribute(CKA_CLASS,                 "CKA_CLASS");
        addAttribute(CKA_TOKEN,                 "CKA_TOKEN");
        addAttribute(CKA_PRIVATE,               "CKA_PRIVATE");
        addAttribute(CKA_LABEL,                 "CKA_LABEL");
        addAttribute(CKA_UNIQUE_ID,             "CKA_UNIQUE_ID");
        addAttribute(CKA_APPLICATION,           "CKA_APPLICATION");
        addAttribute(CKA_VALUE,                 "CKA_VALUE");
        addAttribute(CKA_OBJECT_ID,             "CKA_OBJECT_ID");
        addAttribute(CKA_CERTIFICATE_TYPE,      "CKA_CERTIFICATE_TYPE");
        addAttribute(CKA_ISSUER,                "CKA_ISSUER");
        addAttribute(CKA_SERIAL_NUMBER,         "CKA_SERIAL_NUMBER");
        addAttribute(CKA_AC_ISSUER,             "CKA_AC_ISSUER");
        addAttribute(CKA_OWNER,                 "CKA_OWNER");
        addAttribute(CKA_ATTR_TYPES,            "CKA_ATTR_TYPES");
        addAttribute(CKA_TRUSTED,               "CKA_TRUSTED");
        addAttribute(CKA_CERTIFICATE_CATEGORY,  "CKA_CERTIFICATE_CATEGORY");
        addAttribute(CKA_JAVA_MIDP_SECURITY_DOMAIN,
                                                "CKA_JAVA_MIDP_SECURITY_DOMAIN");
        addAttribute(CKA_URL,                   "CKA_URL");
        addAttribute(CKA_HASH_OF_SUBJECT_PUBLIC_KEY,
                                                "CKA_HASH_OF_SUBJECT_PUBLIC_KEY");
        addAttribute(CKA_HASH_OF_ISSUER_PUBLIC_KEY,
                                                "CKA_HASH_OF_ISSUER_PUBLIC_KEY");
        addAttribute(CKA_NAME_HASH_ALGORITHM,   "CKA_NAME_HASH_ALGORITHM");
        addAttribute(CKA_CHECK_VALUE,           "CKA_CHECK_VALUE");
        addAttribute(CKA_KEY_TYPE,              "CKA_KEY_TYPE");
        addAttribute(CKA_SUBJECT,               "CKA_SUBJECT");
        addAttribute(CKA_ID,                    "CKA_ID");
        addAttribute(CKA_SENSITIVE,             "CKA_SENSITIVE");
        addAttribute(CKA_ENCRYPT,               "CKA_ENCRYPT");
        addAttribute(CKA_DECRYPT,               "CKA_DECRYPT");
        addAttribute(CKA_WRAP,                  "CKA_WRAP");
        addAttribute(CKA_UNWRAP,                "CKA_UNWRAP");
        addAttribute(CKA_SIGN,                  "CKA_SIGN");
        addAttribute(CKA_SIGN_RECOVER,          "CKA_SIGN_RECOVER");
        addAttribute(CKA_VERIFY,                "CKA_VERIFY");
        addAttribute(CKA_VERIFY_RECOVER,        "CKA_VERIFY_RECOVER");
        addAttribute(CKA_DERIVE,                "CKA_DERIVE");
        addAttribute(CKA_START_DATE,            "CKA_START_DATE");
        addAttribute(CKA_END_DATE,              "CKA_END_DATE");
        addAttribute(CKA_MODULUS,               "CKA_MODULUS");
        addAttribute(CKA_MODULUS_BITS,          "CKA_MODULUS_BITS");
        addAttribute(CKA_PUBLIC_EXPONENT,       "CKA_PUBLIC_EXPONENT");
        addAttribute(CKA_PRIVATE_EXPONENT,      "CKA_PRIVATE_EXPONENT");
        addAttribute(CKA_PRIME_1,               "CKA_PRIME_1");
        addAttribute(CKA_PRIME_2,               "CKA_PRIME_2");
        addAttribute(CKA_EXPONENT_1,            "CKA_EXPONENT_1");
        addAttribute(CKA_EXPONENT_2,            "CKA_EXPONENT_2");
        addAttribute(CKA_COEFFICIENT,           "CKA_COEFFICIENT");
        addAttribute(CKA_PUBLIC_KEY_INFO,       "CKA_PUBLIC_KEY_INFO");
        addAttribute(CKA_PRIME,                 "CKA_PRIME");
        addAttribute(CKA_SUBPRIME,              "CKA_SUBPRIME");
        addAttribute(CKA_BASE,                  "CKA_BASE");
        addAttribute(CKA_PRIME_BITS,            "CKA_PRIME_BITS");
        addAttribute(CKA_SUB_PRIME_BITS,        "CKA_SUB_PRIME_BITS");
        addAttribute(CKA_VALUE_BITS,            "CKA_VALUE_BITS");
        addAttribute(CKA_VALUE_LEN,             "CKA_VALUE_LEN");

        addAttribute(CKA_EXTRACTABLE,           "CKA_EXTRACTABLE");
        addAttribute(CKA_LOCAL,                 "CKA_LOCAL");
        addAttribute(CKA_NEVER_EXTRACTABLE,     "CKA_NEVER_EXTRACTABLE");
        addAttribute(CKA_ALWAYS_SENSITIVE,      "CKA_ALWAYS_SENSITIVE");

        addAttribute(CKA_KEY_GEN_MECHANISM,     "CKA_KEY_GEN_MECHANISM");
        addAttribute(CKA_MODIFIABLE,            "CKA_MODIFIABLE");
        addAttribute(CKA_COPYABLE,              "CKA_COPYABLE");
        addAttribute(CKA_DESTROYABLE,           "CKA_DESTROYABLE");

        addAttribute(CKA_EC_PARAMS,             "CKA_EC_PARAMS");
        addAttribute(CKA_EC_POINT,              "CKA_EC_POINT");

        addAttribute(CKA_SECONDARY_AUTH,        "CKA_SECONDARY_AUTH");
        addAttribute(CKA_AUTH_PIN_FLAGS,        "CKA_AUTH_PIN_FLAGS");
        addAttribute(CKA_ALWAYS_AUTHENTICATE,   "CKA_ALWAYS_AUTHENTICATE");
        addAttribute(CKA_WRAP_WITH_TRUSTED,     "CKA_WRAP_WITH_TRUSTED");
        addAttribute(CKA_WRAP_TEMPLATE,         "CKA_WRAP_TEMPLATE");
        addAttribute(CKA_UNWRAP_TEMPLATE,       "CKA_UNWRAP_TEMPLATE");
        addAttribute(CKA_DERIVE_TEMPLATE,       "CKA_DERIVE_TEMPLATE");
        addAttribute(CKA_OTP_FORMAT,            "CKA_OTP_FORMAT");
        addAttribute(CKA_OTP_LENGTH,            "CKA_OTP_LENGTH");
        addAttribute(CKA_OTP_TIME_INTERVAL,     "CKA_OTP_TIME_INTERVAL");
        addAttribute(CKA_OTP_USER_FRIENDLY_MODE,"CKA_OTP_USER_FRIENDLY_MODE");
        addAttribute(CKA_OTP_CHALLENGE_REQUIREMENT,
                                                "CKA_OTP_CHALLENGE_REQUIREMENT");
        addAttribute(CKA_OTP_TIME_REQUIREMENT,  "CKA_OTP_TIME_REQUIREMENT");
        addAttribute(CKA_OTP_COUNTER_REQUIREMENT,
                                                "CKA_OTP_COUNTER_REQUIREMENT");
        addAttribute(CKA_OTP_PIN_REQUIREMENT,   "CKA_OTP_PIN_REQUIREMENT");
        addAttribute(CKA_OTP_COUNTER,           "CKA_OTP_COUNTER");
        addAttribute(CKA_OTP_TIME,              "CKA_OTP_TIME");
        addAttribute(CKA_OTP_USER_IDENTIFIER,   "CKA_OTP_USER_IDENTIFIER");
        addAttribute(CKA_OTP_SERVICE_IDENTIFIER,"CKA_OTP_SERVICE_IDENTIFIER");
        addAttribute(CKA_OTP_SERVICE_LOGO,      "CKA_OTP_SERVICE_LOGO");
        addAttribute(CKA_OTP_SERVICE_LOGO_TYPE, "CKA_OTP_SERVICE_LOGO_TYPE");
        addAttribute(CKA_GOSTR3410_PARAMS,      "CKA_GOSTR3410_PARAMS");
        addAttribute(CKA_GOSTR3411_PARAMS,      "CKA_GOSTR3411_PARAMS");
        addAttribute(CKA_GOST28147_PARAMS,      "CKA_GOST28147_PARAMS");

        addAttribute(CKA_HW_FEATURE_TYPE,       "CKA_HW_FEATURE_TYPE");
        addAttribute(CKA_RESET_ON_INIT,         "CKA_RESET_ON_INIT");
        addAttribute(CKA_HAS_RESET,             "CKA_HAS_RESET");

        addAttribute(CKA_PIXEL_X,               "CKA_PIXEL_X");
        addAttribute(CKA_PIXEL_Y,               "CKA_PIXEL_Y");
        addAttribute(CKA_RESOLUTION,            "CKA_RESOLUTION");
        addAttribute(CKA_CHAR_ROWS,             "CKA_CHAR_ROWS");
        addAttribute(CKA_CHAR_COLUMNS,          "CKA_CHAR_COLUMNS");
        addAttribute(CKA_COLOR,                 "CKA_COLOR");
        addAttribute(CKA_BITS_PER_PIXEL,        "CKA_BITS_PER_PIXEL");
        addAttribute(CKA_CHAR_SETS,             "CKA_CHAR_SETS");
        addAttribute(CKA_ENCODING_METHODS,      "CKA_ENCODING_METHODS");
        addAttribute(CKA_MIME_TYPES,            "CKA_MIME_TYPES");
        addAttribute(CKA_MECHANISM_TYPE,        "CKA_MECHANISM_TYPE");
        addAttribute(CKA_REQUIRED_CMS_ATTRIBUTES,
                                                "CKA_REQUIRED_CMS_ATTRIBUTES");
        addAttribute(CKA_DEFAULT_CMS_ATTRIBUTES,"CKA_DEFAULT_CMS_ATTRIBUTES");
        addAttribute(CKA_SUPPORTED_CMS_ATTRIBUTES,
                                                "CKA_SUPPORTED_CMS_ATTRIBUTES");
        addAttribute(CKA_ALLOWED_MECHANISMS,    "CKA_ALLOWED_MECHANISMS");
        addAttribute(CKA_PROFILE_ID,            "CKA_PROFILE_ID");
        addAttribute(CKA_X2RATCHET_BAG,         "CKA_X2RATCHET_BAG");
        addAttribute(CKA_X2RATCHET_BAGSIZE,     "CKA_X2RATCHET_BAGSIZE");
        addAttribute(CKA_X2RATCHET_BOBS1STMSG,  "CKA_X2RATCHET_BOBS1STMSG");
        addAttribute(CKA_X2RATCHET_CKR,         "CKA_X2RATCHET_CKR");
        addAttribute(CKA_X2RATCHET_CKS,         "CKA_X2RATCHET_CKS");
        addAttribute(CKA_X2RATCHET_DHP,         "CKA_X2RATCHET_DHP");
        addAttribute(CKA_X2RATCHET_DHR,         "CKA_X2RATCHET_DHR");
        addAttribute(CKA_X2RATCHET_DHS,         "CKA_X2RATCHET_DHS");
        addAttribute(CKA_X2RATCHET_HKR,         "CKA_X2RATCHET_HKR");
        addAttribute(CKA_X2RATCHET_HKS,         "CKA_X2RATCHET_HKS");
        addAttribute(CKA_X2RATCHET_ISALICE,     "CKA_X2RATCHET_ISALICE");
        addAttribute(CKA_X2RATCHET_NHKR,        "CKA_X2RATCHET_NHKR");
        addAttribute(CKA_X2RATCHET_NHKS,        "CKA_X2RATCHET_NHKS");
        addAttribute(CKA_X2RATCHET_NR,          "CKA_X2RATCHET_NR");
        addAttribute(CKA_X2RATCHET_NS,          "CKA_X2RATCHET_NS");
        addAttribute(CKA_X2RATCHET_PNS,         "CKA_X2RATCHET_PNS");
        addAttribute(CKA_X2RATCHET_RK,          "CKA_X2RATCHET_RK");

        addAttribute(CKA_VENDOR_DEFINED,        "CKA_VENDOR_DEFINED");
        addAttribute(CKA_NETSCAPE_DB,           "CKA_NETSCAPE_DB");

        addAttribute(CKA_NETSCAPE_TRUST_SERVER_AUTH,      "CKA_NETSCAPE_TRUST_SERVER_AUTH");
        addAttribute(CKA_NETSCAPE_TRUST_CLIENT_AUTH,      "CKA_NETSCAPE_TRUST_CLIENT_AUTH");
        addAttribute(CKA_NETSCAPE_TRUST_CODE_SIGNING,     "CKA_NETSCAPE_TRUST_CODE_SIGNING");
        addAttribute(CKA_NETSCAPE_TRUST_EMAIL_PROTECTION, "CKA_NETSCAPE_TRUST_EMAIL_PROTECTION");
        addAttribute(CKA_NETSCAPE_CERT_SHA1_HASH,         "CKA_NETSCAPE_CERT_SHA1_HASH");
        addAttribute(CKA_NETSCAPE_CERT_MD5_HASH,          "CKA_NETSCAPE_CERT_MD5_HASH");

        addObjectClass(CKO_DATA,                "CKO_DATA");
        addObjectClass(CKO_CERTIFICATE,         "CKO_CERTIFICATE");
        addObjectClass(CKO_PUBLIC_KEY,          "CKO_PUBLIC_KEY");
        addObjectClass(CKO_PRIVATE_KEY,         "CKO_PRIVATE_KEY");
        addObjectClass(CKO_SECRET_KEY,          "CKO_SECRET_KEY");
        addObjectClass(CKO_HW_FEATURE,          "CKO_HW_FEATURE");
        addObjectClass(CKO_DOMAIN_PARAMETERS,   "CKO_DOMAIN_PARAMETERS");
        addObjectClass(CKO_MECHANISM,           "CKO_MECHANISM");
        addObjectClass(CKO_OTP_KEY,             "CKO_OTP_KEY");
        addObjectClass(CKO_PROFILE,             "CKO_PROFILE");
        addObjectClass(CKO_VENDOR_DEFINED,      "CKO_VENDOR_DEFINED");

        addObjectClass(PCKO_ANY,                "*");

        addMGF(CKG_MGF1_SHA1,                   "CKG_MGF1_SHA1");
        addMGF(CKG_MGF1_SHA256,                 "CKG_MGF1_SHA256");
        addMGF(CKG_MGF1_SHA384,                 "CKG_MGF1_SHA384");
        addMGF(CKG_MGF1_SHA512,                 "CKG_MGF1_SHA512");
        addMGF(CKG_MGF1_SHA224,                 "CKG_MGF1_SHA224");
        addMGF(CKG_MGF1_SHA3_224,               "CKG_MGF1_SHA3_224");
        addMGF(CKG_MGF1_SHA3_256,               "CKG_MGF1_SHA3_256");
        addMGF(CKG_MGF1_SHA3_384,               "CKG_MGF1_SHA3_384");
        addMGF(CKG_MGF1_SHA3_512,               "CKG_MGF1_SHA3_512");
    }

}
