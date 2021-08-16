/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include "jni_util.h"
#include "com_sun_security_auth_module_NTSystem.h"

#include <windows.h>
#include <stdio.h>
#include <wchar.h>
#include <ntsecapi.h>
#include <lmerr.h>

static BOOL debug = FALSE;

BOOL getToken(PHANDLE);
BOOL getUser(HANDLE tokenHandle, LPTSTR *userName,
        LPTSTR *domainName, LPTSTR *userSid, LPTSTR *domainSid);
BOOL getPrimaryGroup(HANDLE tokenHandle, LPTSTR *primaryGroup);
BOOL getGroups(HANDLE tokenHandle, PDWORD numGroups, LPTSTR **groups);
BOOL getImpersonationToken(PHANDLE impersonationToken);
BOOL getTextualSid(PSID pSid, LPTSTR TextualSid, LPDWORD lpdwBufferLen);
void DisplayErrorText(DWORD dwLastError);

static void throwIllegalArgumentException(JNIEnv *env, const char *msg) {
    jclass clazz = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (clazz != NULL)
        (*env)->ThrowNew(env, clazz, msg);
}

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

JNIEXPORT jlong JNICALL
Java_com_sun_security_auth_module_NTSystem_getImpersonationToken0
        (JNIEnv *env, jobject obj) {
    HANDLE impersonationToken = 0;      // impersonation token
    if (debug) {
        printf("getting impersonation token\n");
    }
    if (getImpersonationToken(&impersonationToken) == FALSE) {
        return 0;
    }
    return (jlong)impersonationToken;
}

