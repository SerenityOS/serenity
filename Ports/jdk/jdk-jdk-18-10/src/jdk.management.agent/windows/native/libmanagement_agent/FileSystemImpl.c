/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include <windows.h>
#include <malloc.h>
#include <string.h>

#include "jni.h"
#include "jni_util.h"
#include "jdk_internal_agent_FileSystemImpl.h"

JNIEXPORT jint JNICALL DEF_JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv* env;

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_2) != JNI_OK) {
        return JNI_EVERSION; /* JNI version not supported */
    }

    return JNI_VERSION_10;
}


/*
 * Access mask to represent any file access
 */
#define ANY_ACCESS (FILE_GENERIC_READ | FILE_GENERIC_WRITE | FILE_GENERIC_EXECUTE)

/*
 * Returns JNI_TRUE if the specified file is on a file system that supports
 * persistent ACLs (On NTFS file systems returns true, on FAT32 file systems
 * returns false).
 */
static jboolean isSecuritySupported(JNIEnv* env, const char* path) {
    char* root;
    char* p;
    BOOL res;
    DWORD dwMaxComponentLength;
    DWORD dwFlags;
    char fsName[128];
    DWORD fsNameLength;

    /*
     * Get root directory. Assume that files are absolute paths. For UNCs
     * the slash after the share name is required.
     */
    root = strdup(path);
    if (*root == '\\') {
        /*
         * \\server\share\file ==> \\server\share\
         */
        int slashskip = 3;
        p = root;
        while ((*p == '\\') && (slashskip > 0)) {
            char* p2;
            p++;
            p2 = strchr(p, '\\');
            if ((p2 == NULL) || (*p2 != '\\')) {
                free(root);
                JNU_ThrowIOException(env, "Malformed UNC");
                return JNI_FALSE;
            }
            p = p2;
            slashskip--;
        }
        if (slashskip != 0) {
            free(root);
            JNU_ThrowIOException(env, "Malformed UNC");
            return JNI_FALSE;
        }
        p++;
        *p = '\0';

    } else {
        p = strchr(root, '\\');
        if (p == NULL) {
            free(root);
            JNU_ThrowIOException(env, "Absolute filename not specified");
            return JNI_FALSE;
        }
        p++;
        *p = '\0';
    }


    /*
     * Get the volume information - this gives us the file system file and
     * also tells us if the file system supports persistent ACLs.
     */
    fsNameLength = sizeof(fsName)-1;
    res = GetVolumeInformation(root,
                               NULL,        // address of name of the volume, can be NULL
                               0,           // length of volume name
                               NULL,        // address of volume serial number, can be NULL
                               &dwMaxComponentLength,
                               &dwFlags,
                               fsName,
                               fsNameLength);
    if (res == 0) {
        free(root);
        JNU_ThrowIOExceptionWithLastError(env, "GetVolumeInformation failed");
        return JNI_FALSE;
    }

    free(root);
    return (dwFlags & FS_PERSISTENT_ACLS) ? JNI_TRUE : JNI_FALSE;
}


/*
 * Returns the security descriptor for a file.
 */
static SECURITY_DESCRIPTOR* getFileSecurityDescriptor(JNIEnv* env, const char* path) {
    SECURITY_DESCRIPTOR* sd;
    DWORD len = 0;
    SECURITY_INFORMATION info =
        OWNER_SECURITY_INFORMATION | DACL_SECURITY_INFORMATION;

    GetFileSecurityA(path, info , 0, 0, &len);
    if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
        JNU_ThrowIOExceptionWithLastError(env, "GetFileSecurity failed");
        return NULL;
    }
    sd = (SECURITY_DESCRIPTOR *)malloc(len);
    if (sd == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
    } else {
        if (!(*GetFileSecurityA)(path, info, sd, len, &len)) {
            JNU_ThrowIOExceptionWithLastError(env, "GetFileSecurity failed");
            free(sd);
            return NULL;
        }
    }
    return sd;
}

/*
 * Returns pointer to the SID identifying the owner of the specified
 * file.
 */
static SID* getFileOwner(JNIEnv* env, SECURITY_DESCRIPTOR* sd) {
    SID* owner;
    BOOL defaulted;

    if (!GetSecurityDescriptorOwner(sd, &owner, &defaulted)) {
        JNU_ThrowIOExceptionWithLastError(env, "GetSecurityDescriptorOwner failed");
        return NULL;
    }
    return owner;
}

