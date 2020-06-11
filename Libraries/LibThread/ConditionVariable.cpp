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

#include "assert.h"

#include <LibThread/ConditionVariable.h>

LibThread::ConditionVariable::ConditionVariable()
{
    auto rc = pthread_cond_init(&m_condition_variable, NULL);
    ASSERT(!rc);
}

LibThread::ConditionVariable::~ConditionVariable()
{
    auto rc = pthread_cond_destroy(&m_condition_variable);
    ASSERT(!rc);
}

void LibThread::ConditionVariable::notify_one() noexcept
{
    auto rc = pthread_cond_signal(&m_condition_variable);
    ASSERT(!rc);
}

void LibThread::ConditionVariable::notify_all() noexcept
{
    auto rc = pthread_cond_broadcast(&m_condition_variable);
    ASSERT(!rc);
}

void LibThread::ConditionVariable::wait(UniqueLock& lock) noexcept
{
    auto rc = pthread_cond_wait(&m_condition_variable, lock.mutex()->native_handle());
    ASSERT(!rc);
}

pthread_cond_t LibThread::ConditionVariable::native_handle() noexcept
{
    return m_condition_variable;
}