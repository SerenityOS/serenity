/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/AST.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

DeclarativeEnvironment* new_declarative_environment(Environment&);
ObjectEnvironment* new_object_environment(Object&, bool is_with_environment, Environment*);
Environment& get_this_environment(VM&);
Object* get_super_constructor(VM&);
Reference make_super_property_reference(GlobalObject&, Value actual_this, StringOrSymbol const& property_key, bool strict);
ThrowCompletionOr<Value> require_object_coercible(GlobalObject&, Value);
size_t length_of_array_like(GlobalObject&, Object const&);
ThrowCompletionOr<MarkedValueList> create_list_from_array_like(GlobalObject&, Value, Function<ThrowCompletionOr<void>(Value)> = {});
ThrowCompletionOr<FunctionObject*> species_constructor(GlobalObject&, Object const&, FunctionObject& default_constructor);
ThrowCompletionOr<Realm*> get_function_realm(GlobalObject&, FunctionObject const&);
bool is_compatible_property_descriptor(bool extensible, PropertyDescriptor const&, Optional<PropertyDescriptor> const& current);
bool validate_and_apply_property_descriptor(Object*, PropertyName const&, bool extensible, PropertyDescriptor const&, Optional<PropertyDescriptor> const& current);
Object* get_prototype_from_constructor(GlobalObject&, FunctionObject const& constructor, Object* (GlobalObject::*intrinsic_default_prototype)());
Object* create_unmapped_arguments_object(GlobalObject&, Span<Value> arguments);
Object* create_mapped_arguments_object(GlobalObject&, FunctionObject&, Vector<FunctionNode::Parameter> const&, Span<Value> arguments, Environment&);
Value canonical_numeric_index_string(GlobalObject&, PropertyName const&);
String get_substitution(GlobalObject&, Utf16View const& matched, Utf16View const& str, size_t position, Span<Value> captures, Value named_captures, Value replacement);

enum class CallerMode {
    Strict,
    NonStrict
};
enum class EvalMode {
    Direct,
    Indirect
};
Value perform_eval(Value, GlobalObject&, CallerMode, EvalMode);

// 10.1.13 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto [ , internalSlotsList ] ), https://tc39.es/ecma262/#sec-ordinarycreatefromconstructor
template<typename T, typename... Args>
T* ordinary_create_from_constructor(GlobalObject& global_object, FunctionObject const& constructor, Object* (GlobalObject::*intrinsic_default_prototype)(), Args&&... args)
{
    auto& vm = global_object.vm();
    auto* prototype = get_prototype_from_constructor(global_object, constructor, intrinsic_default_prototype);
    if (vm.exception())
        return nullptr;
    return global_object.heap().allocate<T>(global_object, forward<Args>(args)..., *prototype);
}

}
