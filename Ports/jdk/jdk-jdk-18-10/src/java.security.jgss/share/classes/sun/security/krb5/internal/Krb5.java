/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal;

import sun.security.action.GetBooleanAction;

import java.util.Hashtable;

// Constants and other defined values from RFC 4120

public class Krb5 {

    //Recommended KDC values
    public static final int DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * 60; //5 minutes
    public static final int DEFAULT_MINIMUM_LIFETIME = 5 * 60; //5 minutes
    public static final int DEFAULT_MAXIMUM_RENEWABLE_LIFETIME = 7 * 24 * 60 * 60; //1 week
    public static final int DEFAULT_MAXIMUM_TICKET_LIFETIME = 24 * 60 * 60; //1 day
    public static final boolean DEFAULT_FORWARDABLE_ALLOWED = true;
    public static final boolean DEFAULT_PROXIABLE_ALLOWED = true;
    public static final boolean DEFAULT_POSTDATE_ALLOWED = true;
    public static final boolean DEFAULT_RENEWABLE_ALLOWED = true;
    public static final boolean AP_EMPTY_ADDRESSES_ALLOWED = true;

    //AP_REQ Options

    public static final int AP_OPTS_RESERVED        = 0;
    public static final int AP_OPTS_USE_SESSION_KEY = 1;
    public static final int AP_OPTS_MUTUAL_REQUIRED = 2;
    public static final int AP_OPTS_MAX             = 31;

    //Ticket Flags

    public static final int TKT_OPTS_RESERVED     = 0;
    public static final int TKT_OPTS_FORWARDABLE  = 1;
    public static final int TKT_OPTS_FORWARDED    = 2;
    public static final int TKT_OPTS_PROXIABLE    = 3;
    public static final int TKT_OPTS_PROXY        = 4;
    public static final int TKT_OPTS_MAY_POSTDATE = 5;
    public static final int TKT_OPTS_POSTDATED    = 6;
    public static final int TKT_OPTS_INVALID      = 7;
    public static final int TKT_OPTS_RENEWABLE    = 8;
    public static final int TKT_OPTS_INITIAL      = 9;
    public static final int TKT_OPTS_PRE_AUTHENT  = 10;
    public static final int TKT_OPTS_HW_AUTHENT   = 11;
    public static final int TKT_OPTS_DELEGATE     = 13;
    public static final int TKT_OPTS_ENC_PA_REP   = 15;
    public static final int TKT_OPTS_MAX          = 31;

    // KDC Options
    // (option values defined in KDCOptions.java)
    public static final int KDC_OPTS_MAX          = 31;

    // KerberosFlags
    public static final int KRB_FLAGS_MAX         = 31;

    //Last Request types

    public static final int LRTYPE_NONE                 = 0;
    public static final int LRTYPE_TIME_OF_INITIAL_TGT  = 1;
    public static final int LRTYPE_TIME_OF_INITIAL_REQ  = 2;
    public static final int LRTYPE_TIME_OF_NEWEST_TGT   = 3;
    public static final int LRTYPE_TIME_OF_LAST_RENEWAL = 4;
    public static final int LRTYPE_TIME_OF_LAST_REQ     = 5;

    //Host address lengths

    public static final int ADDR_LEN_INET      = 4;
    public static final int ADDR_LEN_CHAOS     = 2;
    public static final int ADDR_LEN_OSI       = 0; //means variable
    public static final int ADDR_LEN_XNS       = 6;
    public static final int ADDR_LEN_APPLETALK = 3;
    public static final int ADDR_LEN_DECNET    = 2;

    //Host address types

