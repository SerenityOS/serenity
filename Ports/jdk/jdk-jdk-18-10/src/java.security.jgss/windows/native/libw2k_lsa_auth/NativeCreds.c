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
 * ===========================================================================
 * (C) Copyright IBM Corp. 2000 All Rights Reserved.
 * ===========================================================================
 */

#define UNICODE
#define _UNICODE

#include <windows.h>
#include <stdio.h>
#include <string.h>
#define SECURITY_WIN32
#include <security.h>
#include <ntsecapi.h>
#include <dsgetdc.h>
#include <lmcons.h>
#include <lmapibuf.h>
#include <jni.h>
#include "jni_util.h"
#include <winsock.h>
#include "sun_security_krb5_Credentials.h"

#undef LSA_SUCCESS
#define LSA_SUCCESS(Status) ((Status) >= 0)
#define EXIT_FAILURE -1 // mdu

/*
 * Library-wide static references
 */

jclass ticketClass = NULL;
jclass principalNameClass = NULL;
jclass encryptionKeyClass = NULL;
jclass ticketFlagsClass = NULL;
jclass kerberosTimeClass = NULL;
jclass javaLangStringClass = NULL;

jmethodID ticketConstructor = 0;
jmethodID principalNameConstructor = 0;
jmethodID encryptionKeyConstructor = 0;
jmethodID ticketFlagsConstructor = 0;
jmethodID kerberosTimeConstructor = 0;
jmethodID krbcredsConstructor = 0;

/*
 * Function prototypes for internal routines
 *
 */
BOOL native_debug = 0;

BOOL PackageConnectLookup(PHANDLE,PULONG);

NTSTATUS ConstructTicketRequest(JNIEnv *env,
                                UNICODE_STRING DomainName,
                                PKERB_RETRIEVE_TKT_REQUEST *outRequest,
                                ULONG *outSize);

DWORD ConcatenateUnicodeStrings(UNICODE_STRING *pTarget,
                                UNICODE_STRING Source1,
                                UNICODE_STRING Source2);

VOID ShowNTError(LPSTR,NTSTATUS);

VOID
InitUnicodeString(
    PUNICODE_STRING DestinationString,
    PCWSTR SourceString OPTIONAL
);

jobject BuildTicket(JNIEnv *env, PUCHAR encodedTicket, ULONG encodedTicketSize);

//mdu
jobject BuildPrincipal(JNIEnv *env, PKERB_EXTERNAL_NAME principalName,
                                UNICODE_STRING domainName);

jobject BuildEncryptionKey(JNIEnv *env, PKERB_CRYPTO_KEY cryptoKey);
jobject BuildTicketFlags(JNIEnv *env, PULONG flags);
jobject BuildKerberosTime(JNIEnv *env, PLARGE_INTEGER kerbtime);

void ThrowOOME(JNIEnv *env, const char *szMessage);

/*
 * Class:     sun_security_krb5_KrbCreds
 * Method:    JNI_OnLoad
 */

