/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibThreading/Thread.h>
#include <pthread.h>
#include <string.h>
#include <unistd.h>

Threading::Thread::Thread(Function<intptr_t()> action, StringView thread_name)
    : Core::Object(nullptr)
    , m_action(move(action))
    , m_thread_name(thread_name.is_null() ? ""sv : thread_name)
{
    register_property("thread_name", [&] { return JsonValue { m_thread_name }; });
#if defined(AK_OS_SERENITY) || defined(AK_OS_LINUX)
    // FIXME: Print out a pretty TID for BSD and macOS platforms, too
    register_property("tid", [&] { return JsonValue { m_tid }; });
#endif
}

Threading::Thread::~Thread()
{
    if (m_tid && !m_detached) {
        dbgln("Destroying thread \"{}\"({}) while it is still running!", m_thread_name, m_tid);
        [[maybe_unused]] auto res = join();
    }
}

void Threading::Thread::start()
{
    int rc = pthread_create(
        &m_tid,
        nullptr,
        [](void* arg) -> void* {
            Thread* self = static_cast<Thread*>(arg);
            auto exit_code = self->m_action();
            self->m_tid = 0;
            return reinterpret_cast<void*>(exit_code);
        },
        static_cast<void*>(this));

    VERIFY(rc == 0);
#ifdef AK_OS_SERENITY
    if (!m_thread_name.is_empty()) {
        rc = pthread_setname_np(m_tid, m_thread_name.characters());
        VERIFY(rc == 0);
    }
#endif
    dbgln("Started thread \"{}\", tid = {}", m_thread_name, m_tid);
    m_started = true;
}

void Threading::Thread::detach()
{
    VERIFY(!m_detached);

    int rc = pthread_detach(m_tid);
    VERIFY(rc == 0);

    m_detached = true;
}
