/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/CommonImplementations.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/RegExpObject.h>

namespace JS::Bytecode {

ThrowCompletionOr<NonnullGCPtr<Object>> base_object_for_get(Bytecode::Interpreter& interpreter, Value base_value)
{
    auto& vm = interpreter.vm();
    if (base_value.is_object())
        return base_value.as_object();

    // OPTIMIZATION: For various primitives we can avoid actually creating a new object for them.
    if (base_value.is_string())
        return vm.current_realm()->intrinsics().string_prototype();
    if (base_value.is_number())
        return vm.current_realm()->intrinsics().number_prototype();
    if (base_value.is_boolean())
        return vm.current_realm()->intrinsics().boolean_prototype();

    return base_value.to_object(vm);
}

ThrowCompletionOr<Value> get_by_id(Bytecode::Interpreter& interpreter, IdentifierTableIndex property, Value base_value, Value this_value, u32 cache_index)
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(property);
    auto& cache = interpreter.current_executable().property_lookup_caches[cache_index];

    if (base_value.is_string()) {
        auto string_value = TRY(base_value.as_string().get(vm, name));
        if (string_value.has_value())
            return *string_value;
    }

    auto base_obj = TRY(base_object_for_get(interpreter, base_value));

    // OPTIMIZATION: If the shape of the object hasn't changed, we can use the cached property offset.
    // NOTE: Unique shapes don't change identity, so we compare their serial numbers instead.
    auto& shape = base_obj->shape();
    if (&shape == cache.shape
        && (!shape.is_unique() || shape.unique_shape_serial_number() == cache.unique_shape_serial_number)) {
        return base_obj->get_direct(cache.property_offset.value());
    }

    CacheablePropertyMetadata cacheable_metadata;
    auto value = TRY(base_obj->internal_get(name, this_value, &cacheable_metadata));

    if (cacheable_metadata.type == CacheablePropertyMetadata::Type::OwnProperty) {
        cache.shape = shape;
        cache.property_offset = cacheable_metadata.property_offset.value();
        cache.unique_shape_serial_number = shape.unique_shape_serial_number();
    }

    return value;
}

ThrowCompletionOr<Value> get_by_value(Bytecode::Interpreter& interpreter, Value base_value, Value property_key_value)
{
    auto& vm = interpreter.vm();
    auto object = TRY(base_object_for_get(interpreter, base_value));

    // OPTIMIZATION: Fast path for simple Int32 indexes in array-like objects.
    if (property_key_value.is_int32()
        && property_key_value.as_i32() >= 0
        && !object->may_interfere_with_indexed_property_access()
        && object->indexed_properties().has_index(property_key_value.as_i32())) {
        auto value = object->indexed_properties().get(property_key_value.as_i32())->value;
        if (!value.is_accessor())
            return value;
    }

    auto property_key = TRY(property_key_value.to_property_key(vm));

    if (base_value.is_string()) {
        auto string_value = TRY(base_value.as_string().get(vm, property_key));
        if (string_value.has_value())
            return *string_value;
    }

    return TRY(object->internal_get(property_key, base_value));
}

ThrowCompletionOr<Value> get_global(Bytecode::Interpreter& interpreter, IdentifierTableIndex identifier, u32 cache_index)
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    auto& cache = interpreter.current_executable().global_variable_caches[cache_index];
    auto& binding_object = realm.global_environment().object_record().binding_object();
    auto& declarative_record = realm.global_environment().declarative_record();

    // OPTIMIZATION: If the shape of the object hasn't changed, we can use the cached property offset.
    // NOTE: Unique shapes don't change identity, so we compare their serial numbers instead.
    auto& shape = binding_object.shape();
    if (cache.environment_serial_number == declarative_record.environment_serial_number()
        && &shape == cache.shape
        && (!shape.is_unique() || shape.unique_shape_serial_number() == cache.unique_shape_serial_number)) {
        return binding_object.get_direct(cache.property_offset.value());
    }

    cache.environment_serial_number = declarative_record.environment_serial_number();

    auto const& name = interpreter.current_executable().get_identifier(identifier);
    if (vm.running_execution_context().script_or_module.has<NonnullGCPtr<Module>>()) {
        // NOTE: GetGlobal is used to access variables stored in the module environment and global environment.
        //       The module environment is checked first since it precedes the global environment in the environment chain.
        auto& module_environment = *vm.running_execution_context().script_or_module.get<NonnullGCPtr<Module>>()->environment();
        if (TRY(module_environment.has_binding(name))) {
            // TODO: Cache offset of binding value
            return TRY(module_environment.get_binding_value(vm, name, vm.in_strict_mode()));
        }
    }

    if (TRY(declarative_record.has_binding(name))) {
        // TODO: Cache offset of binding value
        return TRY(declarative_record.get_binding_value(vm, name, vm.in_strict_mode()));
    }

    if (TRY(binding_object.has_property(name))) {
        CacheablePropertyMetadata cacheable_metadata;
        auto value = TRY(binding_object.internal_get(name, js_undefined(), &cacheable_metadata));
        if (cacheable_metadata.type == CacheablePropertyMetadata::Type::OwnProperty) {
            cache.shape = shape;
            cache.property_offset = cacheable_metadata.property_offset.value();
            cache.unique_shape_serial_number = shape.unique_shape_serial_number();
        }
        return value;
    }

    return vm.throw_completion<ReferenceError>(ErrorType::UnknownIdentifier, name);
}

