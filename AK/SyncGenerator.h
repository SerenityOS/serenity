/*
 * Copyright (c) 2025, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Coroutine.h>
#include <AK/Optional.h>
#include <AK/ScopeGuard.h>

namespace AK {

template<typename Y>
class SyncGenerator {
    struct PromiseType;

    AK_MAKE_NONCOPYABLE(SyncGenerator);

public:
    using YieldType = Y;
    using promise_type = PromiseType;

    ~SyncGenerator()
    {
        if (!m_handle)
            return;

#ifdef AK_COROUTINE_DESTRUCTION_BROKEN
        // FIXME: Calling `m_handle.destroy()` when a coroutine has not reached its final
        //        suspension point is scary on Clang < 19.
        while (!m_handle.done()) {
            m_handle.resume();
            m_value.~Y();
        }
#endif

        m_handle.destroy();
    }

    SyncGenerator(SyncGenerator&& other)
        : m_handle(AK::exchange(other.m_handle, {}))
    {
        if (m_handle)
            m_handle.promise().m_generator = this;
    }

    SyncGenerator& operator=(SyncGenerator&& other)
    {
        if (this != &other) {
            this->~SyncGenerator();
            new (this) SyncGenerator(move(other));
        }
        return *this;
    }

    bool is_done() const
    {
        return m_handle.done();
    }

    Optional<Y> next()
    {
        VERIFY(!m_handle.done());
        m_handle.resume();
        if (m_handle.done()) {
            return {};
        } else {
            ScopeGuard guard = [&] { m_value.~Y(); };
            // We must return a temporary object for RVO to be guaranteed. And a simpler version of
            // this branch (Optional<Y> result = m_value; m_value.~Y(); return result; ) won't do
            // it. See move_count test in TestSyncGenerator.cpp.
            return Optional<Y> { move(m_value) };
        }
    }

private:
    struct PromiseType {
        SyncGenerator get_return_object()
        {
            return { std::coroutine_handle<PromiseType>::from_promise(*this) };
        }

        Detail::SuspendAlways initial_suspend() { return {}; }
        Detail::SuspendAlways final_suspend() noexcept { return {}; }

        void return_void() { }

        Detail::SuspendAlways yield_value(Y&& value)
        {
            new (&m_generator->m_value) Y(move(value));
            return {};
        }

        Detail::SuspendAlways yield_value(Y const& value)
        requires IsCopyConstructible<Y>
        {
            new (&m_generator->m_value) Y(value);
            return {};
        }

        void await_transform(auto&& awaiter) = delete;

        SyncGenerator* m_generator { nullptr };
    };

    SyncGenerator(std::coroutine_handle<PromiseType> handle)
        : m_handle(handle)
    {
        m_handle.promise().m_generator = this;
    }

    std::coroutine_handle<PromiseType> m_handle;

    union {
        Y m_value;
    };
};

}

#ifdef USING_AK_GLOBALLY
using AK::SyncGenerator;
#endif
