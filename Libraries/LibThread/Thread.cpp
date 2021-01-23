/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <LibThread/Thread.h>
#include <pthread.h>
#include <string.h>
#include <unistd.h>

LibThread::Thread::Thread(Function<int()> action, StringView thread_name)
    : Core::Object(nullptr)
    , m_action(move(action))
    , m_thread_name(thread_name.is_null() ? "" : thread_name)
{
    register_property("thread_name", [&] { return JsonValue { m_thread_name }; });
    register_property("tid", [&] { return JsonValue { m_tid }; });
}

LibThread::Thread::~Thread()
{
    if (m_tid) {
        dbgln("Destroying thread \"{}\"({}) while it is still running!", m_thread_name, m_tid);
        [[maybe_unused]] auto res = join();
    }
}

void LibThread::Thread::start()
{
    int rc = pthread_create(
        &m_tid,
        nullptr,
        [](void* arg) -> void* {
            Thread* self = static_cast<Thread*>(arg);
            int exit_code = self->m_action();
            self->m_tid = 0;
            return (void*)exit_code;
        },
        static_cast<void*>(this));

    ASSERT(rc == 0);
    if (!m_thread_name.is_empty()) {
        rc = pthread_setname_np(m_tid, m_thread_name.characters());
        ASSERT(rc == 0);
    }
    dbgln("Started thread \"{}\", tid = {}", m_thread_name, m_tid);
}
