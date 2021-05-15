/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
