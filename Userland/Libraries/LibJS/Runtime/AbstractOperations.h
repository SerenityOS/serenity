/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Forward.h>
#include <LibCrypto/Forward.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Runtime/CanonicalIndex.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrivateEnvironment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

DeclarativeEnvironment* new_declarative_environment(Environment&);
ObjectEnvironment* new_object_environment(Object&, bool is_with_environment, Environment*);
FunctionEnvironment* new_function_environment(ECMAScriptFunctionObject&, Object* new_target);
PrivateEnvironment* new_private_environment(VM& vm, PrivateEnvironment* outer);
Environment& get_this_environment(VM&);
bool can_be_held_weakly(Value);
Object* get_super_constructor(VM&);
ThrowCompletionOr<Reference> make_super_property_reference(VM&, Value actual_this, PropertyKey const&, bool strict);
ThrowCompletionOr<Value> require_object_coercible(VM&, Value);
ThrowCompletionOr<Value> call_impl(VM&, Value function, Value this_value, Optional<MarkedVector<Value>> = {});
ThrowCompletionOr<Value> call_impl(VM&, FunctionObject& function, Value this_value, Optional<MarkedVector<Value>> = {});
ThrowCompletionOr<Object*> construct_impl(VM&, FunctionObject&, Optional<MarkedVector<Value>> = {}, FunctionObject* new_target = nullptr);
ThrowCompletionOr<size_t> length_of_array_like(VM&, Object const&);
ThrowCompletionOr<MarkedVector<Value>> create_list_from_array_like(VM&, Value, Function<ThrowCompletionOr<void>(Value)> = {});
ThrowCompletionOr<FunctionObject*> species_constructor(VM&, Object const&, FunctionObject& default_constructor);
ThrowCompletionOr<Realm*> get_function_realm(VM&, FunctionObject const&);
ThrowCompletionOr<void> initialize_bound_name(VM&, FlyString const&, Value, Environment*);
bool is_compatible_property_descriptor(bool extensible, PropertyDescriptor const&, Optional<PropertyDescriptor> const& current);
bool validate_and_apply_property_descriptor(Object*, PropertyKey const&, bool extensible, PropertyDescriptor const&, Optional<PropertyDescriptor> const& current);
ThrowCompletionOr<Object*> get_prototype_from_constructor(VM&, FunctionObject const& constructor, Object* (Intrinsics::*intrinsic_default_prototype)());
Object* create_unmapped_arguments_object(VM&, Span<Value> arguments);
Object* create_mapped_arguments_object(VM&, FunctionObject&, Vector<FunctionParameter> const&, Span<Value> arguments, Environment&);

enum class CanonicalIndexMode {
    DetectNumericRoundtrip,
    IgnoreNumericRoundtrip,
};
CanonicalIndex canonical_numeric_index_string(PropertyKey const&, CanonicalIndexMode needs_numeric);
ThrowCompletionOr<DeprecatedString> get_substitution(VM&, Utf16View const& matched, Utf16View const& str, size_t position, Span<Value> captures, Value named_captures, Value replacement);

enum class CallerMode {
    Strict,
    NonStrict
};
enum class EvalMode {
    Direct,
    Indirect
};
ThrowCompletionOr<Value> perform_eval(VM&, Value, CallerMode, EvalMode);

ThrowCompletionOr<void> eval_declaration_instantiation(VM& vm, Program const& program, Environment* variable_environment, Environment* lexical_environment, PrivateEnvironment* private_environment, bool strict);

// 7.3.14 Call ( F, V [ , argumentsList ] ), https://tc39.es/ecma262/#sec-call
ALWAYS_INLINE ThrowCompletionOr<Value> call(VM& vm, Value function, Value this_value, MarkedVector<Value> arguments_list)
{
    return call_impl(vm, function, this_value, move(arguments_list));
}

ALWAYS_INLINE ThrowCompletionOr<Value> call(VM& vm, Value function, Value this_value, Optional<MarkedVector<Value>> arguments_list)
{
    return call_impl(vm, function, this_value, move(arguments_list));
}

template<typename... Args>
ALWAYS_INLINE ThrowCompletionOr<Value> call(VM& vm, Value function, Value this_value, Args&&... args)
{
    if constexpr (sizeof...(Args) > 0) {
        MarkedVector<Value> arguments_list { vm.heap() };
        (..., arguments_list.append(forward<Args>(args)));
        return call_impl(vm, function, this_value, move(arguments_list));
    }

    return call_impl(vm, function, this_value);
}

