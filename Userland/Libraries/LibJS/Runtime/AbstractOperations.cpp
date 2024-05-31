/*
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/FloatingPointStringConversions.h>
#include <AK/Function.h>
#include <AK/Optional.h>
#include <AK/Utf16View.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/ModuleLoading.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/ArgumentsObject.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/ProxyObject.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/SuppressedError.h>
#include <LibJS/Runtime/ValueInlines.h>

namespace JS {

// 7.2.1 RequireObjectCoercible ( argument ), https://tc39.es/ecma262/#sec-requireobjectcoercible
ThrowCompletionOr<Value> require_object_coercible(VM& vm, Value value)
{
    if (value.is_nullish())
        return vm.throw_completion<TypeError>(ErrorType::NotObjectCoercible, value.to_string_without_side_effects());
    return value;
}

// 7.3.14 Call ( F, V [ , argumentsList ] ), https://tc39.es/ecma262/#sec-call
ThrowCompletionOr<Value> call_impl(VM& vm, Value function, Value this_value, ReadonlySpan<Value> arguments_list)
{
    // 1. If argumentsList is not present, set argumentsList to a new empty List.

    // 2. If IsCallable(F) is false, throw a TypeError exception.
    if (!function.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, function.to_string_without_side_effects());

    // 3. Return ? F.[[Call]](V, argumentsList).
    return function.as_function().internal_call(this_value, arguments_list);
}

ThrowCompletionOr<Value> call_impl(VM&, FunctionObject& function, Value this_value, ReadonlySpan<Value> arguments_list)
{
    // 1. If argumentsList is not present, set argumentsList to a new empty List.

    // 2. If IsCallable(F) is false, throw a TypeError exception.
    // Note: Called with a FunctionObject ref

    // 3. Return ? F.[[Call]](V, argumentsList).
    return function.internal_call(this_value, arguments_list);
}

// 7.3.15 Construct ( F [ , argumentsList [ , newTarget ] ] ), https://tc39.es/ecma262/#sec-construct
ThrowCompletionOr<NonnullGCPtr<Object>> construct_impl(VM&, FunctionObject& function, ReadonlySpan<Value> arguments_list, FunctionObject* new_target)
{
    // 1. If newTarget is not present, set newTarget to F.
    if (!new_target)
        new_target = &function;

    // 2. If argumentsList is not present, set argumentsList to a new empty List.

    // 3. Return ? F.[[Construct]](argumentsList, newTarget).
    return function.internal_construct(arguments_list, *new_target);
}

// 7.3.19 LengthOfArrayLike ( obj ), https://tc39.es/ecma262/#sec-lengthofarraylike
ThrowCompletionOr<size_t> length_of_array_like(VM& vm, Object const& object)
{
    // OPTIMIZATION: For Array objects with a magical "length" property, it should always reflect the size of indexed property storage.
    if (object.has_magical_length_property())
        return object.indexed_properties().array_like_size();

    // 1. Return ℝ(? ToLength(? Get(obj, "length"))).
    return TRY(object.get(vm.names.length)).to_length(vm);
}

// 7.3.20 CreateListFromArrayLike ( obj [ , elementTypes ] ), https://tc39.es/ecma262/#sec-createlistfromarraylike
ThrowCompletionOr<MarkedVector<Value>> create_list_from_array_like(VM& vm, Value value, Function<ThrowCompletionOr<void>(Value)> check_value)
{
    // 1. If elementTypes is not present, set elementTypes to « Undefined, Null, Boolean, String, Symbol, Number, BigInt, Object ».

    // 2. If Type(obj) is not Object, throw a TypeError exception.
    if (!value.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, value.to_string_without_side_effects());

    auto& array_like = value.as_object();

    // 3. Let len be ? LengthOfArrayLike(obj).
    auto length = TRY(length_of_array_like(vm, array_like));

    // 4. Let list be a new empty List.
    auto list = MarkedVector<Value> { vm.heap() };
    list.ensure_capacity(length);

    // 5. Let index be 0.
    // 6. Repeat, while index < len,
    for (size_t i = 0; i < length; ++i) {
        // a. Let indexName be ! ToString(𝔽(index)).
        auto index_name = PropertyKey { i };

        // b. Let next be ? Get(obj, indexName).
        auto next = TRY(array_like.get(index_name));

        // c. If Type(next) is not an element of elementTypes, throw a TypeError exception.
        if (check_value)
            TRY(check_value(next));

        // d. Append next as the last element of list.
        list.unchecked_append(next);
    }

    // 7. Return list.
    return ThrowCompletionOr(move(list));
}

// 7.3.23 SpeciesConstructor ( O, defaultConstructor ), https://tc39.es/ecma262/#sec-speciesconstructor
ThrowCompletionOr<FunctionObject*> species_constructor(VM& vm, Object const& object, FunctionObject& default_constructor)
{
    // 1. Let C be ? Get(O, "constructor").
    auto constructor = TRY(object.get(vm.names.constructor));

    // 2. If C is undefined, return defaultConstructor.
    if (constructor.is_undefined())
        return &default_constructor;

    // 3. If Type(C) is not Object, throw a TypeError exception.
    if (!constructor.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAConstructor, constructor.to_string_without_side_effects());

    // 4. Let S be ? Get(C, @@species).
    auto species = TRY(constructor.as_object().get(vm.well_known_symbol_species()));

    // 5. If S is either undefined or null, return defaultConstructor.
    if (species.is_nullish())
        return &default_constructor;

    // 6. If IsConstructor(S) is true, return S.
    if (species.is_constructor())
        return &species.as_function();

    // 7. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(ErrorType::NotAConstructor, species.to_string_without_side_effects());
}

// 7.3.25 GetFunctionRealm ( obj ), https://tc39.es/ecma262/#sec-getfunctionrealm
ThrowCompletionOr<Realm*> get_function_realm(VM& vm, FunctionObject const& function)
{
    // 1. If obj has a [[Realm]] internal slot, then
    if (function.realm()) {
        // a. Return obj.[[Realm]].
        return function.realm();
    }

    // 2. If obj is a bound function exotic object, then
    if (is<BoundFunction>(function)) {
        auto& bound_function = static_cast<BoundFunction const&>(function);

        // a. Let target be obj.[[BoundTargetFunction]].
        auto& target = bound_function.bound_target_function();

        // b. Return ? GetFunctionRealm(target).
        return get_function_realm(vm, target);
    }

    // 3. If obj is a Proxy exotic object, then
    if (is<ProxyObject>(function)) {
        auto& proxy = static_cast<ProxyObject const&>(function);

        // a. If obj.[[ProxyHandler]] is null, throw a TypeError exception.
        if (proxy.is_revoked())
            return vm.throw_completion<TypeError>(ErrorType::ProxyRevoked);

        // b. Let proxyTarget be obj.[[ProxyTarget]].
        auto& proxy_target = proxy.target();

        // c. Return ? GetFunctionRealm(proxyTarget).
        VERIFY(proxy_target.is_function());
        return get_function_realm(vm, static_cast<FunctionObject const&>(proxy_target));
    }

    // 4. Return the current Realm Record.
    return vm.current_realm();
}

// 8.5.2.1 InitializeBoundName ( name, value, environment ), https://tc39.es/ecma262/#sec-initializeboundname
ThrowCompletionOr<void> initialize_bound_name(VM& vm, DeprecatedFlyString const& name, Value value, Environment* environment)
{
    // 1. If environment is not undefined, then
    if (environment) {
        // FIXME: The normal is not included in the explicit resource management spec yet, so there is no spec link for it.
        // a. Perform ! environment.InitializeBinding(name, value, normal).
        MUST(environment->initialize_binding(vm, name, value, Environment::InitializeBindingHint::Normal));

        // b. Return unused.
        return {};
    }
    // 2. Else,
    else {
        // a. Let lhs be ? ResolveBinding(name).
        auto lhs = TRY(vm.resolve_binding(name));

        // b. Return ? PutValue(lhs, value).
        return TRY(lhs.put_value(vm, value));
    }

    VERIFY_NOT_REACHED();
}

// 10.1.6.2 IsCompatiblePropertyDescriptor ( Extensible, Desc, Current ), https://tc39.es/ecma262/#sec-iscompatiblepropertydescriptor
bool is_compatible_property_descriptor(bool extensible, PropertyDescriptor const& descriptor, Optional<PropertyDescriptor> const& current)
{
    // 1. Return ValidateAndApplyPropertyDescriptor(undefined, "", Extensible, Desc, Current).
    return validate_and_apply_property_descriptor(nullptr, "", extensible, descriptor, current);
}

// 10.1.6.3 ValidateAndApplyPropertyDescriptor ( O, P, extensible, Desc, current ), https://tc39.es/ecma262/#sec-validateandapplypropertydescriptor
bool validate_and_apply_property_descriptor(Object* object, PropertyKey const& property_key, bool extensible, PropertyDescriptor const& descriptor, Optional<PropertyDescriptor> const& current)
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. If current is undefined, then
    if (!current.has_value()) {
        // a. If extensible is false, return false.
        if (!extensible)
            return false;

        // b. If O is undefined, return true.
        if (object == nullptr)
            return true;

        // c. If IsAccessorDescriptor(Desc) is true, then
        if (descriptor.is_accessor_descriptor()) {
            // i. Create an own accessor property named P of object O whose [[Get]], [[Set]], [[Enumerable]], and [[Configurable]] attributes are set to the value of the corresponding field in Desc if Desc has that field, or to the attribute's default value otherwise.
            auto accessor = Accessor::create(object->vm(), descriptor.get.value_or(nullptr), descriptor.set.value_or(nullptr));
            object->storage_set(property_key, { accessor, descriptor.attributes() });
        }
        // d. Else,
        else {
            // i. Create an own data property named P of object O whose [[Value]], [[Writable]], [[Enumerable]], and [[Configurable]] attributes are set to the value of the corresponding field in Desc if Desc has that field, or to the attribute's default value otherwise.
            auto value = descriptor.value.value_or(js_undefined());
            object->storage_set(property_key, { value, descriptor.attributes() });
        }

        // e. Return true.
        return true;
    }

    // 3. Assert: current is a fully populated Property Descriptor.

    // 4. If Desc does not have any fields, return true.
    if (descriptor.is_empty())
        return true;

    // 5. If current.[[Configurable]] is false, then
    if (!*current->configurable) {
        // a. If Desc has a [[Configurable]] field and Desc.[[Configurable]] is true, return false.
        if (descriptor.configurable.has_value() && *descriptor.configurable)
            return false;

        // b. If Desc has an [[Enumerable]] field and SameValue(Desc.[[Enumerable]], current.[[Enumerable]]) is false, return false.
        if (descriptor.enumerable.has_value() && *descriptor.enumerable != *current->enumerable)
            return false;

        // c. If IsGenericDescriptor(Desc) is false and SameValue(IsAccessorDescriptor(Desc), IsAccessorDescriptor(current)) is false, return false.
        if (!descriptor.is_generic_descriptor() && (descriptor.is_accessor_descriptor() != current->is_accessor_descriptor()))
            return false;

        // d. If IsAccessorDescriptor(current) is true, then
        if (current->is_accessor_descriptor()) {
            // i. If Desc has a [[Get]] field and SameValue(Desc.[[Get]], current.[[Get]]) is false, return false.
            if (descriptor.get.has_value() && *descriptor.get != *current->get)
                return false;

            // ii. If Desc has a [[Set]] field and SameValue(Desc.[[Set]], current.[[Set]]) is false, return false.
            if (descriptor.set.has_value() && *descriptor.set != *current->set)
                return false;
        }
        // e. Else if current.[[Writable]] is false, then
        else if (!*current->writable) {
            // i. If Desc has a [[Writable]] field and Desc.[[Writable]] is true, return false.
            if (descriptor.writable.has_value() && *descriptor.writable)
                return false;

            // ii. If Desc has a [[Value]] field and SameValue(Desc.[[Value]], current.[[Value]]) is false, return false.
            if (descriptor.value.has_value() && (*descriptor.value != *current->value))
                return false;
        }
    }

    // 6. If O is not undefined, then
    if (object != nullptr) {
        // a. If IsDataDescriptor(current) is true and IsAccessorDescriptor(Desc) is true, then
        if (current->is_data_descriptor() && descriptor.is_accessor_descriptor()) {
            // i. If Desc has a [[Configurable]] field, let configurable be Desc.[[Configurable]], else let configurable be current.[[Configurable]].
            auto configurable = descriptor.configurable.value_or(*current->configurable);

            // ii. If Desc has a [[Enumerable]] field, let enumerable be Desc.[[Enumerable]], else let enumerable be current.[[Enumerable]].
            auto enumerable = descriptor.enumerable.value_or(*current->enumerable);

            // iii. Replace the property named P of object O with an accessor property having [[Configurable]] and [[Enumerable]] attributes set to configurable and enumerable, respectively, and each other attribute set to its corresponding value in Desc if present, otherwise to its default value.
            auto accessor = Accessor::create(object->vm(), descriptor.get.value_or(nullptr), descriptor.set.value_or(nullptr));
            PropertyAttributes attributes;
            attributes.set_enumerable(enumerable);
            attributes.set_configurable(configurable);
            object->storage_set(property_key, { accessor, attributes });
        }
        // b. Else if IsAccessorDescriptor(current) is true and IsDataDescriptor(Desc) is true, then
        else if (current->is_accessor_descriptor() && descriptor.is_data_descriptor()) {
            // i. If Desc has a [[Configurable]] field, let configurable be Desc.[[Configurable]], else let configurable be current.[[Configurable]].
            auto configurable = descriptor.configurable.value_or(*current->configurable);

            // ii. If Desc has a [[Enumerable]] field, let enumerable be Desc.[[Enumerable]], else let enumerable be current.[[Enumerable]].
            auto enumerable = descriptor.enumerable.value_or(*current->enumerable);

            // iii. Replace the property named P of object O with a data property having [[Configurable]] and [[Enumerable]] attributes set to configurable and enumerable, respectively, and each other attribute set to its corresponding value in Desc if present, otherwise to its default value.
            auto value = descriptor.value.value_or(js_undefined());
            PropertyAttributes attributes;
            attributes.set_writable(descriptor.writable.value_or(false));
            attributes.set_enumerable(enumerable);
            attributes.set_configurable(configurable);
            object->storage_set(property_key, { value, attributes });
        }
        // c. Else,
        else {
            // i. For each field of Desc, set the corresponding attribute of the property named P of object O to the value of the field.
            Value value;
            if (descriptor.is_accessor_descriptor() || (current->is_accessor_descriptor() && !descriptor.is_data_descriptor())) {
                auto getter = descriptor.get.value_or(current->get.value_or(nullptr));
                auto setter = descriptor.set.value_or(current->set.value_or(nullptr));
                value = Accessor::create(object->vm(), getter, setter);
            } else {
                value = descriptor.value.value_or(current->value.value_or({}));
            }
            PropertyAttributes attributes;
            attributes.set_writable(descriptor.writable.value_or(current->writable.value_or(false)));
            attributes.set_enumerable(descriptor.enumerable.value_or(current->enumerable.value_or(false)));
            attributes.set_configurable(descriptor.configurable.value_or(current->configurable.value_or(false)));
            object->storage_set(property_key, { value, attributes });
        }
    }

    // 7. Return true.
    return true;
}

// 10.1.14 GetPrototypeFromConstructor ( constructor, intrinsicDefaultProto ), https://tc39.es/ecma262/#sec-getprototypefromconstructor
ThrowCompletionOr<Object*> get_prototype_from_constructor(VM& vm, FunctionObject const& constructor, NonnullGCPtr<Object> (Intrinsics::*intrinsic_default_prototype)())
{
    // 1. Assert: intrinsicDefaultProto is this specification's name of an intrinsic object. The corresponding object must be an intrinsic that is intended to be used as the [[Prototype]] value of an object.

    // 2. Let proto be ? Get(constructor, "prototype").
    auto prototype = TRY(constructor.get(vm.names.prototype));

    // 3. If Type(proto) is not Object, then
    if (!prototype.is_object()) {
        // a. Let realm be ? GetFunctionRealm(constructor).
        auto* realm = TRY(get_function_realm(vm, constructor));

        // b. Set proto to realm's intrinsic object named intrinsicDefaultProto.
        prototype = (realm->intrinsics().*intrinsic_default_prototype)();
    }

    // 4. Return proto.
    return &prototype.as_object();
}

// 9.1.2.2 NewDeclarativeEnvironment ( E ), https://tc39.es/ecma262/#sec-newdeclarativeenvironment
NonnullGCPtr<DeclarativeEnvironment> new_declarative_environment(Environment& environment)
{
    auto& heap = environment.heap();

    // 1. Let env be a new Declarative Environment Record containing no bindings.
    // 2. Set env.[[OuterEnv]] to E.
    // 3. Return env.
    return heap.allocate_without_realm<DeclarativeEnvironment>(&environment);
}

// 9.1.2.3 NewObjectEnvironment ( O, W, E ), https://tc39.es/ecma262/#sec-newobjectenvironment
NonnullGCPtr<ObjectEnvironment> new_object_environment(Object& object, bool is_with_environment, Environment* environment)
{
    auto& heap = object.heap();

    // 1. Let env be a new Object Environment Record.
    // 2. Set env.[[BindingObject]] to O.
    // 3. Set env.[[IsWithEnvironment]] to W.
    // 4. Set env.[[OuterEnv]] to E.
    // 5. Return env.
    return heap.allocate_without_realm<ObjectEnvironment>(object, is_with_environment ? ObjectEnvironment::IsWithEnvironment::Yes : ObjectEnvironment::IsWithEnvironment::No, environment);
}

// 9.1.2.4 NewFunctionEnvironment ( F, newTarget ), https://tc39.es/ecma262/#sec-newfunctionenvironment
NonnullGCPtr<FunctionEnvironment> new_function_environment(ECMAScriptFunctionObject& function, Object* new_target)
{
    auto& heap = function.heap();

    // 1. Let env be a new function Environment Record containing no bindings.
    auto env = heap.allocate_without_realm<FunctionEnvironment>(function.environment());

    // 2. Set env.[[FunctionObject]] to F.
    env->set_function_object(function);

    // 3. If F.[[ThisMode]] is lexical, set env.[[ThisBindingStatus]] to lexical.
    if (function.this_mode() == ECMAScriptFunctionObject::ThisMode::Lexical)
        env->set_this_binding_status(FunctionEnvironment::ThisBindingStatus::Lexical);
    // 4. Else, set env.[[ThisBindingStatus]] to uninitialized.
    else
        env->set_this_binding_status(FunctionEnvironment::ThisBindingStatus::Uninitialized);

    // 5. Set env.[[NewTarget]] to newTarget.
    env->set_new_target(new_target ?: js_undefined());

    // 6. Set env.[[OuterEnv]] to F.[[Environment]].
    // NOTE: Done in step 1 via the FunctionEnvironment constructor.

    // 7. Return env.
    return env;
}

// 9.2.1.1 NewPrivateEnvironment ( outerPrivEnv ), https://tc39.es/ecma262/#sec-newprivateenvironment
NonnullGCPtr<PrivateEnvironment> new_private_environment(VM& vm, PrivateEnvironment* outer)
{
    // 1. Let names be a new empty List.
    // 2. Return the PrivateEnvironment Record { [[OuterPrivateEnvironment]]: outerPrivEnv, [[Names]]: names }.
    return vm.heap().allocate_without_realm<PrivateEnvironment>(outer);
}

// 9.4.3 GetThisEnvironment ( ), https://tc39.es/ecma262/#sec-getthisenvironment
NonnullGCPtr<Environment> get_this_environment(VM& vm)
{
    // 1. Let env be the running execution context's LexicalEnvironment.
    // 2. Repeat,
    for (auto* env = vm.lexical_environment(); env; env = env->outer_environment()) {
        // a. Let exists be env.HasThisBinding().
        // b. If exists is true, return env.
        if (env->has_this_binding())
            return *env;

        // c. Let outer be env.[[OuterEnv]].
        // d. Assert: outer is not null.
        // e. Set env to outer.
    }
    VERIFY_NOT_REACHED();
}

// 9.14 CanBeHeldWeakly ( v ), https://tc39.es/proposal-symbols-as-weakmap-keys/#sec-canbeheldweakly-abstract-operation
bool can_be_held_weakly(Value value)
{
    // 1. If Type(v) is Object, return true.
    if (value.is_object())
        return true;

    // 2. If Type(v) is Symbol, then
    if (value.is_symbol()) {
        // a. For each element e of the GlobalSymbolRegistry List (see 19.4.2.2), do
        //     i. If SameValue(e.[[Symbol]], v) is true, return false.
        // b. Return true.
        return !value.as_symbol().is_global();
    }

    // 3. Return false.
    return false;
}

// 13.3.7.2 GetSuperConstructor ( ), https://tc39.es/ecma262/#sec-getsuperconstructor
Object* get_super_constructor(VM& vm)
{
    // 1. Let envRec be GetThisEnvironment().
    auto env = get_this_environment(vm);

    // 2. Assert: envRec is a function Environment Record.
    // 3. Let activeFunction be envRec.[[FunctionObject]].
    // 4. Assert: activeFunction is an ECMAScript function object.
    auto& active_function = verify_cast<FunctionEnvironment>(*env).function_object();

    // 5. Let superConstructor be ! activeFunction.[[GetPrototypeOf]]().
    auto* super_constructor = MUST(active_function.internal_get_prototype_of());

    // 6. Return superConstructor.
    return super_constructor;
}

// 19.2.1.1 PerformEval ( x, strictCaller, direct ), https://tc39.es/ecma262/#sec-performeval
ThrowCompletionOr<Value> perform_eval(VM& vm, Value x, CallerMode strict_caller, EvalMode direct)
{
    // 1. Assert: If direct is false, then strictCaller is also false.
    VERIFY(direct == EvalMode::Direct || strict_caller == CallerMode::NonStrict);

    // 2. If Type(x) is not String, return x.
    if (!x.is_string())
        return x;

    // 3. Let evalRealm be the current Realm Record.
    auto& eval_realm = *vm.running_execution_context().realm;

    // 4. NOTE: In the case of a direct eval, evalRealm is the realm of both the caller of eval and of the eval function itself.
    // 5. Perform ? HostEnsureCanCompileStrings(evalRealm).
    TRY(vm.host_ensure_can_compile_strings(eval_realm));

    // 6. Let inFunction be false.
    bool in_function = false;

    // 7. Let inMethod be false.
    bool in_method = false;

    // 8. Let inDerivedConstructor be false.
    bool in_derived_constructor = false;

    // 9. Let inClassFieldInitializer be false.
    bool in_class_field_initializer = false;

    // 10. If direct is true, then
    if (direct == EvalMode::Direct) {
        // a. Let thisEnvRec be GetThisEnvironment().
        auto this_environment_record = get_this_environment(vm);

        // b. If thisEnvRec is a function Environment Record, then
        if (is<FunctionEnvironment>(*this_environment_record)) {
            auto& this_function_environment_record = static_cast<FunctionEnvironment&>(*this_environment_record);

            // i. Let F be thisEnvRec.[[FunctionObject]].
            auto& function = this_function_environment_record.function_object();

            // ii. Set inFunction to true.
            in_function = true;

            // iii. Set inMethod to thisEnvRec.HasSuperBinding().
            in_method = this_function_environment_record.has_super_binding();

            // iv. If F.[[ConstructorKind]] is derived, set inDerivedConstructor to true.
            if (function.constructor_kind() == ECMAScriptFunctionObject::ConstructorKind::Derived)
                in_derived_constructor = true;

            // v. Let classFieldInitializerName be F.[[ClassFieldInitializerName]].
            auto& class_field_initializer_name = function.class_field_initializer_name();

            // vi. If classFieldInitializerName is not empty, set inClassFieldInitializer to true.
            if (!class_field_initializer_name.has<Empty>())
                in_class_field_initializer = true;
        }
    }

    // 11. Perform the following substeps in an implementation-defined order, possibly interleaving parsing and error detection:
    //     a. Let script be ParseText(StringToCodePoints(x), Script).
    //     c. If script Contains ScriptBody is false, return undefined.
    //     d. Let body be the ScriptBody of script.
    //     NOTE: We do these next steps by passing initial state to the parser.
    //     e. If inFunction is false, and body Contains NewTarget, throw a SyntaxError exception.
    //     f. If inMethod is false, and body Contains SuperProperty, throw a SyntaxError exception.
    //     g. If inDerivedConstructor is false, and body Contains SuperCall, throw a SyntaxError exception.
    //     h. If inClassFieldInitializer is true, and ContainsArguments of body is true, throw a SyntaxError exception.
    auto& code_string = x.as_string();

    Parser::EvalInitialState initial_state {
        .in_eval_function_context = in_function,
        .allow_super_property_lookup = in_method,
        .allow_super_constructor_call = in_derived_constructor,
        .in_class_field_initializer = in_class_field_initializer,
    };

    Parser parser { Lexer { code_string.byte_string() }, Program::Type::Script, move(initial_state) };
    auto program = parser.parse_program(strict_caller == CallerMode::Strict);

    //     b. If script is a List of errors, throw a SyntaxError exception.
    if (parser.has_errors()) {
        auto& error = parser.errors()[0];
        return vm.throw_completion<SyntaxError>(error.to_string());
    }

    bool strict_eval = false;

    // 12. If strictCaller is true, let strictEval be true.
    if (strict_caller == CallerMode::Strict)
        strict_eval = true;
    // 13. Else, let strictEval be IsStrict of script.
    else
        strict_eval = program->is_strict_mode();

    // 14. Let runningContext be the running execution context.
    // 15. NOTE: If direct is true, runningContext will be the execution context that performed the direct eval. If direct is false, runningContext will be the execution context for the invocation of the eval function.
    auto& running_context = vm.running_execution_context();

    Environment* lexical_environment;
    Environment* variable_environment;
    PrivateEnvironment* private_environment;

    // 16. If direct is true, then
    if (direct == EvalMode::Direct) {
        // a. Let lexEnv be NewDeclarativeEnvironment(runningContext's LexicalEnvironment).
        lexical_environment = new_declarative_environment(*running_context.lexical_environment);

        // b. Let varEnv be runningContext's VariableEnvironment.
        variable_environment = running_context.variable_environment;

        // c. Let privateEnv be runningContext's PrivateEnvironment.
        private_environment = running_context.private_environment;
    }
    // 17. Else,
    else {
        // a. Let lexEnv be NewDeclarativeEnvironment(evalRealm.[[GlobalEnv]]).
        lexical_environment = new_declarative_environment(eval_realm.global_environment());

        // b. Let varEnv be evalRealm.[[GlobalEnv]].
        variable_environment = &eval_realm.global_environment();

        // c. Let privateEnv be null.
        private_environment = nullptr;
    }

    // 18. If strictEval is true, set varEnv to lexEnv.
    if (strict_eval)
        variable_environment = lexical_environment;

    if (direct == EvalMode::Direct && !strict_eval) {
        // NOTE: Non-strict direct eval() forces us to deoptimize variable accesses.
        //       Mark the variable environment chain as screwed since we will not be able
        //       to rely on cached environment coordinates from this point on.
        variable_environment->set_permanently_screwed_by_eval();
    }

    // 19. If runningContext is not already suspended, suspend runningContext.
    // FIXME: We don't have this concept yet.

    // 20. Let evalContext be a new ECMAScript code execution context.
    auto eval_context = ExecutionContext::create();

    // 21. Set evalContext's Function to null.
    // NOTE: This was done in the construction of eval_context.

    // 22. Set evalContext's Realm to evalRealm.
    eval_context->realm = &eval_realm;

    // 23. Set evalContext's ScriptOrModule to runningContext's ScriptOrModule.
    eval_context->script_or_module = running_context.script_or_module;

    // 24. Set evalContext's VariableEnvironment to varEnv.
    eval_context->variable_environment = variable_environment;

    // 25. Set evalContext's LexicalEnvironment to lexEnv.
    eval_context->lexical_environment = lexical_environment;

    // 26. Set evalContext's PrivateEnvironment to privateEnv.
    eval_context->private_environment = private_environment;

    // NOTE: This isn't in the spec, but we require it.
    eval_context->is_strict_mode = strict_eval;

    // 27. Push evalContext onto the execution context stack; evalContext is now the running execution context.
    TRY(vm.push_execution_context(*eval_context, {}));

    // NOTE: We use a ScopeGuard to automatically pop the execution context when any of the `TRY`s below return a throw completion.
    ScopeGuard pop_guard = [&] {
        // FIXME: 31. Suspend evalContext and remove it from the execution context stack.

        // 32. Resume the context that is now on the top of the execution context stack as the running execution context.
        vm.pop_execution_context();
    };

    // 28. Let result be Completion(EvalDeclarationInstantiation(body, varEnv, lexEnv, privateEnv, strictEval)).
    TRY(eval_declaration_instantiation(vm, program, variable_environment, lexical_environment, private_environment, strict_eval));

    Optional<Value> eval_result;

    // 29. If result.[[Type]] is normal, then
    //     a. Set result to the result of evaluating body.
    auto executable_result = Bytecode::Generator::generate_from_ast_node(vm, program, {});
    if (executable_result.is_error())
        return vm.throw_completion<InternalError>(ErrorType::NotImplemented, TRY_OR_THROW_OOM(vm, executable_result.error().to_string()));

    auto executable = executable_result.release_value();
    executable->name = "eval"sv;
    if (Bytecode::g_dump_bytecode)
        executable->dump();
    auto result_or_error = vm.bytecode_interpreter().run_executable(*executable, {});
    if (result_or_error.value.is_error())
        return result_or_error.value.release_error();

    auto& result = result_or_error.return_register_value;
    if (!result.is_empty())
        eval_result = result;

    // 30. If result.[[Type]] is normal and result.[[Value]] is empty, then
    //     a. Set result to NormalCompletion(undefined).
    // NOTE: Step 31 and 32 is handled by `pop_guard` above.
    // 33. Return ? result.
    // NOTE: Step 33 is also performed with each use of `TRY` above.
    return eval_result.value_or(js_undefined());
}

// 19.2.1.3 EvalDeclarationInstantiation ( body, varEnv, lexEnv, privateEnv, strict ), https://tc39.es/ecma262/#sec-evaldeclarationinstantiation
// 9.1.1.1 EvalDeclarationInstantiation ( body, varEnv, lexEnv, privateEnv, strict ), https://tc39.es/proposal-explicit-resource-management/#sec-evaldeclarationinstantiation
ThrowCompletionOr<void> eval_declaration_instantiation(VM& vm, Program const& program, Environment* variable_environment, Environment* lexical_environment, PrivateEnvironment* private_environment, bool strict)
{
    auto& realm = *vm.current_realm();
    GlobalEnvironment* global_var_environment = variable_environment->is_global_environment() ? static_cast<GlobalEnvironment*>(variable_environment) : nullptr;

    // 1. Let varNames be the VarDeclaredNames of body.
    // 2. Let varDeclarations be the VarScopedDeclarations of body.
    // 3. If strict is false, then
    if (!strict) {
        // a. If varEnv is a global Environment Record, then
        if (global_var_environment) {
            // i. For each element name of varNames, do
            TRY(program.for_each_var_declared_identifier([&](auto const& identifier) -> ThrowCompletionOr<void> {
                // 1. If varEnv.HasLexicalDeclaration(name) is true, throw a SyntaxError exception.
                if (global_var_environment->has_lexical_declaration(identifier.string()))
                    return vm.throw_completion<SyntaxError>(ErrorType::TopLevelVariableAlreadyDeclared, identifier.string());

                // 2. NOTE: eval will not create a global var declaration that would be shadowed by a global lexical declaration.
                return {};
            }));
        }

        // b. Let thisEnv be lexEnv.
        auto* this_environment = lexical_environment;
        // c. Assert: The following loop will terminate.

        // d. Repeat, while thisEnv is not the same as varEnv,
        while (this_environment != variable_environment) {
            // i. If thisEnv is not an object Environment Record, then
            if (!is<ObjectEnvironment>(*this_environment)) {
                // 1. NOTE: The environment of with statements cannot contain any lexical declaration so it doesn't need to be checked for var/let hoisting conflicts.
                // 2. For each element name of varNames, do
                TRY(program.for_each_var_declared_identifier([&](auto const& identifier) -> ThrowCompletionOr<void> {
                    auto const& name = identifier.string();
                    // a. If ! thisEnv.HasBinding(name) is true, then
                    if (MUST(this_environment->has_binding(name))) {
                        // i. Throw a SyntaxError exception.
                        return vm.throw_completion<SyntaxError>(ErrorType::TopLevelVariableAlreadyDeclared, name);

                        // FIXME: ii. NOTE: Annex B.3.4 defines alternate semantics for the above step.
                        // In particular it only throw the syntax error if it is not an environment from a catchclause.
                    }
                    // b. NOTE: A direct eval will not hoist var declaration over a like-named lexical declaration.
                    return {};
                }));
            }

            // ii. Set thisEnv to thisEnv.[[OuterEnv]].
            this_environment = this_environment->outer_environment();
            VERIFY(this_environment);
        }
    }

    // 4. Let privateIdentifiers be a new empty List.
    // 5. Let pointer be privateEnv.
    // 6. Repeat, while pointer is not null,
    //     a. For each Private Name binding of pointer.[[Names]], do
    //         i. If privateIdentifiers does not contain binding.[[Description]], append binding.[[Description]] to privateIdentifiers.
    //     b. Set pointer to pointer.[[OuterPrivateEnvironment]].
    // 7. If AllPrivateIdentifiersValid of body with argument privateIdentifiers is false, throw a SyntaxError exception.
    // FIXME: Add Private identifiers check here.

    // 8. Let functionsToInitialize be a new empty List.
    Vector<FunctionDeclaration const&> functions_to_initialize;

    // 9. Let declaredFunctionNames be a new empty List.
    HashTable<DeprecatedFlyString> declared_function_names;

    // 10. For each element d of varDeclarations, in reverse List order, do
    TRY(program.for_each_var_function_declaration_in_reverse_order([&](FunctionDeclaration const& function) -> ThrowCompletionOr<void> {
        // a. If d is neither a VariableDeclaration nor a ForBinding nor a BindingIdentifier, then
        // i. Assert: d is either a FunctionDeclaration, a GeneratorDeclaration, an AsyncFunctionDeclaration, or an AsyncGeneratorDeclaration.
        // Note: This is done by for_each_var_function_declaration_in_reverse_order.

        // ii. NOTE: If there are multiple function declarations for the same name, the last declaration is used.
        // iii. Let fn be the sole element of the BoundNames of d.
        // iv. If fn is not an element of declaredFunctionNames, then
        if (declared_function_names.set(function.name()) != AK::HashSetResult::InsertedNewEntry)
            return {};

        // 1. If varEnv is a global Environment Record, then
        if (global_var_environment) {
            // a. Let fnDefinable be ? varEnv.CanDeclareGlobalFunction(fn).

            auto function_definable = TRY(global_var_environment->can_declare_global_function(function.name()));

            // b. If fnDefinable is false, throw a TypeError exception.
            if (!function_definable)
                return vm.throw_completion<TypeError>(ErrorType::CannotDeclareGlobalFunction, function.name());
        }

        // 2. Append fn to declaredFunctionNames.
        // Note: Already done in step iv.

        // 3. Insert d as the first element of functionsToInitialize.
        // NOTE: Since prepending is much slower, we just append
        //       and iterate in reverse order in step 17 below.
        functions_to_initialize.append(function);
        return {};
    }));

    // 11. NOTE: Annex B.3.2.3 adds additional steps at this point.
    // B.3.2.3 Changes to EvalDeclarationInstantiation, https://tc39.es/ecma262/#sec-web-compat-evaldeclarationinstantiation
    // 11. If strict is false, then
    if (!strict) {
        // a. Let declaredFunctionOrVarNames be the list-concatenation of declaredFunctionNames and declaredVarNames.
        // The spec here uses 'declaredVarNames' but that has not been declared yet.
        HashTable<DeprecatedFlyString> hoisted_functions;

        // b. For each FunctionDeclaration f that is directly contained in the StatementList of a Block, CaseClause, or DefaultClause Contained within body, do
        TRY(program.for_each_function_hoistable_with_annexB_extension([&](FunctionDeclaration& function_declaration) -> ThrowCompletionOr<void> {
            // i. Let F be StringValue of the BindingIdentifier of f.
            auto function_name = function_declaration.name();

            // ii. If replacing the FunctionDeclaration f with a VariableStatement that has F as a BindingIdentifier would not produce any Early Errors for body, then
            // Note: This is checked during parsing and for_each_function_hoistable_with_annexB_extension so it always passes here.

            // 1. Let bindingExists be false.
            // 2. Let thisEnv be lexEnv.
            auto* this_environment = lexical_environment;

            // 3. Assert: The following loop will terminate.

            // 4. Repeat, while thisEnv is not the same as varEnv,
            while (this_environment != variable_environment) {
                // a. If thisEnv is not an object Environment Record, then
                if (!is<ObjectEnvironment>(*this_environment)) {
                    // i. If ! thisEnv.HasBinding(F) is true, then
                    if (MUST(this_environment->has_binding(function_name))) {
                        // i. Let bindingExists be true.
                        // Note: When bindingExists is true we skip all the other steps.
                        return {};
                    }
                }

                // b. Set thisEnv to thisEnv.[[OuterEnv]].
                this_environment = this_environment->outer_environment();
                VERIFY(this_environment);
            }

            // Note: At this point bindingExists is false.
            // 5. If bindingExists is false and varEnv is a global Environment Record, then
            if (global_var_environment) {
                // a. If varEnv.HasLexicalDeclaration(F) is false, then
                if (!global_var_environment->has_lexical_declaration(function_name)) {
                    // i. Let fnDefinable be ? varEnv.CanDeclareGlobalVar(F).
                    if (!TRY(global_var_environment->can_declare_global_var(function_name)))
                        return {};
                }
                // b. Else,
                else {
                    // i. Let fnDefinable be false.
                    return {};
                }
            }
            // 6. Else,
            //     a. Let fnDefinable be true.

            // Note: At this point fnDefinable is true.
            // 7. If bindingExists is false and fnDefinable is true, then

            // a. If declaredFunctionOrVarNames does not contain F, then
            if (!declared_function_names.contains(function_name) && !hoisted_functions.contains(function_name)) {
                // i. If varEnv is a global Environment Record, then
                if (global_var_environment) {
                    // i. Perform ? varEnv.CreateGlobalVarBinding(F, true).
                    TRY(global_var_environment->create_global_var_binding(function_name, true));
                }
                // ii. Else,
                else {

                    // i. Let bindingExists be ! varEnv.HasBinding(F).
                    // ii. If bindingExists is false, then
                    if (!MUST(variable_environment->has_binding(function_name))) {
                        // i. Perform ! varEnv.CreateMutableBinding(F, true).
                        MUST(variable_environment->create_mutable_binding(vm, function_name, true));
                        // ii. Perform ! varEnv.InitializeBinding(F, undefined, normal).
                        MUST(variable_environment->initialize_binding(vm, function_name, js_undefined(), Environment::InitializeBindingHint::Normal));
                    }
                }
            }

            // iii. Append F to declaredFunctionOrVarNames.
            hoisted_functions.set(function_name);

            // b. When the FunctionDeclaration f is evaluated, perform the following steps in place of the FunctionDeclaration Evaluation algorithm provided in 15.2.6:
            //     i. Let genv be the running execution context's VariableEnvironment.
            //     ii. Let benv be the running execution context's LexicalEnvironment.
            //     iii. Let fobj be ! benv.GetBindingValue(F, false).
            //     iv. Perform ? genv.SetMutableBinding(F, fobj, false).
            //     v. Return unused.
            function_declaration.set_should_do_additional_annexB_steps();

            return {};
        }));
    }

    // 12. Let declaredVarNames be a new empty List.
    HashTable<DeprecatedFlyString> declared_var_names;

    // 13. For each element d of varDeclarations, do
    TRY(program.for_each_var_scoped_variable_declaration([&](VariableDeclaration const& declaration) {
        // a. If d is a VariableDeclaration, a ForBinding, or a BindingIdentifier, then
        // Note: This is handled by for_each_var_scoped_variable_declaration.

        // i. For each String vn of the BoundNames of d, do
        return declaration.for_each_bound_identifier([&](auto const& identifier) -> ThrowCompletionOr<void> {
            auto const& name = identifier.string();

            // 1. If vn is not an element of declaredFunctionNames, then
            if (!declared_function_names.contains(name)) {
                // a. If varEnv is a global Environment Record, then
                if (global_var_environment) {
                    // i. Let vnDefinable be ? varEnv.CanDeclareGlobalVar(vn).
                    auto variable_definable = TRY(global_var_environment->can_declare_global_var(name));

                    // ii. If vnDefinable is false, throw a TypeError exception.
                    if (!variable_definable)
                        return vm.throw_completion<TypeError>(ErrorType::CannotDeclareGlobalVariable, name);
                }

                // b. If vn is not an element of declaredVarNames, then
                // i. Append vn to declaredVarNames.
                declared_var_names.set(name);
            }
            return {};
        });
    }));

    // 14. NOTE: No abnormal terminations occur after this algorithm step unless varEnv is a global Environment Record and the global object is a Proxy exotic object.

    // 15. Let lexDeclarations be the LexicallyScopedDeclarations of body.
    // 16. For each element d of lexDeclarations, do
    TRY(program.for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        // a. NOTE: Lexically declared names are only instantiated here but not initialized.

        // b. For each element dn of the BoundNames of d, do
        return declaration.for_each_bound_identifier([&](auto const& identifier) -> ThrowCompletionOr<void> {
            auto const& name = identifier.string();

            // i. If IsConstantDeclaration of d is true, then
            if (declaration.is_constant_declaration()) {
                // 1. Perform ? lexEnv.CreateImmutableBinding(dn, true).
                TRY(lexical_environment->create_immutable_binding(vm, name, true));
            }
            // ii. Else,
            else {
                // 1. Perform ? lexEnv.CreateMutableBinding(dn, false).
                TRY(lexical_environment->create_mutable_binding(vm, name, false));
            }
            return {};
        });
    }));

    // 17. For each Parse Node f of functionsToInitialize, do
    // NOTE: We iterate in reverse order since we appended the functions
    //       instead of prepending. We append because prepending is much slower
    //       and we only use the created vector here.
    for (auto& declaration : functions_to_initialize.in_reverse()) {
        // a. Let fn be the sole element of the BoundNames of f.
        // b. Let fo be InstantiateFunctionObject of f with arguments lexEnv and privateEnv.
        auto function = ECMAScriptFunctionObject::create(realm, declaration.name(), declaration.source_text(), declaration.body(), declaration.parameters(), declaration.function_length(), declaration.local_variables_names(), lexical_environment, private_environment, declaration.kind(), declaration.is_strict_mode(),
            declaration.parsing_insights());

        // c. If varEnv is a global Environment Record, then
        if (global_var_environment) {
            // i. Perform ? varEnv.CreateGlobalFunctionBinding(fn, fo, true).
            TRY(global_var_environment->create_global_function_binding(declaration.name(), function, true));
        }
        // d. Else,
        else {
            // i. Let bindingExists be ! varEnv.HasBinding(fn).
            auto binding_exists = MUST(variable_environment->has_binding(declaration.name()));

            // ii. If bindingExists is false, then
            if (!binding_exists) {
                // 1. NOTE: The following invocation cannot return an abrupt completion because of the validation preceding step 14.
                // 2. Perform ! varEnv.CreateMutableBinding(fn, true).
                MUST(variable_environment->create_mutable_binding(vm, declaration.name(), true));

                // 3. Perform ! varEnv.InitializeBinding(fn, fo, normal).
                MUST(variable_environment->initialize_binding(vm, declaration.name(), function, Environment::InitializeBindingHint::Normal));
            }
            // iii. Else,
            else {
                // 1. Perform ! varEnv.SetMutableBinding(fn, fo, false).
                MUST(variable_environment->set_mutable_binding(vm, declaration.name(), function, false));
            }
        }
    }

    // 18. For each String vn of declaredVarNames, do

    for (auto& var_name : declared_var_names) {
        // a. If varEnv is a global Environment Record, then
        if (global_var_environment) {
            // i. Perform ? varEnv.CreateGlobalVarBinding(vn, true).
            TRY(global_var_environment->create_global_var_binding(var_name, true));
        }
        // b. Else,
        else {
            // i. Let bindingExists be ! varEnv.HasBinding(vn).
            auto binding_exists = MUST(variable_environment->has_binding(var_name));

            // ii. If bindingExists is false, then
            if (!binding_exists) {
                // 1. NOTE: The following invocation cannot return an abrupt completion because of the validation preceding step 14.
                // 2. Perform ! varEnv.CreateMutableBinding(vn, true).
                MUST(variable_environment->create_mutable_binding(vm, var_name, true));

                // 3. Perform ! varEnv.InitializeBinding(vn, undefined, normal).
                MUST(variable_environment->initialize_binding(vm, var_name, js_undefined(), Environment::InitializeBindingHint::Normal));
            }
        }
    }

    // 19. Return unused.
    return {};
}

// 10.4.4.6 CreateUnmappedArgumentsObject ( argumentsList ), https://tc39.es/ecma262/#sec-createunmappedargumentsobject
Object* create_unmapped_arguments_object(VM& vm, ReadonlySpan<Value> arguments)
{
    auto& realm = *vm.current_realm();

    // 1. Let len be the number of elements in argumentsList.
    auto length = arguments.size();

    // 2. Let obj be OrdinaryObjectCreate(%Object.prototype%, « [[ParameterMap]] »).
    // 3. Set obj.[[ParameterMap]] to undefined.
    auto object = Object::create(realm, realm.intrinsics().object_prototype());
    object->set_has_parameter_map();

    // 4. Perform ! DefinePropertyOrThrow(obj, "length", PropertyDescriptor { [[Value]]: 𝔽(len), [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    MUST(object->define_property_or_throw(vm.names.length, { .value = Value(length), .writable = true, .enumerable = false, .configurable = true }));

    // 5. Let index be 0.
    // 6. Repeat, while index < len,
    for (size_t index = 0; index < length; ++index) {
        // a. Let val be argumentsList[index].
        auto value = arguments[index];

        // b. Perform ! CreateDataPropertyOrThrow(obj, ! ToString(𝔽(index)), val).
        MUST(object->create_data_property_or_throw(index, value));

        // c. Set index to index + 1.
    }

    // 7. Perform ! DefinePropertyOrThrow(obj, @@iterator, PropertyDescriptor { [[Value]]: %Array.prototype.values%, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    auto array_prototype_values = realm.intrinsics().array_prototype_values_function();
    MUST(object->define_property_or_throw(vm.well_known_symbol_iterator(), { .value = array_prototype_values, .writable = true, .enumerable = false, .configurable = true }));

    // 8. Perform ! DefinePropertyOrThrow(obj, "callee", PropertyDescriptor { [[Get]]: %ThrowTypeError%, [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false }).
    auto throw_type_error = realm.intrinsics().throw_type_error_function();
    MUST(object->define_property_or_throw(vm.names.callee, { .get = throw_type_error, .set = throw_type_error, .enumerable = false, .configurable = false }));

    // 9. Return obj.
    return object;
}

// 10.4.4.7 CreateMappedArgumentsObject ( func, formals, argumentsList, env ), https://tc39.es/ecma262/#sec-createmappedargumentsobject
Object* create_mapped_arguments_object(VM& vm, FunctionObject& function, Vector<FunctionParameter> const& formals, ReadonlySpan<Value> arguments, Environment& environment)
{
    auto& realm = *vm.current_realm();

    // 1. Assert: formals does not contain a rest parameter, any binding patterns, or any initializers. It may contain duplicate identifiers.

    // 2. Let len be the number of elements in argumentsList.
    VERIFY(arguments.size() <= NumericLimits<i32>::max());
    i32 length = static_cast<i32>(arguments.size());

    // 3. Let obj be MakeBasicObject(« [[Prototype]], [[Extensible]], [[ParameterMap]] »).
    // 4. Set obj.[[GetOwnProperty]] as specified in 10.4.4.1.
    // 5. Set obj.[[DefineOwnProperty]] as specified in 10.4.4.2.
    // 6. Set obj.[[Get]] as specified in 10.4.4.3.
    // 7. Set obj.[[Set]] as specified in 10.4.4.4.
    // 8. Set obj.[[Delete]] as specified in 10.4.4.5.
    // 9. Set obj.[[Prototype]] to %Object.prototype%.
    auto object = vm.heap().allocate<ArgumentsObject>(realm, realm, environment);

    // 14. Let index be 0.
    // 15. Repeat, while index < len,
    for (i32 index = 0; index < length; ++index) {
        // a. Let val be argumentsList[index].
        auto value = arguments[index];

        // b. Perform ! CreateDataPropertyOrThrow(obj, ! ToString(𝔽(index)), val).
        MUST(object->create_data_property_or_throw(index, value));

        // c. Set index to index + 1.
    }

    // 16. Perform ! DefinePropertyOrThrow(obj, "length", PropertyDescriptor { [[Value]]: 𝔽(len), [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    MUST(object->define_property_or_throw(vm.names.length, { .value = Value(length), .writable = true, .enumerable = false, .configurable = true }));

    // 17. Let mappedNames be a new empty List.
    HashTable<DeprecatedFlyString> mapped_names;

    // 18. Set index to numberOfParameters - 1.
    // 19. Repeat, while index ≥ 0,
    VERIFY(formals.size() <= NumericLimits<i32>::max());
    for (i32 index = static_cast<i32>(formals.size()) - 1; index >= 0; --index) {
        // a. Let name be parameterNames[index].
        auto const& name = formals[index].binding.get<NonnullRefPtr<Identifier const>>()->string();

        // b. If name is not an element of mappedNames, then
        if (mapped_names.contains(name))
            continue;

        // i. Add name as an element of the list mappedNames.
        mapped_names.set(name);

        // ii. If index < len, then
        if (index < length) {
            // 1. Let g be MakeArgGetter(name, env).
            // 2. Let p be MakeArgSetter(name, env).
            // 3. Perform ! map.[[DefineOwnProperty]](! ToString(𝔽(index)), PropertyDescriptor { [[Set]]: p, [[Get]]: g, [[Enumerable]]: false, [[Configurable]]: true }).
            object->parameter_map().define_native_accessor(
                realm,
                PropertyKey { index },
                [&environment, name](VM& vm) -> ThrowCompletionOr<Value> {
                    return MUST(environment.get_binding_value(vm, name, false));
                },
                [&environment, name](VM& vm) {
                    MUST(environment.set_mutable_binding(vm, name, vm.argument(0), false));
                    return js_undefined();
                },
                Attribute::Configurable);
        }
    }

    // 20. Perform ! DefinePropertyOrThrow(obj, @@iterator, PropertyDescriptor { [[Value]]: %Array.prototype.values%, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    auto array_prototype_values = realm.intrinsics().array_prototype_values_function();
    MUST(object->define_property_or_throw(vm.well_known_symbol_iterator(), { .value = array_prototype_values, .writable = true, .enumerable = false, .configurable = true }));

    // 21. Perform ! DefinePropertyOrThrow(obj, "callee", PropertyDescriptor { [[Value]]: func, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    MUST(object->define_property_or_throw(vm.names.callee, { .value = &function, .writable = true, .enumerable = false, .configurable = true }));

    // 22. Return obj.
    return object;
}

// 7.1.21 CanonicalNumericIndexString ( argument ), https://tc39.es/ecma262/#sec-canonicalnumericindexstring
CanonicalIndex canonical_numeric_index_string(PropertyKey const& property_key, CanonicalIndexMode mode)
{
    // NOTE: If the property name is a number type (An implementation-defined optimized
    // property key type), it can be treated as a string property that has already been
    // converted successfully into a canonical numeric index.

    VERIFY(property_key.is_string() || property_key.is_number());

    if (property_key.is_number())
        return CanonicalIndex(CanonicalIndex::Type::Index, property_key.as_number());

    if (mode != CanonicalIndexMode::DetectNumericRoundtrip)
        return CanonicalIndex(CanonicalIndex::Type::Undefined, 0);

    auto& argument = property_key.as_string();

    // Handle trivial cases without a full round trip test
    // We do not need to check for argument == "0" at this point because we
    // already covered it with the is_number() == true path.
    if (argument.is_empty())
        return CanonicalIndex(CanonicalIndex::Type::Undefined, 0);
    u32 current_index = 0;
    if (argument.characters()[current_index] == '-') {
        current_index++;
        if (current_index == argument.length())
            return CanonicalIndex(CanonicalIndex::Type::Undefined, 0);
    }
    if (argument.characters()[current_index] == '0') {
        current_index++;
        if (current_index == argument.length())
            return CanonicalIndex(CanonicalIndex::Type::Numeric, 0);
        if (argument.characters()[current_index] != '.')
            return CanonicalIndex(CanonicalIndex::Type::Undefined, 0);
        current_index++;
        if (current_index == argument.length())
            return CanonicalIndex(CanonicalIndex::Type::Undefined, 0);
    }

    // Short circuit a few common cases
    if (argument == "Infinity"sv || argument == "-Infinity"sv || argument == "NaN"sv)
        return CanonicalIndex(CanonicalIndex::Type::Numeric, 0);

    // Short circuit any string that doesn't start with digits
    if (char first_non_zero = argument.characters()[current_index]; first_non_zero < '0' || first_non_zero > '9')
        return CanonicalIndex(CanonicalIndex::Type::Undefined, 0);

    // 2. Let n be ! ToNumber(argument).
    auto maybe_double = argument.to_number<double>(AK::TrimWhitespace::No);
    if (!maybe_double.has_value())
        return CanonicalIndex(CanonicalIndex::Type::Undefined, 0);

    // FIXME: We return 0 instead of n but it might not observable?
    // 3. If SameValue(! ToString(n), argument) is true, return n.
    if (number_to_string(*maybe_double) == argument.view())
        return CanonicalIndex(CanonicalIndex::Type::Numeric, 0);

    // 4. Return undefined.
    return CanonicalIndex(CanonicalIndex::Type::Undefined, 0);
}

// 22.1.3.19.1 GetSubstitution ( matched, str, position, captures, namedCaptures, replacementTemplate ), https://tc39.es/ecma262/#sec-getsubstitution
ThrowCompletionOr<String> get_substitution(VM& vm, Utf16View const& matched, Utf16View const& str, size_t position, Span<Value> captures, Value named_captures, Value replacement_template)
{
    // 1. Let stringLength be the length of str.
    auto string_length = str.length_in_code_units();

    // 2. Assert: position ≤ stringLength.
    VERIFY(position <= string_length);

    // 3. Let result be the empty String.
    Utf16Data result;

    // 4. Let templateRemainder be replacementTemplate.
    auto replace_template_string = TRY(replacement_template.to_utf16_string(vm));
    auto template_remainder = replace_template_string.view();

    // 5. Repeat, while templateRemainder is not the empty String,
    while (!template_remainder.is_empty()) {
        // a. NOTE: The following steps isolate ref (a prefix of templateRemainder), determine refReplacement (its replacement), and then append that replacement to result.

        Utf16View ref;
        Utf16View ref_replacement;
        Optional<Utf16String> capture_string;

        // b. If templateRemainder starts with "$$", then
        if (template_remainder.starts_with(u"$$")) {
            // i. Let ref be "$$".
            ref = u"$$";

            // ii. Let refReplacement be "$".
            ref_replacement = u"$";
        }
        // c. Else if templateRemainder starts with "$`", then
        else if (template_remainder.starts_with(u"$`")) {
            // i. Let ref be "$`".
            ref = u"$`";

            // ii. Let refReplacement be the substring of str from 0 to position.
            ref_replacement = str.substring_view(0, position);
        }
        // d. Else if templateRemainder starts with "$&", then
        else if (template_remainder.starts_with(u"$&")) {
            // i. Let ref be "$&".
            ref = u"$&";

            // ii. Let refReplacement be matched.
            ref_replacement = matched;
        }
        // e. Else if templateRemainder starts with "$'" (0x0024 (DOLLAR SIGN) followed by 0x0027 (APOSTROPHE)), then
        else if (template_remainder.starts_with(u"$'")) {
            // i. Let ref be "$'".
            ref = u"$'";

            // ii. Let matchLength be the length of matched.
            auto match_length = matched.length_in_code_units();

            // iii. Let tailPos be position + matchLength.
            auto tail_pos = position + match_length;

            // iv. Let refReplacement be the substring of str from min(tailPos, stringLength).
            ref_replacement = str.substring_view(min(tail_pos, string_length));

            // v. NOTE: tailPos can exceed stringLength only if this abstract operation was invoked by a call to the intrinsic @@replace method of %RegExp.prototype% on an object whose "exec" property is not the intrinsic %RegExp.prototype.exec%.
        }
        // f. Else if templateRemainder starts with "$" followed by 1 or more decimal digits, then
        else if (template_remainder.starts_with(u"$") && template_remainder.length_in_code_units() > 1 && is_ascii_digit(template_remainder.code_unit_at(1))) {
            // i. If templateRemainder starts with "$" followed by 2 or more decimal digits, let digitCount be 2. Otherwise, let digitCount be 1.
            size_t digit_count = 1;

            if (template_remainder.length_in_code_units() > 2 && is_ascii_digit(template_remainder.code_point_at(2)))
                digit_count = 2;

            // ii. Let digits be the substring of templateRemainder from 1 to 1 + digitCount.
            auto digits = template_remainder.substring_view(1, digit_count);

            // iii. Let index be ℝ(StringToNumber(digits)).
            auto utf8_digits = MUST(digits.to_utf8());
            auto index = static_cast<size_t>(string_to_number(utf8_digits));

            // iv. Assert: 0 ≤ index ≤ 99.
            VERIFY(index <= 99);

            // v. Let captureLen be the number of elements in captures.
            auto capture_length = captures.size();

            // vi. If index > captureLen and digitCount = 2, then
            if (index > capture_length && digit_count == 2) {
                // 1. NOTE: When a two-digit replacement pattern specifies an index exceeding the count of capturing groups, it is treated as a one-digit replacement pattern followed by a literal digit.

                // 2. Set digitCount to 1.
                digit_count = 1;

                // 3. Set digits to the substring of digits from 0 to 1.
                digits = digits.substring_view(0, 1);

                // 4. Set index to ℝ(StringToNumber(digits)).
                utf8_digits = MUST(digits.to_utf8());
                index = static_cast<size_t>(string_to_number(utf8_digits));
            }

            // vii. Let ref be the substring of templateRemainder from 0 to 1 + digitCount.
            ref = template_remainder.substring_view(0, 1 + digit_count);

            // viii. If 1 ≤ index ≤ captureLen, then
            if (1 <= index && index <= capture_length) {
                // 1. Let capture be captures[index - 1].
                auto capture = captures[index - 1];

                // 2. If capture is undefined, then
                if (capture.is_undefined()) {
                    // a. Let refReplacement be the empty String.
                    ref_replacement = {};
                }
                // 3. Else,
                else {
                    // a. Let refReplacement be capture.
                    capture_string = TRY(capture.to_utf16_string(vm));
                    ref_replacement = capture_string->view();
                }
            }
            // ix. Else,
            else {
                // 1. Let refReplacement be ref.
                ref_replacement = ref;
            }
        }
        // g. Else if templateRemainder starts with "$<", then
        else if (template_remainder.starts_with(u"$<")) {
            // i. Let gtPos be StringIndexOf(templateRemainder, ">", 0).
            // NOTE: We can actually start at index 2 because we know the string starts with "$<".
            auto greater_than_position = string_index_of(template_remainder, u">", 2);

            // ii. If gtPos = -1 or namedCaptures is undefined, then
            if (!greater_than_position.has_value() || named_captures.is_undefined()) {
                // 1. Let ref be "$<".
                ref = u"$<";

                // 2. Let refReplacement be ref.
                ref_replacement = ref;
            }
            // iii. Else,
            else {
                // 1. Let ref be the substring of templateRemainder from 0 to gtPos + 1.
                ref = template_remainder.substring_view(0, *greater_than_position + 1);

                // 2. Let groupName be the substring of templateRemainder from 2 to gtPos.
                auto group_name_view = template_remainder.substring_view(2, *greater_than_position - 2);
                auto group_name = MUST(group_name_view.to_byte_string(Utf16View::AllowInvalidCodeUnits::Yes));

                // 3. Assert: namedCaptures is an Object.
                VERIFY(named_captures.is_object());

                // 4. Let capture be ? Get(namedCaptures, groupName).
                auto capture = TRY(named_captures.as_object().get(group_name));

                // 5. If capture is undefined, then
                if (capture.is_undefined()) {
                    // a. Let refReplacement be the empty String.
                    ref_replacement = {};
                }
                // 6. Else,
                else {
                    // a. Let refReplacement be ? ToString(capture).
                    capture_string = TRY(capture.to_utf16_string(vm));
                    ref_replacement = capture_string->view();
                }
            }
        }
        // h. Else,
        else {
            // i. Let ref be the substring of templateRemainder from 0 to 1.
            ref = template_remainder.substring_view(0, 1);

            // ii. Let refReplacement be ref.
            ref_replacement = ref;
        }

        // i. Let refLength be the length of ref.
        auto ref_length = ref.length_in_code_units();

        // k. Set result to the string-concatenation of result and refReplacement.
        result.append(ref_replacement.data(), ref_replacement.length_in_code_points());

        // j. Set templateRemainder to the substring of templateRemainder from refLength.
        // NOTE: We do this step last because refReplacement may point to templateRemainder.
        template_remainder = template_remainder.substring_view(ref_length);
    }

    // 6. Return result.
    return MUST(Utf16View { result }.to_utf8());
}

// 2.1.2 AddDisposableResource ( disposable, V, hint [ , method ] ), https://tc39.es/proposal-explicit-resource-management/#sec-adddisposableresource-disposable-v-hint-disposemethod
ThrowCompletionOr<void> add_disposable_resource(VM& vm, Vector<DisposableResource>& disposable, Value value, Environment::InitializeBindingHint hint, FunctionObject* method)
{
    // NOTE: For now only sync is a valid hint
    VERIFY(hint == Environment::InitializeBindingHint::SyncDispose);

    Optional<DisposableResource> resource;

    // 1. If method is not present then,
    if (!method) {
        // a. If V is null or undefined, return NormalCompletion(empty).
        if (value.is_nullish())
            return {};

        // b. If Type(V) is not Object, throw a TypeError exception.
        if (!value.is_object())
            return vm.throw_completion<TypeError>(ErrorType::NotAnObject, value.to_string_without_side_effects());

        // c. Let resource be ? CreateDisposableResource(V, hint).
        resource = TRY(create_disposable_resource(vm, value, hint));
    }
    // 2. Else,
    else {
        // a. If V is null or undefined, then
        if (value.is_nullish()) {
            // i. Let resource be ? CreateDisposableResource(undefined, hint, method).
            resource = TRY(create_disposable_resource(vm, js_undefined(), hint, method));
        }
        // b. Else,
        else {
            // i. If Type(V) is not Object, throw a TypeError exception.
            if (!value.is_object())
                return vm.throw_completion<TypeError>(ErrorType::NotAnObject, value.to_string_without_side_effects());

            // ii. Let resource be ? CreateDisposableResource(V, hint, method).
            resource = TRY(create_disposable_resource(vm, value, hint, method));
        }
    }

    // 3. Append resource to disposable.[[DisposableResourceStack]].
    VERIFY(resource.has_value());
    disposable.append(resource.release_value());

    // 4. Return NormalCompletion(empty).
    return {};
}

// 2.1.3 CreateDisposableResource ( V, hint [ , method ] ), https://tc39.es/proposal-explicit-resource-management/#sec-createdisposableresource
ThrowCompletionOr<DisposableResource> create_disposable_resource(VM& vm, Value value, Environment::InitializeBindingHint hint, FunctionObject* method)
{
    // 1. If method is not present, then
    if (!method) {
        // a. If V is undefined, throw a TypeError exception.
        if (value.is_undefined())
            return vm.throw_completion<TypeError>(ErrorType::IsUndefined, "value");

        // b. Set method to ? GetDisposeMethod(V, hint).
        method = TRY(get_dispose_method(vm, value, hint));

        // c. If method is undefined, throw a TypeError exception.
        if (!method)
            return vm.throw_completion<TypeError>(ErrorType::NoDisposeMethod, value.to_string_without_side_effects());
    }
    // 2. Else,
    // a. If IsCallable(method) is false, throw a TypeError exception.
    // NOTE: This is guaranteed to never occur from the type.
    VERIFY(method);

    // 3. Return the DisposableResource Record { [[ResourceValue]]: V, [[Hint]]: hint, [[DisposeMethod]]: method }.
    // NOTE: Since we only support sync dispose we don't store the hint for now.
    VERIFY(hint == Environment::InitializeBindingHint::SyncDispose);
    return DisposableResource {
        value,
        *method
    };
}

// 2.1.4 GetDisposeMethod ( V, hint ), https://tc39.es/proposal-explicit-resource-management/#sec-getdisposemethod
ThrowCompletionOr<GCPtr<FunctionObject>> get_dispose_method(VM& vm, Value value, Environment::InitializeBindingHint hint)
{
    // NOTE: We only have sync dispose for now which means we ignore step 1.
    VERIFY(hint == Environment::InitializeBindingHint::SyncDispose);

    // 2. Else,
    // a. Let method be ? GetMethod(V, @@dispose).
    return TRY(value.get_method(vm, vm.well_known_symbol_dispose()));
}

// 2.1.5 Dispose ( V, hint, method ), https://tc39.es/proposal-explicit-resource-management/#sec-dispose
Completion dispose(VM& vm, Value value, NonnullGCPtr<FunctionObject> method)
{
    // 1. Let result be ? Call(method, V).
    [[maybe_unused]] auto result = TRY(call(vm, *method, value));

    // NOTE: Hint can only be sync-dispose so we ignore step 2.
    // 2. If hint is async-dispose and result is not undefined, then
    //    a. Perform ? Await(result).

    // 3. Return undefined.
    return js_undefined();
}

// 2.1.6 DisposeResources ( disposable, completion ), https://tc39.es/proposal-explicit-resource-management/#sec-disposeresources-disposable-completion-errors
Completion dispose_resources(VM& vm, Vector<DisposableResource> const& disposable, Completion completion)
{
    // 1. If disposable is not undefined, then
    // NOTE: At this point disposable is always defined.

    // a. For each resource of disposable.[[DisposableResourceStack]], in reverse list order, do
    for (auto const& resource : disposable.in_reverse()) {
        // i. Let result be Dispose(resource.[[ResourceValue]], resource.[[Hint]], resource.[[DisposeMethod]]).
        auto result = dispose(vm, resource.resource_value, resource.dispose_method);

        // ii. If result.[[Type]] is throw, then
        if (result.is_error()) {
            // 1. If completion.[[Type]] is throw, then
            if (completion.is_error()) {
                // a. Set result to result.[[Value]].

                // b. Let suppressed be completion.[[Value]].
                auto suppressed = completion.value().value();

                // c. Let error be a newly created SuppressedError object.
                auto error = SuppressedError::create(*vm.current_realm());

                // d. Perform ! DefinePropertyOrThrow(error, "error", PropertyDescriptor { [[Configurable]]: true, [[Enumerable]]: false, [[Writable]]: true, [[Value]]: result }).
                MUST(error->define_property_or_throw(vm.names.error, { .value = result.value(), .writable = true, .enumerable = true, .configurable = true }));

                // e. Perform ! DefinePropertyOrThrow(error, "suppressed", PropertyDescriptor { [[Configurable]]: true, [[Enumerable]]: false, [[Writable]]: true, [[Value]]: suppressed }).
                MUST(error->define_property_or_throw(vm.names.suppressed, { .value = suppressed, .writable = true, .enumerable = false, .configurable = true }));

                // f. Set completion to ThrowCompletion(error).
                completion = throw_completion(error);
            }
            // 2. Else,
            else {
                // a. Set completion to result.
                completion = result;
            }
        }
    }

    // 2. Return completion.
    return completion;
}

Completion dispose_resources(VM& vm, GCPtr<DeclarativeEnvironment> disposable, Completion completion)
{
    // 1. If disposable is not undefined, then
    if (disposable)
        return dispose_resources(vm, disposable->disposable_resource_stack(), completion);

    // 2. Return completion.
    return completion;
}

// https://tc39.es/proposal-import-attributes/#sec-AllImportAttributesSupported
static bool all_import_attributes_supported(VM& vm, Vector<ImportAttribute> const& attributes)
{
    // 1. Let supported be HostGetSupportedImportAttributes().
    auto supported = vm.host_get_supported_import_attributes();

    // 2. For each ImportAttribute Record attribute of attributes, do
    for (auto const& attribute : attributes) {
        // a. If supported does not contain attribute.[[Key]], return false.
        if (!supported.contains_slow(attribute.key))
            return false;
    }

    // 3. Return true.
    return true;
}

ThrowCompletionOr<Value> perform_import_call(VM& vm, Value specifier, Value options_value)
{
    auto& realm = *vm.current_realm();

    // 13.3.10.2 EvaluateImportCall ( specifierExpression [ , optionsExpression ] ), https://tc39.es/proposal-import-attributes/#sec-evaluate-import-call
    // 1. Let referrer be GetActiveScriptOrModule().
    auto referrer = [&]() -> ImportedModuleReferrer {
        auto active_script_or_module = vm.get_active_script_or_module();

        // 2. If referrer is null, set referrer to the current Realm Record.
        if (active_script_or_module.has<Empty>())
            return NonnullGCPtr<Realm> { realm };

        if (active_script_or_module.has<NonnullGCPtr<Script>>())
            return active_script_or_module.get<NonnullGCPtr<Script>>();

        return NonnullGCPtr<CyclicModule> { verify_cast<CyclicModule>(*active_script_or_module.get<NonnullGCPtr<Module>>()) };
    }();

    // 7. Let promiseCapability be ! NewPromiseCapability(%Promise%).
    auto promise_capability = MUST(new_promise_capability(vm, realm.intrinsics().promise_constructor()));

    // 8. Let specifierString be Completion(ToString(specifier)).
    // 9. IfAbruptRejectPromise(specifierString, promiseCapability).
    auto specifier_string = TRY_OR_REJECT_WITH_VALUE(vm, promise_capability, specifier.to_byte_string(vm));

    // 10. Let attributes be a new empty List.
    Vector<ImportAttribute> attributes;

    // 11. If options is not undefined, then
    if (!options_value.is_undefined()) {
        // a. If Type(options) is not Object,
        if (!options_value.is_object()) {
            auto error = TypeError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted(ErrorType::NotAnObject.message(), "ImportOptions")));
            // i. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
            MUST(call(vm, *promise_capability->reject(), js_undefined(), error));

            // ii. Return promiseCapability.[[Promise]].
            return Value { promise_capability->promise() };
        }

        // b. Let attributesObj be Completion(Get(options, "with")).
        // c. IfAbruptRejectPromise(attributesObj, promiseCapability).
        auto attributes_obj = TRY_OR_REJECT_WITH_VALUE(vm, promise_capability, options_value.get(vm, vm.names.with));

        // d. Normative Optional, Deprecated
        // 11. If the host supports the deprecated assert keyword for import attributes and attributesObj is undefined, then
        if (attributes_obj.is_undefined()) {
            // i. Set attributesObj to Completion(Get(options, "assert")).
            // ii. IfAbruptRejectPromise(attributesObj, promiseCapability).
            attributes_obj = TRY_OR_REJECT_WITH_VALUE(vm, promise_capability, options_value.get(vm, vm.names.assert));
        }

        // e. If attributesObj is not undefined,
        if (!attributes_obj.is_undefined()) {
            // i. If Type(attributesObj) is not Object,
            if (!attributes_obj.is_object()) {
                auto error = TypeError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted(ErrorType::NotAnObject.message(), "ImportOptionsAssertions")));
                // 1. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
                MUST(call(vm, *promise_capability->reject(), js_undefined(), error));

                // 2. Return promiseCapability.[[Promise]].
                return Value { promise_capability->promise() };
            }

            // ii. Let entries be Completion(EnumerableOwnProperties(attributesObj, key+value)).
            // iii. IfAbruptRejectPromise(entries, promiseCapability).
            auto entries = TRY_OR_REJECT_WITH_VALUE(vm, promise_capability, attributes_obj.as_object().enumerable_own_property_names(Object::PropertyKind::KeyAndValue));

            // iv. For each entry of entries, do
            for (auto const& entry : entries) {
                // 1. Let key be ! Get(entry, "0").
                auto key = MUST(entry.get(vm, PropertyKey(0)));

                // 2. Let value be ! Get(entry, "1").
                auto value = MUST(entry.get(vm, PropertyKey(1)));

                // 3. If Type(value) is not String, then
                if (!value.is_string()) {
                    auto error = TypeError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted(ErrorType::NotAString.message(), "Import Assertion option value")));
                    // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
                    MUST(call(vm, *promise_capability->reject(), js_undefined(), error));

                    // b. Return promiseCapability.[[Promise]].
                    return Value { promise_capability->promise() };
                }

                // 4. Append the ImportAttribute Record { [[Key]]: key, [[Value]]: value } to attributes.
                attributes.empend(key.as_string().byte_string(), value.as_string().byte_string());
            }
        }

        // f. If AllImportAttributesSupported(attributes) is false, then
        if (!all_import_attributes_supported(vm, attributes)) {
            auto error = TypeError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted(ErrorType::NotAnObject.message(), "ImportOptionsAssertions")));
            // i. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
            MUST(call(vm, *promise_capability->reject(), js_undefined(), error));

            // ii. Return promiseCapability.[[Promise]].
            return Value { promise_capability->promise() };
        }

        // g. Sort attributes according to the lexicographic order of their [[Key]] fields,
        //    treating the value of each such field as a sequence of UTF-16 code unit values.
        //    NOTE: This sorting is observable only in that hosts are prohibited from
        //    distinguishing among attributes by the order they occur in.
        // NOTE: This is done when constructing the ModuleRequest.
    }

    // 12. Let moduleRequest be a new ModuleRequest Record { [[Specifier]]: specifierString, [[Attributes]]: attributes }.
    ModuleRequest request { specifier_string, attributes };

    // 13. Perform HostLoadImportedModule(referrer, moduleRequest, empty, promiseCapability).
    vm.host_load_imported_module(referrer, move(request), nullptr, promise_capability);

    // 13. Return promiseCapability.[[Promise]].
    return Value { promise_capability->promise() };
}

}