JNIEXPORT jint JNICALL DEF_JNI_OnLoad(
        JavaVM  *jvm,
        void    *reserved) {

    jclass cls;
    JNIEnv *env;
    jfieldID fldDEBUG;

    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
        return JNI_EVERSION; /* JNI version not supported */
    }

    cls = (*env)->FindClass(env,"sun/security/krb5/internal/Krb5");
    if (cls == NULL) {
        printf("LSA: Couldn't find Krb5\n");
        return JNI_ERR;
    }
    fldDEBUG = (*env)->GetStaticFieldID(env, cls, "DEBUG", "Z");
    if (fldDEBUG == NULL) {
        printf("LSA: Krb5 has no DEBUG field\n");
        return JNI_ERR;
    }
    native_debug = (*env)->GetStaticBooleanField(env, cls, fldDEBUG);

    cls = (*env)->FindClass(env,"sun/security/krb5/internal/Ticket");

    if (cls == NULL) {
        printf("LSA: Couldn't find Ticket\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found Ticket\n");
    }

    ticketClass = (*env)->NewWeakGlobalRef(env,cls);
    if (ticketClass == NULL) {
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Made NewWeakGlobalRef\n");
    }

    cls = (*env)->FindClass(env, "sun/security/krb5/PrincipalName");

    if (cls == NULL) {
        printf("LSA: Couldn't find PrincipalName\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found PrincipalName\n");
    }

    principalNameClass = (*env)->NewWeakGlobalRef(env,cls);
    if (principalNameClass == NULL) {
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Made NewWeakGlobalRef\n");
    }

    cls = (*env)->FindClass(env,"sun/security/krb5/EncryptionKey");

    if (cls == NULL) {
        printf("LSA: Couldn't find EncryptionKey\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found EncryptionKey\n");
    }

    encryptionKeyClass = (*env)->NewWeakGlobalRef(env,cls);
    if (encryptionKeyClass == NULL) {
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Made NewWeakGlobalRef\n");
    }

    cls = (*env)->FindClass(env,"sun/security/krb5/internal/TicketFlags");

    if (cls == NULL) {
        printf("LSA: Couldn't find TicketFlags\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found TicketFlags\n");
    }

    ticketFlagsClass = (*env)->NewWeakGlobalRef(env,cls);
    if (ticketFlagsClass == NULL) {
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Made NewWeakGlobalRef\n");
    }

    cls = (*env)->FindClass(env,"sun/security/krb5/internal/KerberosTime");

    if (cls == NULL) {
        printf("LSA: Couldn't find KerberosTime\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found KerberosTime\n");
    }

    kerberosTimeClass = (*env)->NewWeakGlobalRef(env,cls);
    if (kerberosTimeClass == NULL) {
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Made NewWeakGlobalRef\n");
    }

    cls = (*env)->FindClass(env,"java/lang/String");

    if (cls == NULL) {
        printf("LSA: Couldn't find String\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found String\n");
    }

    javaLangStringClass = (*env)->NewWeakGlobalRef(env,cls);
    if (javaLangStringClass == NULL) {
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Made NewWeakGlobalRef\n");
    }

    ticketConstructor = (*env)->GetMethodID(env, ticketClass,
                            "<init>", "([B)V");
    if (ticketConstructor == 0) {
        printf("LSA: Couldn't find Ticket constructor\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found Ticket constructor\n");
    }

    principalNameConstructor = (*env)->GetMethodID(env, principalNameClass,
                        "<init>", "([Ljava/lang/String;Ljava/lang/String;)V");
    if (principalNameConstructor == 0) {
        printf("LSA: Couldn't find PrincipalName constructor\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found PrincipalName constructor\n");
    }

    encryptionKeyConstructor = (*env)->GetMethodID(env, encryptionKeyClass,
                                            "<init>", "(I[B)V");
    if (encryptionKeyConstructor == 0) {
        printf("LSA: Couldn't find EncryptionKey constructor\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found EncryptionKey constructor\n");
    }

    ticketFlagsConstructor = (*env)->GetMethodID(env, ticketFlagsClass,
                                            "<init>", "(I[B)V");
    if (ticketFlagsConstructor == 0) {
        printf("LSA: Couldn't find TicketFlags constructor\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found TicketFlags constructor\n");
    }

    kerberosTimeConstructor = (*env)->GetMethodID(env, kerberosTimeClass,
                                    "<init>", "(Ljava/lang/String;)V");
    if (kerberosTimeConstructor == 0) {
        printf("LSA: Couldn't find KerberosTime constructor\n");
        return JNI_ERR;
    }
    if (native_debug) {
        printf("LSA: Found KerberosTime constructor\n");
    }

    if (native_debug) {
        printf("LSA: Finished OnLoad processing\n");
    }

    return JNI_VERSION_1_2;
}

/*
 * Class:     sun_security_jgss_KrbCreds
 * Method:    JNI_OnUnload
 */

JNIEXPORT void JNICALL DEF_JNI_OnUnload(
        JavaVM  *jvm,
        void    *reserved) {

    JNIEnv *env;

    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
        return; /* Nothing else we can do */
    }

    if (ticketClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,ticketClass);
    }
    if (principalNameClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,principalNameClass);
    }
    if (encryptionKeyClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,encryptionKeyClass);
    }
    if (ticketFlagsClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,ticketFlagsClass);
    }
    if (kerberosTimeClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,kerberosTimeClass);
    }
    if (javaLangStringClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,javaLangStringClass);
    }

    return;
}