    public static final int ADDRTYPE_UNIX      = 1;  // Local
    public static final int ADDRTYPE_INET      = 2;  // Internet
    public static final int ADDRTYPE_IMPLINK   = 3;  // Arpanet
    public static final int ADDRTYPE_PUP       = 4;  // PUP
    public static final int ADDRTYPE_CHAOS     = 5;  // CHAOS
    public static final int ADDRTYPE_XNS       = 6;  // XEROX Network Services
    public static final int ADDRTYPE_IPX       = 6;  // IPX
    public static final int ADDRTYPE_ISO       = 7;  // ISO
    public static final int ADDRTYPE_ECMA      = 8;  // European Computer Manufacturers
    public static final int ADDRTYPE_DATAKIT   = 9;  // Datakit
    public static final int ADDRTYPE_CCITT     = 10; // CCITT
    public static final int ADDRTYPE_SNA       = 11; // SNA
    public static final int ADDRTYPE_DECNET    = 12; // DECnet
    public static final int ADDRTYPE_DLI       = 13; // Direct Data Link Interface
    public static final int ADDRTYPE_LAT       = 14; // LAT
    public static final int ADDRTYPE_HYLINK    = 15; // NSC Hyperchannel
    public static final int ADDRTYPE_APPLETALK = 16; // AppleTalk
    public static final int ADDRTYPE_NETBIOS   = 17; // NetBios
    public static final int ADDRTYPE_VOICEVIEW = 18; // VoiceView
    public static final int ADDRTYPE_FIREFOX   = 19; // Firefox
    public static final int ADDRTYPE_BAN       = 21; // Banyan
    public static final int ADDRTYPE_ATM       = 22; // ATM
    public static final int ADDRTYPE_INET6     = 24; // Internet Protocol V6

    //IP Transport UDP Port for KDC Messages

    public static final int KDC_INET_DEFAULT_PORT = 88;

    // number of retries before giving up

    public static final int KDC_RETRY_LIMIT = 3;
    public static final int KDC_DEFAULT_UDP_PREF_LIMIT = 1465;
    public static final int KDC_HARD_UDP_LIMIT = 32700;

    //OSI authentication mechanism OID

    //public static final int[] OSI_AUTH_MECH_TYPE = { /*iso*/ 1, /*org*/ 3,
    //                                               /*dod*/ 5, /*internet*/ 1, /*security*/ 5, /*kerberosv5*/ 2 };

    //Protocol constants and associated values

    //Key Types
    public static final int KEYTYPE_NULL = 0;
    public static final int KEYTYPE_DES  = 1;

    public static final int KEYTYPE_DES3 = 2;
    public static final int KEYTYPE_AES  = 3;
    public static final int KEYTYPE_ARCFOUR_HMAC = 4;


    //----------------------------------------+-----------------
    //                      padata type       |padata-type value
    //----------------------------------------+-----------------
    public static final int PA_TGS_REQ       = 1;
    public static final int PA_ENC_TIMESTAMP = 2;
    public static final int PA_PW_SALT       = 3;

    // new preauth types
    public static final int PA_ETYPE_INFO    = 11;
    public static final int PA_ETYPE_INFO2   = 19;

    // S4U2user info
    public static final int PA_FOR_USER      = 129;
    public static final int PA_PAC_OPTIONS   = 167;

    // FAST (RFC 6806)
    public static final int PA_REQ_ENC_PA_REP = 149;

    //-------------------------------+-------------
    //authorization data type        |ad-type value
    //-------------------------------+-------------
    //reserved values                 0-63
    public static final int OSF_DCE = 64;
    public static final int SESAME  = 65;

    //----------------------------------------------+-----------------
    //alternate authentication type                 |method-type value
    //----------------------------------------------+-----------------
    //                      reserved values          0-63
    public static final int ATT_CHALLENGE_RESPONSE = 64;

    //--------------------------------------------+-------------
    //transited encoding type                     |tr-type value
    //--------------------------------------------+-------------
    public static final int DOMAIN_X500_COMPRESS = 1;
    //                      reserved values        all others

    //----------------------------+-------+-----------------------------------------
    //                      Label |Value  |Meaning
    //----------------------------+-------+-----------------------------------------
    public static final int PVNO = 5;   // current Kerberos protocol version number
    public static final int AUTHNETICATOR_VNO = 5;   // current authenticator version number
    public static final int TICKET_VNO = 5;   // current ticket version number

    //message types

