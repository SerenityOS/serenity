/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/Assertions.h>
#include <LibC/errno_numbers.h>

enum KSuccessTag {
    KSuccess
};

class KResult {
public:
    explicit KResult(int negative_e)
        : m_error(negative_e)
    {
        ASSERT(negative_e <= 0);
    }
    KResult(KSuccessTag)
        : m_error(0)
    {
    }
    operator int() const { return m_error; }
    int error() const { return m_error; }

    bool is_success() const { return m_error == ESUCCESS; }
    bool is_error() const { return !is_success(); }

private:
    template<typename T>
    friend class KResultOr;
    KResult() {}

    int m_error { 0 };
};

template<typename T>
class alignas(T) KResultOr {
public:
    KResultOr(KResult error)
        : m_error(error)
        , m_is_error(true)
    {
    }

    KResultOr(T&& value)
    {
        new (&m_storage) T(move(value));
    }

    template<typename U>
    KResultOr(U&& value)
    {
        new (&m_storage) T(move(value));
    }

    KResultOr(KResultOr&& other)
    {
        m_is_error = other.m_is_error;
        if (m_is_error)
            m_error = other.m_error;
        else {
            new (&m_storage) T(move(other.value()));
            other.value().~T();
        }
        other.m_is_error = true;
        other.m_error = KSuccess;
    }

    KResultOr& operator=(KResultOr&& other)
    {
        if (!m_is_error)
            value().~T();
        m_is_error = other.m_is_error;
        if (m_is_error)
            m_error = other.m_error;
        else {
            new (&m_storage) T(move(other.value()));
            other.value().~T();
        }
        other.m_is_error = true;
        other.m_error = KSuccess;
        return *this;
    }

    ~KResultOr()
    {
        if (!m_is_error)
            value().~T();
    }

    bool is_error() const { return m_is_error; }
    KResult error() const
    {
        ASSERT(m_is_error);
        return m_error;
    }
    KResult result() const { return m_is_error ? KSuccess : m_error; }
    T& value()
    {
        ASSERT(!m_is_error);
        return *reinterpret_cast<T*>(&m_storage);
    }
    const T& value() const
    {
        ASSERT(!m_is_error);
        return *reinterpret_cast<T*>(&m_storage);
    }

    T release_value()
    {
        ASSERT(!m_is_error);
        T released_value = *reinterpret_cast<T*>(&m_storage);
        value().~T();
        return released_value;
    }

private:
    alignas(T) char m_storage[sizeof(T)];
    KResult m_error;
    bool m_is_error { false };
};
