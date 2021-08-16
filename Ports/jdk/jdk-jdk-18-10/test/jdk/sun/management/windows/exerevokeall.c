/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

#include <stdio.h>
#include <windows.h>
#include <malloc.h>
#include <string.h>

/*
 * Simple Windows utility to remove all non-owner access to a given file.
 */


/*
 * Access mask to represent any file access
 */
#define ANY_ACCESS (FILE_GENERIC_READ | FILE_GENERIC_WRITE | FILE_GENERIC_EXECUTE)


/*
 * Print error message to stderr
 */
static void printLastError(const char* msg) {
    int len;
    char buf[128];
    DWORD errval;

    buf[0] = '\0';
    len = sizeof(buf);

    errval = GetLastError();
    if (errval != 0) {
        int n = FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS,
                              NULL, errval,
                              0, buf, len, NULL);
        if (n > 3) {
            /* Drop final '.', CR, LF */
            if (buf[n - 1] == '\n') n--;
            if (buf[n - 1] == '\r') n--;
            if (buf[n - 1] == '.') n--;
            buf[n] = '\0';
        }
    }

    if (strlen(buf) > 0) {
        fprintf(stderr, "revokeall %s: %s\n", msg, buf);
    } else {
        fprintf(stderr, "revokeall %s\n", msg);
    }
}



/*
 * Return a string that includes all the components of a given SID.
 * See here for a description of the SID components :-
 * http://msdn.microsoft.com/library/default.asp?url=/library/en-us/security/security/sid_components.asp
 */
static char *getTextualSid(SID* sid) {
    SID_IDENTIFIER_AUTHORITY* sia;
    DWORD i, count;
    DWORD len;
    char* name;

    /*
     * Get the identifier authority and the number of sub-authorities
     */
    sia = GetSidIdentifierAuthority(sid);
    count = *GetSidSubAuthorityCount(sid);

    /*
     * Allocate buffer for the string - buffer is :-
     * S-SID_REVISION- + identifierAuthority- + subauthorities- + NULL
     */
    len=(15 + 12 + (12 * count) + 1) * sizeof(char);
    name = (char*)malloc(len);
    if (name == NULL) {
        return NULL;
    }

    // S-SID_REVISION
    sprintf(name, "S-%lu-", SID_REVISION );

    // Identifier authority
    if ((sia->Value[0] != 0) || (sia->Value[1] != 0))
    {
        sprintf(name + strlen(name), "0x%02hx%02hx%02hx%02hx%02hx%02hx",
                (USHORT)sia->Value[0],
                (USHORT)sia->Value[1],
                (USHORT)sia->Value[2],
                (USHORT)sia->Value[3],
                (USHORT)sia->Value[4],
                (USHORT)sia->Value[5]);
    }
    else
    {
        sprintf(name + strlen(name), "%lu",
                (ULONG)(sia->Value[5]      )   +
                (ULONG)(sia->Value[4] <<  8)   +
                (ULONG)(sia->Value[3] << 16)   +
                (ULONG)(sia->Value[2] << 24)   );
    }

    // finally, the sub-authorities
    for (i=0 ; i<count; i++) {
        sprintf(name + strlen(name), "-%lu",
                *GetSidSubAuthority(sid, i) );
    }

    return name;
}

/*
 * Returns a string to represent the given security identifier (SID).
 * If the account is known to the local computer then the account
 * domain is returned. The format will be \\name or domain\\name depending
 * on if the computer belongs to a domain.
 * If the account name is not known then the textual representation of
 * SID is returned -- eg: S-1-5-21-2818032319-470147023-1036452850-13037.
 */
static char *getSIDString(SID* sid) {
    char domain[255];
    char name[255];
    DWORD domainLen = sizeof(domain);
    DWORD nameLen = sizeof(name);
    SID_NAME_USE use;

    if(!IsValidSid(sid)) {
        return strdup("<Invalid SID>");
    }

    if (LookupAccountSid(NULL, sid, name, &nameLen, domain, &domainLen, &use)) {
        size_t len = strlen(name) + strlen(domain) + 3;
        char* s = (char*)malloc(len);
        if (s != NULL) {
            strcpy(s, domain);
            strcat(s, "\\\\");
            strcat(s, name);
        }
        return s;
    } else {
        return getTextualSid(sid);
    }
}



/*
 * Returns 1 if the specified file is on a file system that supports
 * persistent ACLs (On NTFS file systems returns true, on FAT32 file systems
 * returns false), otherwise 0. Returns -1 if error.
 */
static int isSecuritySupported(const char* path) {
    char* root;
    char* p;
    BOOL res;
    DWORD dwMaxComponentLength;
    DWORD dwFlags;
    char fsName[128];
    DWORD fsNameLength;

    /*
     * Get root directory. For UNCs the slash after the share name is required.
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
                fprintf(stderr, "Malformed UNC");
                return -1;
            }
            p = p2;
            slashskip--;
        }
        if (slashskip != 0) {
            free(root);
            fprintf(stderr, "Malformed UNC");
            return -1;
        }
        p++;
        *p = '\0';

    } else {
        p = strchr(root, '\\');

        /*
         * Relative path so use current directory
         */
        if (p == NULL) {
            free(root);
            root = malloc(255);
            if (GetCurrentDirectory(255, root) == 0) {
                printLastError("GetCurrentDirectory failed");
                return -1;
            }
            p = strchr(root, '\\');
            if (p == NULL) {
                fprintf(stderr, "GetCurrentDirectory doesn't include drive letter!!!!\n");
                return -1;
            }
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
        printLastError("GetVolumeInformation failed");
        free(root);
        return -1;
    }

    free(root);
    return (dwFlags & FS_PERSISTENT_ACLS) ? 1 : 0;
}


