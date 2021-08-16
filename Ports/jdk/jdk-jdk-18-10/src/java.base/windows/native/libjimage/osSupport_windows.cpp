/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <io.h>
#include <malloc.h>

#include "jni.h"
#include "osSupport.hpp"

/**
 * Open a regular file read-only.
 * Return the file descriptor.
 */
jint osSupport::openReadOnly(const char *path) {
    // jimage file descriptors must not be inherited by child processes
    return ::open(path, O_BINARY | O_NOINHERIT, O_RDONLY);
}

/**
 * Close a file descriptor.
 */
jint osSupport::close(jint fd) {
    return ::close(fd);
}

/**
 * Return the size of a regular file.
 */
jlong osSupport::size(const char *path) {
    struct stat statbuf;
    if (stat(path, &statbuf) < 0 ||
            (statbuf.st_mode & S_IFREG) != S_IFREG) {
        return -1;
    }
    return (jlong) statbuf.st_size;
}

/**
 * Read nBytes at offset into a buffer.
 */
jlong osSupport::read(jint fd, char *buf, jlong nBytes, jlong offset) {
    OVERLAPPED ov;
    DWORD nread;
    BOOL result;

    ZeroMemory(&ov, sizeof (ov));
    ov.Offset = (DWORD) offset;
    ov.OffsetHigh = (DWORD) (offset >> 32);

    HANDLE h = (HANDLE)::_get_osfhandle(fd);

    result = ReadFile(h, (LPVOID) buf, (DWORD) nBytes, &nread, &ov);

    return result ? nread : 0;
}

/**
 * Map nBytes at offset into memory and return the address.
 * The system chooses the address.
 */
void* osSupport::map_memory(jint fd, const char *file_name, size_t file_offset, size_t bytes) {
    HANDLE hFile;
    char* base = NULL;

    // Get a handle to the file
    hFile = CreateFile(file_name, GENERIC_READ, FILE_SHARE_READ, NULL,
            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (hFile != NULL) {
        // Create a file mapping handle
        HANDLE hMap = CreateFileMapping(hFile, NULL, PAGE_READONLY, 0, 0,
                NULL /* file_name */);
        if (hMap != NULL) {
            // Map the file into the address space at the offset
            base = (char*) MapViewOfFileEx(hMap, FILE_MAP_READ, 0, (DWORD) file_offset,
                    (DWORD) bytes, NULL);
            CloseHandle(hMap); // The mapping is no longer needed
        }
        CloseHandle(hFile); // The file handle is no longer needed
    }
    return base;
}

/**
 * Unmap nBytes of memory at address.
 */
int osSupport::unmap_memory(void* addr, size_t bytes) {
    BOOL result = UnmapViewOfFile(addr);
    return result;
}

/**
 * A CriticalSection to protect a small section of code.
 */
void SimpleCriticalSection::enter() {
    EnterCriticalSection(&critical_section);
}

void SimpleCriticalSection::exit() {
    LeaveCriticalSection(&critical_section);
}

SimpleCriticalSection::SimpleCriticalSection() {
    InitializeCriticalSection(&critical_section);
}

//SimpleCriticalSection::~SimpleCriticalSection() {
//    DeleteCriticalSection(&critical_section);
//}

