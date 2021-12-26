/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Function.h>
#include <AK/Optional.h>
#include <AK/Result.h>
#include <AK/TemporaryChange.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/ArgumentsObject.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/ProxyObject.h>
#include <LibJS/Runtime/Reference.h>

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
    auto result = object.get(vm.names.length);
    if (vm.exception())
        return INVALID;
    return result.to_length(global_object);
}

// 7.3.19 CreateListFromArrayLike ( obj [ , elementTypes ] ), https://tc39.es/ecma262/#sec-createlistfromarraylike
MarkedValueList create_list_from_array_like(GlobalObject& global_object, Value value, Function<void(Value)> check_value)
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
        auto next = array_like.get(index_name);
        if (vm.exception())
            return MarkedValueList { heap };
        if (check_value) {
            check_value(next);
            if (vm.exception())
                return MarkedValueList { heap };
        }
        list.append(next);
    }
    return list;
}

// 7.3.22 SpeciesConstructor ( O, defaultConstructor ), https://tc39.es/ecma262/#sec-speciesconstructor
FunctionObject* species_constructor(GlobalObject& global_object, Object const& object, FunctionObject& default_constructor)
{
    auto& vm = global_object.vm();
    auto constructor = object.get(vm.names.constructor);
    if (vm.exception())
        return nullptr;
    if (constructor.is_undefined())
        return &default_constructor;
    if (!constructor.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());
        return nullptr;
    }
    auto species = constructor.as_object().get(*vm.well_known_symbol_species());
    if (species.is_nullish())
        return &default_constructor;
    if (species.is_constructor())
        return &species.as_function();
    vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, species.to_string_without_side_effects());
    return nullptr;
}

// 7.3.24 GetFunctionRealm ( obj ), https://tc39.es/ecma262/#sec-getfunctionrealm
GlobalObject* get_function_realm(GlobalObject& global_object, FunctionObject const& function)
{
    auto& vm = global_object.vm();

    // 1. Assert: ! IsCallable(obj) is true.

    // 2. If obj has a [[Realm]] internal slot, then
    if (function.realm()) {
        // a. Return obj.[[Realm]].
        return function.realm();
    }

    // 3. If obj is a bound function exotic object, then
    if (is<BoundFunction>(function)) {
        auto& bound_function = static_cast<BoundFunction const&>(function);

        // a. Let target be obj.[[BoundTargetFunction]].
        auto& target = bound_function.target_function();

        // b. Return ? GetFunctionRealm(target).
        return get_function_realm(global_object, target);
    }

    // 4. If obj is a Proxy exotic object, then
    if (is<ProxyObject>(function)) {
        auto& proxy = static_cast<ProxyObject const&>(function);

        // a. If obj.[[ProxyHandler]] is null, throw a TypeError exception.
        if (proxy.is_revoked()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
            return nullptr;
        }

        // b. Let proxyTarget be obj.[[ProxyTarget]].
        auto& proxy_target = proxy.target();

        // c. Return ? GetFunctionRealm(proxyTarget).
        VERIFY(proxy_target.is_function());
        return get_function_realm(global_object, static_cast<FunctionObject const&>(proxy_target));
    }

    // 5. Return the current Realm Record.
    return &global_object;
}

// 10.1.6.2 IsCompatiblePropertyDescriptor ( Extensible, Desc, Current ), https://tc39.es/ecma262/#sec-iscompatiblepropertydescriptor
bool is_compatible_property_descriptor(bool extensible, PropertyDescriptor const& descriptor, Optional<PropertyDescriptor> const& current)
{
    // 1. Return ValidateAndApplyPropertyDescriptor(undefined, undefined, Extensible, Desc, Current).
    return validate_and_apply_property_descriptor(nullptr, {}, extensible, descriptor, current);
}