JNIEXPORT void JNICALL
Java_com_sun_security_auth_module_NTSystem_getCurrent
    (JNIEnv *env, jobject obj, jboolean debugNative) {

    long i, j = 0;
    HANDLE tokenHandle = INVALID_HANDLE_VALUE;

    LPTSTR userName = NULL;             // user name
    LPTSTR userSid = NULL;              // user sid
    LPTSTR domainName = NULL;           // domain name
    LPTSTR domainSid = NULL;            // domain sid
    LPTSTR primaryGroup = NULL;         // primary group sid
    DWORD numGroups = 0;                // num groups
    LPTSTR *groups = NULL;              // groups array
    long pIndex = -1;                   // index of primaryGroup in groups array

    jfieldID fid;
    jstring jstr;
    jobjectArray jgroups;
    jclass stringClass = 0;
    jclass cls = (*env)->GetObjectClass(env, obj);

    debug = debugNative;

    // get NT information first

    if (debug) {
        printf("getting access token\n");
    }
    if (getToken(&tokenHandle) == FALSE) {
        return;
    }

    if (debug) {
        printf("getting user info\n");
    }
    if (getUser
        (tokenHandle, &userName, &domainName, &userSid, &domainSid) == FALSE) {
        return;
    }

    if (debug) {
        printf("getting primary group\n");
    }
    if (getPrimaryGroup(tokenHandle, &primaryGroup) == FALSE) {
        return;
    }

    if (debug) {
        printf("getting supplementary groups\n");
    }
    if (getGroups(tokenHandle, &numGroups, &groups) == FALSE) {
        return;
    }

    // then set values into NTSystem

    fid = (*env)->GetFieldID(env, cls, "userName", "Ljava/lang/String;");
    if (fid == 0) {
        (*env)->ExceptionClear(env);
        throwIllegalArgumentException(env, "invalid field: userName");
        goto cleanup;
    }
    jstr = (*env)->NewStringUTF(env, userName);
    if (jstr == NULL)
        goto cleanup;
    (*env)->SetObjectField(env, obj, fid, jstr);

    fid = (*env)->GetFieldID(env, cls, "userSID", "Ljava/lang/String;");
    if (fid == 0) {
        (*env)->ExceptionClear(env);
        throwIllegalArgumentException(env, "invalid field: userSID");
        goto cleanup;
    }
    jstr = (*env)->NewStringUTF(env, userSid);
    if (jstr == NULL)
        goto cleanup;
    (*env)->SetObjectField(env, obj, fid, jstr);

    fid = (*env)->GetFieldID(env, cls, "domain", "Ljava/lang/String;");
    if (fid == 0) {
        (*env)->ExceptionClear(env);
        throwIllegalArgumentException(env, "invalid field: domain");
        goto cleanup;
    }
    jstr = (*env)->NewStringUTF(env, domainName);
    if (jstr == NULL)
        goto cleanup;
    (*env)->SetObjectField(env, obj, fid, jstr);

    if (domainSid != NULL) {
        fid = (*env)->GetFieldID(env, cls, "domainSID", "Ljava/lang/String;");
        if (fid == 0) {
            (*env)->ExceptionClear(env);
            throwIllegalArgumentException(env, "invalid field: domainSID");
            goto cleanup;
        }
        jstr = (*env)->NewStringUTF(env, domainSid);
        if (jstr == NULL)
            goto cleanup;
        (*env)->SetObjectField(env, obj, fid, jstr);
    }

    fid = (*env)->GetFieldID(env, cls, "primaryGroupID", "Ljava/lang/String;");
    if (fid == 0) {
        (*env)->ExceptionClear(env);
        throwIllegalArgumentException(env, "invalid field: PrimaryGroupID");
        goto cleanup;
    }
    jstr = (*env)->NewStringUTF(env, primaryGroup);
    if (jstr == NULL)
        goto cleanup;
    (*env)->SetObjectField(env, obj, fid, jstr);

    // primary group may or may not be part of supplementary groups
    for (i = 0; i < (long)numGroups; i++) {
        if (strcmp(primaryGroup, groups[i]) == 0) {
            // found primary group in groups array
            pIndex = i;
            break;
        }
    }

    if (numGroups == 0 || (pIndex == 0 && numGroups == 1)) {
        // primary group is only group in groups array

        if (debug) {
            printf("no secondary groups\n");
        }
    } else {

        // the groups array is non-empty,
        // and may or may not contain the primary group

        fid = (*env)->GetFieldID(env, cls, "groupIDs", "[Ljava/lang/String;");
        if (fid == 0) {
            (*env)->ExceptionClear(env);
            throwIllegalArgumentException(env, "groupIDs");
            goto cleanup;
        }

        stringClass = (*env)->FindClass(env, "java/lang/String");
        if (stringClass == NULL)
            goto cleanup;

        if (pIndex == -1) {
            // primary group not in groups array
            jgroups = (*env)->NewObjectArray(env, numGroups, stringClass, 0);
        } else {
            // primary group in groups array -
            // allocate one less array entry and do not add into new array
            jgroups = (*env)->NewObjectArray(env, numGroups-1, stringClass, 0);
        }
        if (jgroups == NULL)
            goto cleanup;

        for (i = 0, j = 0; i < (long)numGroups; i++) {
            if (pIndex == i) {
                // continue if equal to primary group
                continue;
            }
            jstr = (*env)->NewStringUTF(env, groups[i]);
            if (jstr == NULL)
                goto cleanup;
            (*env)->SetObjectArrayElement(env, jgroups, j++, jstr);
        }
        (*env)->SetObjectField(env, obj, fid, jgroups);
    }

cleanup:
    if (userName != NULL) {
        HeapFree(GetProcessHeap(), 0, userName);
    }
    if (domainName != NULL) {
        HeapFree(GetProcessHeap(), 0, domainName);
    }
    if (userSid != NULL) {
        HeapFree(GetProcessHeap(), 0, userSid);
    }
    if (domainSid != NULL) {
        HeapFree(GetProcessHeap(), 0, domainSid);
    }
    if (primaryGroup != NULL) {
        HeapFree(GetProcessHeap(), 0, primaryGroup);
    }
    if (groups != NULL) {
        for (i = 0; i < (long)numGroups; i++) {
            if (groups[i] != NULL) {
                HeapFree(GetProcessHeap(), 0, groups[i]);
            }
        }
        HeapFree(GetProcessHeap(), 0, groups);
    }
    CloseHandle(tokenHandle);

    return;
}

BOOL getToken(PHANDLE tokenHandle) {

    // first try the thread token
    if (OpenThreadToken(GetCurrentThread(),
                        TOKEN_READ,
                        FALSE,
                        tokenHandle) == 0) {
        if (debug) {
            printf("  [getToken] OpenThreadToken error [%d]: ", GetLastError());
            DisplayErrorText(GetLastError());
        }

        // next try the process token
        if (OpenProcessToken(GetCurrentProcess(),
                        TOKEN_READ,
                        tokenHandle) == 0) {
            if (debug) {
                printf("  [getToken] OpenProcessToken error [%d]: ",
                        GetLastError());
                DisplayErrorText(GetLastError());
            }
            return FALSE;
        }
    }

    if (debug) {
        printf("  [getToken] got user access token\n");
    }

    return TRUE;
}