    // there are several message sub-components not included here
    public static final int KRB_AS_REQ =  10;     //Request for initial authentication
    public static final int KRB_AS_REP =  11;     //Response to KRB_AS_REQ request
    public static final int KRB_TGS_REQ = 12;     //Request for authentication based on TGT
    public static final int KRB_TGS_REP = 13;     //Response to KRB_TGS_REQ request
    public static final int KRB_AP_REQ =  14;     //application request to server
    public static final int KRB_AP_REP =  15;     //Response to KRB_AP_REQ_MUTUAL
    public static final int KRB_SAFE =    20;     //Safe (checksummed) application message
    public static final int KRB_PRIV =    21;     //Private (encrypted) application message
    public static final int KRB_CRED =    22;     //Private (encrypted) message to forward credentials
    public static final int KRB_ERROR =   30;     //Error response

    //message component types

    public static final int KRB_TKT               = 1;  //Ticket
    public static final int KRB_AUTHENTICATOR     = 2;  //Authenticator
    public static final int KRB_ENC_TKT_PART      = 3;  //Encrypted ticket part
    public static final int KRB_ENC_AS_REP_PART   = 25; //Encrypted initial authentication part
    public static final int KRB_ENC_TGS_REP_PART  = 26; //Encrypted TGS request part
    public static final int KRB_ENC_AP_REP_PART   = 27; //Encrypted application request part
    public static final int KRB_ENC_KRB_PRIV_PART = 28; //Encrypted application message part
    public static final int KRB_ENC_KRB_CRED_PART = 29; //Encrypted credentials forward part


    //error codes

    public static final int KDC_ERR_NONE                 =  0;   //No error
    public static final int KDC_ERR_NAME_EXP             =  1;   //Client's entry in database expired
    public static final int KDC_ERR_SERVICE_EXP          =  2;   //Server's entry in database has expired
    public static final int KDC_ERR_BAD_PVNO             =  3;   //Requested protocol version number not supported
    public static final int KDC_ERR_C_OLD_MAST_KVNO      =  4;   //Client's key encrypted in old master key
    public static final int KDC_ERR_S_OLD_MAST_KVNO      =  5;   //Server's key encrypted in old master key
    public static final int KDC_ERR_C_PRINCIPAL_UNKNOWN  =  6;   //Client not found in Kerberos database
    public static final int KDC_ERR_S_PRINCIPAL_UNKNOWN  =  7;   //Server not found in Kerberos database
    public static final int KDC_ERR_PRINCIPAL_NOT_UNIQUE =  8;   //Multiple principal entries in database
    public static final int KDC_ERR_NULL_KEY             =  9;   //The client or server has a null key
    public static final int KDC_ERR_CANNOT_POSTDATE      = 10;   //Ticket not eligible for postdating
    public static final int KDC_ERR_NEVER_VALID          = 11;   //Requested start time is later than end time
    public static final int KDC_ERR_POLICY               = 12;   //KDC policy rejects request
    public static final int KDC_ERR_BADOPTION            = 13;   //KDC cannot accommodate requested option
    public static final int KDC_ERR_ETYPE_NOSUPP         = 14;   //KDC has no support for encryption type
    public static final int KDC_ERR_SUMTYPE_NOSUPP       = 15;   //KDC has no support for checksum type
    public static final int KDC_ERR_PADATA_TYPE_NOSUPP   = 16;   //KDC has no support for padata type
    public static final int KDC_ERR_TRTYPE_NOSUPP        = 17;   //KDC has no support for transited type
    public static final int KDC_ERR_CLIENT_REVOKED       = 18;   //Clients credentials have been revoked
    public static final int KDC_ERR_SERVICE_REVOKED      = 19;   //Credentials for server have been revoked
    public static final int KDC_ERR_TGT_REVOKED          = 20;   //TGT has been revoked
    public static final int KDC_ERR_CLIENT_NOTYET        = 21;   //Client not yet valid - try again later
    public static final int KDC_ERR_SERVICE_NOTYET       = 22;   //Server not yet valid - try again later
    public static final int KDC_ERR_KEY_EXPIRED          = 23;   //Password has expired - change password to reset
    public static final int KDC_ERR_PREAUTH_FAILED       = 24;   //Pre-authentication information was invalid
    public static final int KDC_ERR_PREAUTH_REQUIRED     = 25;   //Additional pre-authentication required
    public static final int KRB_AP_ERR_BAD_INTEGRITY     = 31;   //Integrity check on decrypted field failed
    public static final int KRB_AP_ERR_TKT_EXPIRED       = 32;   //Ticket expired
    public static final int KRB_AP_ERR_TKT_NYV           = 33;   //Ticket not yet valid
    public static final int KRB_AP_ERR_REPEAT            = 34;   //Request is a replay
    public static final int KRB_AP_ERR_NOT_US            = 35;   //The ticket isn't for us
    public static final int KRB_AP_ERR_BADMATCH          = 36;   //Ticket and authenticator don't match
    public static final int KRB_AP_ERR_SKEW              = 37;   //Clock skew too great
    public static final int KRB_AP_ERR_BADADDR           = 38;   //Incorrect net address
    public static final int KRB_AP_ERR_BADVERSION        = 39;   //Protocol version mismatch
    public static final int KRB_AP_ERR_MSG_TYPE          = 40;   //Invalid msg type
    public static final int KRB_AP_ERR_MODIFIED          = 41;   //Message stream modified
    public static final int KRB_AP_ERR_BADORDER          = 42;   //Message out of order
    public static final int KRB_AP_ERR_BADKEYVER         = 44;   //Specified version of key is not available
    public static final int KRB_AP_ERR_NOKEY             = 45;   //Service key not available
    public static final int KRB_AP_ERR_MUT_FAIL          = 46;   //Mutual authentication failed
    public static final int KRB_AP_ERR_BADDIRECTION      = 47;   //Incorrect message direction
    public static final int KRB_AP_ERR_METHOD            = 48;   //Alternative authentication method required
    public static final int KRB_AP_ERR_BADSEQ            = 49;   //Incorrect sequence number in message
    public static final int KRB_AP_ERR_INAPP_CKSUM       = 50;   //Inappropriate type of checksum in message
    public static final int KRB_ERR_RESPONSE_TOO_BIG     = 52;   //Response too big for UDP, retry with TCP
    public static final int KRB_ERR_GENERIC              = 60;   //Generic error (description in e-text)
    public static final int KRB_ERR_FIELD_TOOLONG        = 61;   //Field is too long for this implementation
    public static final int KRB_ERR_WRONG_REALM          = 68;   //Wrong realm
    public static final int KRB_CRYPTO_NOT_SUPPORT      = 100;    //Client does not support this crypto type
    public static final int KRB_AP_ERR_NOREALM          = 62;
    public static final int KRB_AP_ERR_GEN_CRED         = 63;
    //  public static final int KRB_AP_ERR_CKSUM_NOKEY          =101;    //Lack of the key to generate the checksum
    // error codes specific to this implementation
    public static final int KRB_AP_ERR_REQ_OPTIONS = 101; //Invalid TGS_REQ
    public static final int API_INVALID_ARG               = 400;  //Invalid argument