// 10.1.6.3 ValidateAndApplyPropertyDescriptor ( O, P, extensible, Desc, current ), https://tc39.es/ecma262/#sec-validateandapplypropertydescriptor
bool validate_and_apply_property_descriptor(Object* object, PropertyName const& property_name, bool extensible, PropertyDescriptor const& descriptor, Optional<PropertyDescriptor> const& current)
{
    // 1. Assert: If O is not undefined, then IsPropertyKey(P) is true.
    if (object)
        VERIFY(property_name.is_valid());

    // 2. If current is undefined, then
    if (!current.has_value()) {
        // a. If extensible is false, return false.
        if (!extensible)
            return false;

        // b. Assert: extensible is true.
        // c. If IsGenericDescriptor(Desc) is true or IsDataDescriptor(Desc) is true, then
        if (descriptor.is_generic_descriptor() || descriptor.is_data_descriptor()) {
            // i. If O is not undefined, create an own data property named P of object O whose [[Value]], [[Writable]],
            // [[Enumerable]], and [[Configurable]] attribute values are described by Desc.
            // If the value of an attribute field of Desc is absent, the attribute of the newly created property is set
            // to its default value.
            if (object) {
                auto value = descriptor.value.value_or(js_undefined());
                object->storage_set(property_name, { value, descriptor.attributes() });
            }
        }
        // d. Else,
        else {
            // i. Assert: ! IsAccessorDescriptor(Desc) is true.
            VERIFY(descriptor.is_accessor_descriptor());

            // ii. If O is not undefined, create an own accessor property named P of object O whose [[Get]], [[Set]],
            // [[Enumerable]], and [[Configurable]] attribute values are described by Desc.
            // If the value of an attribute field of Desc is absent, the attribute of the newly created property is set
            // to its default value.
            if (object) {
                auto accessor = Accessor::create(object->vm(), descriptor.get.value_or(nullptr), descriptor.set.value_or(nullptr));
                object->storage_set(property_name, { accessor, descriptor.attributes() });
            }
        }
        // e. Return true.
        return true;
    }

    // 3. If every field in Desc is absent, return true.
    if (descriptor.is_empty())
        return true;

    // 4. If current.[[Configurable]] is false, then
    if (!*current->configurable) {
        // a. If Desc.[[Configurable]] is present and its value is true, return false.
        if (descriptor.configurable.has_value() && *descriptor.configurable)
            return false;

        // b. If Desc.[[Enumerable]] is present and ! SameValue(Desc.[[Enumerable]], current.[[Enumerable]]) is false, return false.
        if (descriptor.enumerable.has_value() && *descriptor.enumerable != *current->enumerable)
            return false;
    }

    // 5. If ! IsGenericDescriptor(Desc) is true, then
    if (descriptor.is_generic_descriptor()) {
        // a. NOTE: No further validation is required.
    }
    // 6. Else if ! SameValue(! IsDataDescriptor(current), ! IsDataDescriptor(Desc)) is false, then
    else if (current->is_data_descriptor() != descriptor.is_data_descriptor()) {
        // a. If current.[[Configurable]] is false, return false.
        if (!*current->configurable)
            return false;

        // b. If IsDataDescriptor(current) is true, then
        if (current->is_data_descriptor()) {
            // If O is not undefined, convert the property named P of object O from a data property to an accessor property.
            // Preserve the existing values of the converted property's [[Configurable]] and [[Enumerable]] attributes and
            // set the rest of the property's attributes to their default values.
            if (object) {
                auto accessor = Accessor::create(object->vm(), nullptr, nullptr);
                object->storage_set(property_name, { accessor, current->attributes() });
            }
        }
        // c. Else,
        else {
            // If O is not undefined, convert the property named P of object O from an accessor property to a data property.
            // Preserve the existing values of the converted property's [[Configurable]] and [[Enumerable]] attributes and
            // set the rest of the property's attributes to their default values.
            if (object) {
                auto value = js_undefined();
                object->storage_set(property_name, { value, current->attributes() });
            }
        }
    }
    // 7. Else if IsDataDescriptor(current) and IsDataDescriptor(Desc) are both true, then
    else if (current->is_data_descriptor() && descriptor.is_data_descriptor()) {
        // a. If current.[[Configurable]] is false and current.[[Writable]] is false, then
        if (!*current->configurable && !*current->writable) {
            // i. If Desc.[[Writable]] is present and Desc.[[Writable]] is true, return false.
            if (descriptor.writable.has_value() && *descriptor.writable)
                return false;

            // ii. If Desc.[[Value]] is present and SameValue(Desc.[[Value]], current.[[Value]]) is false, return false.
            if (descriptor.value.has_value() && !same_value(*descriptor.value, *current->value))
                return false;

            // iii. Return true.
            return true;
        }
    }
    // 8. Else,
    else {
        // a. Assert: ! IsAccessorDescriptor(current) and ! IsAccessorDescriptor(Desc) are both true.
        VERIFY(current->is_accessor_descriptor());
        VERIFY(descriptor.is_accessor_descriptor());

        // b. If current.[[Configurable]] is false, then
        if (!*current->configurable) {
            // i. If Desc.[[Set]] is present and SameValue(Desc.[[Set]], current.[[Set]]) is false, return false.
            if (descriptor.set.has_value() && *descriptor.set != *current->set)
                return false;

            // ii. If Desc.[[Get]] is present and SameValue(Desc.[[Get]], current.[[Get]]) is false, return false.
            if (descriptor.get.has_value() && *descriptor.get != *current->get)
                return false;

            // iii. Return true.
            return true;
        }
    }

    // 9. If O is not undefined, then
    if (object) {
        // a. For each field of Desc that is present, set the corresponding attribute of the property named P of object O to the value of the field.
        Value value;
        if (descriptor.is_accessor_descriptor() || (current->is_accessor_descriptor() && !descriptor.is_data_descriptor())) {
            auto* getter = descriptor.get.value_or(current->get.value_or(nullptr));
            auto* setter = descriptor.set.value_or(current->set.value_or(nullptr));
            value = Accessor::create(object->vm(), getter, setter);
        } else {
            value = descriptor.value.value_or(current->value.value_or({}));
        }
        PropertyAttributes attributes;
        attributes.set_writable(descriptor.writable.value_or(current->writable.value_or(false)));
        attributes.set_enumerable(descriptor.enumerable.value_or(current->enumerable.value_or(false)));
        attributes.set_configurable(descriptor.configurable.value_or(current->configurable.value_or(false)));
        object->storage_set(property_name, { value, attributes });
    }

    // 10. Return true.
    return true;
}

