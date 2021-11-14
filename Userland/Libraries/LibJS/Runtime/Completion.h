/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Optional.h>
#include <AK/Try.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// Temporary helper akin to TRY(), but returning a default-constructed type (e.g. empty JS::Value)
// instead of the throw completion record. Use this as the bridge between functions that have
// already been updated to use completions and functions that haven't.
#define TRY_OR_DISCARD(expression)             \
    ({                                         \
        auto _temporary_result = (expression); \
        if (_temporary_result.is_error())      \
            return {};                         \
        _temporary_result.release_value();     \
    })

// 6.2.3 The Completion Record Specification Type, https://tc39.es/ecma262/#sec-completion-record-specification-type
class [[nodiscard]] Completion {
public:
    enum class Type {
        Normal,
        Break,
        Continue,
        Return,
        Throw,
    };

    Completion(Type type, Optional<Value> value, Optional<FlyString> target)
        : m_type(type)
        , m_value(move(value))
        , m_target(move(target))
    {
        if (m_value.has_value())
            VERIFY(!m_value->is_empty());
    }

    Completion(ThrowCompletionOr<Value> const&);

    // 5.2.3.1 Implicit Completion Values, https://tc39.es/ecma262/#sec-implicit-completion-values
    // Not `explicit` on purpose.
    Completion(Value value)
        : Completion(Type::Normal, value, {})
    {
    }
    Completion()
        : Completion(js_undefined())
    {
    }

    [[nodiscard]] Type type() const { return m_type; }

    [[nodiscard]] bool has_value() const { return m_value.has_value(); }
    [[nodiscard]] Value value() const { return *m_value; }

    [[nodiscard]] bool has_target() const { return m_target.has_value(); }
    [[nodiscard]] FlyString const& target() const { return *m_target; }

    // "abrupt completion refers to any completion with a [[Type]] value other than normal"
    [[nodiscard]] bool is_abrupt() const { return m_type != Type::Normal; }

    // These are for compatibility with the TRY() macro in AK.
    [[nodiscard]] bool is_error() const { return m_type == Type::Throw; }
    [[nodiscard]] Value release_value() { return m_value.release_value(); }
    Completion release_error()
    {
        VERIFY(is_error());
        return { m_type, release_value(), move(m_target) };
    }

    // 6.2.3.4 UpdateEmpty ( completionRecord, value ), https://tc39.es/ecma262/#sec-updateempty
    Completion update_empty(Value value) const
    {
        // 1. Assert: If completionRecord.[[Type]] is either return or throw, then completionRecord.[[Value]] is not empty.
        if (m_type == Type::Return || m_type == Type::Throw)
            VERIFY(m_value.has_value());

        // 2. If completionRecord.[[Value]] is not empty, return Completion(completionRecord).
        if (m_value.has_value())
            return *this;

        // 3. Return Completion { [[Type]]: completionRecord.[[Type]], [[Value]]: value, [[Target]]: completionRecord.[[Target]] }.
        return { m_type, value, m_target };
    }

private:
    Type m_type { Type::Normal }; // [[Type]]
    Optional<Value> m_value;      // [[Value]]
    Optional<FlyString> m_target; // [[Target]]
};

template<typename ValueType>
class [[nodiscard]] ThrowCompletionOr {
public:
    ThrowCompletionOr() requires(IsSame<ValueType, Empty>)
        : m_value(Empty {})
    {
    }

    // Not `explicit` on purpose so that `return vm.throw_completion<Error>(...);` is possible.
    ThrowCompletionOr(Completion throw_completion)
        : m_throw_completion(move(throw_completion))
    {
        VERIFY(throw_completion.is_error());
    }

    // Not `explicit` on purpose so that `return value;` is possible.
    ThrowCompletionOr(ValueType value)
        : m_value(move(value))
    {
        if constexpr (IsSame<ValueType, Value>)
            VERIFY(!value.is_empty());
    }

    // Allows implicit construction of ThrowCompletionOr<T> from a type U if T(U) is a supported constructor.
    // Most commonly: Value from Object* or similar, so we can omit the curly braces from "return { TRY(...) };".
    // Disabled for POD types to avoid weird conversion shenanigans.
    template<typename WrappedValueType>
    ThrowCompletionOr(WrappedValueType value) requires(!IsPOD<ValueType>)
        : m_value(move(value))
    {
    }

    [[nodiscard]] bool is_throw_completion() const { return m_throw_completion.has_value(); }
    Completion const& throw_completion() const { return *m_throw_completion; }

    [[nodiscard]] bool has_value() const requires(!IsSame<ValueType, Empty>) { return m_value.has_value(); }
    [[nodiscard]] ValueType const& value() const requires(!IsSame<ValueType, Empty>) { return *m_value; }

    // These are for compatibility with the TRY() macro in AK.
    [[nodiscard]] bool is_error() const { return m_throw_completion.has_value(); }
    [[nodiscard]] ValueType release_value() { return m_value.release_value(); }
    Completion release_error() { return m_throw_completion.release_value(); }

private:
    Optional<Completion> m_throw_completion;
    Optional<ValueType> m_value;
};

template<>
class ThrowCompletionOr<void> : public ThrowCompletionOr<Empty> {
public:
    using ThrowCompletionOr<Empty>::ThrowCompletionOr;
};

ThrowCompletionOr<Value> await(GlobalObject&, Value);

// 6.2.3.2 NormalCompletion ( value ), https://tc39.es/ecma262/#sec-normalcompletion
inline Completion normal_completion(Optional<Value> value)
{
    return { Completion::Type::Normal, value, {} };
}

// 6.2.3.3 ThrowCompletion ( value ), https://tc39.es/ecma262/#sec-throwcompletion
inline Completion throw_completion(Value value)
{
    return { Completion::Type::Throw, value, {} };
}

}