    public static final int BITSTRING_SIZE_INVALID        = 500;  //BitString size does not match input byte array
    public static final int BITSTRING_INDEX_OUT_OF_BOUNDS = 501;  //BitString bit index does not fall within size
    public static final int BITSTRING_BAD_LENGTH          = 502;  //BitString length is wrong for the expected type

    public static final int REALM_ILLCHAR                 = 600;  //Illegal character in realm name; one of: '/', ':', '\0'
    public static final int REALM_NULL                    = 601;  //Null realm name

    public static final int ASN1_BAD_TIMEFORMAT           = 900;  //Input not in GeneralizedTime format
    public static final int ASN1_MISSING_FIELD            = 901;  //Structure is missing a required field
    public static final int ASN1_MISPLACED_FIELD          = 902;  //Unexpected field number
    public static final int ASN1_TYPE_MISMATCH            = 903;  //Type numbers are inconsistent
    public static final int ASN1_OVERFLOW                 = 904;  //Value too large
    public static final int ASN1_OVERRUN                  = 905;  //Encoding ended unexpectedly
    public static final int ASN1_BAD_ID                   = 906;  //Identifier doesn't match expected value
    public static final int ASN1_BAD_LENGTH               = 907;  //Length doesn't match expected value
    public static final int ASN1_BAD_FORMAT               = 908;  //Badly-formatted encoding
    public static final int ASN1_PARSE_ERROR              = 909;  //Parse error
    public static final int ASN1_BAD_CLASS                = 910;  //Bad class number
    public static final int ASN1_BAD_TYPE                 = 911;  //Bad type number
    public static final int ASN1_BAD_TAG                  = 912;  //Bad tag number
    public static final int ASN1_UNSUPPORTED_TYPE         = 913;  //Unsupported ASN.1 type encountered
    public static final int ASN1_CANNOT_ENCODE            = 914;  //Encoding failed due to invalid parameter(s)

