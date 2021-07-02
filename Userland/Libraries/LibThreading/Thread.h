/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Function.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <LibCore/Object.h>
#include <pthread.h>

namespace Threading {

TYPEDEF_DISTINCT_ORDERED_ID(intptr_t, ThreadError);

class Thread final : public Core::Object {
    C_OBJECT(Thread);

public:
    virtual ~Thread();

    void start();
    void detach();

    template<typename T = void>
    Result<T, ThreadError> join();

    String thread_name() const { return m_thread_name; }
    pthread_t tid() const { return m_tid; }

private:
    explicit Thread(Function<intptr_t()> action, StringView thread_name = nullptr);
    Function<intptr_t()> m_action;
    pthread_t m_tid { 0 };
    String m_thread_name;
    bool m_detached { false };
};

template<typename T>
Result<T, ThreadError> Thread::join()
{
    void* thread_return = nullptr;
    int rc = pthread_join(m_tid, &thread_return);
    if (rc != 0) {
        return ThreadError { rc };
    }

    m_tid = 0;
    if constexpr (IsVoid<T>)
        return {};
    else
        return { static_cast<T>(thread_return) };
}

}