// 10.1.14 GetPrototypeFromConstructor ( constructor, intrinsicDefaultProto )
Object* get_prototype_from_constructor(GlobalObject& global_object, FunctionObject const& constructor, Object* (GlobalObject::*intrinsic_default_prototype)())
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
DeclarativeEnvironment* new_declarative_environment(Environment& environment)
{
    auto& global_object = environment.global_object();
    return global_object.heap().allocate<DeclarativeEnvironment>(global_object, &environment);
}

// 9.1.2.3 NewObjectEnvironment ( O, W, E ), https://tc39.es/ecma262/#sec-newobjectenvironment
ObjectEnvironment* new_object_environment(Object& object, bool is_with_environment, Environment* environment)
{
    auto& global_object = object.global_object();
    return global_object.heap().allocate<ObjectEnvironment>(global_object, object, is_with_environment ? ObjectEnvironment::IsWithEnvironment::Yes : ObjectEnvironment::IsWithEnvironment::No, environment);
}

// 9.4.3 GetThisEnvironment ( ), https://tc39.es/ecma262/#sec-getthisenvironment
Environment& get_this_environment(VM& vm)
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
    auto& active_function = verify_cast<FunctionEnvironment>(env).function_object();
    auto* super_constructor = active_function.internal_get_prototype_of();
    return super_constructor;
}