    private static Hashtable<Integer,String> errMsgList;

    public static String getErrorMessage(int i) {
        return errMsgList.get(i);
    }

    // Warning: used by NativeCreds.c
    public static final boolean DEBUG = GetBooleanAction
            .privilegedGetProperty("sun.security.krb5.debug");

    public static final sun.security.util.HexDumpEncoder hexDumper =
        new sun.security.util.HexDumpEncoder();

    static {
        errMsgList = new Hashtable<Integer,String> ();
        errMsgList.put(KDC_ERR_NONE, "No error");
        errMsgList.put(KDC_ERR_NAME_EXP, "Client's entry in database expired");
        errMsgList.put(KDC_ERR_SERVICE_EXP, "Server's entry in database has expired");
        errMsgList.put(KDC_ERR_BAD_PVNO, "Requested protocol version number not supported");
        errMsgList.put(KDC_ERR_C_OLD_MAST_KVNO, "Client's key encrypted in old master key");
        errMsgList.put(KDC_ERR_S_OLD_MAST_KVNO, "Server's key encrypted in old master key");
        errMsgList.put(KDC_ERR_C_PRINCIPAL_UNKNOWN, "Client not found in Kerberos database");
        errMsgList.put(KDC_ERR_S_PRINCIPAL_UNKNOWN, "Server not found in Kerberos database");
        errMsgList.put(KDC_ERR_PRINCIPAL_NOT_UNIQUE, "Multiple principal entries in database");
        errMsgList.put(KDC_ERR_NULL_KEY, "The client or server has a null key");
        errMsgList.put(KDC_ERR_CANNOT_POSTDATE, "Ticket not eligible for postdating");
        errMsgList.put(KDC_ERR_NEVER_VALID, "Requested start time is later than end time");
        errMsgList.put(KDC_ERR_POLICY, "KDC policy rejects request");
        errMsgList.put(KDC_ERR_BADOPTION, "KDC cannot accommodate requested option");
        errMsgList.put(KDC_ERR_ETYPE_NOSUPP, "KDC has no support for encryption type");
        errMsgList.put(KDC_ERR_SUMTYPE_NOSUPP, "KDC has no support for checksum type");
        errMsgList.put(KDC_ERR_PADATA_TYPE_NOSUPP, "KDC has no support for padata type");
        errMsgList.put(KDC_ERR_TRTYPE_NOSUPP, "KDC has no support for transited type");
        errMsgList.put(KDC_ERR_CLIENT_REVOKED, "Clients credentials have been revoked");
        errMsgList.put(KDC_ERR_SERVICE_REVOKED, "Credentials for server have been revoked");
        errMsgList.put(KDC_ERR_TGT_REVOKED, "TGT has been revoked");
        errMsgList.put(KDC_ERR_CLIENT_NOTYET, "Client not yet valid - try again later");
        errMsgList.put(KDC_ERR_SERVICE_NOTYET, "Server not yet valid - try again later");
        errMsgList.put(KDC_ERR_KEY_EXPIRED, "Password has expired - change password to reset");
        errMsgList.put(KDC_ERR_PREAUTH_FAILED, "Pre-authentication information was invalid");
        errMsgList.put(KDC_ERR_PREAUTH_REQUIRED, "Additional pre-authentication required");
        errMsgList.put(KRB_AP_ERR_BAD_INTEGRITY, "Integrity check on decrypted field failed");
        errMsgList.put(KRB_AP_ERR_TKT_EXPIRED, "Ticket expired");
        errMsgList.put(KRB_AP_ERR_TKT_NYV, "Ticket not yet valid");
        errMsgList.put(KRB_AP_ERR_REPEAT, "Request is a replay");
        errMsgList.put(KRB_AP_ERR_NOT_US, "The ticket isn't for us");
        errMsgList.put(KRB_AP_ERR_BADMATCH, "Ticket and authenticator don't match");
        errMsgList.put(KRB_AP_ERR_SKEW, "Clock skew too great");
        errMsgList.put(KRB_AP_ERR_BADADDR, "Incorrect net address");
        errMsgList.put(KRB_AP_ERR_BADVERSION, "Protocol version mismatch");
        errMsgList.put(KRB_AP_ERR_MSG_TYPE, "Invalid msg type");
        errMsgList.put(KRB_AP_ERR_MODIFIED, "Message stream modified");
        errMsgList.put(KRB_AP_ERR_BADORDER, "Message out of order");
        errMsgList.put(KRB_AP_ERR_BADKEYVER, "Specified version of key is not available");
        errMsgList.put(KRB_AP_ERR_NOKEY, "Service key not available");
        errMsgList.put(KRB_AP_ERR_MUT_FAIL, "Mutual authentication failed");
        errMsgList.put(KRB_AP_ERR_BADDIRECTION, "Incorrect message direction");
        errMsgList.put(KRB_AP_ERR_METHOD, "Alternative authentication method required");
        errMsgList.put(KRB_AP_ERR_BADSEQ, "Incorrect sequence number in message");
        errMsgList.put(KRB_AP_ERR_INAPP_CKSUM, "Inappropriate type of checksum in message");
        errMsgList.put(KRB_ERR_RESPONSE_TOO_BIG, "Response too big for UDP, retry with TCP");
        errMsgList.put(KRB_ERR_GENERIC, "Generic error (description in e-text)");
        errMsgList.put(KRB_ERR_FIELD_TOOLONG, "Field is too long for this implementation");
        errMsgList.put(KRB_AP_ERR_NOREALM, "Realm name not available"); //used in setDefaultCreds() in sun.security.krb5.Credentials

        // error messages specific to this implementation

        errMsgList.put(API_INVALID_ARG, "Invalid argument");

        errMsgList.put(BITSTRING_SIZE_INVALID, "BitString size does not match input byte array");
        errMsgList.put(BITSTRING_INDEX_OUT_OF_BOUNDS, "BitString bit index does not fall within size");
        errMsgList.put(BITSTRING_BAD_LENGTH, "BitString length is wrong for the expected type");

        errMsgList.put(REALM_ILLCHAR, "Illegal character in realm name; one of: '/', ':', '\0'");
        errMsgList.put(REALM_NULL, "Null realm name");

        errMsgList.put(ASN1_BAD_TIMEFORMAT, "Input not in GeneralizedTime format");
        errMsgList.put(ASN1_MISSING_FIELD, "Structure is missing a required field");
        errMsgList.put(ASN1_MISPLACED_FIELD, "Unexpected field number");
        errMsgList.put(ASN1_TYPE_MISMATCH, "Type numbers are inconsistent");
        errMsgList.put(ASN1_OVERFLOW, "Value too large");
        errMsgList.put(ASN1_OVERRUN, "Encoding ended unexpectedly");
        errMsgList.put(ASN1_BAD_ID, "Identifier doesn't match expected value");
        errMsgList.put(ASN1_BAD_LENGTH, "Length doesn't match expected value");
        errMsgList.put(ASN1_BAD_FORMAT, "Badly-formatted encoding");
        errMsgList.put(ASN1_PARSE_ERROR, "Parse error");
        errMsgList.put(ASN1_BAD_CLASS, "Bad class number");
        errMsgList.put(ASN1_BAD_TYPE, "Bad type number");
        errMsgList.put(ASN1_BAD_TAG, "Bad tag number");
        errMsgList.put(ASN1_UNSUPPORTED_TYPE, "Unsupported ASN.1 type encountered");
        errMsgList.put(ASN1_CANNOT_ENCODE, "Encoding failed due to invalid parameter(s)");
        errMsgList.put(KRB_CRYPTO_NOT_SUPPORT, "Client has no support for crypto type");
        errMsgList.put(KRB_AP_ERR_REQ_OPTIONS, "Invalid option setting in ticket request.");
        errMsgList.put(KRB_AP_ERR_GEN_CRED, "Fail to create credential.");
    }

}