/*
 * Class:     sun_security_krb5_Credentials
 * Method:    acquireDefaultNativeCreds
 * Signature: ([I])Lsun/security/krb5/Credentials;
 */
JNIEXPORT jobject JNICALL Java_sun_security_krb5_Credentials_acquireDefaultNativeCreds(
        JNIEnv *env,
        jclass krbcredsClass,
        jintArray jetypes) {

    KERB_QUERY_TKT_CACHE_REQUEST CacheRequest;
    PKERB_RETRIEVE_TKT_RESPONSE TktCacheResponse = NULL;
    PKERB_RETRIEVE_TKT_REQUEST pTicketRequest = NULL;
    PKERB_RETRIEVE_TKT_RESPONSE pTicketResponse = NULL;
    NTSTATUS Status, SubStatus;
    ULONG requestSize = 0;
    ULONG responseSize = 0;
    ULONG rspSize = 0;
    HANDLE LogonHandle = NULL;
    ULONG PackageId;
    jobject ticket, clientPrincipal, targetPrincipal, encryptionKey;
    jobject ticketFlags, startTime, endTime, krbCreds = NULL;
    jobject authTime, renewTillTime, hostAddresses = NULL;
    KERB_EXTERNAL_TICKET *msticket;
    int found = 0;
    FILETIME Now, EndTime;

    int i, netypes;
    jint *etypes = NULL;

    while (TRUE) {

        if (krbcredsConstructor == 0) {
            krbcredsConstructor = (*env)->GetMethodID(env, krbcredsClass, "<init>",
                    "(Lsun/security/krb5/internal/Ticket;"
                    "Lsun/security/krb5/PrincipalName;"
                    "Lsun/security/krb5/PrincipalName;"
                    "Lsun/security/krb5/PrincipalName;"
                    "Lsun/security/krb5/PrincipalName;"
                    "Lsun/security/krb5/EncryptionKey;"
                    "Lsun/security/krb5/internal/TicketFlags;"
                    "Lsun/security/krb5/internal/KerberosTime;"
                    "Lsun/security/krb5/internal/KerberosTime;"
                    "Lsun/security/krb5/internal/KerberosTime;"
                    "Lsun/security/krb5/internal/KerberosTime;"
                    "Lsun/security/krb5/internal/HostAddresses;)V");
            if (krbcredsConstructor == 0) {
                printf("LSA: Couldn't find sun.security.krb5.Credentials constructor\n");
                break;
            }
        }

        if (native_debug) {
            printf("LSA: Found KrbCreds constructor\n");
        }

        //
        // Get the logon handle and package ID from the
        // Kerberos package
        //
        if (!PackageConnectLookup(&LogonHandle, &PackageId))
            break;

        if (native_debug) {
            printf("LSA: Got handle to Kerberos package\n");
        }

        // Get the MS TGT from cache
        CacheRequest.MessageType = KerbRetrieveTicketMessage;
        CacheRequest.LogonId.LowPart = 0;
        CacheRequest.LogonId.HighPart = 0;

        Status = LsaCallAuthenticationPackage(
                        LogonHandle,
                        PackageId,
                        &CacheRequest,
                        sizeof(CacheRequest),
                        &TktCacheResponse,
                        &rspSize,
                        &SubStatus
                        );

        if (native_debug) {
            printf("LSA: Response size is %d\n", rspSize);
        }

        if (!LSA_SUCCESS(Status) || !LSA_SUCCESS(SubStatus)) {
            if (!LSA_SUCCESS(Status)) {
                ShowNTError("LsaCallAuthenticationPackage", Status);
            } else {
                ShowNTError("Protocol status", SubStatus);
            }
            break;
        }

        // got the native MS TGT
        msticket = &(TktCacheResponse->Ticket);

        netypes = (*env)->GetArrayLength(env, jetypes);
        etypes = (jint *) (*env)->GetIntArrayElements(env, jetypes, NULL);

        if (etypes == NULL) {
            break;
        }

        // check TGT validity
        if (native_debug) {
            printf("LSA: TICKET SessionKey KeyType is %d\n", msticket->SessionKey.KeyType);
        }

        if ((msticket->TicketFlags & KERB_TICKET_FLAGS_invalid) == 0) {
            GetSystemTimeAsFileTime(&Now);
            EndTime.dwLowDateTime = msticket->EndTime.LowPart;
            EndTime.dwHighDateTime = msticket->EndTime.HighPart;
            if (CompareFileTime(&Now, &EndTime) < 0) {
                for (i=0; i<netypes; i++) {
                    if (etypes[i] == msticket->SessionKey.KeyType) {
                        found = 1;
                        if (native_debug) {
                            printf("LSA: Valid etype found: %d\n", etypes[i]);
                        }
                        break;
                    }
                }
            }
        }

        if (!found) {
            if (native_debug) {
                printf("LSA: MS TGT in cache is invalid/not supported; request new ticket\n");
            }

            // use domain to request Ticket
            Status = ConstructTicketRequest(env, msticket->TargetDomainName,
                                &pTicketRequest, &requestSize);
            if (!LSA_SUCCESS(Status)) {
                ShowNTError("ConstructTicketRequest status", Status);
                break;
            }

            pTicketRequest->MessageType = KerbRetrieveEncodedTicketMessage;
            pTicketRequest->CacheOptions = KERB_RETRIEVE_TICKET_DONT_USE_CACHE;

            for (i=0; i<netypes; i++) {
                pTicketRequest->EncryptionType = etypes[i];
                Status = LsaCallAuthenticationPackage(
                            LogonHandle,
                            PackageId,
                            pTicketRequest,
                            requestSize,
                            &pTicketResponse,
                            &responseSize,
                            &SubStatus
                            );

                if (native_debug) {
                    printf("LSA: Response size is %d for %d\n", responseSize, etypes[i]);
                }

                if (!LSA_SUCCESS(Status) || !LSA_SUCCESS(SubStatus)) {
                    if (!LSA_SUCCESS(Status)) {
                        ShowNTError("LsaCallAuthenticationPackage", Status);
                    } else {
                        ShowNTError("Protocol status", SubStatus);
                    }
                    continue;
                }

                // got the native MS Kerberos TGT
                msticket = &(pTicketResponse->Ticket);

                if (msticket->SessionKey.KeyType != etypes[i]) {
                    if (native_debug) {
                        printf("LSA: Response etype is %d for %d. Retry.\n", msticket->SessionKey.KeyType, etypes[i]);
                    }
                    continue;
                }
                found = 1;
                break;
            }
        }

        if (etypes != NULL) {
            (*env)->ReleaseIntArrayElements(env, jetypes, etypes, 0);
        }

        /*

        typedef struct _KERB_RETRIEVE_TKT_RESPONSE {
            KERB_EXTERNAL_TICKET Ticket;
        } KERB_RETRIEVE_TKT_RESPONSE, *PKERB_RETRIEVE_TKT_RESPONSE;

        typedef struct _KERB_EXTERNAL_TICKET {
            PKERB_EXTERNAL_NAME ServiceName;
            PKERB_EXTERNAL_NAME TargetName;
            PKERB_EXTERNAL_NAME ClientName;
            UNICODE_STRING DomainName;
            UNICODE_STRING TargetDomainName;
            UNICODE_STRING AltTargetDomainName;
            KERB_CRYPTO_KEY SessionKey;
            ULONG TicketFlags;
            ULONG Flags;
            LARGE_INTEGER KeyExpirationTime;
            LARGE_INTEGER StartTime;
            LARGE_INTEGER EndTime;
            LARGE_INTEGER RenewUntil;
            LARGE_INTEGER TimeSkew;
            ULONG EncodedTicketSize;
            PUCHAR EncodedTicket; <========== Here's the good stuff
        } KERB_EXTERNAL_TICKET, *PKERB_EXTERNAL_TICKET;

        typedef struct _KERB_EXTERNAL_NAME {
            SHORT NameType;
            USHORT NameCount;
            UNICODE_STRING Names[ANYSIZE_ARRAY];
        } KERB_EXTERNAL_NAME, *PKERB_EXTERNAL_NAME;

        typedef struct _LSA_UNICODE_STRING {
            USHORT Length;
            USHORT MaximumLength;
            PWSTR  Buffer;
        } LSA_UNICODE_STRING, *PLSA_UNICODE_STRING;

        typedef LSA_UNICODE_STRING UNICODE_STRING, *PUNICODE_STRING;

        typedef struct KERB_CRYPTO_KEY {
            LONG KeyType;
            ULONG Length;
            PUCHAR Value;
        } KERB_CRYPTO_KEY, *PKERB_CRYPTO_KEY;

        */
        if (!found) {
            break;
        }

        // Build a com.sun.security.krb5.Ticket
        ticket = BuildTicket(env, msticket->EncodedTicket,
                                msticket->EncodedTicketSize);
        if (ticket == NULL) {
            break;
        }
        // OK, have a Ticket, now need to get the client name
        clientPrincipal = BuildPrincipal(env, msticket->ClientName,
                                msticket->TargetDomainName); // mdu
        if (clientPrincipal == NULL) {
            break;
        }

        // and the "name" of tgt
        targetPrincipal = BuildPrincipal(env, msticket->ServiceName,
                        msticket->DomainName);
        if (targetPrincipal == NULL) {
            break;
        }

        // Get the encryption key
        encryptionKey = BuildEncryptionKey(env, &(msticket->SessionKey));
        if (encryptionKey == NULL) {
            break;
        }

        // and the ticket flags
        ticketFlags = BuildTicketFlags(env, &(msticket->TicketFlags));
        if (ticketFlags == NULL) {
            break;
        }

        // Get the start time
        startTime = BuildKerberosTime(env, &(msticket->StartTime));
        if (startTime == NULL) {
            break;
        }

        /*
         * mdu: No point storing the eky expiration time in the auth
         * time field. Set it to be same as startTime. Looks like
         * windows does not have post-dated tickets.
         */
        authTime = startTime;

        // and the end time
        endTime = BuildKerberosTime(env, &(msticket->EndTime));
        if (endTime == NULL) {
            break;
        }

        // Get the renew till time
        renewTillTime = BuildKerberosTime(env, &(msticket->RenewUntil));
        if (renewTillTime == NULL) {
            break;
        }

        // and now go build a KrbCreds object
        krbCreds = (*env)->NewObject(
                env,
                krbcredsClass,
                krbcredsConstructor,
                ticket,
                clientPrincipal,
                NULL,
                targetPrincipal,
                NULL,
                encryptionKey,
                ticketFlags,
                authTime, // mdu
                startTime,
                endTime,
                renewTillTime, //mdu
                hostAddresses);

        break;
    } // end of WHILE. This WHILE will never loop.

    // clean up resources
    if (TktCacheResponse != NULL) {
        LsaFreeReturnBuffer(TktCacheResponse);
    }
    if (pTicketRequest) {
        LocalFree(pTicketRequest);
    }
    if (pTicketResponse != NULL) {
        LsaFreeReturnBuffer(pTicketResponse);
    }

    return krbCreds;
}