// 13.3.7.3 MakeSuperPropertyReference ( actualThis, propertyKey, strict )
Reference make_super_property_reference(GlobalObject& global_object, Value actual_this, StringOrSymbol const& property_key, bool strict)
{
    auto& vm = global_object.vm();
    // 1. Let env be GetThisEnvironment().
    auto& env = verify_cast<FunctionEnvironment>(get_this_environment(vm));
    // 2. Assert: env.HasSuperBinding() is true.
    VERIFY(env.has_super_binding());
    // 3. Let baseValue be ? env.GetSuperBase().
    auto base_value = env.get_super_base();
    // 4. Let bv be ? RequireObjectCoercible(baseValue).
    auto bv = require_object_coercible(global_object, base_value);
    if (vm.exception())
        return {};
    // 5. Return the Reference Record { [[Base]]: bv, [[ReferencedName]]: propertyKey, [[Strict]]: strict, [[ThisValue]]: actualThis }.
    // 6. NOTE: This returns a Super Reference Record.
    return Reference { bv, property_key, actual_this, strict };
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

    TemporaryChange scope_change(vm.running_execution_context().lexical_environment, static_cast<Environment*>(&caller_realm.environment()));
    TemporaryChange scope_change_strict(vm.running_execution_context().is_strict_mode, strict_caller == CallerMode::Strict);
    return interpreter.execute_statement(caller_realm, program).value_or(js_undefined());
}

// 10.4.4.6 CreateUnmappedArgumentsObject ( argumentsList ), https://tc39.es/ecma262/#sec-createunmappedargumentsobject
Object* create_unmapped_arguments_object(GlobalObject& global_object, Vector<Value> const& arguments)
{
    auto& vm = global_object.vm();

    // 1. Let len be the number of elements in argumentsList.
    auto length = arguments.size();

    // 2. Let obj be ! OrdinaryObjectCreate(%Object.prototype%, « [[ParameterMap]] »).
    // 3. Set obj.[[ParameterMap]] to undefined.
    auto* object = Object::create(global_object, global_object.object_prototype());
    object->set_has_parameter_map();

    // 4. Perform DefinePropertyOrThrow(obj, "length", PropertyDescriptor { [[Value]]: 𝔽(len), [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    object->define_property_or_throw(vm.names.length, { .value = Value(length), .writable = true, .enumerable = false, .configurable = true });
    VERIFY(!vm.exception());

    // 5. Let index be 0.
    // 6. Repeat, while index < len,
    for (size_t index = 0; index < length; ++index) {
        // a. Let val be argumentsList[index].
        auto value = arguments[index];

        // b. Perform ! CreateDataPropertyOrThrow(obj, ! ToString(𝔽(index)), val).
        object->create_data_property_or_throw(index, value);
        VERIFY(!vm.exception());

        // c. Set index to index + 1.
    }

    // 7. Perform ! DefinePropertyOrThrow(obj, @@iterator, PropertyDescriptor { [[Value]]: %Array.prototype.values%, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    // FIXME: This is not guaranteed to be %Array.prototype.values%!
    auto array_prototype_values = global_object.array_prototype()->get(vm.names.values);
    if (vm.exception())
        return {};
    object->define_property_or_throw(*vm.well_known_symbol_iterator(), { .value = array_prototype_values, .writable = true, .enumerable = false, .configurable = true });
    VERIFY(!vm.exception());

    // 8. Perform ! DefinePropertyOrThrow(obj, "callee", PropertyDescriptor { [[Get]]: %ThrowTypeError%, [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false }).
    auto* throw_type_error = global_object.throw_type_error_function();
    object->define_property_or_throw(vm.names.callee, { .get = throw_type_error, .set = throw_type_error, .enumerable = false, .configurable = false });
    VERIFY(!vm.exception());

    // 9. Return obj.
    return object;
}