/*
 * Returns pointer discretionary access-control list (ACL) from the security
 * descriptor of the specified file.
 */
static ACL* getFileDACL(JNIEnv* env, SECURITY_DESCRIPTOR* sd) {
    ACL *acl;
    int defaulted, present;

    if (!GetSecurityDescriptorDacl(sd, &present, &acl, &defaulted)) {
        JNU_ThrowIOExceptionWithLastError(env, "GetSecurityDescriptorDacl failed");
        return NULL;
    }
    if (!present) {
        JNU_ThrowInternalError(env, "Security descriptor does not contain a DACL");
        return NULL;
    }
    return acl;
}

/*
 * Returns JNI_TRUE if the specified owner is the only SID will access
 * to the file.
 */
static jboolean isAccessUserOnly(JNIEnv* env, SID* owner, ACL* acl) {
    ACL_SIZE_INFORMATION acl_size_info;
    DWORD i;

    /*
     * If there's no DACL then there's no access to the file
     */
    if (acl == NULL) {
        return JNI_TRUE;
    }

    /*
     * Get the ACE count
     */
    if (!GetAclInformation(acl, (void *) &acl_size_info, sizeof(acl_size_info),
                           AclSizeInformation)) {
        JNU_ThrowIOExceptionWithLastError(env, "GetAclInformation failed");
        return JNI_FALSE;
    }

    /*
     * Iterate over the ACEs. For each "allow" type check that the SID
     * matches the owner, and check that the access is read only.
     */
    for (i = 0; i < acl_size_info.AceCount; i++) {
        void* ace;
        ACCESS_ALLOWED_ACE *access;
        SID* sid;

        if (!GetAce(acl, i, &ace)) {
            JNU_ThrowIOExceptionWithLastError(env, "GetAce failed");
            return -1;
        }
        if (((ACCESS_ALLOWED_ACE *)ace)->Header.AceType != ACCESS_ALLOWED_ACE_TYPE) {
            continue;
        }
        access = (ACCESS_ALLOWED_ACE *)ace;
        sid = (SID *) &access->SidStart;
        if (!EqualSid(owner, sid)) {
            /*
             * If the ACE allows any access then the file is not secure.
             */
            if (access->Mask & ANY_ACCESS) {
                return JNI_FALSE;
            }
        }
    }
    return JNI_TRUE;
}


/*
 * Class:     jdk_internal_agent_FileSystemImpl
 * Method:    init0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jdk_internal_agent_FileSystemImpl_init0
  (JNIEnv *env, jclass ignored)
{
        /* nothing to do */
}

/*
 * Class:     jdk_internal_agent_FileSystemImpl
 * Method:    isSecuritySupported0
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_jdk_internal_agent_FileSystemImpl_isSecuritySupported0
  (JNIEnv *env, jclass ignored, jstring str)
{
    jboolean res;
    jboolean isCopy;
    const char* path;

    path = JNU_GetStringPlatformChars(env, str, &isCopy);
    if (path != NULL) {
        res = isSecuritySupported(env, path);
        if (isCopy) {
            JNU_ReleaseStringPlatformChars(env, str, path);
        }
        return res;
    } else {
        /* exception thrown - doesn't matter what we return */
        return JNI_TRUE;
    }
}


/*
 * Class:     jdk_internal_agent_FileSystemImpl
 * Method:    isAccessUserOnly0
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_jdk_internal_agent_FileSystemImpl_isAccessUserOnly0
  (JNIEnv *env, jclass ignored, jstring str)
{
    jboolean res = JNI_FALSE;
    jboolean isCopy;
    const char* path;

    path = JNU_GetStringPlatformChars(env, str, &isCopy);
    if (path != NULL) {
        /*
         * From the security descriptor get the file owner and
         * DACL. Then check if anybody but the owner has access
         * to the file.
         */
        SECURITY_DESCRIPTOR* sd = getFileSecurityDescriptor(env, path);
        if (sd != NULL) {
            SID *owner = getFileOwner(env, sd);
            if (owner != NULL) {
                ACL* acl = getFileDACL(env, sd);
                if (acl != NULL) {
                    res = isAccessUserOnly(env, owner, acl);
                } else {
                    /*
                     * If acl is NULL it means that an exception was thrown
                     * or there is "all acess" to the file.
                     */
                    res = JNI_FALSE;
                }
            }
            free(sd);
        }
        if (isCopy) {
            JNU_ReleaseStringPlatformChars(env, str, path);
        }
    }
    return res;
}