BOOL getUser(HANDLE tokenHandle, LPTSTR *userName,
        LPTSTR *domainName, LPTSTR *userSid, LPTSTR *domainSid) {

    BOOL error = FALSE;
    DWORD bufSize = 0;
    DWORD buf2Size = 0;
    DWORD retBufSize = 0;
    PTOKEN_USER tokenUserInfo = NULL;   // getTokenInformation
    SID_NAME_USE nameUse;               // LookupAccountSid

    PSID dSid = NULL;
    LPTSTR domainSidName = NULL;

    // get token information
    GetTokenInformation(tokenHandle,
                        TokenUser,
                        NULL,   // TokenInformation - if NULL get buffer size
                        0,      // since TokenInformation is NULL
                        &bufSize);

    tokenUserInfo = (PTOKEN_USER)HeapAlloc(GetProcessHeap(), 0, bufSize);
    if (GetTokenInformation(tokenHandle,
                        TokenUser,
                        tokenUserInfo,
                        bufSize,
                        &retBufSize) == 0) {
        if (debug) {
            printf("  [getUser] GetTokenInformation error [%d]: ",
                GetLastError());
            DisplayErrorText(GetLastError());
        }
        error = TRUE;
        goto cleanup;
    }

    if (debug) {
        printf("  [getUser] Got TokenUser info\n");
    }

    // get userName
    bufSize = 0;
    buf2Size = 0;
    LookupAccountSid(NULL,      // local host
                tokenUserInfo->User.Sid,
                NULL,
                &bufSize,
                NULL,
                &buf2Size,
                &nameUse);

    *userName = (LPTSTR)HeapAlloc(GetProcessHeap(), 0, bufSize);
    *domainName = (LPTSTR)HeapAlloc(GetProcessHeap(), 0, buf2Size);
    if (LookupAccountSid(NULL,  // local host
                tokenUserInfo->User.Sid,
                *userName,
                &bufSize,
                *domainName,
                &buf2Size,
                &nameUse) == 0) {
        if (debug) {
            printf("  [getUser] LookupAccountSid error [%d]: ",
                GetLastError());
            DisplayErrorText(GetLastError());
        }
        error = TRUE;
        goto cleanup;
    }

    if (debug) {
        printf("  [getUser] userName: %s, domainName = %s\n",
                *userName, *domainName);
    }

    bufSize = 0;
    getTextualSid(tokenUserInfo->User.Sid, NULL, &bufSize);
    *userSid = (LPTSTR)HeapAlloc(GetProcessHeap(), 0, bufSize);
    getTextualSid(tokenUserInfo->User.Sid, *userSid, &bufSize);
    if (debug) {
        printf("  [getUser] userSid: %s\n", *userSid);
    }

    // get domainSid
    bufSize = 0;
    buf2Size = 0;
    LookupAccountName(NULL,     // local host
                *domainName,
                NULL,
                &bufSize,
                NULL,
                &buf2Size,
                &nameUse);

    dSid = (PSID)HeapAlloc(GetProcessHeap(), 0, bufSize);
    domainSidName = (LPTSTR)HeapAlloc(GetProcessHeap(), 0, buf2Size);
    if (LookupAccountName(NULL, // local host
                *domainName,
                dSid,
                &bufSize,
                domainSidName,
                &buf2Size,
                &nameUse) == 0) {
        if (debug) {
            printf("  [getUser] LookupAccountName error [%d]: ",
                GetLastError());
            DisplayErrorText(GetLastError());
        }
        // ok not to have a domain SID (no error)
        goto cleanup;
    }

    bufSize = 0;
    getTextualSid(dSid, NULL, &bufSize);
    *domainSid = (LPTSTR)HeapAlloc(GetProcessHeap(), 0, bufSize);
    getTextualSid(dSid, *domainSid, &bufSize);
    if (debug) {
        printf("  [getUser] domainSid: %s\n", *domainSid);
    }

cleanup:
    if (tokenUserInfo != NULL) {
        HeapFree(GetProcessHeap(), 0, tokenUserInfo);
    }
    if (dSid != NULL) {
        HeapFree(GetProcessHeap(), 0, dSid);
    }
    if (domainSidName != NULL) {
        HeapFree(GetProcessHeap(), 0, domainSidName);
    }
    if (error) {
        return FALSE;
    }
    return TRUE;
}