// 10.4.4.7 CreateMappedArgumentsObject ( func, formals, argumentsList, env ), https://tc39.es/ecma262/#sec-createmappedargumentsobject
Object* create_mapped_arguments_object(GlobalObject& global_object, FunctionObject& function, Vector<FunctionNode::Parameter> const& formals, Vector<Value> const& arguments, Environment& environment)
{
    auto& vm = global_object.vm();

    // 1. Assert: formals does not contain a rest parameter, any binding patterns, or any initializers. It may contain duplicate identifiers.

    // 2. Let len be the number of elements in argumentsList.
    VERIFY(arguments.size() <= NumericLimits<i32>::max());
    i32 length = static_cast<i32>(arguments.size());

    // 3. Let obj be ! MakeBasicObject(« [[Prototype]], [[Extensible]], [[ParameterMap]] »).
    // 4. Set obj.[[GetOwnProperty]] as specified in 10.4.4.1.
    // 5. Set obj.[[DefineOwnProperty]] as specified in 10.4.4.2.
    // 6. Set obj.[[Get]] as specified in 10.4.4.3.
    // 7. Set obj.[[Set]] as specified in 10.4.4.4.
    // 8. Set obj.[[Delete]] as specified in 10.4.4.5.
    // 9. Set obj.[[Prototype]] to %Object.prototype%.
    auto* object = vm.heap().allocate<ArgumentsObject>(global_object, global_object, environment);
    if (vm.exception())
        return nullptr;

    // 14. Let index be 0.
    // 15. Repeat, while index < len,
    for (i32 index = 0; index < length; ++index) {
        // a. Let val be argumentsList[index].
        auto value = arguments[index];

        // b. Perform ! CreateDataPropertyOrThrow(obj, ! ToString(𝔽(index)), val).
        object->create_data_property_or_throw(index, value);
        VERIFY(!vm.exception());

        // c. Set index to index + 1.
    }

    // 16. Perform ! DefinePropertyOrThrow(obj, "length", PropertyDescriptor { [[Value]]: 𝔽(len), [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    object->define_property_or_throw(vm.names.length, { .value = Value(length), .writable = true, .enumerable = false, .configurable = true });
    VERIFY(!vm.exception());

    // 17. Let mappedNames be a new empty List.
    HashTable<FlyString> mapped_names;

    // 18. Set index to numberOfParameters - 1.
    // 19. Repeat, while index ≥ 0,
    VERIFY(formals.size() <= NumericLimits<i32>::max());
    for (i32 index = static_cast<i32>(formals.size()) - 1; index >= 0; --index) {
        // a. Let name be parameterNames[index].
        auto const& name = formals[index].binding.get<FlyString>();

        // b. If name is not an element of mappedNames, then
        if (mapped_names.contains(name))
            continue;

        // i. Add name as an element of the list mappedNames.
        mapped_names.set(name);

        // ii. If index < len, then
        if (index < length) {
            // 1. Let g be MakeArgGetter(name, env).
            // 2. Let p be MakeArgSetter(name, env).
            // 3. Perform map.[[DefineOwnProperty]](! ToString(𝔽(index)), PropertyDescriptor { [[Set]]: p, [[Get]]: g, [[Enumerable]]: false, [[Configurable]]: true }).
            object->parameter_map().define_native_accessor(
                String::number(index),
                [&environment, name](VM&, GlobalObject&) -> Value {
                    auto variable = environment.get_from_environment(name);
                    if (!variable.has_value())
                        return {};
                    return variable->value;
                },
                [&environment, name](VM& vm, GlobalObject&) {
                    auto value = vm.argument(0);
                    environment.put_into_environment(name, Variable { value, DeclarationKind::Var });
                    return js_undefined();
                },
                Attribute::Configurable);
        }
    }

    // 20. Perform ! DefinePropertyOrThrow(obj, @@iterator, PropertyDescriptor { [[Value]]: %Array.prototype.values%, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    // FIXME: This is not guaranteed to be %Array.prototype.values%!
    auto array_prototype_values = global_object.array_prototype()->get(vm.names.values);
    if (vm.exception())
        return {};
    object->define_property_or_throw(*vm.well_known_symbol_iterator(), { .value = array_prototype_values, .writable = true, .enumerable = false, .configurable = true });
    VERIFY(!vm.exception());

    // 21. Perform ! DefinePropertyOrThrow(obj, "callee", PropertyDescriptor { [[Value]]: func, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }).
    object->define_property_or_throw(vm.names.callee, { .value = &function, .writable = true, .enumerable = false, .configurable = true });
    VERIFY(!vm.exception());

    // 22. Return obj.
    return object;
}