static NTSTATUS
ConstructTicketRequest(JNIEnv *env, UNICODE_STRING DomainName,
                PKERB_RETRIEVE_TKT_REQUEST *outRequest, ULONG *outSize)
{
    NTSTATUS Status;
    UNICODE_STRING TargetPrefix;
    USHORT TargetSize;
    ULONG RequestSize;
    ULONG Length;
    PKERB_RETRIEVE_TKT_REQUEST pTicketRequest = NULL;

    *outRequest = NULL;
    *outSize = 0;

    //
    // Set up the "krbtgt/" target prefix into a UNICODE_STRING so we
    // can easily concatenate it later.
    //

    TargetPrefix.Buffer = L"krbtgt/";
    Length = (ULONG)wcslen(TargetPrefix.Buffer) * sizeof(WCHAR);
    TargetPrefix.Length = (USHORT)Length;
    TargetPrefix.MaximumLength = TargetPrefix.Length;

    //
    // We will need to concatenate the "krbtgt/" prefix and the
    // Logon Session's DnsDomainName into our request's target name.
    //
    // Therefore, first compute the necessary buffer size for that.
    //
    // Note that we might theoretically have integer overflow.
    //

    TargetSize = TargetPrefix.Length + DomainName.Length;

    //
    // The ticket request buffer needs to be a single buffer.  That buffer
    // needs to include the buffer for the target name.
    //

    RequestSize = sizeof (*pTicketRequest) + TargetSize;

    //
    // Allocate the request buffer and make sure it's zero-filled.
    //

    pTicketRequest = (PKERB_RETRIEVE_TKT_REQUEST)
                    LocalAlloc(LMEM_ZEROINIT, RequestSize);
    if (!pTicketRequest) {
        ThrowOOME(env, "Can't allocate memory for ticket");
        return GetLastError();
    }

    //
    // Concatenate the target prefix with the previous response's
    // target domain.
    //

    pTicketRequest->TargetName.Length = 0;
    pTicketRequest->TargetName.MaximumLength = TargetSize;
    pTicketRequest->TargetName.Buffer = (PWSTR) (pTicketRequest + 1);
    Status = ConcatenateUnicodeStrings(&(pTicketRequest->TargetName),
                                    TargetPrefix,
                                    DomainName);
    *outRequest = pTicketRequest;
    *outSize    = RequestSize;
    return Status;
}

