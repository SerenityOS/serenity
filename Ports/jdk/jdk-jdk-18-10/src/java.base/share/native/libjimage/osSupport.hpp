/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef LIBJIMAGE_OSSUPPORT_HPP
#define LIBJIMAGE_OSSUPPORT_HPP

#ifdef WIN32
#include <Windows.h>
#else
#include <pthread.h>
#endif

class osSupport {
public:
    /**
     * Open a regular file read-only.
     * Return the file descriptor.
     */
    static jint openReadOnly(const char *path);

    /**
     * Close a file descriptor.
     */
    static jint close(jint fd);

    /**
     * Return the size of a regular file.
     */
    static jlong size(const char *path);

    /**
     * Read nBytes at offset into a buffer.
     */
    static jlong read(jint fd, char *buf, jlong nBytes, jlong offset);

    /**
     * Map nBytes at offset into memory and return the address.
     * The system chooses the address.
     */
    static void* map_memory(jint fd, const char *filename, size_t file_offset, size_t bytes);

    /**
     * Unmap nBytes of memory at address.
     */
    static int unmap_memory(void* addr, size_t bytes);
};

/**
 * A CriticalSection to protect a small section of code.
 */
class SimpleCriticalSection {
    friend class SimpleCriticalSectionLock;
private:
    void enter();
    void exit();
public:
    SimpleCriticalSection();
    //~SimpleCriticalSection(); // Cretes a dependency on Solaris on a C++ exit registration

private:
#ifdef WIN32
    CRITICAL_SECTION critical_section;
#else
    pthread_mutex_t mutex;
#endif // WIN32
};

/**
 * SimpleCriticalSectionLock instance.
 * The constructor locks a SimpleCriticalSection and the
 * destructor does the unlock.
 */
class SimpleCriticalSectionLock {
private:
    SimpleCriticalSection *lock;
public:

    SimpleCriticalSectionLock(SimpleCriticalSection *cslock) {
        this->lock = cslock;
        lock->enter();
    }

    ~SimpleCriticalSectionLock() {
        lock->exit();
    }
};

#endif  // LIBJIMAGE_OSSUPPORT_HPP
