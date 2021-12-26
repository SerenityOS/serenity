/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/Result.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/ProxyObject.h>

namespace JS {

// Used in various abstract operations to make it obvious when a non-optional return value must be discarded.
static constexpr double INVALID { 0 };

// 7.2.1 RequireObjectCoercible ( argument ), https://tc39.es/ecma262/#sec-requireobjectcoercible
Value require_object_coercible(GlobalObject& global_object, Value value)
{
    auto& vm = global_object.vm();
    if (value.is_nullish()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotObjectCoercible, value.to_string_without_side_effects());
        return {};
    }
    return value;
}

// 7.3.10 GetMethod ( V, P ), https://tc39.es/ecma262/#sec-getmethod
Function* get_method(GlobalObject& global_object, Value value, PropertyName const& property_name)
{
    auto& vm = global_object.vm();
    auto* object = value.to_object(global_object);
    if (vm.exception())
        return nullptr;
    auto property_value = object->get(property_name);
    if (vm.exception())
        return nullptr;
    if (property_value.is_empty() || property_value.is_nullish())
        return nullptr;
    if (!property_value.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, property_value.to_string_without_side_effects());
        return nullptr;
    }
    return &property_value.as_function();
}

// 7.3.18 LengthOfArrayLike ( obj ), https://tc39.es/ecma262/#sec-lengthofarraylike
size_t length_of_array_like(GlobalObject& global_object, Object const& object)
{
    auto& vm = global_object.vm();
    auto result = object.get(vm.names.length).value_or(js_undefined());
    if (vm.exception())
        return INVALID;
    return result.to_length(global_object);
}

// 7.3.19 CreateListFromArrayLike ( obj [ , elementTypes ] ), https://tc39.es/ecma262/#sec-createlistfromarraylike
MarkedValueList create_list_from_array_like(GlobalObject& global_object, Value value, AK::Function<Result<void, ErrorType>(Value)> check_value)
{
    auto& vm = global_object.vm();
    auto& heap = global_object.heap();
    if (!value.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, value.to_string_without_side_effects());
        return MarkedValueList { heap };
    }
    auto& array_like = value.as_object();
    auto length = length_of_array_like(global_object, array_like);
    if (vm.exception())
        return MarkedValueList { heap };
    auto list = MarkedValueList { heap };
    for (size_t i = 0; i < length; ++i) {
        auto index_name = String::number(i);
        auto next = array_like.get(index_name).value_or(js_undefined());
        if (vm.exception())
            return MarkedValueList { heap };
        if (check_value) {
            auto result = check_value(next);
            if (result.is_error()) {
                vm.throw_exception<TypeError>(global_object, result.release_error());
                return MarkedValueList { heap };
            }
        }
        list.append(next);
    }
    return list;
}

// 7.3.22 SpeciesConstructor ( O, defaultConstructor ), https://tc39.es/ecma262/#sec-speciesconstructor
Function* species_constructor(GlobalObject& global_object, Object const& object, Function& default_constructor)
{
    auto& vm = global_object.vm();
    auto constructor = object.get(vm.names.constructor).value_or(js_undefined());
    if (vm.exception())
        return nullptr;
    if (constructor.is_undefined())
        return &default_constructor;
    if (!constructor.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());
        return nullptr;
    }
    auto species = constructor.as_object().get(vm.well_known_symbol_species()).value_or(js_undefined());
    if (species.is_nullish())
        return &default_constructor;
    if (species.is_constructor())
        return &species.as_function();
    vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, species.to_string_without_side_effects());
    return nullptr;
}

// 7.3.24 GetFunctionRealm ( obj ), https://tc39.es/ecma262/#sec-getfunctionrealm
GlobalObject* get_function_realm(GlobalObject& global_object, Function const& function)
{
    auto& vm = global_object.vm();

    // FIXME: not sure how to do this currently.
    // 2. If obj has a [[Realm]] internal slot, then
    //     a. Return obj.[[Realm]].
    if (is<BoundFunction>(function)) {
        auto& bound_function = static_cast<BoundFunction const&>(function);
        auto& target = bound_function.target_function();
        return get_function_realm(global_object, target);
    }
    if (is<ProxyObject>(function)) {
        auto& proxy = static_cast<ProxyObject const&>(function);
        if (proxy.is_revoked()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
            return nullptr;
        }
        auto& proxy_target = proxy.target();
        VERIFY(proxy_target.is_function());
        return get_function_realm(global_object, static_cast<Function const&>(proxy_target));
    }
    // 5. Return the current Realm Record.
    return &global_object;
}

// 10.1.14 GetPrototypeFromConstructor ( constructor, intrinsicDefaultProto )
Object* get_prototype_from_constructor(GlobalObject& global_object, Function const& constructor, Object* (GlobalObject::*intrinsic_default_prototype)())
{
    auto& vm = global_object.vm();
    auto prototype = constructor.get(vm.names.prototype);
    if (vm.exception())
        return nullptr;
    if (!prototype.is_object()) {
        auto* realm = get_function_realm(global_object, constructor);
        if (vm.exception())
            return nullptr;
        prototype = (realm->*intrinsic_default_prototype)();
    }
    return &prototype.as_object();
}

}
