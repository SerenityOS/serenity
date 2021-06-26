/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/Result.h>
#include <AK/TemporaryChange.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/DeclarativeEnvironmentRecord.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/FunctionEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/ObjectEnvironmentRecord.h>
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
    auto species = constructor.as_object().get(*vm.well_known_symbol_species()).value_or(js_undefined());
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

// 9.1.2.2 NewDeclarativeEnvironment ( E ), https://tc39.es/ecma262/#sec-newdeclarativeenvironment
DeclarativeEnvironmentRecord* new_declarative_environment(EnvironmentRecord& environment_record)
{
    auto& global_object = environment_record.global_object();
    return global_object.heap().allocate<DeclarativeEnvironmentRecord>(global_object, &environment_record);
}

// 9.1.2.3 NewObjectEnvironment ( O, W, E ), https://tc39.es/ecma262/#sec-newobjectenvironment
ObjectEnvironmentRecord* new_object_environment(Object& object, bool is_with_environment, EnvironmentRecord* environment_record)
{
    auto& global_object = object.global_object();
    return global_object.heap().allocate<ObjectEnvironmentRecord>(global_object, object, is_with_environment ? ObjectEnvironmentRecord::IsWithEnvironment::Yes : ObjectEnvironmentRecord::IsWithEnvironment::No, environment_record);
}

// 9.4.3 GetThisEnvironment ( ), https://tc39.es/ecma262/#sec-getthisenvironment
EnvironmentRecord& get_this_environment(VM& vm)
{
    for (auto* env = vm.lexical_environment(); env; env = env->outer_environment()) {
        if (env->has_this_binding())
            return *env;
    }
    VERIFY_NOT_REACHED();
}

// 13.3.7.2 GetSuperConstructor ( ), https://tc39.es/ecma262/#sec-getsuperconstructor
Object* get_super_constructor(VM& vm)
{
    auto& env = get_this_environment(vm);
    auto& active_function = verify_cast<FunctionEnvironmentRecord>(env).function_object();
    auto* super_constructor = active_function.prototype();
    return super_constructor;
}

// 19.2.1.1 PerformEval ( x, callerRealm, strictCaller, direct ), https://tc39.es/ecma262/#sec-performeval
Value perform_eval(Value x, GlobalObject& caller_realm, CallerMode strict_caller, EvalMode direct)
{
    VERIFY(direct == EvalMode::Direct || strict_caller == CallerMode::NonStrict);
    if (!x.is_string())
        return x;

    auto& vm = caller_realm.vm();
    auto& code_string = x.as_string();
    Parser parser { Lexer { code_string.string() } };
    auto program = parser.parse_program(strict_caller == CallerMode::Strict);

    if (parser.has_errors()) {
        auto& error = parser.errors()[0];
        vm.throw_exception<SyntaxError>(caller_realm, error.to_string());
        return {};
    }

    auto& interpreter = vm.interpreter();
    if (direct == EvalMode::Direct)
        return interpreter.execute_statement(caller_realm, program).value_or(js_undefined());

    TemporaryChange scope_change(vm.running_execution_context().lexical_environment, static_cast<EnvironmentRecord*>(&caller_realm.environment_record()));
    return interpreter.execute_statement(caller_realm, program).value_or(js_undefined());
}

}