/*
 * Returns the security descriptor for a file.
 */
static SECURITY_DESCRIPTOR* getFileSecurityDescriptor(const char* path) {
    SECURITY_DESCRIPTOR* sd;
    DWORD len = 0;
    SECURITY_INFORMATION info =
        OWNER_SECURITY_INFORMATION | DACL_SECURITY_INFORMATION;

    GetFileSecurity(path, info , 0, 0, &len);
    if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
        printLastError("GetFileSecurity failed");
        return NULL;
    }
    sd = (SECURITY_DESCRIPTOR *)malloc(len);
    if (sd == NULL) {
        fprintf(stderr, "Out of memory");
    } else {
        if (!GetFileSecurity(path, info, sd, len, &len)) {
            printLastError("GetFileSecurity failed");
            free(sd);
            return NULL;
        }
    }
    return sd;
}


/*
 * Revoke all access to the specific file
 */
static int revokeAll(const char* path) {
    SECURITY_DESCRIPTOR* sd;
    SID* owner;
    ACL *acl;
    BOOL defaulted, present;
    ACL_SIZE_INFORMATION acl_size_info;
    DWORD i, count;
    char* str;

    /*
     * Get security descriptor for file; From security descriptor get the
     * owner SID, and the DACL.
     */
    sd = getFileSecurityDescriptor(path);
    if (sd == NULL) {
        return -1;      /* error already reported */
    }
    if (!GetSecurityDescriptorOwner(sd, &owner, &defaulted)) {
        printLastError("GetSecurityDescriptorOwner failed");
        return -1;
    }
    str = getSIDString(owner);
    if (str != NULL) {
        printf("owner: %s\n", str);
        free(str);
    }
    if (!GetSecurityDescriptorDacl(sd, &present, &acl, &defaulted)) {
        printLastError("GetSecurityDescriptorDacl failed");
        return -1;
    }
    if (!present) {
        fprintf(stderr, "Security descriptor does not contain a DACL");
        return -1;
    }

    /*
     * If DACL is NULL there is no access to the file - we are done
     */
    if (acl == NULL) {
        return 1;
    }

    /*
     * Iterate over the ACEs. For each "allow" type check that the SID
     * matches the owner - if not we remove the ACE from the ACL
     */
    if (!GetAclInformation(acl, (void *) &acl_size_info, sizeof(acl_size_info),
                                  AclSizeInformation)) {
        printLastError("GetAclInformation failed");
        return -1;
    }
    count = acl_size_info.AceCount;
    i = 0;
    while (count > 0) {
        void* ace;
        ACCESS_ALLOWED_ACE *access;
        SID* sid;
        BOOL deleted;

        if (!GetAce(acl, i, &ace)) {
            printLastError("GetAce failed");
            return -1;
        }
        if (((ACCESS_ALLOWED_ACE *)ace)->Header.AceType != ACCESS_ALLOWED_ACE_TYPE) {
            i++;
            count--;
            continue;
        }
        access = (ACCESS_ALLOWED_ACE *)ace;
        sid = (SID *) &access->SidStart;


        deleted = FALSE;
        if (!EqualSid(owner, sid)) {
            /*
             * If the ACE allows any access then the file then we
             * delete it.
             */
            if (access->Mask & ANY_ACCESS) {
                str = getSIDString(sid);
                if (str != NULL) {
                    printf("remove ALLOW %s\n", str);
                    free(str);
                }
                if (DeleteAce(acl, i) == 0) {
                    printLastError("DeleteAce failed");
                    return -1;
                }
                deleted = TRUE;
            }
        }

        if (!deleted) {
            str = getSIDString(sid);
            if (str != NULL) {
                printf("ALLOW %s (access mask=%x)\n", str, access->Mask);
                free(str);
            }

            /* onto the next ACE */
            i++;
        }
        count--;
    }

    /*
     * No changes - only owner has access
     */
    if (i == acl_size_info.AceCount) {
        printf("No changes.\n");
        return 1;
    }

    /*
     * Create security descriptor and set its DACL to the version
     * that we just edited
     */
    if (!InitializeSecurityDescriptor(sd, SECURITY_DESCRIPTOR_REVISION)) {
        printLastError("InitializeSecurityDescriptor failed");
        return -1;
    }
    if (!SetSecurityDescriptorDacl(sd, present, acl, defaulted)) {
        printLastError("SetSecurityDescriptorDacl failed");
        return -1;
    }
    if (!SetFileSecurity(path, DACL_SECURITY_INFORMATION, sd)) {
        printLastError("SetFileSecurity failed");
        return -1;
    }

    printf("File updated.\n");

    return 1;
}

/*
 * Convert slashes in the pathname to backslashes if needed.
 */
static char* convert_path(const char* p) {
   int i = 0;
   char* path = strdup(p);
   while (p[i] != '\0') {
       if (p[i] == '/') {
           path[i] = '\\';
       }
       i++;
   }
   return path;
}

/*
 * Usage: revokeall file
 */
int main( int argc, char *argv[])
{
    int rc;
    const char* path;

    if (argc != 2) {
        fprintf(stderr, "Usage: %s file\n", argv[0]);
        return -1;
    }
    path = convert_path(argv[1]);
    printf("Revoking all non-owner access to %s\n", path);
    rc = isSecuritySupported(path);
    if (rc != 1) {
        if (rc == 0) {
            printf("File security not supported on this file system\n");
        }
        return rc;
    } else {
        return revokeAll(path);
    }
}