// 7.1.21 CanonicalNumericIndexString ( argument ), https://tc39.es/ecma262/#sec-canonicalnumericindexstring
Value canonical_numeric_index_string(GlobalObject& global_object, PropertyName const& property_name)
{
    // NOTE: If the property name is a number type (An implementation-defined optimized
    // property key type), it can be treated as a string property that has already been
    // converted successfully into a canonical numeric index.

    VERIFY(property_name.is_string() || property_name.is_number());

    if (property_name.is_number())
        return Value(property_name.as_number());

    // 1. Assert: Type(argument) is String.
    auto argument = Value(js_string(global_object.vm(), property_name.as_string()));

    // 2. If argument is "-0", return -0𝔽.
    if (argument.as_string().string() == "-0")
        return Value(-0.0);

    // 3. Let n be ! ToNumber(argument).
    auto n = argument.to_number(global_object);

    // 4. If SameValue(! ToString(n), argument) is false, return undefined.
    if (!same_value(n.to_primitive_string(global_object), argument))
        return js_undefined();

    // 5. Return n.
    return n;
}

// 22.1.3.17.1 GetSubstitution ( matched, str, position, captures, namedCaptures, replacement ), https://tc39.es/ecma262/#sec-getsubstitution
String get_substitution(GlobalObject& global_object, String const& matched, String const& str, size_t position, Vector<Value> const& captures, Value named_captures, Value replacement)
{
    auto& vm = global_object.vm();

    auto replace_string = replacement.to_string(global_object);
    if (vm.exception())
        return {};

    StringBuilder result;

    for (size_t i = 0; i < replace_string.length(); ++i) {
        char curr = replace_string[i];

        if ((curr != '$') || (i + 1 >= replace_string.length())) {
            result.append(curr);
            continue;
        }

        char next = replace_string[i + 1];

        if (next == '$') {
            result.append(next);
            ++i;
        } else if (next == '&') {
            result.append(matched);
            ++i;
        } else if (next == '`') {
            result.append(str.substring_view(0, position));
            ++i;
        } else if (next == '\'') {
            auto tail_pos = position + matched.length();
            if (tail_pos < str.length())
                result.append(str.substring_view(tail_pos));
            ++i;
        } else if (is_ascii_digit(next)) {
            bool is_two_digits = (i + 2 < replace_string.length()) && is_ascii_digit(replace_string[i + 2]);

            auto capture_postition_string = replace_string.substring_view(i + 1, is_two_digits ? 2 : 1);
            auto capture_position = capture_postition_string.to_uint();

            if (capture_position.has_value() && (*capture_position > 0) && (*capture_position <= captures.size())) {
                auto& value = captures[*capture_position - 1];

                if (!value.is_undefined()) {
                    auto value_string = value.to_string(global_object);
                    if (vm.exception())
                        return {};

                    result.append(value_string);
                }

                i += is_two_digits ? 2 : 1;
            } else {
                result.append(curr);
            }
        } else if (next == '<') {
            auto start_position = i + 2;
            auto end_position = replace_string.find('>', start_position);

            if (named_captures.is_undefined() || !end_position.has_value()) {
                result.append(curr);
            } else {
                auto group_name = replace_string.substring(start_position, *end_position - start_position);

                auto capture = named_captures.as_object().get(group_name);
                if (vm.exception())
                    return {};

                if (!capture.is_undefined()) {
                    auto capture_string = capture.to_string(global_object);
                    if (vm.exception())
                        return {};

                    result.append(capture_string);
                }

                i = *end_position;
            }
        } else {
            result.append(curr);
        }
    }

    return result.build();
}

}