DWORD
ConcatenateUnicodeStrings(
    UNICODE_STRING *pTarget,
    UNICODE_STRING Source1,
    UNICODE_STRING Source2
    )
{
    //
    // The buffers for Source1 and Source2 cannot overlap pTarget's
    // buffer.  Source1.Length + Source2.Length must be <= 0xFFFF,
    // otherwise we overflow...
    //

    USHORT TotalSize = Source1.Length + Source2.Length;
    PBYTE buffer = (PBYTE) pTarget->Buffer;

    if (TotalSize > pTarget->MaximumLength)
        return ERROR_INSUFFICIENT_BUFFER;

    pTarget->Length = TotalSize;
    memcpy(buffer, Source1.Buffer, Source1.Length);
    memcpy(buffer + Source1.Length, Source2.Buffer, Source2.Length);
    return ERROR_SUCCESS;
}

BOOL
PackageConnectLookup(
    HANDLE *pLogonHandle,
    ULONG *pPackageId
    )
{
    LSA_STRING Name;
    NTSTATUS Status;

    Status = LsaConnectUntrusted(
                pLogonHandle
                );

    if (!LSA_SUCCESS(Status))
    {
        ShowNTError("LsaConnectUntrusted", Status);
        return FALSE;
    }

    Name.Buffer = MICROSOFT_KERBEROS_NAME_A;
    Name.Length = (USHORT)strlen(Name.Buffer);
    Name.MaximumLength = Name.Length + 1;

    Status = LsaLookupAuthenticationPackage(
                *pLogonHandle,
                &Name,
                pPackageId
                );

    if (!LSA_SUCCESS(Status))
    {
        ShowNTError("LsaLookupAuthenticationPackage", Status);
        return FALSE;
    }

    return TRUE;

}

