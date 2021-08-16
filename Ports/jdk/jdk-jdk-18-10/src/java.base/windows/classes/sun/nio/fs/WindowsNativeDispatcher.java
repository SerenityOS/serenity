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

package sun.nio.fs;

import jdk.internal.misc.Unsafe;

import static sun.nio.fs.WindowsConstants.*;

/**
 * Win32 and library calls.
 */

class WindowsNativeDispatcher {
    private WindowsNativeDispatcher() { }

    /**
     * HANDLE CreateEvent(
     *   LPSECURITY_ATTRIBUTES lpEventAttributes,
     *   BOOL bManualReset,
     *   BOOL bInitialState,
     *   PCTSTR lpName
     * );
     */
    static native long CreateEvent(boolean bManualReset, boolean bInitialState)
        throws WindowsException;

    /**
     * HANDLE CreateFile(
     *   LPCTSTR lpFileName,
     *   DWORD dwDesiredAccess,
     *   DWORD dwShareMode,
     *   LPSECURITY_ATTRIBUTES lpSecurityAttributes,
     *   DWORD dwCreationDisposition,
     *   DWORD dwFlagsAndAttributes,
     *   HANDLE hTemplateFile
     * )
     */
    static long CreateFile(String path,
                           int dwDesiredAccess,
                           int dwShareMode,
                           long lpSecurityAttributes,
                           int dwCreationDisposition,
                           int dwFlagsAndAttributes)
        throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return CreateFile0(buffer.address(),
                               dwDesiredAccess,
                               dwShareMode,
                               lpSecurityAttributes,
                               dwCreationDisposition,
                               dwFlagsAndAttributes);
        } finally {
            buffer.release();
        }
    }
    static long CreateFile(String path,
                           int dwDesiredAccess,
                           int dwShareMode,
                           int dwCreationDisposition,
                           int dwFlagsAndAttributes)
        throws WindowsException
    {
        return CreateFile(path, dwDesiredAccess, dwShareMode, 0L,
                          dwCreationDisposition, dwFlagsAndAttributes);
    }
    private static native long CreateFile0(long lpFileName,
                                           int dwDesiredAccess,
                                           int dwShareMode,
                                           long lpSecurityAttributes,
                                           int dwCreationDisposition,
                                           int dwFlagsAndAttributes)
        throws WindowsException;

    /**
     * CloseHandle(
     *   HANDLE hObject
     * )
     */
    static native void CloseHandle(long handle);

    /**
     * DeleteFile(
     *   LPCTSTR lpFileName
     * )
     */
    static void DeleteFile(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            DeleteFile0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    private static native void DeleteFile0(long lpFileName)
        throws WindowsException;

    /**
     * CreateDirectory(
     *   LPCTSTR lpPathName,
     *   LPSECURITY_ATTRIBUTES lpSecurityAttributes
     * )
     */
    static void CreateDirectory(String path, long lpSecurityAttributes) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            CreateDirectory0(buffer.address(), lpSecurityAttributes);
        } finally {
            buffer.release();
        }
    }
    private static native void CreateDirectory0(long lpFileName, long lpSecurityAttributes)
        throws WindowsException;

    /**
     * RemoveDirectory(
     *   LPCTSTR lpPathName
     * )
     */
    static void RemoveDirectory(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            RemoveDirectory0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    private static native void RemoveDirectory0(long lpFileName)
        throws WindowsException;

    /**
     * Marks a file as a sparse file.
     *
     * DeviceIoControl(
     *   FSCTL_SET_SPARSE
     * )
     */
    static native void DeviceIoControlSetSparse(long handle)
        throws WindowsException;

    /**
     * Retrieves the reparse point data associated with the file or directory.
     *
     * DeviceIoControl(
     *   FSCTL_GET_REPARSE_POINT
     * )
     */
    static native void DeviceIoControlGetReparsePoint(long handle,
        long bufferAddress, int bufferSize) throws WindowsException;

    /**
     * Retrieves the size of the specified file.
     *
     * BOOL GetFileSizeEx(
     *   HANDLE hFile,
     *   PLARGE_INTEGER lpFileSize
     * )
     */
    static native long GetFileSizeEx(long handle) throws WindowsException;

    /**
     * HANDLE FindFirstFile(
     *   LPCTSTR lpFileName,
     *   LPWIN32_FIND_DATA lpFindFileData
     * )
     */
    static FirstFile FindFirstFile(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            FirstFile data = new FirstFile();
            FindFirstFile0(buffer.address(), data);
            return data;
        } finally {
            buffer.release();
        }
    }
    static class FirstFile {
        private long handle;
        private String name;
        private int attributes;

        private FirstFile() { }
        public long handle()    { return handle; }
        public String name()    { return name; }
        public int attributes() { return attributes; }
    }
    private static native void FindFirstFile0(long lpFileName, FirstFile obj)
        throws WindowsException;

    /**
     * HANDLE FindFirstFile(
     *   LPCTSTR lpFileName,
     *   LPWIN32_FIND_DATA lpFindFileData
     * )
     */
    static long FindFirstFile(String path, long address) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return FindFirstFile1(buffer.address(), address);
        } finally {
            buffer.release();
        }
    }
    private static native long FindFirstFile1(long lpFileName, long address)
        throws WindowsException;

    /**
     * FindNextFile(
     *   HANDLE hFindFile,
     *   LPWIN32_FIND_DATA lpFindFileData
     * )
     *
     * @return  lpFindFileData->cFileName or null
     */
    static native String FindNextFile(long handle, long address)
        throws WindowsException;

    /**
     * HANDLE FindFirstStreamW(
     *   LPCWSTR lpFileName,
     *   STREAM_INFO_LEVELS InfoLevel,
     *   LPVOID lpFindStreamData,
     *   DWORD dwFlags
     * )
     */
    static FirstStream FindFirstStream(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            FirstStream data = new FirstStream();
            FindFirstStream0(buffer.address(), data);
            if (data.handle() == WindowsConstants.INVALID_HANDLE_VALUE)
                return null;
            return data;
        } finally {
            buffer.release();
        }
    }
    static class FirstStream {
        private long handle;
        private String name;

        private FirstStream() { }
        public long handle()    { return handle; }
        public String name()    { return name; }
    }
    private static native void FindFirstStream0(long lpFileName, FirstStream obj)
        throws WindowsException;

    /*
     * FindNextStreamW(
     *   HANDLE hFindStream,
     *   LPVOID lpFindStreamData
     * )
     */
    static native String FindNextStream(long handle) throws WindowsException;

    /**
     * FindClose(
     *   HANDLE hFindFile
     * )
     */
    static native void FindClose(long handle) throws WindowsException;

    /**
     * GetFileInformationByHandle(
     *   HANDLE hFile,
     *   LPBY_HANDLE_FILE_INFORMATION lpFileInformation
     * )
     */
    static native void GetFileInformationByHandle(long handle, long address)
        throws WindowsException;

    /**
     * CopyFileEx(
     *   LPCWSTR lpExistingFileName
     *   LPCWSTR lpNewFileName,
     *   LPPROGRESS_ROUTINE lpProgressRoutine
     *   LPVOID lpData,
     *   LPBOOL pbCancel,
     *   DWORD dwCopyFlags
     * )
     */
    static void CopyFileEx(String source, String target, int flags,
                           long addressToPollForCancel)
        throws WindowsException
    {
        NativeBuffer sourceBuffer = asNativeBuffer(source);
        NativeBuffer targetBuffer = asNativeBuffer(target);
        try {
            CopyFileEx0(sourceBuffer.address(), targetBuffer.address(), flags,
                        addressToPollForCancel);
        } finally {
            targetBuffer.release();
            sourceBuffer.release();
        }
    }
    private static native void CopyFileEx0(long existingAddress, long newAddress,
        int flags, long addressToPollForCancel) throws WindowsException;

    /**
     * MoveFileEx(
     *   LPCTSTR lpExistingFileName,
     *   LPCTSTR lpNewFileName,
     *   DWORD dwFlags
     * )
     */
    static void MoveFileEx(String source, String target, int flags)
        throws WindowsException
    {
        NativeBuffer sourceBuffer = asNativeBuffer(source);
        NativeBuffer targetBuffer = asNativeBuffer(target);
        try {
            MoveFileEx0(sourceBuffer.address(), targetBuffer.address(), flags);
        } finally {
            targetBuffer.release();
            sourceBuffer.release();
        }
    }
    private static native void MoveFileEx0(long existingAddress, long newAddress,
        int flags) throws WindowsException;

    /**
     * DWORD GetFileAttributes(
     *   LPCTSTR lpFileName
     * )
     */
    static int GetFileAttributes(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return GetFileAttributes0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    private static native int GetFileAttributes0(long lpFileName)
        throws WindowsException;

    /**
     * SetFileAttributes(
     *   LPCTSTR lpFileName,
     *   DWORD dwFileAttributes
     */
    static void SetFileAttributes(String path, int dwFileAttributes)
        throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            SetFileAttributes0(buffer.address(), dwFileAttributes);
        } finally {
            buffer.release();
        }
    }
    private static native void SetFileAttributes0(long lpFileName,
        int dwFileAttributes) throws WindowsException;

    /**
     * GetFileAttributesEx(
     *   LPCTSTR lpFileName,
     *   GET_FILEEX_INFO_LEVELS fInfoLevelId,
     *   LPVOID lpFileInformation
     * );
     */
    static void GetFileAttributesEx(String path, long address) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            GetFileAttributesEx0(buffer.address(), address);
        } finally {
            buffer.release();
        }
    }
    private static native void GetFileAttributesEx0(long lpFileName, long address)
        throws WindowsException;
    /**
     * SetFileTime(
     *   HANDLE hFile,
     *   CONST FILETIME *lpCreationTime,
     *   CONST FILETIME *lpLastAccessTime,
     *   CONST FILETIME *lpLastWriteTime
     * )
     */
    static native void SetFileTime(long handle,
                                   long createTime,
                                   long lastAccessTime,
                                   long lastWriteTime)
        throws WindowsException;

    /**
     * SetEndOfFile(
     *   HANDLE hFile
     * )
     */
    static native void SetEndOfFile(long handle) throws WindowsException;

    /**
     * DWORD GetLogicalDrives(VOID)
     */
    static native int GetLogicalDrives() throws WindowsException;

    /**
     * GetVolumeInformation(
     *   LPCTSTR lpRootPathName,
     *   LPTSTR lpVolumeNameBuffer,
     *   DWORD nVolumeNameSize,
     *   LPDWORD lpVolumeSerialNumber,
     *   LPDWORD lpMaximumComponentLength,
     *   LPDWORD lpFileSystemFlags,
     *   LPTSTR lpFileSystemNameBuffer,
     *   DWORD nFileSystemNameSize
     * )
     */
    static VolumeInformation GetVolumeInformation(String root)
        throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(root);
        try {
            VolumeInformation info = new VolumeInformation();
            GetVolumeInformation0(buffer.address(), info);
            return info;
        } finally {
            buffer.release();
        }
    }
    static class VolumeInformation {
        private String fileSystemName;
        private String volumeName;
        private int volumeSerialNumber;
        private int flags;
        private VolumeInformation() { }

        public String fileSystemName()      { return fileSystemName; }
        public String volumeName()          { return volumeName; }
        public int volumeSerialNumber()     { return volumeSerialNumber; }
        public int flags()                  { return flags; }
    }
    private static native void GetVolumeInformation0(long lpRoot,
                                                     VolumeInformation obj)
        throws WindowsException;

    /**
     * UINT GetDriveType(
     *   LPCTSTR lpRootPathName
     * )
     */
    static int GetDriveType(String root) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(root);
        try {
            return GetDriveType0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    private static native int GetDriveType0(long lpRoot) throws WindowsException;

    /**
     * GetDiskFreeSpaceEx(
     *   LPCTSTR lpDirectoryName,
     *   PULARGE_INTEGER lpFreeBytesAvailableToCaller,
     *   PULARGE_INTEGER lpTotalNumberOfBytes,
     *   PULARGE_INTEGER lpTotalNumberOfFreeBytes
     * )
     */
    static DiskFreeSpace GetDiskFreeSpaceEx(String path)
        throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            DiskFreeSpace space = new DiskFreeSpace();
            GetDiskFreeSpaceEx0(buffer.address(), space);
            return space;
        } finally {
            buffer.release();
        }
    }

    /**
     * GetDiskFreeSpace(
     *   LPCTSTR lpRootPathName,
     *   LPDWORD lpSectorsPerCluster,
     *   LPDWORD lpBytesPerSector,
     *   LPDWORD lpNumberOfFreeClusters,
     *   LPDWORD lpTotalNumberOfClusters
     * )
     */
    static DiskFreeSpace GetDiskFreeSpace(String path)
        throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            DiskFreeSpace space = new DiskFreeSpace();
            GetDiskFreeSpace0(buffer.address(), space);
            return space;
        } finally {
            buffer.release();
        }
    }

    static class DiskFreeSpace {
        private long freeBytesAvailable;
        private long totalNumberOfBytes;
        private long totalNumberOfFreeBytes;
        private long bytesPerSector;
        private DiskFreeSpace() { }

        public long freeBytesAvailable()      { return freeBytesAvailable; }
        public long totalNumberOfBytes()      { return totalNumberOfBytes; }
        public long totalNumberOfFreeBytes()  { return totalNumberOfFreeBytes; }
        public long bytesPerSector()          { return bytesPerSector; }
    }
    private static native void GetDiskFreeSpaceEx0(long lpDirectoryName,
                                                   DiskFreeSpace obj)
        throws WindowsException;


    private static native void GetDiskFreeSpace0(long lpRootPathName,
                                                 DiskFreeSpace obj)
        throws WindowsException;

    /**
     * GetVolumePathName(
     *   LPCTSTR lpszFileName,
     *   LPTSTR lpszVolumePathName,
     *   DWORD cchBufferLength
     * )
     *
     * @return  lpFileName
     */
    static String GetVolumePathName(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return GetVolumePathName0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    private static native String GetVolumePathName0(long lpFileName)
        throws WindowsException;


    /**
     * InitializeSecurityDescriptor(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   DWORD dwRevision
     * )
     */
    static native void InitializeSecurityDescriptor(long sdAddress)
        throws WindowsException;

    /**
     * InitializeAcl(
     *   PACL pAcl,
     *   DWORD nAclLength,
     *   DWORD dwAclRevision
     * )
     */
    static native void InitializeAcl(long aclAddress, int size)
         throws WindowsException;

    /**
     * GetFileSecurity(
     *   LPCTSTR lpFileName,
     *   SECURITY_INFORMATION RequestedInformation,
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   DWORD nLength,
     *   LPDWORD lpnLengthNeeded
     * )
     */
    static int GetFileSecurity(String path,
                               int requestedInformation,
                               long pSecurityDescriptor,
                               int nLength) throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return GetFileSecurity0(buffer.address(), requestedInformation,
                pSecurityDescriptor, nLength);
        } finally {
            buffer.release();
        }
    }
    private static native int GetFileSecurity0(long lpFileName,
                                               int requestedInformation,
                                               long pSecurityDescriptor,
                                               int nLength) throws WindowsException;

    /**
     * SetFileSecurity(
     *   LPCTSTR lpFileName,
     *   SECURITY_INFORMATION SecurityInformation,
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor
     * )
     */
    static void SetFileSecurity(String path,
                                int securityInformation,
                                long pSecurityDescriptor)
        throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            SetFileSecurity0(buffer.address(), securityInformation,
                pSecurityDescriptor);
        } finally {
            buffer.release();
        }
    }
    static native void SetFileSecurity0(long lpFileName, int securityInformation,
        long pSecurityDescriptor) throws WindowsException;

    /**
     * GetSecurityDescriptorOwner(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor
     *   PSID *pOwner,
     *   LPBOOL lpbOwnerDefaulted
     * )
     *
     * @return  pOwner
     */
    static native long GetSecurityDescriptorOwner(long pSecurityDescriptor)
        throws WindowsException;

    /**
     * SetSecurityDescriptorOwner(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   PSID pOwner,
     *   BOOL bOwnerDefaulted
     * )
     */
    static native void SetSecurityDescriptorOwner(long pSecurityDescriptor,
                                                  long pOwner)
        throws WindowsException;

    /**
     * GetSecurityDescriptorDacl(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   LPBOOL lpbDaclPresent,
     *   PACL *pDacl,
     *   LPBOOL lpbDaclDefaulted
     * )
     */
    static native long GetSecurityDescriptorDacl(long pSecurityDescriptor);

    /**
     * SetSecurityDescriptorDacl(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   BOOL bDaclPresent,
     *   PACL pDacl,
     *   BOOL bDaclDefaulted
     * )
     */
    static native void SetSecurityDescriptorDacl(long pSecurityDescriptor, long pAcl)
        throws WindowsException;


    /**
     * GetAclInformation(
     *   PACL pAcl,
     *   LPVOID pAclInformation,
     *   DWORD nAclInformationLength,
     *   ACL_INFORMATION_CLASS dwAclInformationClass
     * )
     */
    static AclInformation GetAclInformation(long aclAddress) {
        AclInformation info = new AclInformation();
        GetAclInformation0(aclAddress, info);
        return info;
    }
    static class AclInformation {
        private int aceCount;
        private AclInformation() { }

        public int aceCount()   { return aceCount; }
    }
    private static native void GetAclInformation0(long aclAddress,
        AclInformation obj);

    /**
     * GetAce(
     *   PACL pAcl,
     *   DWORD dwAceIndex,
     *   LPVOID *pAce
     * )
     */
    static native long GetAce(long aclAddress, int aceIndex);

    /**
     * AddAccessAllowedAceEx(
     *   PACL pAcl,
     *   DWORD dwAceRevision,
     *   DWORD AceFlags,
     *   DWORD AccessMask,
     *   PSID pSid
     * )
     */
    static native void AddAccessAllowedAceEx(long aclAddress, int flags,
        int mask, long sidAddress) throws WindowsException;

    /**
     * AddAccessDeniedAceEx(
     *   PACL pAcl,
     *   DWORD dwAceRevision,
     *   DWORD AceFlags,
     *   DWORD AccessMask,
     *   PSID pSid
     * )
     */
    static native void AddAccessDeniedAceEx(long aclAddress, int flags,
        int mask, long sidAddress) throws WindowsException;

    /**
     * LookupAccountSid(
     *   LPCTSTR lpSystemName,
     *   PSID Sid,
     *   LPTSTR Name,
     *   LPDWORD cbName,
     *   LPTSTR ReferencedDomainName,
     *   LPDWORD cbReferencedDomainName,
     *   PSID_NAME_USE peUse
     * )
     */
    static Account LookupAccountSid(long sidAddress) throws WindowsException {
        Account acc = new Account();
        LookupAccountSid0(sidAddress, acc);
        return acc;
    }
    static class Account {
        private String domain;
        private String name;
        private int use;
        private Account() { }

        public String domain()  { return domain; }
        public String name()    { return name; }
        public int use()        { return use; }
    }
    private static native void LookupAccountSid0(long sidAddress, Account obj)
        throws WindowsException;

    /**
     * LookupAccountName(
     *   LPCTSTR lpSystemName,
     *   LPCTSTR lpAccountName,
     *   PSID Sid,
     *   LPDWORD cbSid,
     *   LPTSTR ReferencedDomainName,
     *   LPDWORD cbReferencedDomainName,
     *   PSID_NAME_USE peUse
     * )
     *
     * @return  cbSid
     */
    static int LookupAccountName(String accountName,
                                 long pSid,
                                 int cbSid) throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(accountName);
        try {
            return LookupAccountName0(buffer.address(), pSid, cbSid);
        } finally {
            buffer.release();
        }
    }
    private static native int LookupAccountName0(long lpAccountName, long pSid,
        int cbSid) throws WindowsException;

    /**
     * DWORD GetLengthSid(
     *   PSID pSid
     * )
     */
    static native int GetLengthSid(long sidAddress);

    /**
     * ConvertSidToStringSid(
     *   PSID Sid,
     *   LPTSTR* StringSid
     * )
     *
     * @return  StringSid
     */
    static native String ConvertSidToStringSid(long sidAddress)
        throws WindowsException;

    /**
     * ConvertStringSidToSid(
     *   LPCTSTR StringSid,
     *   PSID* pSid
     * )
     *
     * @return  pSid
     */
    static long ConvertStringSidToSid(String sidString)
        throws WindowsException
    {
        NativeBuffer buffer = asNativeBuffer(sidString);
        try {
            return ConvertStringSidToSid0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    private static native long ConvertStringSidToSid0(long lpStringSid)
        throws WindowsException;

    /**
     * HANDLE GetCurrentProcess(VOID)
     */
    static native long GetCurrentProcess();

    /**
     * HANDLE GetCurrentThread(VOID)
     */
    static native long GetCurrentThread();

    /**
     * OpenProcessToken(
     *   HANDLE ProcessHandle,
     *   DWORD DesiredAccess,
     *   PHANDLE TokenHandle
     * )
     */
    static native long OpenProcessToken(long hProcess, int desiredAccess)
        throws WindowsException;

    /**
     * OpenThreadToken(
     *   HANDLE ThreadHandle,
     *   DWORD DesiredAccess,
     *   BOOL OpenAsSelf,
     *   PHANDLE TokenHandle
     * )
     */
    static native long OpenThreadToken(long hThread, int desiredAccess,
        boolean openAsSelf) throws WindowsException;

    /**
     */
    static native long DuplicateTokenEx(long hThread, int desiredAccess)
        throws WindowsException;

    /**
     * SetThreadToken(
     *   PHANDLE Thread,
     *   HANDLE Token
     * )
     */
    static native void SetThreadToken(long thread, long hToken)
        throws WindowsException;

    /**
     * GetTokenInformation(
     *   HANDLE TokenHandle,
     *   TOKEN_INFORMATION_CLASS TokenInformationClass,
     *   LPVOID TokenInformation,
     *   DWORD TokenInformationLength,
     *   PDWORD ReturnLength
     * )
     */
    static native int GetTokenInformation(long token, int tokenInfoClass,
        long pTokenInfo, int tokenInfoLength) throws WindowsException;

    /**
     * AdjustTokenPrivileges(
     *   HANDLE TokenHandle,
     *   BOOL DisableAllPrivileges
     *   PTOKEN_PRIVILEGES NewState
     *   DWORD BufferLength
     *   PTOKEN_PRIVILEGES
     *   PDWORD ReturnLength
     * )
     */
    static native void AdjustTokenPrivileges(long token, long luid, int attributes)
        throws WindowsException;


    /**
     * AccessCheck(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   HANDLE ClientToken,
     *   DWORD DesiredAccess,
     *   PGENERIC_MAPPING GenericMapping,
     *   PPRIVILEGE_SET PrivilegeSet,
     *   LPDWORD PrivilegeSetLength,
     *   LPDWORD GrantedAccess,
     *   LPBOOL AccessStatus
     * )
     */
    static native boolean AccessCheck(long token, long securityInfo, int accessMask,
        int genericRead, int genericWrite, int genericExecute, int genericAll)
        throws WindowsException;

    /**
     */
    static long LookupPrivilegeValue(String name) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(name);
        try {
            return LookupPrivilegeValue0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    private static native long LookupPrivilegeValue0(long lpName)
        throws WindowsException;

    /**
     * CreateSymbolicLink(
     *   LPCWSTR lpSymlinkFileName,
     *   LPCWSTR lpTargetFileName,
     *   DWORD dwFlags
     * )
     *
     * Creates a symbolic link, conditionally retrying with the addition of
     * the flag SYMBOLIC_LINK_FLAG_ALLOW_UNPRIVILEGED_CREATE if the initial
     * attempt fails with ERROR_PRIVILEGE_NOT_HELD. If the retry fails, throw
     * the original exception due to ERROR_PRIVILEGE_NOT_HELD. The retry will
     * succeed only on Windows build 14972 or later if Developer Mode is on.
     */
    static void CreateSymbolicLink(String link, String target, int flags)
        throws WindowsException
    {
        NativeBuffer linkBuffer = asNativeBuffer(link);
        NativeBuffer targetBuffer = asNativeBuffer(target);
        try {
            CreateSymbolicLink0(linkBuffer.address(), targetBuffer.address(),
                                flags);
        } catch (WindowsException x) {
            if (x.lastError() == ERROR_PRIVILEGE_NOT_HELD) {
                flags |= SYMBOLIC_LINK_FLAG_ALLOW_UNPRIVILEGED_CREATE;
                try {
                    CreateSymbolicLink0(linkBuffer.address(),
                                        targetBuffer.address(), flags);
                    return;
                } catch (WindowsException ignored) {
                    // Will fail with ERROR_INVALID_PARAMETER for Windows
                    // builds older than 14972.
                }
            }
            throw x;
        } finally {
            targetBuffer.release();
            linkBuffer.release();
        }
    }
    private static native void CreateSymbolicLink0(long linkAddress,
        long targetAddress, int flags) throws WindowsException;

    /**
     * CreateHardLink(
     *    LPCTSTR lpFileName,
     *    LPCTSTR lpExistingFileName,
     *    LPSECURITY_ATTRIBUTES lpSecurityAttributes
     * )
     */
    static void CreateHardLink(String newFile, String existingFile)
        throws WindowsException
    {
        NativeBuffer newFileBuffer = asNativeBuffer(newFile);
        NativeBuffer existingFileBuffer = asNativeBuffer(existingFile);
        try {
            CreateHardLink0(newFileBuffer.address(), existingFileBuffer.address());
        } finally {
            existingFileBuffer.release();
            newFileBuffer.release();
        }
    }
    private static native void CreateHardLink0(long newFileBuffer,
        long existingFileBuffer) throws WindowsException;

    /**
     * GetFullPathName(
     *   LPCTSTR lpFileName,
     *   DWORD nBufferLength,
     *   LPTSTR lpBuffer,
     *   LPTSTR *lpFilePart
     * )
     */
    static String GetFullPathName(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return GetFullPathName0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    private static native String GetFullPathName0(long pathAddress)
        throws WindowsException;

    /**
     * GetFinalPathNameByHandle(
     *   HANDLE hFile,
     *   LPTSTR lpszFilePath,
     *   DWORD cchFilePath,
     *   DWORD dwFlags
     * )
     */
    static native String GetFinalPathNameByHandle(long handle)
        throws WindowsException;

    /**
     * FormatMessage(
     *   DWORD dwFlags,
     *   LPCVOID lpSource,
     *   DWORD dwMessageId,
     *   DWORD dwLanguageId,
     *   LPTSTR lpBuffer,
     *   DWORD nSize,
     *   va_list *Arguments
     * )
     */
    static native String FormatMessage(int errorCode);

    /**
     * LocalFree(
     *   HLOCAL hMem
     * )
     */
    static native void LocalFree(long address);

    /**
     * HANDLE CreateIoCompletionPort (
     *   HANDLE FileHandle,
     *   HANDLE ExistingCompletionPort,
     *   ULONG_PTR CompletionKey,
     *   DWORD NumberOfConcurrentThreads
     * )
     */
    static native long CreateIoCompletionPort(long fileHandle, long existingPort,
        long completionKey) throws WindowsException;


    /**
     * GetQueuedCompletionStatus(
     *   HANDLE CompletionPort,
     *   LPDWORD lpNumberOfBytesTransferred,
     *   PULONG_PTR lpCompletionKey,
     *   LPOVERLAPPED *lpOverlapped,
     *   DWORD dwMilliseconds
     */
    static CompletionStatus GetQueuedCompletionStatus(long completionPort)
        throws WindowsException
    {
        CompletionStatus status = new CompletionStatus();
        GetQueuedCompletionStatus0(completionPort, status);
        return status;
    }
    static class CompletionStatus {
        private int error;
        private int bytesTransferred;
        private long completionKey;
        private CompletionStatus() { }

        int error() { return error; }
        int bytesTransferred() { return bytesTransferred; }
        long completionKey() { return completionKey; }
    }
    private static native void GetQueuedCompletionStatus0(long completionPort,
        CompletionStatus status) throws WindowsException;

    /**
     * PostQueuedCompletionStatus(
     *   HANDLE CompletionPort,
     *   DWORD dwNumberOfBytesTransferred,
     *   ULONG_PTR dwCompletionKey,
     *   LPOVERLAPPED lpOverlapped
     * )
     */
    static native void PostQueuedCompletionStatus(long completionPort,
        long completionKey) throws WindowsException;

    /**
     * ReadDirectoryChangesW(
     *   HANDLE hDirectory,
     *   LPVOID lpBuffer,
     *   DWORD nBufferLength,
     *   BOOL bWatchSubtree,
     *   DWORD dwNotifyFilter,
     *   LPDWORD lpBytesReturned,
     *   LPOVERLAPPED lpOverlapped,
     *   LPOVERLAPPED_COMPLETION_ROUTINE lpCompletionRoutine
     * )
     */
    static native void ReadDirectoryChangesW(long hDirectory,
                                             long bufferAddress,
                                             int bufferLength,
                                             boolean watchSubTree,
                                             int filter,
                                             long bytesReturnedAddress,
                                             long pOverlapped)
        throws WindowsException;


    /**
     * CancelIo(
     *   HANDLE hFile
     * )
     */
    static native void CancelIo(long hFile) throws WindowsException;

    /**
     * GetOverlappedResult(
     *   HANDLE hFile,
     *   LPOVERLAPPED lpOverlapped,
     *   LPDWORD lpNumberOfBytesTransferred,
     *   BOOL bWait
     * );
     */
    static native int GetOverlappedResult(long hFile, long lpOverlapped)
        throws WindowsException;

    // -- support for copying String with a NativeBuffer --

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    static NativeBuffer asNativeBuffer(String s) throws WindowsException {
        if (s.length() > (Integer.MAX_VALUE - 2)/2) {
            throw new WindowsException
                ("String too long to convert to native buffer");
        }

        int stringLengthInBytes = s.length() << 1;
        int sizeInBytes = stringLengthInBytes + 2;  // char terminator

        // get a native buffer of sufficient size
        NativeBuffer buffer = NativeBuffers.getNativeBufferFromCache(sizeInBytes);
        if (buffer == null) {
            buffer = NativeBuffers.allocNativeBuffer(sizeInBytes);
        } else {
            // buffer already contains the string contents
            if (buffer.owner() == s)
                return buffer;
        }

        // copy into buffer and zero terminate
        char[] chars = s.toCharArray();
        unsafe.copyMemory(chars, Unsafe.ARRAY_CHAR_BASE_OFFSET, null,
            buffer.address(), (long)stringLengthInBytes);
        unsafe.putChar(buffer.address() + stringLengthInBytes, (char)0);
        buffer.setOwner(s);
        return buffer;
    }

    // -- native library initialization --

    private static native void initIDs();

    static {
        // nio.dll has dependency on net.dll
        jdk.internal.loader.BootLoader.loadLibrary("net");
        jdk.internal.loader.BootLoader.loadLibrary("nio");
        initIDs();
    }

}