BOOL getPrimaryGroup(HANDLE tokenHandle, LPTSTR *primaryGroup) {

    BOOL error = FALSE;
    DWORD bufSize = 0;
    DWORD retBufSize = 0;

    PTOKEN_PRIMARY_GROUP tokenGroupInfo = NULL;

    // get token information
    GetTokenInformation(tokenHandle,
                        TokenPrimaryGroup,
                        NULL,   // TokenInformation - if NULL get buffer size
                        0,      // since TokenInformation is NULL
                        &bufSize);

    tokenGroupInfo = (PTOKEN_PRIMARY_GROUP)HeapAlloc
                        (GetProcessHeap(), 0, bufSize);
    if (GetTokenInformation(tokenHandle,
                        TokenPrimaryGroup,
                        tokenGroupInfo,
                        bufSize,
                        &retBufSize) == 0) {
        if (debug) {
            printf("  [getPrimaryGroup] GetTokenInformation error [%d]: ",
                GetLastError());
            DisplayErrorText(GetLastError());
        }
        error = TRUE;
        goto cleanup;
    }

    if (debug) {
        printf("  [getPrimaryGroup] Got TokenPrimaryGroup info\n");
    }

    bufSize = 0;
    getTextualSid(tokenGroupInfo->PrimaryGroup, NULL, &bufSize);
    *primaryGroup = (LPTSTR)HeapAlloc(GetProcessHeap(), 0, bufSize);
    getTextualSid(tokenGroupInfo->PrimaryGroup, *primaryGroup, &bufSize);
    if (debug) {
        printf("  [getPrimaryGroup] primaryGroup: %s\n", *primaryGroup);
    }

cleanup:
    if (tokenGroupInfo != NULL) {
        HeapFree(GetProcessHeap(), 0, tokenGroupInfo);
    }
    if (error) {
        return FALSE;
    }
    return TRUE;
}

BOOL getGroups(HANDLE tokenHandle, PDWORD numGroups, LPTSTR **groups) {

    BOOL error = FALSE;
    DWORD bufSize = 0;
    DWORD retBufSize = 0;
    long i = 0;

    PTOKEN_GROUPS tokenGroupInfo = NULL;

    // get token information
    GetTokenInformation(tokenHandle,
                        TokenGroups,
                        NULL,   // TokenInformation - if NULL get buffer size
                        0,      // since TokenInformation is NULL
                        &bufSize);

    tokenGroupInfo = (PTOKEN_GROUPS)HeapAlloc(GetProcessHeap(), 0, bufSize);
    if (GetTokenInformation(tokenHandle,
                        TokenGroups,
                        tokenGroupInfo,
                        bufSize,
                        &retBufSize) == 0) {
        if (debug) {
            printf("  [getGroups] GetTokenInformation error [%d]: ",
                GetLastError());
            DisplayErrorText(GetLastError());
        }
        error = TRUE;
        goto cleanup;
    }

    if (debug) {
        printf("  [getGroups] Got TokenGroups info\n");
    }

    if (tokenGroupInfo->GroupCount == 0) {
        // no groups
        goto cleanup;
    }

    // return group info
    *numGroups = tokenGroupInfo->GroupCount;
    *groups = (LPTSTR *)HeapAlloc
                (GetProcessHeap(), 0, (*numGroups) * sizeof(LPTSTR));
    for (i = 0; i < (long)*numGroups; i++) {
        bufSize = 0;
        getTextualSid(tokenGroupInfo->Groups[i].Sid, NULL, &bufSize);
        (*groups)[i] = (LPTSTR)HeapAlloc(GetProcessHeap(), 0, bufSize);
        getTextualSid(tokenGroupInfo->Groups[i].Sid, (*groups)[i], &bufSize);
        if (debug) {
            printf("  [getGroups] group %d: %s\n", i, (*groups)[i]);
        }
    }

cleanup:
    if (tokenGroupInfo != NULL) {
        HeapFree(GetProcessHeap(), 0, tokenGroupInfo);
    }
    if (error) {
        return FALSE;
    }
    return TRUE;
}

