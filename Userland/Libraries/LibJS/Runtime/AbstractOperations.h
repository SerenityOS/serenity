/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

Value require_object_coercible(GlobalObject&, Value);
Function* get_method(GlobalObject& global_object, Value, PropertyName const&);
size_t length_of_array_like(GlobalObject&, Object const&);
MarkedValueList create_list_from_array_like(GlobalObject&, Value, AK::Function<Result<void, ErrorType>(Value)> = {});
Function* species_constructor(GlobalObject&, Object const&, Function& default_constructor);
GlobalObject* get_function_realm(GlobalObject&, Function const&);
Object* get_prototype_from_constructor(GlobalObject&, Function const& constructor, Object* (GlobalObject::*intrinsic_default_prototype)());

// 10.1.13 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto [ , internalSlotsList ] ), https://tc39.es/ecma262/#sec-ordinarycreatefromconstructor
template<typename T, typename... Args>
T* ordinary_create_from_constructor(GlobalObject& global_object, Function const& constructor, Object* (GlobalObject::*intrinsic_default_prototype)(), Args&&... args)
{
    auto& vm = global_object.vm();
    auto* prototype = get_prototype_from_constructor(global_object, constructor, intrinsic_default_prototype);
    if (vm.exception())
        return nullptr;
    return global_object.heap().allocate<T>(global_object, forward<Args>(args)..., *prototype);
}

}
