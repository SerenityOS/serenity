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
#include <AK/Platform.h>
#include <AK/Optional.h>

namespace AK {

template<typename T, typename E>
class CONSUMABLE(unknown) Result {
public:
    RETURN_TYPESTATE(unknown)
    Result(const T& res)
        : m_result(res)
    {}

    RETURN_TYPESTATE(unknown)
    Result(const E& error)
        : m_error(error)
    {
    }

    RETURN_TYPESTATE(unknown)
    Result(const T& res, const E& error)
        : m_result(res)
        , m_error(error)
    {
    }

    RETURN_TYPESTATE(unknown)
    Result(Result&& other)
        : m_result(move(other.m_result))
        , m_error(move(other.m_error))
    {
    }

    RETURN_TYPESTATE(unknown)
    Result(Result& other)
        : m_result(other.m_result)
        , m_error(other.m_error)
    {
    }

    CALLABLE_WHEN("unknown", "consumed")
    ~Result()
    {}

    CALLABLE_WHEN(consumed)
    T& unwrap() {
        return m_result.value();
    }

    CALLABLE_WHEN(consumed)
    E& error() {
        return m_error.value();
    }

    bool has_error() const {
        return m_error.has_value();
    }
    bool has_value() const {
        return m_result.has_value();
    }

    SET_TYPESTATE(consumed)
    bool failed() const {
        return m_error.value().failed();
    }

private:
    Optional<T> m_result;
    Optional<E> m_error;
};

}

using AK::Result;