ThrowCompletionOr<void> put_by_property_key(VM& vm, Value base, Value this_value, Value value, PropertyKey name, Op::PropertyKind kind)
{
    auto object = TRY(base.to_object(vm));
    if (kind == Op::PropertyKind::Getter || kind == Op::PropertyKind::Setter) {
        // The generator should only pass us functions for getters and setters.
        VERIFY(value.is_function());
    }
    switch (kind) {
    case Op::PropertyKind::Getter: {
        auto& function = value.as_function();
        if (function.name().is_empty() && is<ECMAScriptFunctionObject>(function))
            static_cast<ECMAScriptFunctionObject*>(&function)->set_name(DeprecatedString::formatted("get {}", name));
        object->define_direct_accessor(name, &function, nullptr, Attribute::Configurable | Attribute::Enumerable);
        break;
    }
    case Op::PropertyKind::Setter: {
        auto& function = value.as_function();
        if (function.name().is_empty() && is<ECMAScriptFunctionObject>(function))
            static_cast<ECMAScriptFunctionObject*>(&function)->set_name(DeprecatedString::formatted("set {}", name));
        object->define_direct_accessor(name, nullptr, &function, Attribute::Configurable | Attribute::Enumerable);
        break;
    }
    case Op::PropertyKind::KeyValue: {
        bool succeeded = TRY(object->internal_set(name, value, this_value));
        if (!succeeded && vm.in_strict_mode())
            return vm.throw_completion<TypeError>(ErrorType::ReferenceNullishSetProperty, name, base.to_string_without_side_effects());
        break;
    }
    case Op::PropertyKind::DirectKeyValue:
        object->define_direct_property(name, value, Attribute::Enumerable | Attribute::Writable | Attribute::Configurable);
        break;
    case Op::PropertyKind::Spread:
        TRY(object->copy_data_properties(vm, value, {}));
        break;
    case Op::PropertyKind::ProtoSetter:
        if (value.is_object() || value.is_null())
            MUST(object->internal_set_prototype_of(value.is_object() ? &value.as_object() : nullptr));
        break;
    }

    return {};
}

ThrowCompletionOr<Value> perform_call(Interpreter& interpreter, Value this_value, Op::CallType call_type, Value callee, MarkedVector<Value> argument_values)
{
    auto& vm = interpreter.vm();
    auto& function = callee.as_function();
    Value return_value;
    if (call_type == Op::CallType::DirectEval) {
        if (callee == interpreter.realm().intrinsics().eval_function())
            return_value = TRY(perform_eval(vm, !argument_values.is_empty() ? argument_values[0].value_or(JS::js_undefined()) : js_undefined(), vm.in_strict_mode() ? CallerMode::Strict : CallerMode::NonStrict, EvalMode::Direct));
        else
            return_value = TRY(JS::call(vm, function, this_value, move(argument_values)));
    } else if (call_type == Op::CallType::Call)
        return_value = TRY(JS::call(vm, function, this_value, move(argument_values)));
    else
        return_value = TRY(construct(vm, function, move(argument_values)));

    return return_value;
}

static Completion throw_type_error_for_callee(Bytecode::Interpreter& interpreter, Value callee, StringView callee_type, Optional<StringTableIndex> const& expression_string)
{
    auto& vm = interpreter.vm();

    if (expression_string.has_value())
        return vm.throw_completion<TypeError>(ErrorType::IsNotAEvaluatedFrom, callee.to_string_without_side_effects(), callee_type, interpreter.current_executable().get_string(expression_string->value()));

    return vm.throw_completion<TypeError>(ErrorType::IsNotA, callee.to_string_without_side_effects(), callee_type);
}