BOOL getImpersonationToken(PHANDLE impersonationToken) {

    HANDLE dupToken;

    if (OpenThreadToken(GetCurrentThread(),
                        TOKEN_DUPLICATE,
                        FALSE,
                        &dupToken) == 0) {
        if (OpenProcessToken(GetCurrentProcess(),
                                TOKEN_DUPLICATE,
                                &dupToken) == 0) {
            if (debug) {
                printf
                    ("  [getImpersonationToken] OpenProcessToken error [%d]: ",
                    GetLastError());
                DisplayErrorText(GetLastError());
            }
            return FALSE;
        }
    }

    if (DuplicateToken(dupToken,
                        SecurityImpersonation,
                        impersonationToken) == 0) {
        if (debug) {
            printf("  [getImpersonationToken] DuplicateToken error [%d]: ",
                GetLastError());
            DisplayErrorText(GetLastError());
        }
        return FALSE;
    }
    CloseHandle(dupToken);

    if (debug) {
        printf("  [getImpersonationToken] token = %p\n",
            (void *)*impersonationToken);
    }
    return TRUE;
}

BOOL getTextualSid
    (PSID pSid,                 // binary SID
    LPTSTR TextualSid,          // buffer for Textual representation of SID
    LPDWORD lpdwBufferLen) {    // required/provided TextualSid buffersize

    PSID_IDENTIFIER_AUTHORITY psia;
    DWORD dwSubAuthorities;
    DWORD dwSidRev=SID_REVISION;
    DWORD dwCounter;
    DWORD dwSidSize;

    // Validate the binary SID.
    if(!IsValidSid(pSid)) return FALSE;

    // Get the identifier authority value from the SID.
    psia = GetSidIdentifierAuthority(pSid);

    // Get the number of subauthorities in the SID.
    dwSubAuthorities = *GetSidSubAuthorityCount(pSid);

    // Compute the buffer length.
    // S-SID_REVISION- + IdentifierAuthority- + subauthorities- + NULL
    dwSidSize=(15 + 12 + (12 * dwSubAuthorities) + 1) * sizeof(TCHAR);

    // Check input buffer length.
    // If too small, indicate the proper size and set last error.
    if (*lpdwBufferLen < dwSidSize) {
        *lpdwBufferLen = dwSidSize;
        SetLastError(ERROR_INSUFFICIENT_BUFFER);
        return FALSE;
    }

    // Add 'S' prefix and revision number to the string.
    dwSidSize=wsprintf(TextualSid, TEXT("S-%lu-"), dwSidRev );

    // Add SID identifier authority to the string.
    if ((psia->Value[0] != 0) || (psia->Value[1] != 0)) {
        dwSidSize+=wsprintf(TextualSid + lstrlen(TextualSid),
                TEXT("0x%02hx%02hx%02hx%02hx%02hx%02hx"),
                (USHORT)psia->Value[0],
                (USHORT)psia->Value[1],
                (USHORT)psia->Value[2],
                (USHORT)psia->Value[3],
                (USHORT)psia->Value[4],
                (USHORT)psia->Value[5]);
    } else {
        dwSidSize+=wsprintf(TextualSid + lstrlen(TextualSid),
                TEXT("%lu"),
                (ULONG)(psia->Value[5]  )   +
                (ULONG)(psia->Value[4] <<  8)   +
                (ULONG)(psia->Value[3] << 16)   +
                (ULONG)(psia->Value[2] << 24)   );
    }

    // Add SID subauthorities to the string.
    for (dwCounter=0 ; dwCounter < dwSubAuthorities ; dwCounter++) {
        dwSidSize+=wsprintf(TextualSid + dwSidSize, TEXT("-%lu"),
                *GetSidSubAuthority(pSid, dwCounter) );
    }

    return TRUE;
}

