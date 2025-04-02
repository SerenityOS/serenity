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
class [[nodiscard]] SyncGenerator {
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
        while (!m_handle.done())
            m_handle.resume();
#endif

        m_handle.destroy();
    }

    SyncGenerator(SyncGenerator&& other)
        : m_handle(AK::exchange(other.m_handle, {}))
        , m_value(move(other.m_value))
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

    bool has_next() const
    {
        return !m_handle.done();
    }

    Y next()
    {
        Y value = m_value.release_value();
        m_handle.resume();
        return value;
    }

private:
    struct PromiseType {
        SyncGenerator get_return_object()
        {
            return { std::coroutine_handle<PromiseType>::from_promise(*this) };
        }

        Detail::SuspendNever initial_suspend() { return {}; }
        Detail::SuspendAlways final_suspend() noexcept { return {}; }

        void return_void() { }

        Detail::SuspendAlways yield_value(Y&& value)
        {
            m_generator->m_value = move(value);
            return {};
        }

        Detail::SuspendAlways yield_value(Y const& value)
        requires IsCopyConstructible<Y>
        {
            m_generator->m_value = value;
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
    Optional<Y> m_value;
};

}

#ifdef USING_AK_GLOBALLY
using AK::SyncGenerator;
#endif