ALWAYS_INLINE ThrowCompletionOr<Value> call(VM& vm, FunctionObject& function, Value this_value, MarkedVector<Value> arguments_list)
{
    return call_impl(vm, function, this_value, move(arguments_list));
}

ALWAYS_INLINE ThrowCompletionOr<Value> call(VM& vm, FunctionObject& function, Value this_value, Optional<MarkedVector<Value>> arguments_list)
{
    return call_impl(vm, function, this_value, move(arguments_list));
}

template<typename... Args>
ALWAYS_INLINE ThrowCompletionOr<Value> call(VM& vm, FunctionObject& function, Value this_value, Args&&... args)
{
    if constexpr (sizeof...(Args) > 0) {
        MarkedVector<Value> arguments_list { vm.heap() };
        (..., arguments_list.append(forward<Args>(args)));
        return call_impl(vm, function, this_value, move(arguments_list));
    }

    return call_impl(vm, function, this_value);
}

// 7.3.15 Construct ( F [ , argumentsList [ , newTarget ] ] ), https://tc39.es/ecma262/#sec-construct
template<typename... Args>
ALWAYS_INLINE ThrowCompletionOr<Object*> construct(VM& vm, FunctionObject& function, Args&&... args)
{
    if constexpr (sizeof...(Args) > 0) {
        MarkedVector<Value> arguments_list { vm.heap() };
        (..., arguments_list.append(forward<Args>(args)));
        return construct_impl(vm, function, move(arguments_list));
    }

    return construct_impl(vm, function);
}

ALWAYS_INLINE ThrowCompletionOr<Object*> construct(VM& vm, FunctionObject& function, MarkedVector<Value> arguments_list, FunctionObject* new_target = nullptr)
{
    return construct_impl(vm, function, move(arguments_list), new_target);
}

ALWAYS_INLINE ThrowCompletionOr<Object*> construct(VM& vm, FunctionObject& function, Optional<MarkedVector<Value>> arguments_list, FunctionObject* new_target = nullptr)
{
    return construct_impl(vm, function, move(arguments_list), new_target);
}

// 10.1.13 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto [ , internalSlotsList ] ), https://tc39.es/ecma262/#sec-ordinarycreatefromconstructor
template<typename T, typename... Args>
ThrowCompletionOr<T*> ordinary_create_from_constructor(VM& vm, FunctionObject const& constructor, Object* (Intrinsics::*intrinsic_default_prototype)(), Args&&... args)
{
    auto& realm = *vm.current_realm();
    auto* prototype = TRY(get_prototype_from_constructor(vm, constructor, intrinsic_default_prototype));
    return realm.heap().allocate<T>(realm, forward<Args>(args)..., *prototype);
}

// 14.1 MergeLists ( a, b ), https://tc39.es/proposal-temporal/#sec-temporal-mergelists
template<typename T>
Vector<T> merge_lists(Vector<T> const& a, Vector<T> const& b)
{
    // 1. Let merged be a new empty List.
    Vector<T> merged;

    // 2. For each element element of a, do
    for (auto const& element : a) {
        // a. If merged does not contain element, then
        if (!merged.contains_slow(element)) {
            // i. Append element to merged.
            merged.append(element);
        }
    }

    // 3. For each element element of b, do
    for (auto const& element : b) {
        // a. If merged does not contain element, then
        if (!merged.contains_slow(element)) {
            // i. Append element to merged.
            merged.append(element);
        }
    }

    // 4. Return merged.
    return merged;
}

// x modulo y, https://tc39.es/ecma262/#eqn-modulo
template<Arithmetic T, Arithmetic U>
auto modulo(T x, U y)
{
    // The notation “x modulo y” (y must be finite and non-zero) computes a value k of the same sign as y (or zero) such that abs(k) < abs(y) and x - k = q × y for some integer q.
    VERIFY(y != 0);
    if constexpr (IsFloatingPoint<T> || IsFloatingPoint<U>) {
        if constexpr (IsFloatingPoint<U>)
            VERIFY(isfinite(y));
        return fmod(fmod(x, y) + y, y);
    } else {
        return ((x % y) + y) % y;
    }
}

auto modulo(Crypto::BigInteger auto const& x, Crypto::BigInteger auto const& y)
{
    VERIFY(!y.is_zero());
    auto result = x.divided_by(y).remainder;
    if (result.is_negative())
        result = result.plus(y);
    return result;
}

}