void DisplayErrorText(DWORD dwLastError) {
    HMODULE hModule = NULL; // default to system source
    LPSTR MessageBuffer;
    DWORD dwBufferLength;

    DWORD dwFormatFlags = FORMAT_MESSAGE_ALLOCATE_BUFFER |
                        FORMAT_MESSAGE_IGNORE_INSERTS |
                        FORMAT_MESSAGE_FROM_SYSTEM ;

    //
    // If dwLastError is in the network range,
    //  load the message source.
    //

    if(dwLastError >= NERR_BASE && dwLastError <= MAX_NERR) {
        hModule = LoadLibraryEx(TEXT("netmsg.dll"),
                                NULL,
                                LOAD_LIBRARY_AS_DATAFILE);

        if(hModule != NULL)
            dwFormatFlags |= FORMAT_MESSAGE_FROM_HMODULE;
    }

    //
    // Call FormatMessage() to allow for message
    //  text to be acquired from the system
    //  or from the supplied module handle.
    //

    if(dwBufferLength = FormatMessageA(dwFormatFlags,
                hModule, // module to get message from (NULL == system)
                dwLastError,
                MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // default language
                (LPSTR) &MessageBuffer,
                0,
                NULL)) {
        DWORD dwBytesWritten;

        //
        // Output message string on stderr.
        //
        WriteFile(GetStdHandle(STD_ERROR_HANDLE),
                MessageBuffer,
                dwBufferLength,
                &dwBytesWritten,
                NULL);

        //
        // Free the buffer allocated by the system.
        //
        LocalFree(MessageBuffer);
    }

    //
    // If we loaded a message source, unload it.
    //
    if(hModule != NULL)
        FreeLibrary(hModule);
}

/**
 * 1. comment out first two #includes
 * 2. set 'debug' to TRUE
 * 3. comment out 'getCurrent'
 * 4. uncomment 'main'
 * 5. cc -c nt.c
 * 6. link nt.obj user32.lib advapi32.lib /out:nt.exe
 */
/*
void main(int argc, char *argv[]) {

    long i = 0;
    HANDLE tokenHandle = INVALID_HANDLE_VALUE;

    LPTSTR userName = NULL;
    LPTSTR userSid = NULL;
    LPTSTR domainName = NULL;
    LPTSTR domainSid = NULL;
    LPTSTR primaryGroup = NULL;
    DWORD numGroups = 0;
    LPTSTR *groups = NULL;
    HANDLE impersonationToken = 0;

    printf("getting access token\n");
    if (getToken(&tokenHandle) == FALSE) {
        exit(1);
    }

    printf("getting user info\n");
    if (getUser
        (tokenHandle, &userName, &domainName, &userSid, &domainSid) == FALSE) {
        exit(1);
    }

    printf("getting primary group\n");
    if (getPrimaryGroup(tokenHandle, &primaryGroup) == FALSE) {
        exit(1);
    }

    printf("getting supplementary groups\n");
    if (getGroups(tokenHandle, &numGroups, &groups) == FALSE) {
        exit(1);
    }

    printf("getting impersonation token\n");
    if (getImpersonationToken(&impersonationToken) == FALSE) {
        exit(1);
    }

    printf("userName = %s, userSid = %s, domainName = %s, domainSid = %s\n",
        userName, userSid, domainName, domainSid);
    printf("primaryGroup = %s\n", primaryGroup);
    for (i = 0; i < numGroups; i++) {
        printf("Group[%d] = %s\n", i, groups[i]);
    }
    printf("impersonationToken = %ld\n", impersonationToken);

    if (userName != NULL) {
        HeapFree(GetProcessHeap(), 0, userName);
    }
    if (userSid != NULL) {
        HeapFree(GetProcessHeap(), 0, userSid);
    }
    if (domainName != NULL) {
        HeapFree(GetProcessHeap(), 0, domainName);
    }
    if (domainSid != NULL) {
        HeapFree(GetProcessHeap(), 0, domainSid);
    }
    if (primaryGroup != NULL) {
        HeapFree(GetProcessHeap(), 0, primaryGroup);
    }
    if (groups != NULL) {
        for (i = 0; i < numGroups; i++) {
            if (groups[i] != NULL) {
                HeapFree(GetProcessHeap(), 0, groups[i]);
            }
        }
        HeapFree(GetProcessHeap(), 0, groups);
    }
    CloseHandle(impersonationToken);
    CloseHandle(tokenHandle);
}
*/

/**
 * extra main method for testing debug printing
 */
/*
void main(int argc, char *argv[]) {
    if(argc != 2) {
        fprintf(stderr,"Usage: %s <error number>\n", argv[0]);
    }

    DisplayErrorText(atoi(argv[1]));
}
*/
