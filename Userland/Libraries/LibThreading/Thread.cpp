/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibThreading/Thread.h>
#include <pthread.h>
#include <string.h>
#include <unistd.h>

namespace Threading {

Thread::Thread(Function<intptr_t()> action, StringView thread_name)
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

Thread::~Thread()
{
    if (m_tid && !m_detached) {
        dbgln("Destroying thread \"{}\"({}) while it is still running!", m_thread_name, m_tid);
        [[maybe_unused]] auto res = join();
    }
}

ErrorOr<void> Thread::set_priority(int priority)
{
    // MacOS has an extra __opaque field, so list initialization will not compile on MacOS Lagom.
    sched_param scheduling_parameters {};
    scheduling_parameters.sched_priority = priority;
    int result = pthread_setschedparam(m_tid, 0, &scheduling_parameters);
    if (result != 0)
        return Error::from_errno(result);
    return {};
}

ErrorOr<int> Thread::get_priority() const
{
    sched_param scheduling_parameters {};
    int policy;
    int result = pthread_getschedparam(m_tid, &policy, &scheduling_parameters);
    if (result != 0)
        return Error::from_errno(result);
    return scheduling_parameters.sched_priority;
}

void Thread::start()
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

void Thread::detach()
{
    VERIFY(!m_detached);

    int rc = pthread_detach(m_tid);
    VERIFY(rc == 0);

    m_detached = true;
}

}
