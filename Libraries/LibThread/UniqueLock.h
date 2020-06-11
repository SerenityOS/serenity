/*
 * Copyright (c) 2020, Christopher Joseph Dean Schaefer <disks86@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <pthread.h>

#include <LibThread/Mutex.h>

namespace LibThread {

class UniqueLock {

public:
    UniqueLock(Mutex& mutex); // TODO: add support for && for ownership transfer
    ~UniqueLock();
    inline void lock() noexcept
    {
        m_mutex.lock();
    };
    inline bool try_lock() noexcept
    {
        return m_mutex.try_lock();
    };
    // TODO: add support for try_lock()
    // TODO: add support for try_lock_until()
    inline void unlock() noexcept
    {
        m_mutex.unlock();
    };
    inline bool owns_lock() const noexcept
    {
        return m_owns_lock;
    };
    inline Mutex* mutex() const noexcept
    {
        return &m_mutex;
    };
    inline explicit operator bool() const noexcept
    {
        return owns_lock();
    };

private:
    LibThread::Mutex& m_mutex;
    bool m_owns_lock { false };
};
}