VOID
ShowLastError(
        LPSTR szAPI,
        DWORD dwError
        )
{
    #define MAX_MSG_SIZE 256

    static WCHAR szMsgBuf[MAX_MSG_SIZE];
    DWORD dwRes;

    if (native_debug) {
        printf("LSA: Error calling function %s: %lu\n", szAPI, dwError);
    }

    dwRes = FormatMessage (
            FORMAT_MESSAGE_FROM_SYSTEM,
            NULL,
            dwError,
            0,
            szMsgBuf,
            MAX_MSG_SIZE,
            NULL);
    if (native_debug) {
        if (0 == dwRes) {
            printf("LSA: FormatMessage failed with %d\n", GetLastError());
            // ExitProcess(EXIT_FAILURE);
        } else {
            printf("LSA: %S",szMsgBuf);
        }
    }
}

VOID
ShowNTError(
        LPSTR szAPI,
        NTSTATUS Status
        )
{
    //
    // Convert the NTSTATUS to Winerror. Then call ShowLastError().
    //
    ShowLastError(szAPI, LsaNtStatusToWinError(Status));
}

VOID
InitUnicodeString(
        PUNICODE_STRING DestinationString,
    PCWSTR SourceString OPTIONAL
    )
{
    ULONG Length;

    DestinationString->Buffer = (PWSTR)SourceString;
    if (SourceString != NULL) {
        Length = (ULONG)wcslen( SourceString ) * sizeof( WCHAR );
        DestinationString->Length = (USHORT)Length;
        DestinationString->MaximumLength = (USHORT)(Length + sizeof(UNICODE_NULL));
    }
    else {
        DestinationString->MaximumLength = 0;
        DestinationString->Length = 0;
    }
}

