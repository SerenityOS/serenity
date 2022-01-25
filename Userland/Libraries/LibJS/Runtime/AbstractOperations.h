/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibCrypto/Forward.h>
#include <LibJS/AST.h>
#include <LibJS/Forward.h>
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
Object* get_super_constructor(VM&);
ThrowCompletionOr<Reference> make_super_property_reference(GlobalObject&, Value actual_this, PropertyKey const&, bool strict);
ThrowCompletionOr<Value> require_object_coercible(GlobalObject&, Value);
ThrowCompletionOr<Value> call_impl(GlobalObject&, Value function, Value this_value, Optional<MarkedValueList> = {});
ThrowCompletionOr<Value> call_impl(GlobalObject&, FunctionObject& function, Value this_value, Optional<MarkedValueList> = {});
ThrowCompletionOr<Object*> construct_impl(GlobalObject&, FunctionObject&, Optional<MarkedValueList> = {}, FunctionObject* new_target = nullptr);
ThrowCompletionOr<size_t> length_of_array_like(GlobalObject&, Object const&);
ThrowCompletionOr<MarkedValueList> create_list_from_array_like(GlobalObject&, Value, Function<ThrowCompletionOr<void>(Value)> = {});
ThrowCompletionOr<FunctionObject*> species_constructor(GlobalObject&, Object const&, FunctionObject& default_constructor);
ThrowCompletionOr<Realm*> get_function_realm(GlobalObject&, FunctionObject const&);
ThrowCompletionOr<void> initialize_bound_name(GlobalObject&, FlyString const&, Value, Environment*);
bool is_compatible_property_descriptor(bool extensible, PropertyDescriptor const&, Optional<PropertyDescriptor> const& current);
bool validate_and_apply_property_descriptor(Object*, PropertyKey const&, bool extensible, PropertyDescriptor const&, Optional<PropertyDescriptor> const& current);
ThrowCompletionOr<Object*> get_prototype_from_constructor(GlobalObject&, FunctionObject const& constructor, Object* (GlobalObject::*intrinsic_default_prototype)());
Object* create_unmapped_arguments_object(GlobalObject&, Span<Value> arguments);
Object* create_mapped_arguments_object(GlobalObject&, FunctionObject&, Vector<FunctionNode::Parameter> const&, Span<Value> arguments, Environment&);
Value canonical_numeric_index_string(GlobalObject&, PropertyKey const&);
ThrowCompletionOr<String> get_substitution(GlobalObject&, Utf16View const& matched, Utf16View const& str, size_t position, Span<Value> captures, Value named_captures, Value replacement);

enum class CallerMode {
    Strict,
    NonStrict
};
enum class EvalMode {
    Direct,
    Indirect
};
ThrowCompletionOr<Value> perform_eval(Value, GlobalObject&, CallerMode, EvalMode);

ThrowCompletionOr<void> eval_declaration_instantiation(VM& vm, GlobalObject& global_object, Program const& program, Environment* variable_environment, Environment* lexical_environment, PrivateEnvironment* private_environment, bool strict);

// 7.3.14 Call ( F, V [ , argumentsList ] ), https://tc39.es/ecma262/#sec-call
ALWAYS_INLINE ThrowCompletionOr<Value> call(GlobalObject& global_object, Value function, Value this_value, MarkedValueList arguments_list)
{
    return call_impl(global_object, function, this_value, move(arguments_list));
}

ALWAYS_INLINE ThrowCompletionOr<Value> call(GlobalObject& global_object, Value function, Value this_value, Optional<MarkedValueList> arguments_list)
{
    return call_impl(global_object, function, this_value, move(arguments_list));
}

template<typename... Args>
ALWAYS_INLINE ThrowCompletionOr<Value> call(GlobalObject& global_object, Value function, Value this_value, Args&&... args)
{
    if constexpr (sizeof...(Args) > 0) {
        MarkedValueList arguments_list { global_object.heap() };
        (..., arguments_list.append(forward<Args>(args)));
        return call_impl(global_object, function, this_value, move(arguments_list));
    }

    return call_impl(global_object, function, this_value);
}

ALWAYS_INLINE ThrowCompletionOr<Value> call(GlobalObject& global_object, FunctionObject& function, Value this_value, MarkedValueList arguments_list)
{
    return call_impl(global_object, function, this_value, move(arguments_list));
}

ALWAYS_INLINE ThrowCompletionOr<Value> call(GlobalObject& global_object, FunctionObject& function, Value this_value, Optional<MarkedValueList> arguments_list)
{
    return call_impl(global_object, function, this_value, move(arguments_list));
}

template<typename... Args>
ALWAYS_INLINE ThrowCompletionOr<Value> call(GlobalObject& global_object, FunctionObject& function, Value this_value, Args&&... args)
{
    if constexpr (sizeof...(Args) > 0) {
        MarkedValueList arguments_list { global_object.heap() };
        (..., arguments_list.append(forward<Args>(args)));
        return call_impl(global_object, function, this_value, move(arguments_list));
    }

    return call_impl(global_object, function, this_value);
}

// 7.3.15 Construct ( F [ , argumentsList [ , newTarget ] ] ), https://tc39.es/ecma262/#sec-construct
template<typename... Args>
ALWAYS_INLINE ThrowCompletionOr<Object*> construct(GlobalObject& global_object, FunctionObject& function, Args&&... args)
{
    if constexpr (sizeof...(Args) > 0) {
        MarkedValueList arguments_list { global_object.heap() };
        (..., arguments_list.append(forward<Args>(args)));
        return construct_impl(global_object, function, move(arguments_list));
    }

    return construct_impl(global_object, function);
}

ALWAYS_INLINE ThrowCompletionOr<Object*> construct(GlobalObject& global_object, FunctionObject& function, MarkedValueList arguments_list, FunctionObject* new_target = nullptr)
{
    return construct_impl(global_object, function, move(arguments_list), new_target);
}

ALWAYS_INLINE ThrowCompletionOr<Object*> construct(GlobalObject& global_object, FunctionObject& function, Optional<MarkedValueList> arguments_list, FunctionObject* new_target = nullptr)
{
    return construct_impl(global_object, function, move(arguments_list), new_target);
}

// 10.1.13 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto [ , internalSlotsList ] ), https://tc39.es/ecma262/#sec-ordinarycreatefromconstructor
template<typename T, typename... Args>
ThrowCompletionOr<T*> ordinary_create_from_constructor(GlobalObject& global_object, FunctionObject const& constructor, Object* (GlobalObject::*intrinsic_default_prototype)(), Args&&... args)
{
    auto* prototype = TRY(get_prototype_from_constructor(global_object, constructor, intrinsic_default_prototype));
    return global_object.heap().allocate<T>(global_object, forward<Args>(args)..., *prototype);
}

// x modulo y, https://tc39.es/ecma262/#eqn-modulo
template<typename T, typename U>
auto modulo(T x, U y) requires(IsArithmetic<T>, IsArithmetic<U>)
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
    VERIFY(y != "0"_bigint);
    auto result = x.divided_by(y).remainder;
    if (result.is_negative())
        result = result.plus(y);
    return result;
}

}
