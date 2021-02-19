/*
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
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

#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <LibWeb/DOM/DOMException.h>

namespace Web::DOM {

template<typename ValueType>
class ExceptionOr {
public:
    ExceptionOr(const ValueType& result)
        : m_result(result)
    {
    }

    ExceptionOr(ValueType&& result)
        : m_result(move(result))
    {
    }

    ExceptionOr(const NonnullRefPtr<DOMException> exception)
        : m_exception(exception)
    {
    }

    ExceptionOr(ExceptionOr&& other) = default;
    ExceptionOr(const ExceptionOr& other) = default;
    ~ExceptionOr() = default;

    ValueType& value()
    {
        return m_result.value();
    }

    ValueType release_value()
    {
        return m_result.release_value();
    }

    const DOMException& exception() const
    {
        return *m_exception;
    }

    bool is_exception() const
    {
        return m_exception;
    }

private:
    Optional<ValueType> m_result;
    RefPtr<DOMException> m_exception;
};

template<>
class ExceptionOr<void> {
public:
    ExceptionOr(const NonnullRefPtr<DOMException> exception)
        : m_exception(exception)
    {
    }

    ExceptionOr() = default;
    ExceptionOr(ExceptionOr&& other) = default;
    ExceptionOr(const ExceptionOr& other) = default;
    ~ExceptionOr() = default;

    const DOMException& exception() const
    {
        return *m_exception;
    }

    bool is_exception() const
    {
        return m_exception;
    }

private:
    RefPtr<DOMException> m_exception;
};

}
