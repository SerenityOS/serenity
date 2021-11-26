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

#define ENUMERATE_SIMPLE_WEBIDL_EXCEPTION_TYPES(E) \
    E(EvalError)                                   \
    E(RangeError)                                  \
    E(ReferenceError)                              \
    E(TypeError)                                   \
    E(URIError)

#define E(x) x,
enum class SimpleExceptionType {
    ENUMERATE_SIMPLE_WEBIDL_EXCEPTION_TYPES(E)
};
#undef E

struct SimpleException {
    SimpleExceptionType type;
    String message;
};

template<typename ValueType>
class ExceptionOr {
public:
    ExceptionOr() requires(IsSame<ValueType, Empty>) = default;

    ExceptionOr(const ValueType& result)
        : m_result(result)
    {
    }

    ExceptionOr(ValueType&& result)
        : m_result(move(result))
    {
    }

    ExceptionOr(NonnullRefPtr<DOMException> exception)
        : m_exception(move(exception))
    {
    }

    ExceptionOr(SimpleException exception)
        : m_exception(move(exception))
    {
    }

    ExceptionOr(Variant<SimpleException, NonnullRefPtr<DOMException>> exception)
        : m_exception(move(exception).template downcast<Empty, SimpleException, NonnullRefPtr<DOMException>>())
    {
    }

    ExceptionOr(ExceptionOr&& other) = default;
    ExceptionOr(const ExceptionOr& other) = default;
    ~ExceptionOr() = default;

    ValueType& value() requires(!IsSame<ValueType, Empty>)
    {
        return m_result.value();
    }

    ValueType release_value() requires(!IsSame<ValueType, Empty>)
    {
        return m_result.release_value();
    }

    Variant<SimpleException, NonnullRefPtr<DOMException>> exception() const
    {
        return m_exception.template downcast<SimpleException, NonnullRefPtr<DOMException>>();
    }

    bool is_exception() const
    {
        return !m_exception.template has<Empty>();
    }

private:
    Optional<ValueType> m_result;
    // https://webidl.spec.whatwg.org/#idl-exceptions
    Variant<Empty, SimpleException, NonnullRefPtr<DOMException>> m_exception {};
};

template<>
class ExceptionOr<void> : public ExceptionOr<Empty> {
public:
    using ExceptionOr<Empty>::ExceptionOr;
};

}