jobject BuildTicket(JNIEnv *env, PUCHAR encodedTicket, ULONG encodedTicketSize) {

    // To build a Ticket, we need to make a byte array out of the EncodedTicket.

    jobject ticket;
    jbyteArray ary;

    ary = (*env)->NewByteArray(env,encodedTicketSize);
    if (ary == NULL) {
        return (jobject) NULL;
    }

    (*env)->SetByteArrayRegion(env, ary, (jsize) 0, encodedTicketSize,
                                    (jbyte *)encodedTicket);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->DeleteLocalRef(env, ary);
        return (jobject) NULL;
    }

    ticket = (*env)->NewObject(env, ticketClass, ticketConstructor, ary);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->DeleteLocalRef(env, ary);
        return (jobject) NULL;
    }
    (*env)->DeleteLocalRef(env, ary);
    return ticket;
}

// mdu
jobject BuildPrincipal(JNIEnv *env, PKERB_EXTERNAL_NAME principalName,
                                UNICODE_STRING domainName) {

    /*
     * To build the Principal, we need to get the names out of
     * this goofy MS structure
     */
    jobject principal = NULL;
    jobject realmStr = NULL;
    jobjectArray stringArray;
    jstring tempString;
    int nameCount,i;
    PUNICODE_STRING scanner;
    WCHAR *realm;
    ULONG realmLen;

    realm = (WCHAR *) LocalAlloc(LMEM_ZEROINIT,
            ((domainName.Length)*sizeof(WCHAR) + sizeof(UNICODE_NULL)));
    if (realm == NULL) {
        ThrowOOME(env, "Can't allocate memory for realm");
        return NULL;
    }
    wcsncpy(realm, domainName.Buffer, domainName.Length/sizeof(WCHAR));

    if (native_debug) {
        printf("LSA: Principal domain is %S\n", realm);
        printf("LSA: Name type is %x\n", principalName->NameType);
        printf("LSA: Name count is %x\n", principalName->NameCount);
    }

    nameCount = principalName->NameCount;
    stringArray = (*env)->NewObjectArray(env, nameCount,
                            javaLangStringClass, NULL);
    if (stringArray == NULL) {
        if (native_debug) {
            printf("LSA: Can't allocate String array for Principal\n");
        }
        goto cleanup;
    }

    for (i=0; i<nameCount; i++) {
        // get the principal name
        scanner = &(principalName->Names[i]);

        // OK, got a Char array, so construct a String
        tempString = (*env)->NewString(env, (const jchar*)scanner->Buffer,
                            scanner->Length/sizeof(WCHAR));

        if (tempString == NULL) {
            goto cleanup;
        }

        // Set the String into the StringArray
        (*env)->SetObjectArrayElement(env, stringArray, i, tempString);

        if ((*env)->ExceptionCheck(env)) {
            goto cleanup;
        }

        // Do I have to worry about storage reclamation here?
    }
    // now set the realm in the principal
    realmLen = (ULONG)wcslen((PWCHAR)realm);
    realmStr = (*env)->NewString(env, (PWCHAR)realm, (USHORT)realmLen);

    if (realmStr == NULL) {
        goto cleanup;
    }

    principal = (*env)->NewObject(env, principalNameClass,
                    principalNameConstructor, stringArray, realmStr);

cleanup:
    // free local resources
    LocalFree(realm);

    return principal;
}

