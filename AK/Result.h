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
#include <AK/Optional.h>

namespace AK {

template<typename ValueType, typename ErrorType>
class [[nodiscard]] Result
{
public:
    Result(const ValueType& res)
        : m_result(res)
    {
    }
    Result(ValueType && res)
        : m_result(move(res))
    {
    }
    Result(const ErrorType& error)
        : m_error(error)
    {
    }
    Result(ErrorType && error)
        : m_error(move(error))
    {
    }

    Result(const ValueType& res, const ErrorType& error)
        : m_result(res)
        , m_error(error)
    {
    }

    // FIXME: clang-format gets confused about Result. Why?
    // clang-format off
    Result(Result&& other)
        // clang-format on
        : m_result(move(other.m_result))
        , m_error(move(other.m_error))
    {
    }

    // FIXME: clang-format gets confused about Result. Why?
    // clang-format off
    Result(Result& other)
        // clang-format on
        : m_result(other.m_result)
        , m_error(other.m_error)
    {
    }

    ~Result()
    {
    }

    ValueType& value()
    {
        return m_result.value();
    }

    ErrorType& error()
    {
        return m_error.value();
    }

    bool is_error() const
    {
        return m_error.has_value();
    }

private:
    Optional<ValueType> m_result;
    Optional<ErrorType> m_error;
};

}

using AK::Result;