ThrowCompletionOr<void> throw_if_needed_for_call(Interpreter& interpreter, Value callee, Op::CallType call_type, Optional<StringTableIndex> const& expression_string)
{
    if (call_type == Op::CallType::Call && !callee.is_function())
        return throw_type_error_for_callee(interpreter, callee, "function"sv, expression_string);
    if (call_type == Op::CallType::Construct && !callee.is_constructor())
        return throw_type_error_for_callee(interpreter, callee, "constructor"sv, expression_string);
    return {};
}

ThrowCompletionOr<Value> typeof_variable(VM& vm, DeprecatedFlyString const& string)
{
    // 1. Let val be the result of evaluating UnaryExpression.
    auto reference = TRY(vm.resolve_binding(string));

    // 2. If val is a Reference Record, then
    //    a. If IsUnresolvableReference(val) is true, return "undefined".
    if (reference.is_unresolvable())
        return PrimitiveString::create(vm, "undefined"_string);

    // 3. Set val to ? GetValue(val).
    auto value = TRY(reference.get_value(vm));

    // 4. NOTE: This step is replaced in section B.3.6.3.
    // 5. Return a String according to Table 41.
    return PrimitiveString::create(vm, value.typeof());
}

ThrowCompletionOr<void> set_variable(
    VM& vm,
    DeprecatedFlyString const& name,
    Value value,
    Op::EnvironmentMode mode,
    Op::SetVariable::InitializationMode initialization_mode)
{
    auto environment = mode == Op::EnvironmentMode::Lexical ? vm.running_execution_context().lexical_environment : vm.running_execution_context().variable_environment;
    auto reference = TRY(vm.resolve_binding(name, environment));
    switch (initialization_mode) {
    case Op::SetVariable::InitializationMode::Initialize:
        TRY(reference.initialize_referenced_binding(vm, value));
        break;
    case Op::SetVariable::InitializationMode::Set:
        TRY(reference.put_value(vm, value));
        break;
    }
    return {};
}

Value new_function(VM& vm, FunctionExpression const& function_node, Optional<IdentifierTableIndex> const& lhs_name, Optional<Register> const& home_object)
{
    Value value;

    if (!function_node.has_name()) {
        DeprecatedFlyString name = {};
        if (lhs_name.has_value())
            name = vm.bytecode_interpreter().current_executable().get_identifier(lhs_name.value());
        value = function_node.instantiate_ordinary_function_expression(vm, name);
    } else {
        value = ECMAScriptFunctionObject::create(*vm.current_realm(), function_node.name(), function_node.source_text(), function_node.body(), function_node.parameters(), function_node.function_length(), function_node.local_variables_names(), vm.lexical_environment(), vm.running_execution_context().private_environment, function_node.kind(), function_node.is_strict_mode(), function_node.might_need_arguments_object(), function_node.contains_direct_call_to_eval(), function_node.is_arrow_function());
    }

    if (home_object.has_value()) {
        auto home_object_value = vm.bytecode_interpreter().reg(home_object.value());
        static_cast<ECMAScriptFunctionObject&>(value.as_function()).set_home_object(&home_object_value.as_object());
    }

    return value;
}

ThrowCompletionOr<void> put_by_value(VM& vm, Value base, Value property_key_value, Value value, Op::PropertyKind kind)
{
    // OPTIMIZATION: Fast path for simple Int32 indexes in array-like objects.
    if (base.is_object() && property_key_value.is_int32() && property_key_value.as_i32() >= 0) {
        auto& object = base.as_object();
        auto* storage = object.indexed_properties().storage();
        auto index = static_cast<u32>(property_key_value.as_i32());
        if (storage
            && storage->is_simple_storage()
            && !object.may_interfere_with_indexed_property_access()
            && storage->has_index(index)) {
            auto existing_value = storage->get(index)->value;
            if (!existing_value.is_accessor()) {
                storage->put(index, value);
                return {};
            }
        }
    }

    auto property_key = kind != Op::PropertyKind::Spread ? TRY(property_key_value.to_property_key(vm)) : PropertyKey {};
    TRY(put_by_property_key(vm, base, base, value, property_key, kind));
    return {};
}

ThrowCompletionOr<Value> get_variable(Bytecode::Interpreter& interpreter, DeprecatedFlyString const& name, u32 cache_index)
{
    auto& vm = interpreter.vm();

    auto& cached_environment_coordinate = interpreter.current_executable().environment_variable_caches[cache_index];
    if (cached_environment_coordinate.has_value()) {
        auto environment = vm.running_execution_context().lexical_environment;
        for (size_t i = 0; i < cached_environment_coordinate->hops; ++i)
            environment = environment->outer_environment();
        VERIFY(environment);
        VERIFY(environment->is_declarative_environment());
        if (!environment->is_permanently_screwed_by_eval()) {
            return TRY(verify_cast<DeclarativeEnvironment>(*environment).get_binding_value_direct(vm, cached_environment_coordinate.value().index, vm.in_strict_mode()));
        }
        cached_environment_coordinate = {};
    }

    auto reference = TRY(vm.resolve_binding(name));
    if (reference.environment_coordinate().has_value())
        cached_environment_coordinate = reference.environment_coordinate();
    return TRY(reference.get_value(vm));
}