jobject BuildEncryptionKey(JNIEnv *env, PKERB_CRYPTO_KEY cryptoKey) {
    // First, need to build a byte array
    jbyteArray ary;
    jobject encryptionKey = NULL;
    unsigned int i;

    for (i=0; i<cryptoKey->Length; i++) {
        if (cryptoKey->Value[i]) break;
    }
    if (i == cryptoKey->Length) {
        if (native_debug) {
            printf("LSA: Session key all zero. Stop.\n");
        }
        return NULL;
    }

    ary = (*env)->NewByteArray(env,cryptoKey->Length);
    if (ary == NULL) {
        return (jobject) NULL;
    }
    (*env)->SetByteArrayRegion(env, ary, (jsize) 0, cryptoKey->Length,
                                    (jbyte *)cryptoKey->Value);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->DeleteLocalRef(env, ary);
    } else {
        encryptionKey = (*env)->NewObject(env, encryptionKeyClass,
                encryptionKeyConstructor, cryptoKey->KeyType, ary);
    }

    return encryptionKey;
}

jobject BuildTicketFlags(JNIEnv *env, PULONG flags) {
    jobject ticketFlags = NULL;
    jbyteArray ary;
    /*
     * mdu: Convert the bytes to nework byte order before copying
     * them to a Java byte array.
     */
    ULONG nlflags = htonl(*flags);

    ary = (*env)->NewByteArray(env, sizeof(*flags));
    if (ary == NULL) {
        return (jobject) NULL;
    }
    (*env)->SetByteArrayRegion(env, ary, (jsize) 0, sizeof(*flags),
                                    (jbyte *)&nlflags);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->DeleteLocalRef(env, ary);
    } else {
        ticketFlags = (*env)->NewObject(env, ticketFlagsClass,
                ticketFlagsConstructor, sizeof(*flags)*8, ary);
    }

    return ticketFlags;
}

jobject BuildKerberosTime(JNIEnv *env, PLARGE_INTEGER kerbtime) {
    jobject kerberosTime = NULL;
    jstring stringTime = NULL;
    SYSTEMTIME systemTime;
    WCHAR timeString[16];
    WCHAR month[3];
    WCHAR day[3];
    WCHAR hour[3];
    WCHAR minute[3];
    WCHAR second[3];

    if (FileTimeToSystemTime((FILETIME *)kerbtime, &systemTime)) {
        // XXX Cannot use %02.2ld, because the leading 0 is ignored for integers.
        // So, print them to strings, and then print them to the master string with a
        // format pattern that makes it two digits and prefix with a 0 if necessary.
        swprintf( (wchar_t *)month, 3, L"%2.2d", systemTime.wMonth);
        swprintf( (wchar_t *)day, 3, L"%2.2d", systemTime.wDay);
        swprintf( (wchar_t *)hour, 3, L"%2.2d", systemTime.wHour);
        swprintf( (wchar_t *)minute, 3, L"%2.2d", systemTime.wMinute);
        swprintf( (wchar_t *)second, 3, L"%2.2d", systemTime.wSecond);
        swprintf( (wchar_t *)timeString, 16,
                L"%ld%02.2s%02.2s%02.2s%02.2s%02.2sZ",
                systemTime.wYear,
                month,
                day,
                hour,
                minute,
                second );
        if (native_debug) {
            printf("LSA: %S\n", (wchar_t *)timeString);
        }
        stringTime = (*env)->NewString(env, timeString,
                (sizeof(timeString)/sizeof(WCHAR))-1);
        if (stringTime != NULL) { // everything's OK so far
            kerberosTime = (*env)->NewObject(env, kerberosTimeClass,
                    kerberosTimeConstructor, stringTime);
        }
    }
    return kerberosTime;
}

void ThrowOOME(JNIEnv *env, const char *szMessage) {
    jclass exceptionClazz = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (exceptionClazz != NULL) {
        (*env)->ThrowNew(env, exceptionClazz, szMessage);
    }
}
