/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <LibJS/Runtime/Completion.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::WebIDL {

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
    ExceptionOr() requires(IsSame<ValueType, Empty>)
        : m_result(Empty {})
    {
    }

    ExceptionOr(ValueType const& result)
        : m_result(result)
    {
    }

    ExceptionOr(ValueType&& result)
        : m_result(move(result))
    {
    }

    // Allows implicit construction of ExceptionOr<T> from a type U if T(U) is a supported constructor.
    // Most commonly: Value from Object* or similar, so we can omit the curly braces from "return { TRY(...) };".
    // Disabled for POD types to avoid weird conversion shenanigans.
    template<typename WrappedValueType>
    ExceptionOr(WrappedValueType result) requires(!IsPOD<ValueType>)
        : m_result(move(result))
    {
    }

    ExceptionOr(JS::NonnullGCPtr<DOMException> exception)
        : m_exception(move(exception))
    {
    }

    ExceptionOr(SimpleException exception)
        : m_exception(move(exception))
    {
    }

    ExceptionOr(JS::Completion exception)
        : m_exception(move(exception))
    {
        auto const& completion = m_exception.get<JS::Completion>();
        VERIFY(completion.is_error());
    }

    ExceptionOr(Variant<SimpleException, JS::NonnullGCPtr<DOMException>, JS::Completion> exception)
        : m_exception(move(exception).template downcast<Empty, SimpleException, JS::NonnullGCPtr<DOMException>, JS::Completion>())
    {
        if (auto* completion = m_exception.template get_pointer<JS::Completion>())
            VERIFY(completion->is_error());
    }

    ExceptionOr(ExceptionOr&& other) = default;
    ExceptionOr(ExceptionOr const& other) = default;
    ~ExceptionOr() = default;

    ValueType& value() requires(!IsSame<ValueType, Empty>)
    {
        return m_result.value();
    }

    ValueType release_value()
    {
        return m_result.release_value();
    }

    Variant<SimpleException, JS::NonnullGCPtr<DOMException>, JS::Completion> exception() const
    {
        return m_exception.template downcast<SimpleException, JS::NonnullGCPtr<DOMException>, JS::Completion>();
    }

    bool is_exception() const
    {
        return !m_exception.template has<Empty>();
    }

    // These are for compatibility with the TRY() macro in AK.
    [[nodiscard]] bool is_error() const { return is_exception(); }
    Variant<SimpleException, JS::NonnullGCPtr<DOMException>, JS::Completion> release_error() { return exception(); }

private:
    Optional<ValueType> m_result;

    // https://webidl.spec.whatwg.org/#idl-exceptions
    Variant<Empty, SimpleException, JS::NonnullGCPtr<DOMException>, JS::Completion> m_exception {};
};

template<>
class ExceptionOr<void> : public ExceptionOr<Empty> {
public:
    using ExceptionOr<Empty>::ExceptionOr;
};

}
