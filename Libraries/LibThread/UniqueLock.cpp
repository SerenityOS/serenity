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

#include "UniqueLock.h"

LibThread::UniqueLock::UniqueLock(Mutex& mutex)
    : m_mutex(mutex)
{
    m_mutex.lock();
}

LibThread::UniqueLock::~UniqueLock()
{
    if (m_is_owner)
        pthread_mutex_destroy(m_mutex.native_handle());
}

void LibThread::UniqueLock::lock()
{
    m_mutex.lock();
}

bool LibThread::UniqueLock::try_lock()
{
    return m_mutex.try_lock();
}

void LibThread::UniqueLock::unlock()
{
    m_mutex.unlock();
}

bool LibThread::UniqueLock::owns_lock() const noexcept
{
    return m_is_owner;
}

LibThread::Mutex* LibThread::UniqueLock::mutex() const noexcept
{
    return &m_mutex;
}

LibThread::UniqueLock::operator bool() const noexcept
{
    return owns_lock();
}