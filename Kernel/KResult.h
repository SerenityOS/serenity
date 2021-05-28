/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

enum KSuccessTag {
    KSuccess
};

class [[nodiscard]] KResult {
public:
    KResult(ErrnoCode error)
        : m_error(-error)
    {
    }
    KResult(KSuccessTag)
        : m_error(0)
    {
    }
    operator int() const { return m_error; }
    [[nodiscard]] int error() const { return m_error; }

    [[nodiscard]] bool is_success() const { return m_error == 0; }
    [[nodiscard]] bool is_error() const { return !is_success(); }

private:
    template<typename T>
    friend class KResultOr;
    KResult() = default;

    int m_error { 0 };
};

template<typename T>
class alignas(T) [[nodiscard]] KResultOr {
public:
    KResultOr(KResult error)
        : m_error(error)
        , m_is_error(true)
    {
    }

    KResultOr(ErrnoCode error)
        : m_error(error)
        , m_is_error(true)
    {
    }

    ALWAYS_INLINE KResultOr(T&& value)
    {
        new (&m_storage) T(move(value));
        m_have_storage = true;
    }

    template<typename U>
    ALWAYS_INLINE KResultOr(U&& value)
    {
        new (&m_storage) T(move(value));
        m_have_storage = true;
    }

    KResultOr(KResultOr&& other)
    {
        m_is_error = other.m_is_error;
        if (m_is_error)
            m_error = other.m_error;
        else {
            if (other.m_have_storage) {
                new (&m_storage) T(move(other.value()));
                m_have_storage = true;
                other.value().~T();
                other.m_have_storage = false;
            }
        }
        other.m_is_error = true;
        other.m_error = KSuccess;
    }

    KResultOr& operator=(KResultOr&& other)
    {
        if (&other == this)
            return *this;
        if (!m_is_error && m_have_storage) {
            value().~T();
            m_have_storage = false;
        }
        m_is_error = other.m_is_error;
        if (m_is_error)
            m_error = other.m_error;
        else {
            if (other.m_have_storage) {
                new (&m_storage) T(move(other.value()));
                m_have_storage = true;
                other.value().~T();
                other.m_have_storage = false;
            }
        }
        other.m_is_error = true;
        other.m_error = KSuccess;
        return *this;
    }

    ~KResultOr()
    {
        if (!m_is_error && m_have_storage)
            value().~T();
    }

    [[nodiscard]] bool is_error() const { return m_is_error; }

    [[nodiscard]] ALWAYS_INLINE KResult error() const
    {
        VERIFY(m_is_error);
        return m_error;
    }

    [[nodiscard]] KResult result() const { return m_is_error ? m_error : KSuccess; }

    [[nodiscard]] ALWAYS_INLINE T& value()
    {
        VERIFY(!m_is_error);
        return *reinterpret_cast<T*>(&m_storage);
    }

    [[nodiscard]] ALWAYS_INLINE const T& value() const
    {
        VERIFY(!m_is_error);
        return *reinterpret_cast<T*>(&m_storage);
    }

    [[nodiscard]] ALWAYS_INLINE T release_value()
    {
        VERIFY(!m_is_error);
        VERIFY(m_have_storage);
        T released_value(move(*reinterpret_cast<T*>(&m_storage)));
        value().~T();
        m_have_storage = false;
        return released_value;
    }

private:
    union {
        alignas(T) char m_storage[sizeof(T)];
        KResult m_error;
    };
    bool m_is_error { false };
    bool m_have_storage { false };
};

}

template<>
struct AK::Formatter<Kernel::KResult> : Formatter<int> {
    void format(FormatBuilder& builder, Kernel::KResult value)
    {
        return Formatter<int>::format(builder, value);
    }
};
