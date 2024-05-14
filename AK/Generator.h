/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Coroutine.h>
#include <AK/Variant.h>

namespace AK {

namespace Detail {
class YieldAwaiter {
public:
    YieldAwaiter(std::coroutine_handle<> control_transfer, std::coroutine_handle<>& awaiter)
        : m_control_transfer(control_transfer)
        , m_awaiter(awaiter)
    {
    }

    bool await_ready() const { return false; }

    auto await_suspend(std::coroutine_handle<> handle)
    {
        m_awaiter = handle;
        return m_control_transfer;
    }

    void await_resume() { }

private:
    std::coroutine_handle<> m_control_transfer;
    std::coroutine_handle<>& m_awaiter;
};
}

template<typename Y, typename R>
class [[nodiscard]] Generator {
    struct GeneratorPromiseType;

    AK_MAKE_NONCOPYABLE(Generator);

public:
    using YieldType = Y;
    using ReturnType = R;
    using promise_type = GeneratorPromiseType;

    ~Generator()
    {
        destroy_stored_object();
        if (m_handle)
            m_handle.destroy();
    }

    Generator(Generator&& other)
    {
        m_handle = AK::exchange(other.m_handle, {});
        m_read_returned_object = exchange(other.m_read_returned_object, false);

        m_currently_stored_type = other.m_currently_stored_type;
        if (m_currently_stored_type == CurrentlyStoredType::Yield) {
            new (m_data) YieldType(move(*reinterpret_cast<YieldType*>(other.m_data)));
        } else if (m_currently_stored_type == CurrentlyStoredType::Return) {
            new (m_data) ReturnType(move(*reinterpret_cast<ReturnType*>(other.m_data)));
        }
        other.destroy_stored_object();

        if (m_handle)
            m_handle.promise().m_coroutine = this;
    }

    Generator& operator=(Generator&& other)
    {
        if (this != &other) {
            this->~Generator();
            new (this) Generator(move(other));
        }
        return *this;
    }

    bool is_done() const { return !m_handle || m_handle.done(); }

    void destroy()
    {
        VERIFY(m_handle && !m_handle.promise().m_awaiter);
        destroy_stored_object();
        m_handle.destroy();
        m_handle = {};
    }

    Coroutine<Variant<Y, R>> next()
    {
        if (!is_done()) {
            VERIFY(m_currently_stored_type != CurrentlyStoredType::Return);
            co_await Detail::YieldAwaiter { m_handle, m_handle.promise().m_awaiter };
            if (m_handle)
                m_handle.promise().m_awaiter = {};
        }

        if (is_done()) {
            VERIFY(m_currently_stored_type == CurrentlyStoredType::Return && !m_read_returned_object);
            m_read_returned_object = true;
            co_return move(*reinterpret_cast<ReturnType*>(m_data));
        } else {
            VERIFY(m_currently_stored_type == CurrentlyStoredType::Yield);
            co_return move(*reinterpret_cast<YieldType*>(m_data));
        }
    }

private:
    template<typename U>
    friend struct Detail::TryAwaiter;

    struct GeneratorPromiseType {
        Generator get_return_object()
        {
            return { std::coroutine_handle<promise_type>::from_promise(*this) };
        }

        Detail::SuspendAlways initial_suspend() { return {}; }

        Detail::SymmetricControlTransfer final_suspend() noexcept
        {
            VERIFY(m_awaiter);
            return { m_awaiter };
        }

        template<typename U>
        requires requires { { T(forward<U>(declval<U>())) }; }
        void return_value(U&& returned_object)
        {
            m_coroutine->place_returned_object(forward<U>(returned_object));
        }

        void return_value(ReturnType&& returned_object)
        {
            m_coroutine->place_returned_object(move(returned_object));
        }

        Detail::SymmetricControlTransfer yield_value(YieldType&& yield_value)
        {
            m_coroutine->place_yield_object(move(yield_value));
            VERIFY(m_awaiter);
            return { m_awaiter };
        }

        std::coroutine_handle<> m_awaiter;
        Generator* m_coroutine { nullptr }; // Must be named `m_coroutine` for CO_TRY to work
    };

    Generator(std::coroutine_handle<promise_type>&& handle)
        : m_handle(move(handle))
    {
        m_handle.promise().m_coroutine = this;
    }

    void destroy_stored_object()
    {
        switch (m_currently_stored_type) {
        case CurrentlyStoredType::Empty:
            break;
        case CurrentlyStoredType::Yield:
            reinterpret_cast<YieldType*>(m_data)->~YieldType();
            break;
        case CurrentlyStoredType::Return:
            reinterpret_cast<ReturnType*>(m_data)->~ReturnType();
            break;
        }
        m_currently_stored_type = CurrentlyStoredType::Empty;
    }

    template<typename... Args>
    YieldType* place_yield_object(Args&&... args)
    {
        destroy_stored_object();
        m_currently_stored_type = CurrentlyStoredType::Yield;
        return new (m_data) YieldType(forward<Args>(args)...);
    }

    template<typename... Args>
    ReturnType* place_returned_object(Args&&... args)
    {
        destroy_stored_object();
        m_currently_stored_type = CurrentlyStoredType::Return;
        return new (m_data) ReturnType(forward<Args>(args)...);
    }

    ReturnType* return_value() // Must be defined for CO_TRY.
    {
        destroy_stored_object();
        m_currently_stored_type = CurrentlyStoredType::Return;
        return reinterpret_cast<ReturnType*>(m_data);
    }

    std::coroutine_handle<promise_type> m_handle;

    enum class CurrentlyStoredType {
        Empty,
        Yield,
        Return,
    } m_currently_stored_type
        = CurrentlyStoredType::Empty;
    bool m_read_returned_object { false };
    alignas(max(alignof(YieldType), alignof(ReturnType))) u8 m_data[max(sizeof(YieldType), sizeof(ReturnType))];
};

}

#ifdef USING_AK_GLOBALLY
using AK::Generator;
#endif