ThrowCompletionOr<CalleeAndThis> get_callee_and_this_from_environment(Bytecode::Interpreter& interpreter, DeprecatedFlyString const& name, u32 cache_index)
{
    auto& vm = interpreter.vm();

    Value callee = js_undefined();
    Value this_value = js_undefined();

    auto& cached_environment_coordinate = interpreter.current_executable().environment_variable_caches[cache_index];
    if (cached_environment_coordinate.has_value()) {
        auto environment = vm.running_execution_context().lexical_environment;
        for (size_t i = 0; i < cached_environment_coordinate->hops; ++i)
            environment = environment->outer_environment();
        VERIFY(environment);
        VERIFY(environment->is_declarative_environment());
        if (!environment->is_permanently_screwed_by_eval()) {
            callee = TRY(verify_cast<DeclarativeEnvironment>(*environment).get_binding_value_direct(vm, cached_environment_coordinate.value().index, vm.in_strict_mode()));
            this_value = js_undefined();
            if (auto base_object = environment->with_base_object())
                this_value = base_object;
            return CalleeAndThis {
                .callee = callee,
                .this_value = this_value,
            };
        }
        cached_environment_coordinate = {};
    }

    auto reference = TRY(vm.resolve_binding(name));
    if (reference.environment_coordinate().has_value())
        cached_environment_coordinate = reference.environment_coordinate();

    callee = TRY(reference.get_value(vm));

    if (reference.is_property_reference()) {
        this_value = reference.get_this_value();
    } else {
        if (reference.is_environment_reference()) {
            if (auto base_object = reference.base_environment().with_base_object(); base_object != nullptr)
                this_value = base_object;
        }
    }

    return CalleeAndThis {
        .callee = callee,
        .this_value = this_value,
    };
}

// 13.2.7.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-regular-expression-literals-runtime-semantics-evaluation
Value new_regexp(VM& vm, ParsedRegex const& parsed_regex, DeprecatedString const& pattern, DeprecatedString const& flags)
{
    // 1. Let pattern be CodePointsToString(BodyText of RegularExpressionLiteral).
    // 2. Let flags be CodePointsToString(FlagText of RegularExpressionLiteral).

    // 3. Return ! RegExpCreate(pattern, flags).
    auto& realm = *vm.current_realm();
    Regex<ECMA262> regex(parsed_regex.regex, parsed_regex.pattern, parsed_regex.flags);
    // NOTE: We bypass RegExpCreate and subsequently RegExpAlloc as an optimization to use the already parsed values.
    auto regexp_object = RegExpObject::create(realm, move(regex), pattern, flags);
    // RegExpAlloc has these two steps from the 'Legacy RegExp features' proposal.
    regexp_object->set_realm(realm);
    // We don't need to check 'If SameValue(newTarget, thisRealm.[[Intrinsics]].[[%RegExp%]]) is true'
    // here as we know RegExpCreate calls RegExpAlloc with %RegExp% for newTarget.
    regexp_object->set_legacy_features_enabled(true);
    return regexp_object;
}

// 13.3.8.1 https://tc39.es/ecma262/#sec-runtime-semantics-argumentlistevaluation
MarkedVector<Value> argument_list_evaluation(Bytecode::Interpreter& interpreter)
{
    // Note: Any spreading and actual evaluation is handled in preceding opcodes
    // Note: The spec uses the concept of a list, while we create a temporary array
    //       in the preceding opcodes, so we have to convert in a manner that is not
    //       visible to the user
    auto& vm = interpreter.vm();

    MarkedVector<Value> argument_values { vm.heap() };
    auto arguments = interpreter.accumulator();

    auto& argument_array = arguments.as_array();
    auto array_length = argument_array.indexed_properties().array_like_size();

    argument_values.ensure_capacity(array_length);

    for (size_t i = 0; i < array_length; ++i) {
        if (auto maybe_value = argument_array.indexed_properties().get(i); maybe_value.has_value())
            argument_values.append(maybe_value.release_value().value);
        else
            argument_values.append(js_undefined());
    }

    return argument_values;
}

}
