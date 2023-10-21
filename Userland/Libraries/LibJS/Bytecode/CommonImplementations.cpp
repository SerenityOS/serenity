/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/CommonImplementations.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/ObjectEnvironment.h>

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

static Completion throw_type_error_for_callee(Bytecode::Interpreter& interpreter, auto& call, StringView callee_type)
{
    auto& vm = interpreter.vm();
    auto callee = interpreter.reg(call.callee());

    if (call.expression_string().has_value())
        return vm.throw_completion<TypeError>(ErrorType::IsNotAEvaluatedFrom, callee.to_string_without_side_effects(), callee_type, interpreter.current_executable().get_string(call.expression_string()->value()));

    return vm.throw_completion<TypeError>(ErrorType::IsNotA, callee.to_string_without_side_effects(), callee_type);
}

template<typename InstructionType>
ThrowCompletionOr<void> throw_if_needed_for_call(Interpreter& interpreter, InstructionType const& call, Value callee)
{
    if (call.call_type() == Op::CallType::Call && !callee.is_function())
        return throw_type_error_for_callee(interpreter, call, "function"sv);
    if (call.call_type() == Op::CallType::Construct && !callee.is_constructor())
        return throw_type_error_for_callee(interpreter, call, "constructor"sv);
    return {};
}

template ThrowCompletionOr<void> throw_if_needed_for_call(Interpreter&, Op::Call const&, Value);
template ThrowCompletionOr<void> throw_if_needed_for_call(Interpreter&, Op::CallWithArgumentArray const&, Value);

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

}
