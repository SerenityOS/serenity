/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <direct.h>
#include <malloc.h>
#include <io.h>
#include <windows.h>
#include <aclapi.h>
#include <winioctl.h>
#include <Sddl.h>

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"

#include "sun_nio_fs_WindowsNativeDispatcher.h"

/**
 * jfieldIDs
 */
static jfieldID findFirst_handle;
static jfieldID findFirst_name;
static jfieldID findFirst_attributes;

static jfieldID findStream_handle;
static jfieldID findStream_name;

static jfieldID volumeInfo_fsName;
static jfieldID volumeInfo_volName;
static jfieldID volumeInfo_volSN;
static jfieldID volumeInfo_flags;

static jfieldID diskSpace_bytesAvailable;
static jfieldID diskSpace_totalBytes;
static jfieldID diskSpace_totalFree;

static jfieldID diskSpace_bytesPerSector;

static jfieldID account_domain;
static jfieldID account_name;
static jfieldID account_use;

static jfieldID aclInfo_aceCount;

static jfieldID completionStatus_error;
static jfieldID completionStatus_bytesTransferred;
static jfieldID completionStatus_completionKey;

static void throwWindowsException(JNIEnv* env, DWORD lastError) {
    jobject x = JNU_NewObjectByName(env, "sun/nio/fs/WindowsException",
        "(I)V", lastError);
    if (x != NULL) {
        (*env)->Throw(env, x);
    }
}

/**
 * Initializes jfieldIDs and get address of Win32 calls that are located
 * at runtime.
 */
JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_initIDs(JNIEnv* env, jclass this)
{
    jclass clazz;

    clazz = (*env)->FindClass(env, "sun/nio/fs/WindowsNativeDispatcher$FirstFile");
    CHECK_NULL(clazz);
    findFirst_handle = (*env)->GetFieldID(env, clazz, "handle", "J");
    CHECK_NULL(findFirst_handle);
    findFirst_name = (*env)->GetFieldID(env, clazz, "name", "Ljava/lang/String;");
    CHECK_NULL(findFirst_name);
    findFirst_attributes = (*env)->GetFieldID(env, clazz, "attributes", "I");
    CHECK_NULL(findFirst_attributes);

    clazz = (*env)->FindClass(env, "sun/nio/fs/WindowsNativeDispatcher$FirstStream");
    CHECK_NULL(clazz);
    findStream_handle = (*env)->GetFieldID(env, clazz, "handle", "J");
    CHECK_NULL(findStream_handle);
    findStream_name = (*env)->GetFieldID(env, clazz, "name", "Ljava/lang/String;");
    CHECK_NULL(findStream_name);

    clazz = (*env)->FindClass(env, "sun/nio/fs/WindowsNativeDispatcher$VolumeInformation");
    CHECK_NULL(clazz);
    volumeInfo_fsName = (*env)->GetFieldID(env, clazz, "fileSystemName", "Ljava/lang/String;");
    CHECK_NULL(volumeInfo_fsName);
    volumeInfo_volName = (*env)->GetFieldID(env, clazz, "volumeName", "Ljava/lang/String;");
    CHECK_NULL(volumeInfo_volName);
    volumeInfo_volSN = (*env)->GetFieldID(env, clazz, "volumeSerialNumber", "I");
    CHECK_NULL(volumeInfo_volSN);
    volumeInfo_flags = (*env)->GetFieldID(env, clazz, "flags", "I");
    CHECK_NULL(volumeInfo_flags);

    clazz = (*env)->FindClass(env, "sun/nio/fs/WindowsNativeDispatcher$DiskFreeSpace");
    CHECK_NULL(clazz);
    diskSpace_bytesAvailable = (*env)->GetFieldID(env, clazz, "freeBytesAvailable", "J");
    CHECK_NULL(diskSpace_bytesAvailable);
    diskSpace_totalBytes = (*env)->GetFieldID(env, clazz, "totalNumberOfBytes", "J");
    CHECK_NULL(diskSpace_totalBytes);
    diskSpace_totalFree = (*env)->GetFieldID(env, clazz, "totalNumberOfFreeBytes", "J");
    CHECK_NULL(diskSpace_totalFree);
    diskSpace_bytesPerSector = (*env)->GetFieldID(env, clazz, "bytesPerSector", "J");
    CHECK_NULL(diskSpace_bytesPerSector);

    clazz = (*env)->FindClass(env, "sun/nio/fs/WindowsNativeDispatcher$Account");
    CHECK_NULL(clazz);
    account_domain = (*env)->GetFieldID(env, clazz, "domain", "Ljava/lang/String;");
    CHECK_NULL(account_domain);
    account_name = (*env)->GetFieldID(env, clazz, "name", "Ljava/lang/String;");
    CHECK_NULL(account_name);
    account_use = (*env)->GetFieldID(env, clazz, "use", "I");
    CHECK_NULL(account_use);

    clazz = (*env)->FindClass(env, "sun/nio/fs/WindowsNativeDispatcher$AclInformation");
    CHECK_NULL(clazz);
    aclInfo_aceCount = (*env)->GetFieldID(env, clazz, "aceCount", "I");
    CHECK_NULL(aclInfo_aceCount);

    clazz = (*env)->FindClass(env, "sun/nio/fs/WindowsNativeDispatcher$CompletionStatus");
    CHECK_NULL(clazz);
    completionStatus_error = (*env)->GetFieldID(env, clazz, "error", "I");
    CHECK_NULL(completionStatus_error);
    completionStatus_bytesTransferred = (*env)->GetFieldID(env, clazz, "bytesTransferred", "I");
    CHECK_NULL(completionStatus_bytesTransferred);
    completionStatus_completionKey = (*env)->GetFieldID(env, clazz, "completionKey", "J");
    CHECK_NULL(completionStatus_completionKey);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CreateEvent(JNIEnv* env, jclass this,
    jboolean bManualReset, jboolean bInitialState)
{
    HANDLE hEvent = CreateEventW(NULL, bManualReset, bInitialState, NULL);
    if (hEvent == NULL) {
        throwWindowsException(env, GetLastError());
    }
    return ptr_to_jlong(hEvent);
}

JNIEXPORT jstring JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_FormatMessage(JNIEnv* env, jclass this, jint errorCode) {
    WCHAR message[255];

    DWORD len = FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM,
                               NULL,
                               (DWORD)errorCode,
                               0,
                               &message[0],
                               255,
                               NULL);


    if (len == 0) {
        return NULL;
    } else {
        if (len > 3) {
            // Drop final '.', CR, LF
            if (message[len - 1] == L'\n') len--;
            if (message[len - 1] == L'\r') len--;
            if (message[len - 1] == L'.') len--;
            message[len] = L'\0';
        }

        return (*env)->NewString(env, (const jchar *)message, (jsize)wcslen(message));
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_LocalFree(JNIEnv* env, jclass this, jlong address)
{
    HLOCAL hMem = (HLOCAL)jlong_to_ptr(address);
    LocalFree(hMem);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CreateFile0(JNIEnv* env, jclass this,
    jlong address, jint dwDesiredAccess, jint dwShareMode, jlong sdAddress,
    jint dwCreationDisposition, jint dwFlagsAndAttributes)
{
    HANDLE handle;
    LPCWSTR lpFileName = jlong_to_ptr(address);

    SECURITY_ATTRIBUTES securityAttributes;
    LPSECURITY_ATTRIBUTES lpSecurityAttributes;
    PSECURITY_DESCRIPTOR lpSecurityDescriptor = jlong_to_ptr(sdAddress);


    if (lpSecurityDescriptor == NULL) {
        lpSecurityAttributes = NULL;
    } else {
        securityAttributes.nLength = sizeof(SECURITY_ATTRIBUTES);
        securityAttributes.lpSecurityDescriptor = lpSecurityDescriptor;
        securityAttributes.bInheritHandle = FALSE;
        lpSecurityAttributes = &securityAttributes;
    }

    handle = CreateFileW(lpFileName,
                        (DWORD)dwDesiredAccess,
                        (DWORD)dwShareMode,
                        lpSecurityAttributes,
                        (DWORD)dwCreationDisposition,
                        (DWORD)dwFlagsAndAttributes,
                        NULL);
    if (handle == INVALID_HANDLE_VALUE) {
        throwWindowsException(env, GetLastError());
    }
    return ptr_to_jlong(handle);
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_DeviceIoControlSetSparse(JNIEnv* env, jclass this,
    jlong handle)
{
    DWORD bytesReturned;
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    if (DeviceIoControl(h, FSCTL_SET_SPARSE, NULL, 0, NULL, 0, &bytesReturned, NULL) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_DeviceIoControlGetReparsePoint(JNIEnv* env, jclass this,
    jlong handle, jlong bufferAddress, jint bufferSize)
{
    DWORD bytesReturned;
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    LPVOID outBuffer = (LPVOID)jlong_to_ptr(bufferAddress);

    if (DeviceIoControl(h, FSCTL_GET_REPARSE_POINT, NULL, 0, outBuffer, (DWORD)bufferSize,
                        &bytesReturned, NULL) == 0)
    {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_DeleteFile0(JNIEnv* env, jclass this, jlong address)
{
    LPCWSTR lpFileName = jlong_to_ptr(address);
    if (DeleteFileW(lpFileName) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CreateDirectory0(JNIEnv* env, jclass this,
    jlong address, jlong sdAddress)
{
    LPCWSTR lpFileName = jlong_to_ptr(address);

    SECURITY_ATTRIBUTES securityAttributes;
    LPSECURITY_ATTRIBUTES lpSecurityAttributes;
    PSECURITY_DESCRIPTOR lpSecurityDescriptor = jlong_to_ptr(sdAddress);


    if (lpSecurityDescriptor == NULL) {
        lpSecurityAttributes = NULL;
    } else {
        securityAttributes.nLength = sizeof(SECURITY_ATTRIBUTES);
        securityAttributes.lpSecurityDescriptor = lpSecurityDescriptor;
        securityAttributes.bInheritHandle = FALSE;
        lpSecurityAttributes = &securityAttributes;
    }

    if (CreateDirectoryW(lpFileName, lpSecurityAttributes) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_RemoveDirectory0(JNIEnv* env, jclass this, jlong address)
{
    LPCWSTR lpFileName = jlong_to_ptr(address);
    if (RemoveDirectoryW(lpFileName) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CloseHandle(JNIEnv* env, jclass this,
    jlong handle)
{
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    CloseHandle(h);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetFileSizeEx(JNIEnv *env,
    jclass this, jlong handle)
{
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    LARGE_INTEGER size;
    if (GetFileSizeEx(h, &size) == 0) {
        throwWindowsException(env, GetLastError());
    }
    return long_to_jlong(size.QuadPart);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_FindFirstFile0(JNIEnv* env, jclass this,
    jlong address, jobject obj)
{
    WIN32_FIND_DATAW data;
    LPCWSTR lpFileName = jlong_to_ptr(address);

    HANDLE handle = FindFirstFileW(lpFileName, &data);
    if (handle != INVALID_HANDLE_VALUE) {
        jstring name = (*env)->NewString(env, data.cFileName, (jsize)wcslen(data.cFileName));
        if (name == NULL) {
            FindClose(handle);
            return;
        }
        (*env)->SetLongField(env, obj, findFirst_handle, ptr_to_jlong(handle));
        (*env)->SetObjectField(env, obj, findFirst_name, name);
        (*env)->SetIntField(env, obj, findFirst_attributes, data.dwFileAttributes);
    } else {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_FindFirstFile1(JNIEnv* env, jclass this,
    jlong pathAddress, jlong dataAddress)
{
    LPCWSTR lpFileName = jlong_to_ptr(pathAddress);
    WIN32_FIND_DATAW* data = (WIN32_FIND_DATAW*)jlong_to_ptr(dataAddress);

    HANDLE handle = FindFirstFileW(lpFileName, data);
    if (handle == INVALID_HANDLE_VALUE) {
        throwWindowsException(env, GetLastError());
    }
    return ptr_to_jlong(handle);
}

JNIEXPORT jstring JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_FindNextFile(JNIEnv* env, jclass this,
    jlong handle, jlong dataAddress)
{
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    WIN32_FIND_DATAW* data = (WIN32_FIND_DATAW*)jlong_to_ptr(dataAddress);

    if (FindNextFileW(h, data) != 0) {
        return (*env)->NewString(env, data->cFileName, (jsize)wcslen(data->cFileName));
    } else {
    if (GetLastError() != ERROR_NO_MORE_FILES)
        throwWindowsException(env, GetLastError());
        return NULL;
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_FindFirstStream0(JNIEnv* env, jclass this,
    jlong address, jobject obj)
{
    WIN32_FIND_STREAM_DATA data;
    LPCWSTR lpFileName = jlong_to_ptr(address);
    HANDLE handle;

    handle = FindFirstStreamW(lpFileName, FindStreamInfoStandard, &data, 0);
    if (handle != INVALID_HANDLE_VALUE) {
        jstring name = (*env)->NewString(env, data.cStreamName, (jsize)wcslen(data.cStreamName));
        if (name == NULL) {
            FindClose(handle);
            return;
        }
        (*env)->SetLongField(env, obj, findStream_handle, ptr_to_jlong(handle));
        (*env)->SetObjectField(env, obj, findStream_name, name);
    } else {
        if (GetLastError() == ERROR_HANDLE_EOF) {
             (*env)->SetLongField(env, obj, findStream_handle, ptr_to_jlong(handle));
        } else {
            throwWindowsException(env, GetLastError());
        }
    }

}

JNIEXPORT jstring JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_FindNextStream(JNIEnv* env, jclass this,
    jlong handle)
{
    WIN32_FIND_STREAM_DATA data;
    HANDLE h = (HANDLE)jlong_to_ptr(handle);

    if (FindNextStreamW(h, &data) != 0) {
        return (*env)->NewString(env, data.cStreamName, (jsize)wcslen(data.cStreamName));
    } else {
        if (GetLastError() != ERROR_HANDLE_EOF)
            throwWindowsException(env, GetLastError());
        return NULL;
    }
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_FindClose(JNIEnv* env, jclass this,
    jlong handle)
{
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    if (FindClose(h) == 0) {
        throwWindowsException(env, GetLastError());
    }
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetFileInformationByHandle(JNIEnv* env, jclass this,
    jlong handle, jlong address)
{
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    BY_HANDLE_FILE_INFORMATION* info =
        (BY_HANDLE_FILE_INFORMATION*)jlong_to_ptr(address);
    if (GetFileInformationByHandle(h, info) == 0) {
        throwWindowsException(env, GetLastError());
    }
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CopyFileEx0(JNIEnv* env, jclass this,
    jlong existingAddress, jlong newAddress, jint flags, jlong cancelAddress)
{
    LPCWSTR lpExistingFileName = jlong_to_ptr(existingAddress);
    LPCWSTR lpNewFileName = jlong_to_ptr(newAddress);
    LPBOOL cancel = (LPBOOL)jlong_to_ptr(cancelAddress);
    if (CopyFileExW(lpExistingFileName, lpNewFileName, NULL, NULL, cancel,
                    (DWORD)flags) == 0)
    {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_MoveFileEx0(JNIEnv* env, jclass this,
    jlong existingAddress, jlong newAddress, jint flags)
{
    LPCWSTR lpExistingFileName = jlong_to_ptr(existingAddress);
    LPCWSTR lpNewFileName = jlong_to_ptr(newAddress);
    if (MoveFileExW(lpExistingFileName, lpNewFileName, (DWORD)flags) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetLogicalDrives(JNIEnv* env, jclass this)
{
    DWORD res = GetLogicalDrives();
    if (res == 0) {
        throwWindowsException(env, GetLastError());
    }
    return (jint)res;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetFileAttributes0(JNIEnv* env, jclass this,
    jlong address)
{
    LPCWSTR lpFileName = jlong_to_ptr(address);
    DWORD value = GetFileAttributesW(lpFileName);

    if (value == INVALID_FILE_ATTRIBUTES) {
        throwWindowsException(env, GetLastError());
    }
    return (jint)value;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_SetFileAttributes0(JNIEnv* env, jclass this,
    jlong address, jint value)
{
    LPCWSTR lpFileName = jlong_to_ptr(address);
    if (SetFileAttributesW(lpFileName, (DWORD)value) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetFileAttributesEx0(JNIEnv* env, jclass this,
    jlong pathAddress, jlong dataAddress)
{
    LPCWSTR lpFileName = jlong_to_ptr(pathAddress);
    WIN32_FILE_ATTRIBUTE_DATA* data = (WIN32_FILE_ATTRIBUTE_DATA*)jlong_to_ptr(dataAddress);

    BOOL res = GetFileAttributesExW(lpFileName, GetFileExInfoStandard, (LPVOID)data);
    if (res == 0)
        throwWindowsException(env, GetLastError());
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_SetFileTime(JNIEnv* env, jclass this,
    jlong handle, jlong createTime, jlong lastAccessTime, jlong lastWriteTime)
{
    HANDLE h = (HANDLE)jlong_to_ptr(handle);

    if (SetFileTime(h,
        (createTime == (jlong)-1) ? NULL : (CONST FILETIME *)&createTime,
        (lastAccessTime == (jlong)-1) ? NULL : (CONST FILETIME *)&lastAccessTime,
        (lastWriteTime == (jlong)-1) ? NULL : (CONST FILETIME *)&lastWriteTime) == 0)
    {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_SetEndOfFile(JNIEnv* env, jclass this,
    jlong handle)
{
    HANDLE h = (HANDLE)jlong_to_ptr(handle);

    if (SetEndOfFile(h) == 0)
        throwWindowsException(env, GetLastError());
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetVolumeInformation0(JNIEnv* env, jclass this,
    jlong address, jobject obj)
{
    WCHAR volumeName[MAX_PATH+1];
    DWORD volumeSerialNumber;
    DWORD maxComponentLength;
    DWORD flags;
    WCHAR fileSystemName[MAX_PATH+1];
    LPCWSTR lpFileName = jlong_to_ptr(address);
    jstring str;

    BOOL res = GetVolumeInformationW(lpFileName,
                                     &volumeName[0],
                                     MAX_PATH+1,
                                     &volumeSerialNumber,
                                     &maxComponentLength,
                                     &flags,
                                     &fileSystemName[0],
                                     MAX_PATH+1);
    if (res == 0) {
        throwWindowsException(env, GetLastError());
        return;
    }

    str = (*env)->NewString(env, (const jchar *)fileSystemName, (jsize)wcslen(fileSystemName));
    if (str == NULL) return;
    (*env)->SetObjectField(env, obj, volumeInfo_fsName, str);

    str = (*env)->NewString(env, (const jchar *)volumeName, (jsize)wcslen(volumeName));
    if (str == NULL) return;
    (*env)->SetObjectField(env, obj, volumeInfo_volName, str);

    (*env)->SetIntField(env, obj, volumeInfo_volSN, (jint)volumeSerialNumber);
    (*env)->SetIntField(env, obj, volumeInfo_flags, (jint)flags);
}


JNIEXPORT jint JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetDriveType0(JNIEnv* env, jclass this, jlong address) {
    LPCWSTR lpRootPathName = jlong_to_ptr(address);
    return (jint)GetDriveTypeW(lpRootPathName);
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetDiskFreeSpaceEx0(JNIEnv* env, jclass this,
    jlong address, jobject obj)
{
    ULARGE_INTEGER freeBytesAvailable;
    ULARGE_INTEGER totalNumberOfBytes;
    ULARGE_INTEGER totalNumberOfFreeBytes;
    LPCWSTR lpDirName = jlong_to_ptr(address);


    BOOL res = GetDiskFreeSpaceExW(lpDirName,
                                   &freeBytesAvailable,
                                   &totalNumberOfBytes,
                                   &totalNumberOfFreeBytes);
    if (res == 0) {
        throwWindowsException(env, GetLastError());
        return;
    }

    (*env)->SetLongField(env, obj, diskSpace_bytesAvailable,
        long_to_jlong(freeBytesAvailable.QuadPart));
    (*env)->SetLongField(env, obj, diskSpace_totalBytes,
        long_to_jlong(totalNumberOfBytes.QuadPart));
    (*env)->SetLongField(env, obj, diskSpace_totalFree,
        long_to_jlong(totalNumberOfFreeBytes.QuadPart));
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetDiskFreeSpace0(JNIEnv* env, jclass this,
    jlong address, jobject obj)
{
    DWORD sectorsPerCluster;
    DWORD bytesPerSector;
    DWORD numberOfFreeClusters;
    DWORD totalNumberOfClusters;
    LPCWSTR lpRootPathName = jlong_to_ptr(address);


    BOOL res = GetDiskFreeSpaceW(lpRootPathName,
                                 &sectorsPerCluster,
                                 &bytesPerSector,
                                 &numberOfFreeClusters,
                                 &totalNumberOfClusters);
    if (res == 0) {
        throwWindowsException(env, GetLastError());
        return;
    }

    (*env)->SetLongField(env, obj, diskSpace_bytesPerSector,
        long_to_jlong(bytesPerSector));
}

JNIEXPORT jstring JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetVolumePathName0(JNIEnv* env, jclass this,
    jlong address)
{
    WCHAR volumeName[MAX_PATH+1];
    LPCWSTR lpFileName = jlong_to_ptr(address);


    BOOL res = GetVolumePathNameW(lpFileName,
                                  &volumeName[0],
                                  MAX_PATH+1);
    if (res == 0) {
        throwWindowsException(env, GetLastError());
        return NULL;
    } else {
        return (*env)->NewString(env, (const jchar *)volumeName, (jsize)wcslen(volumeName));
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_InitializeSecurityDescriptor(JNIEnv* env, jclass this,
    jlong address)
{
    PSECURITY_DESCRIPTOR pSecurityDescriptor =
        (PSECURITY_DESCRIPTOR)jlong_to_ptr(address);

    if (InitializeSecurityDescriptor(pSecurityDescriptor, SECURITY_DESCRIPTOR_REVISION) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_InitializeAcl(JNIEnv* env, jclass this,
    jlong address, jint size)
{
    PACL pAcl = (PACL)jlong_to_ptr(address);

    if (InitializeAcl(pAcl, (DWORD)size, ACL_REVISION) == 0) {
        throwWindowsException(env, GetLastError());
    }
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_SetFileSecurity0(JNIEnv* env, jclass this,
    jlong pathAddress, jint requestedInformation, jlong descAddress)
{
    LPCWSTR lpFileName = jlong_to_ptr(pathAddress);
    PSECURITY_DESCRIPTOR pSecurityDescriptor = jlong_to_ptr(descAddress);
    DWORD lengthNeeded = 0;

    BOOL res = SetFileSecurityW(lpFileName,
                                (SECURITY_INFORMATION)requestedInformation,
                                pSecurityDescriptor);

    if (res == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetFileSecurity0(JNIEnv* env, jclass this,
    jlong pathAddress, jint requestedInformation, jlong descAddress, jint nLength)
{
    LPCWSTR lpFileName = jlong_to_ptr(pathAddress);
    PSECURITY_DESCRIPTOR pSecurityDescriptor = jlong_to_ptr(descAddress);
    DWORD lengthNeeded = 0;

    BOOL res = GetFileSecurityW(lpFileName,
                                (SECURITY_INFORMATION)requestedInformation,
                                pSecurityDescriptor,
                                (DWORD)nLength,
                                &lengthNeeded);

    if (res == 0) {
        if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
            return (jint)lengthNeeded;
        } else {
            throwWindowsException(env, GetLastError());
            return 0;
        }
    } else {
        return (jint)nLength;
    }
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetSecurityDescriptorOwner(JNIEnv* env,
    jclass this, jlong address)
{
    PSECURITY_DESCRIPTOR pSecurityDescriptor = jlong_to_ptr(address);
    PSID pOwner;
    BOOL bOwnerDefaulted;


    if (GetSecurityDescriptorOwner(pSecurityDescriptor, &pOwner, &bOwnerDefaulted) == 0) {
        throwWindowsException(env, GetLastError());
    }
    return ptr_to_jlong(pOwner);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_SetSecurityDescriptorOwner(JNIEnv* env,
    jclass this, jlong descAddress, jlong ownerAddress)
{
    PSECURITY_DESCRIPTOR pSecurityDescriptor = jlong_to_ptr(descAddress);
    PSID pOwner = jlong_to_ptr(ownerAddress);

    if (SetSecurityDescriptorOwner(pSecurityDescriptor, pOwner, FALSE) == 0) {
        throwWindowsException(env, GetLastError());
    }
}


JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetSecurityDescriptorDacl(JNIEnv* env,
    jclass this, jlong address)
{
    PSECURITY_DESCRIPTOR pSecurityDescriptor = jlong_to_ptr(address);
    BOOL bDaclPresent;
    PACL pDacl;
    BOOL bDaclDefaulted;

    if (GetSecurityDescriptorDacl(pSecurityDescriptor, &bDaclPresent, &pDacl, &bDaclDefaulted) == 0) {
        throwWindowsException(env, GetLastError());
        return (jlong)0;
    } else {
        return (bDaclPresent) ? ptr_to_jlong(pDacl) : (jlong)0;
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_SetSecurityDescriptorDacl(JNIEnv* env,
    jclass this, jlong descAddress, jlong aclAddress)
{
    PSECURITY_DESCRIPTOR pSecurityDescriptor = (PSECURITY_DESCRIPTOR)jlong_to_ptr(descAddress);
    PACL pAcl = (PACL)jlong_to_ptr(aclAddress);

    if (SetSecurityDescriptorDacl(pSecurityDescriptor, TRUE, pAcl, FALSE) == 0) {
        throwWindowsException(env, GetLastError());
    }
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetAclInformation0(JNIEnv* env,
    jclass this, jlong address, jobject obj)
{
    PACL pAcl = (PACL)jlong_to_ptr(address);
    ACL_SIZE_INFORMATION acl_size_info;

    if (GetAclInformation(pAcl, (void *) &acl_size_info, sizeof(acl_size_info), AclSizeInformation) == 0) {
        throwWindowsException(env, GetLastError());
    } else {
        (*env)->SetIntField(env, obj, aclInfo_aceCount, (jint)acl_size_info.AceCount);
    }
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetAce(JNIEnv* env, jclass this, jlong address,
    jint aceIndex)
{
    PACL pAcl = (PACL)jlong_to_ptr(address);
    LPVOID pAce;

    if (GetAce(pAcl, (DWORD)aceIndex, &pAce) == 0) {
        throwWindowsException(env, GetLastError());
        return (jlong)0;
    } else {
        return ptr_to_jlong(pAce);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_AddAccessAllowedAceEx(JNIEnv* env,
    jclass this, jlong aclAddress, jint flags, jint mask, jlong sidAddress)
{
    PACL pAcl = (PACL)jlong_to_ptr(aclAddress);
    PSID pSid = (PSID)jlong_to_ptr(sidAddress);

    if (AddAccessAllowedAceEx(pAcl, ACL_REVISION, (DWORD)flags, (DWORD)mask, pSid) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_AddAccessDeniedAceEx(JNIEnv* env,
    jclass this, jlong aclAddress, jint flags, jint mask, jlong sidAddress)
{
    PACL pAcl = (PACL)jlong_to_ptr(aclAddress);
    PSID pSid = (PSID)jlong_to_ptr(sidAddress);

    if (AddAccessDeniedAceEx(pAcl, ACL_REVISION, (DWORD)flags, (DWORD)mask, pSid) == 0) {
        throwWindowsException(env, GetLastError());
    }
}


JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_LookupAccountSid0(JNIEnv* env,
    jclass this, jlong address, jobject obj)
{
    WCHAR domain[255];
    WCHAR name[255];
    DWORD domainLen = sizeof(domain);
    DWORD nameLen = sizeof(name);
    SID_NAME_USE use;
    PSID sid = jlong_to_ptr(address);
    jstring s;

    if (LookupAccountSidW(NULL, sid, &name[0], &nameLen, &domain[0], &domainLen, &use) == 0) {
        throwWindowsException(env, GetLastError());
        return;
    }

    s = (*env)->NewString(env, (const jchar *)domain, (jsize)wcslen(domain));
    if (s == NULL)
        return;
    (*env)->SetObjectField(env, obj, account_domain, s);

    s = (*env)->NewString(env, (const jchar *)name, (jsize)wcslen(name));
    if (s == NULL)
        return;
    (*env)->SetObjectField(env, obj, account_name, s);
    (*env)->SetIntField(env, obj, account_use, (jint)use);
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_LookupAccountName0(JNIEnv* env,
    jclass this, jlong nameAddress, jlong sidAddress, jint cbSid)
{

    LPCWSTR accountName = jlong_to_ptr(nameAddress);
    PSID sid = jlong_to_ptr(sidAddress);
    WCHAR domain[255];
    DWORD domainLen = sizeof(domain);
    SID_NAME_USE use;

    if (LookupAccountNameW(NULL, accountName, sid, (LPDWORD)&cbSid,
                           &domain[0], &domainLen, &use) == 0)
    {
        if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
            throwWindowsException(env, GetLastError());
        }
    }

    return cbSid;
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetLengthSid(JNIEnv* env,
    jclass this, jlong address)
{
    PSID sid = jlong_to_ptr(address);
    return (jint)GetLengthSid(sid);
}


JNIEXPORT jstring JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_ConvertSidToStringSid(JNIEnv* env,
    jclass this, jlong address)
{
    PSID sid = jlong_to_ptr(address);
    LPWSTR string;
    if (ConvertSidToStringSidW(sid, &string) == 0) {
        throwWindowsException(env, GetLastError());
        return NULL;
    } else {
        jstring s = (*env)->NewString(env, (const jchar *)string,
            (jsize)wcslen(string));
        LocalFree(string);
        return s;
    }
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_ConvertStringSidToSid0(JNIEnv* env,
    jclass this, jlong address)
{
    LPWSTR lpStringSid = jlong_to_ptr(address);
    PSID pSid;
    if (ConvertStringSidToSidW(lpStringSid, &pSid) == 0)
        throwWindowsException(env, GetLastError());
    return ptr_to_jlong(pSid);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetCurrentProcess(JNIEnv* env, jclass this) {
    HANDLE hProcess = GetCurrentProcess();
    return ptr_to_jlong(hProcess);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetCurrentThread(JNIEnv* env, jclass this) {
    HANDLE hThread = GetCurrentThread();
    return ptr_to_jlong(hThread);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_OpenProcessToken(JNIEnv* env,
    jclass this, jlong process, jint desiredAccess)
{
    HANDLE hProcess = (HANDLE)jlong_to_ptr(process);
    HANDLE hToken;

    if (OpenProcessToken(hProcess, (DWORD)desiredAccess, &hToken) == 0)
        throwWindowsException(env, GetLastError());
    return ptr_to_jlong(hToken);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_OpenThreadToken(JNIEnv* env,
    jclass this, jlong thread, jint desiredAccess, jboolean openAsSelf)
{
    HANDLE hThread = (HANDLE)jlong_to_ptr(thread);
    HANDLE hToken;
    BOOL bOpenAsSelf = (openAsSelf == JNI_TRUE) ? TRUE : FALSE;

    if (OpenThreadToken(hThread, (DWORD)desiredAccess, bOpenAsSelf, &hToken) == 0) {
        if (GetLastError() == ERROR_NO_TOKEN)
            return (jlong)0;
        throwWindowsException(env, GetLastError());
    }
    return ptr_to_jlong(hToken);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_DuplicateTokenEx(JNIEnv* env,
    jclass this, jlong token, jint desiredAccess)
{
    HANDLE hToken = (HANDLE)jlong_to_ptr(token);
    HANDLE resultToken;
    BOOL res;

    res = DuplicateTokenEx(hToken,
                           (DWORD)desiredAccess,
                           NULL,
                           SecurityImpersonation,
                           TokenImpersonation,
                           &resultToken);
    if (res == 0)
        throwWindowsException(env, GetLastError());
    return ptr_to_jlong(resultToken);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_SetThreadToken(JNIEnv* env,
    jclass this, jlong thread, jlong token)
{
    HANDLE hThread = (HANDLE)jlong_to_ptr(thread);
    HANDLE hToken = (HANDLE)jlong_to_ptr(token);

    if (SetThreadToken(hThread, hToken) == 0)
        throwWindowsException(env, GetLastError());
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetTokenInformation(JNIEnv* env,
    jclass this, jlong token, jint tokenInfoClass, jlong tokenInfo, jint tokenInfoLength)
{
    BOOL res;
    DWORD lengthNeeded;
    HANDLE hToken = (HANDLE)jlong_to_ptr(token);
    LPVOID result = (LPVOID)jlong_to_ptr(tokenInfo);

    res = GetTokenInformation(hToken, (TOKEN_INFORMATION_CLASS)tokenInfoClass, (LPVOID)result,
                              tokenInfoLength, &lengthNeeded);
    if (res == 0) {
        if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
            return (jint)lengthNeeded;
        } else {
            throwWindowsException(env, GetLastError());
            return 0;
        }
    } else {
        return tokenInfoLength;
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_AdjustTokenPrivileges(JNIEnv* env,
    jclass this, jlong token, jlong luid, jint attributes)
{
    TOKEN_PRIVILEGES privs[1];
    HANDLE hToken = (HANDLE)jlong_to_ptr(token);
    PLUID pLuid = (PLUID)jlong_to_ptr(luid);

    privs[0].PrivilegeCount = 1;
    privs[0].Privileges[0].Luid = *pLuid;
    privs[0].Privileges[0].Attributes = (DWORD)attributes;

    if (AdjustTokenPrivileges(hToken, FALSE, &privs[0], 1, NULL, NULL) == 0)
        throwWindowsException(env, GetLastError());
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_AccessCheck(JNIEnv* env,
    jclass this, jlong token, jlong securityInfo, jint accessMask,
    jint genericRead, jint genericWrite, jint genericExecute, jint genericAll)
{
    HANDLE hImpersonatedToken = (HANDLE)jlong_to_ptr(token);
    PSECURITY_DESCRIPTOR security = (PSECURITY_DESCRIPTOR)jlong_to_ptr(securityInfo);
    DWORD checkAccessRights = (DWORD)accessMask;
    GENERIC_MAPPING mapping = {
        genericRead,
        genericWrite,
        genericExecute,
        genericAll};
    PRIVILEGE_SET privileges = {0};
    DWORD privilegesLength = sizeof(privileges);
    DWORD grantedAccess = 0;
    BOOL result = FALSE;

    /* checkAccessRights is in-out parameter */
    MapGenericMask(&checkAccessRights, &mapping);
    if (AccessCheck(security, hImpersonatedToken, checkAccessRights,
            &mapping, &privileges, &privilegesLength, &grantedAccess, &result) == 0)
        throwWindowsException(env, GetLastError());

    return (result == FALSE) ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_LookupPrivilegeValue0(JNIEnv* env,
    jclass this, jlong name)
{
    LPCWSTR lpName = (LPCWSTR)jlong_to_ptr(name);
    PLUID pLuid = LocalAlloc(0, sizeof(LUID));

    if (pLuid == NULL) {
        JNU_ThrowInternalError(env, "Unable to allocate LUID structure");
    } else {
        if (LookupPrivilegeValueW(NULL, lpName, pLuid) == 0) {
            LocalFree(pLuid);
            throwWindowsException(env, GetLastError());
            return (jlong)0;
        }
    }
    return ptr_to_jlong(pLuid);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CreateSymbolicLink0(JNIEnv* env,
    jclass this, jlong linkAddress, jlong targetAddress, jint flags)
{
    LPCWSTR link = jlong_to_ptr(linkAddress);
    LPCWSTR target = jlong_to_ptr(targetAddress);

    if (CreateSymbolicLinkW(link, target, (DWORD)flags) == 0)
        throwWindowsException(env, GetLastError());
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CreateHardLink0(JNIEnv* env,
    jclass this, jlong newFileAddress, jlong existingFileAddress)
{
    LPCWSTR newFile = jlong_to_ptr(newFileAddress);
    LPCWSTR existingFile = jlong_to_ptr(existingFileAddress);

    if (CreateHardLinkW(newFile, existingFile, NULL) == 0)
        throwWindowsException(env, GetLastError());
}

JNIEXPORT jstring JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetFullPathName0(JNIEnv *env,
                                                         jclass clz,
                                                         jlong pathAddress)
{
    jstring rv = NULL;
    WCHAR *lpBuf = NULL;
    WCHAR buf[MAX_PATH];
    DWORD len;
    LPCWSTR lpFileName = jlong_to_ptr(pathAddress);

    len = GetFullPathNameW(lpFileName, MAX_PATH, buf, NULL);
    if (len > 0) {
        if (len < MAX_PATH) {
            rv = (*env)->NewString(env, buf, len);
        } else {
            len += 1;  /* return length does not include terminator */
            lpBuf = (WCHAR*)malloc(len * sizeof(WCHAR));
            if (lpBuf != NULL) {
                len = GetFullPathNameW(lpFileName, len, lpBuf, NULL);
                if (len > 0) {
                    rv = (*env)->NewString(env, lpBuf, len);
                } else {
                    JNU_ThrowInternalError(env, "GetFullPathNameW failed");
                }
                free(lpBuf);
            } else {
                JNU_ThrowOutOfMemoryError(env, "native memory allocation failure");
            }
        }
    } else {
        throwWindowsException(env, GetLastError());
    }

    return rv;
}

JNIEXPORT jstring JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetFinalPathNameByHandle(JNIEnv* env,
    jclass this, jlong handle)
{
    jstring rv = NULL;
    WCHAR *lpBuf = NULL;
    WCHAR path[MAX_PATH];
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    DWORD len;

    len = GetFinalPathNameByHandleW(h, path, MAX_PATH, 0);
    if (len > 0) {
        if (len < MAX_PATH) {
            rv = (*env)->NewString(env, (const jchar *)path, (jsize)len);
        } else {
            len += 1;  /* return length does not include terminator */
            lpBuf = (WCHAR*)malloc(len * sizeof(WCHAR));
            if (lpBuf != NULL) {
                len = GetFinalPathNameByHandleW(h, lpBuf, len, 0);
                if (len > 0)  {
                    rv = (*env)->NewString(env, (const jchar *)lpBuf, (jsize)len);
                } else {
                    JNU_ThrowInternalError(env, "GetFinalPathNameByHandleW failed");
                }
                free(lpBuf);
            } else {
                JNU_ThrowOutOfMemoryError(env, "native memory allocation failure");
            }
        }
    } else {
        throwWindowsException(env, GetLastError());
    }
    return rv;
}

JNIEXPORT jlong JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CreateIoCompletionPort(JNIEnv* env, jclass this,
    jlong fileHandle, jlong existingPort, jlong completionKey)
{
    HANDLE port = CreateIoCompletionPort((HANDLE)jlong_to_ptr(fileHandle),
                                         (HANDLE)jlong_to_ptr(existingPort),
                                         (ULONG_PTR)completionKey,
                                         0);
    if (port == NULL) {
        throwWindowsException(env, GetLastError());
    }
    return ptr_to_jlong(port);
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetQueuedCompletionStatus0(JNIEnv* env, jclass this,
    jlong completionPort, jobject obj)
{
    DWORD bytesTransferred;
    ULONG_PTR completionKey;
    OVERLAPPED *lpOverlapped;
    BOOL res;

    res = GetQueuedCompletionStatus((HANDLE)jlong_to_ptr(completionPort),
                                  &bytesTransferred,
                                  &completionKey,
                                  &lpOverlapped,
                                  INFINITE);
    if (res == 0 && lpOverlapped == NULL) {
        throwWindowsException(env, GetLastError());
    } else {
        DWORD ioResult = (res == 0) ? GetLastError() : 0;
        (*env)->SetIntField(env, obj, completionStatus_error, ioResult);
        (*env)->SetIntField(env, obj, completionStatus_bytesTransferred,
            (jint)bytesTransferred);
        (*env)->SetLongField(env, obj, completionStatus_completionKey,
            (jlong)completionKey);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_PostQueuedCompletionStatus(JNIEnv* env, jclass this,
    jlong completionPort, jlong completionKey)
{
    BOOL res;

    res = PostQueuedCompletionStatus((HANDLE)jlong_to_ptr(completionPort),
                                     (DWORD)0,  /* dwNumberOfBytesTransferred */
                                     (ULONG_PTR)completionKey,
                                     NULL);  /* lpOverlapped */
    if (res == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_CancelIo(JNIEnv* env, jclass this, jlong hFile) {
    if (CancelIo((HANDLE)jlong_to_ptr(hFile)) == 0) {
        throwWindowsException(env, GetLastError());
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_GetOverlappedResult(JNIEnv *env, jclass this,
    jlong hFile, jlong lpOverlapped)
{
    BOOL res;
    DWORD bytesTransferred = -1;

    res = GetOverlappedResult((HANDLE)jlong_to_ptr(hFile),
                              (LPOVERLAPPED)jlong_to_ptr(lpOverlapped),
                              &bytesTransferred,
                              TRUE);
    if (res == 0) {
        throwWindowsException(env, GetLastError());
    }

    return (jint)bytesTransferred;
}

JNIEXPORT void JNICALL
Java_sun_nio_fs_WindowsNativeDispatcher_ReadDirectoryChangesW(JNIEnv* env, jclass this,
    jlong hDirectory, jlong bufferAddress, jint bufferLength, jboolean watchSubTree, jint filter,
    jlong bytesReturnedAddress, jlong pOverlapped)
{
    BOOL res;
    BOOL subtree = (watchSubTree == JNI_TRUE) ? TRUE : FALSE;
    LPOVERLAPPED ov = (LPOVERLAPPED)jlong_to_ptr(pOverlapped);

    res = ReadDirectoryChangesW((HANDLE)jlong_to_ptr(hDirectory),
                                (LPVOID)jlong_to_ptr(bufferAddress),
                                (DWORD)bufferLength,
                                subtree,
                                (DWORD)filter,
                                (LPDWORD)jlong_to_ptr(bytesReturnedAddress),
                                (LPOVERLAPPED)jlong_to_ptr(pOverlapped),
                                NULL);
    if (res == 0) {
        throwWindowsException(env, GetLastError());
    }
}
