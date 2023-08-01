/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/SourceTextModule.h>

namespace JS::Bytecode {

DeprecatedString Instruction::to_deprecated_string(Bytecode::Executable const& executable) const
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op const&>(*this).to_deprecated_string_impl(executable);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

}

namespace JS::Bytecode::Op {

static ThrowCompletionOr<void> put_by_property_key(VM& vm, Value base, Value this_value, Value value, PropertyKey name, PropertyKind kind)
{
    auto object = TRY(base.to_object(vm));
    if (kind == PropertyKind::Getter || kind == PropertyKind::Setter) {
        // The generator should only pass us functions for getters and setters.
        VERIFY(value.is_function());
    }
    switch (kind) {
    case PropertyKind::Getter: {
        auto& function = value.as_function();
        if (function.name().is_empty() && is<ECMAScriptFunctionObject>(function))
            static_cast<ECMAScriptFunctionObject*>(&function)->set_name(DeprecatedString::formatted("get {}", name));
        object->define_direct_accessor(name, &function, nullptr, Attribute::Configurable | Attribute::Enumerable);
        break;
    }
    case PropertyKind::Setter: {
        auto& function = value.as_function();
        if (function.name().is_empty() && is<ECMAScriptFunctionObject>(function))
            static_cast<ECMAScriptFunctionObject*>(&function)->set_name(DeprecatedString::formatted("set {}", name));
        object->define_direct_accessor(name, nullptr, &function, Attribute::Configurable | Attribute::Enumerable);
        break;
    }
    case PropertyKind::KeyValue: {
        bool succeeded = TRY(object->internal_set(name, value, this_value));
        if (!succeeded && vm.in_strict_mode())
            return vm.throw_completion<TypeError>(ErrorType::ReferenceNullishSetProperty, name, TRY_OR_THROW_OOM(vm, base.to_string_without_side_effects()));
        break;
    }
    case PropertyKind::DirectKeyValue:
        object->define_direct_property(name, value, Attribute::Enumerable | Attribute::Writable | Attribute::Configurable);
        break;
    case PropertyKind::Spread:
        TRY(object->copy_data_properties(vm, value, {}));
        break;
    case PropertyKind::ProtoSetter:
        if (value.is_object() || value.is_null())
            MUST(object->internal_set_prototype_of(value.is_object() ? &value.as_object() : nullptr));
        break;
    }

    return {};
}

ThrowCompletionOr<void> Load::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.reg(m_src);
    return {};
}

ThrowCompletionOr<void> LoadImmediate::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = m_value;
    return {};
}

ThrowCompletionOr<void> Store::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = interpreter.accumulator();
    return {};
}

static ThrowCompletionOr<Value> abstract_inequals(VM& vm, Value src1, Value src2)
{
    return Value(!TRY(is_loosely_equal(vm, src1, src2)));
}

static ThrowCompletionOr<Value> abstract_equals(VM& vm, Value src1, Value src2)
{
    return Value(TRY(is_loosely_equal(vm, src1, src2)));
}

static ThrowCompletionOr<Value> typed_inequals(VM&, Value src1, Value src2)
{
    return Value(!is_strictly_equal(src1, src2));
}

static ThrowCompletionOr<Value> typed_equals(VM&, Value src1, Value src2)
{
    return Value(is_strictly_equal(src1, src2));
}

#define JS_DEFINE_COMMON_BINARY_OP(OpTitleCase, op_snake_case)                                  \
    ThrowCompletionOr<void> OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const \
    {                                                                                           \
        auto& vm = interpreter.vm();                                                            \
        auto lhs = interpreter.reg(m_lhs_reg);                                                  \
        auto rhs = interpreter.accumulator();                                                   \
        interpreter.accumulator() = TRY(op_snake_case(vm, lhs, rhs));                           \
        return {};                                                                              \
    }                                                                                           \
    DeprecatedString OpTitleCase::to_deprecated_string_impl(Bytecode::Executable const&) const  \
    {                                                                                           \
        return DeprecatedString::formatted(#OpTitleCase " {}", m_lhs_reg);                      \
    }

JS_ENUMERATE_COMMON_BINARY_OPS(JS_DEFINE_COMMON_BINARY_OP)

static ThrowCompletionOr<Value> not_(VM&, Value value)
{
    return Value(!value.to_boolean());
}

static ThrowCompletionOr<Value> typeof_(VM& vm, Value value)
{
    return MUST_OR_THROW_OOM(PrimitiveString::create(vm, value.typeof()));
}

#define JS_DEFINE_COMMON_UNARY_OP(OpTitleCase, op_snake_case)                                   \
    ThrowCompletionOr<void> OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const \
    {                                                                                           \
        auto& vm = interpreter.vm();                                                            \
        interpreter.accumulator() = TRY(op_snake_case(vm, interpreter.accumulator()));          \
        return {};                                                                              \
    }                                                                                           \
    DeprecatedString OpTitleCase::to_deprecated_string_impl(Bytecode::Executable const&) const  \
    {                                                                                           \
        return #OpTitleCase;                                                                    \
    }

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DEFINE_COMMON_UNARY_OP)

ThrowCompletionOr<void> NewBigInt::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = BigInt::create(vm, m_bigint);
    return {};
}

ThrowCompletionOr<void> NewArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto array = MUST(Array::create(interpreter.realm(), 0));
    for (size_t i = 0; i < m_element_count; i++) {
        auto& value = interpreter.reg(Register(m_elements[0].index() + i));
        array->indexed_properties().put(i, value, default_attributes);
    }
    interpreter.accumulator() = array;
    return {};
}

ThrowCompletionOr<void> Append::execute_impl(Bytecode::Interpreter& interpreter) const
{
    // Note: This OpCode is used to construct array literals and argument arrays for calls,
    //       containing at least one spread element,
    //       Iterating over such a spread element to unpack it has to be visible by
    //       the user courtesy of
    //       (1) https://tc39.es/ecma262/#sec-runtime-semantics-arrayaccumulation
    //          SpreadElement : ... AssignmentExpression
    //              1. Let spreadRef be ? Evaluation of AssignmentExpression.
    //              2. Let spreadObj be ? GetValue(spreadRef).
    //              3. Let iteratorRecord be ? GetIterator(spreadObj).
    //              4. Repeat,
    //                  a. Let next be ? IteratorStep(iteratorRecord).
    //                  b. If next is false, return nextIndex.
    //                  c. Let nextValue be ? IteratorValue(next).
    //                  d. Perform ! CreateDataPropertyOrThrow(array, ! ToString(𝔽(nextIndex)), nextValue).
    //                  e. Set nextIndex to nextIndex + 1.
    //       (2) https://tc39.es/ecma262/#sec-runtime-semantics-argumentlistevaluation
    //          ArgumentList : ... AssignmentExpression
    //              1. Let list be a new empty List.
    //              2. Let spreadRef be ? Evaluation of AssignmentExpression.
    //              3. Let spreadObj be ? GetValue(spreadRef).
    //              4. Let iteratorRecord be ? GetIterator(spreadObj).
    //              5. Repeat,
    //                  a. Let next be ? IteratorStep(iteratorRecord).
    //                  b. If next is false, return list.
    //                  c. Let nextArg be ? IteratorValue(next).
    //                  d. Append nextArg to list.
    //          ArgumentList : ArgumentList , ... AssignmentExpression
    //             1. Let precedingArgs be ? ArgumentListEvaluation of ArgumentList.
    //             2. Let spreadRef be ? Evaluation of AssignmentExpression.
    //             3. Let iteratorRecord be ? GetIterator(? GetValue(spreadRef)).
    //             4. Repeat,
    //                 a. Let next be ? IteratorStep(iteratorRecord).
    //                 b. If next is false, return precedingArgs.
    //                 c. Let nextArg be ? IteratorValue(next).
    //                 d. Append nextArg to precedingArgs.

    auto& vm = interpreter.vm();

    // Note: We know from codegen, that lhs is a plain array with only indexed properties
    auto& lhs = interpreter.reg(m_lhs).as_array();
    auto lhs_size = lhs.indexed_properties().array_like_size();

    auto rhs = interpreter.accumulator();

    if (m_is_spread) {
        // ...rhs
        size_t i = lhs_size;
        TRY(get_iterator_values(vm, rhs, [&i, &lhs](Value iterator_value) -> Optional<Completion> {
            lhs.indexed_properties().put(i, iterator_value, default_attributes);
            ++i;
            return {};
        }));
    } else {
        lhs.indexed_properties().put(lhs_size, rhs, default_attributes);
    }

    return {};
}

ThrowCompletionOr<void> ImportCall::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto specifier = interpreter.reg(m_specifier);
    auto options_value = interpreter.reg(m_options);
    interpreter.accumulator() = TRY(perform_import_call(vm, specifier, options_value));
    return {};
}

// FIXME: Since the accumulator is a Value, we store an object there and have to convert back and forth between that an Iterator records. Not great.
// Make sure to put this into the accumulator before the iterator object disappears from the stack to prevent the members from being GC'd.
static Object* iterator_to_object(VM& vm, IteratorRecord iterator)
{
    auto& realm = *vm.current_realm();
    auto object = Object::create(realm, nullptr);
    object->define_direct_property(vm.names.iterator, iterator.iterator, 0);
    object->define_direct_property(vm.names.next, iterator.next_method, 0);
    object->define_direct_property(vm.names.done, Value(iterator.done), 0);
    return object;
}

static IteratorRecord object_to_iterator(VM& vm, Object& object)
{
    return IteratorRecord {
        .iterator = &MUST(object.get(vm.names.iterator)).as_object(),
        .next_method = MUST(object.get(vm.names.next)),
        .done = MUST(object.get(vm.names.done)).as_bool()
    };
}

ThrowCompletionOr<void> IteratorToArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, iterator_object);

    auto array = MUST(Array::create(interpreter.realm(), 0));
    size_t index = 0;

    while (true) {
        auto iterator_result = TRY(iterator_next(vm, iterator));

        auto complete = TRY(iterator_complete(vm, iterator_result));

        if (complete) {
            interpreter.accumulator() = array;
            return {};
        }

        auto value = TRY(iterator_value(vm, iterator_result));

        MUST(array->create_data_property_or_throw(index, value));
        index++;
    }
    return {};
}

ThrowCompletionOr<void> NewString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = PrimitiveString::create(interpreter.vm(), interpreter.current_executable().get_string(m_string));
    return {};
}

ThrowCompletionOr<void> NewObject::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    interpreter.accumulator() = Object::create(realm, realm.intrinsics().object_prototype());
    return {};
}

// 13.2.7.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-regular-expression-literals-runtime-semantics-evaluation
ThrowCompletionOr<void> NewRegExp::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto& realm = *vm.current_realm();

    // 1. Let pattern be CodePointsToString(BodyText of RegularExpressionLiteral).
    auto pattern = interpreter.current_executable().get_string(m_source_index);

    // 2. Let flags be CodePointsToString(FlagText of RegularExpressionLiteral).
    auto flags = interpreter.current_executable().get_string(m_flags_index);

    // 3. Return ! RegExpCreate(pattern, flags).
    auto& parsed_regex = interpreter.current_executable().regex_table->get(m_regex_index);
    Regex<ECMA262> regex(parsed_regex.regex, parsed_regex.pattern, parsed_regex.flags);
    // NOTE: We bypass RegExpCreate and subsequently RegExpAlloc as an optimization to use the already parsed values.
    auto regexp_object = RegExpObject::create(realm, move(regex), move(pattern), move(flags));
    // RegExpAlloc has these two steps from the 'Legacy RegExp features' proposal.
    regexp_object->set_realm(*vm.current_realm());
    // We don't need to check 'If SameValue(newTarget, thisRealm.[[Intrinsics]].[[%RegExp%]]) is true'
    // here as we know RegExpCreate calls RegExpAlloc with %RegExp% for newTarget.
    regexp_object->set_legacy_features_enabled(true);
    interpreter.accumulator() = regexp_object;
    return {};
}

#define JS_DEFINE_NEW_BUILTIN_ERROR_OP(ErrorName)                                                                                             \
    ThrowCompletionOr<void> New##ErrorName::execute_impl(Bytecode::Interpreter& interpreter) const                                            \
    {                                                                                                                                         \
        auto& vm = interpreter.vm();                                                                                                          \
        auto& realm = *vm.current_realm();                                                                                                    \
        interpreter.accumulator() = MUST_OR_THROW_OOM(ErrorName::create(realm, interpreter.current_executable().get_string(m_error_string))); \
        return {};                                                                                                                            \
    }                                                                                                                                         \
    DeprecatedString New##ErrorName::to_deprecated_string_impl(Bytecode::Executable const& executable) const                                  \
    {                                                                                                                                         \
        return DeprecatedString::formatted("New" #ErrorName " {} (\"{}\")", m_error_string, executable.string_table->get(m_error_string));    \
    }

JS_ENUMERATE_NEW_BUILTIN_ERROR_OPS(JS_DEFINE_NEW_BUILTIN_ERROR_OP)

ThrowCompletionOr<void> CopyObjectExcludingProperties::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    auto from_object = interpreter.reg(m_from_object);

    auto to_object = Object::create(realm, realm.intrinsics().object_prototype());

    HashTable<PropertyKey> excluded_names;
    for (size_t i = 0; i < m_excluded_names_count; ++i) {
        excluded_names.set(TRY(interpreter.reg(m_excluded_names[i]).to_property_key(vm)));
    }

    TRY(to_object->copy_data_properties(vm, from_object, excluded_names));

    interpreter.accumulator() = to_object;
    return {};
}

ThrowCompletionOr<void> ConcatString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto string = TRY(interpreter.accumulator().to_primitive_string(vm));
    interpreter.reg(m_lhs) = PrimitiveString::create(vm, interpreter.reg(m_lhs).as_string(), string);
    return {};
}

ThrowCompletionOr<void> GetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto get_reference = [&]() -> ThrowCompletionOr<Reference> {
        auto const& string = interpreter.current_executable().get_identifier(m_identifier);
        if (m_cached_environment_coordinate.has_value()) {
            auto environment = vm.running_execution_context().lexical_environment;
            for (size_t i = 0; i < m_cached_environment_coordinate->hops; ++i)
                environment = environment->outer_environment();
            VERIFY(environment);
            VERIFY(environment->is_declarative_environment());
            if (!environment->is_permanently_screwed_by_eval()) {
                return Reference { *environment, string, vm.in_strict_mode(), m_cached_environment_coordinate };
            }
            m_cached_environment_coordinate = {};
        }

        auto reference = TRY(vm.resolve_binding(string));
        if (reference.environment_coordinate().has_value())
            m_cached_environment_coordinate = reference.environment_coordinate();
        return reference;
    };
    auto reference = TRY(get_reference());
    interpreter.accumulator() = TRY(reference.get_value(vm));
    return {};
}

ThrowCompletionOr<void> GetGlobal::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    auto const& name = interpreter.current_executable().get_identifier(m_identifier);

    auto& cache = interpreter.current_executable().global_variable_caches[m_cache_index];
    auto& binding_object = realm.global_environment().object_record().binding_object();
    auto& declarative_record = realm.global_environment().declarative_record();

    // OPTIMIZATION: If the shape of the object hasn't changed, we can use the cached property offset.
    // NOTE: Unique shapes don't change identity, so we compare their serial numbers instead.
    auto& shape = binding_object.shape();
    if (cache.environment_serial_number == declarative_record.environment_serial_number()
        && &shape == cache.shape
        && (!shape.is_unique() || shape.unique_shape_serial_number() == cache.unique_shape_serial_number)) {
        interpreter.accumulator() = binding_object.get_direct(cache.property_offset.value());
        return {};
    }

    cache.environment_serial_number = declarative_record.environment_serial_number();

    if (vm.running_execution_context().script_or_module.has<NonnullGCPtr<Module>>()) {
        // NOTE: GetGlobal is used to access variables stored in the module environment and global environment.
        //       The module environment is checked first since it precedes the global environment in the environment chain.
        auto& module_environment = *vm.running_execution_context().script_or_module.get<NonnullGCPtr<Module>>()->environment();
        if (TRY(module_environment.has_binding(name))) {
            // TODO: Cache offset of binding value
            interpreter.accumulator() = TRY(module_environment.get_binding_value(vm, name, vm.in_strict_mode()));
            return {};
        }
    }

    if (TRY(declarative_record.has_binding(name))) {
        // TODO: Cache offset of binding value
        interpreter.accumulator() = TRY(declarative_record.get_binding_value(vm, name, vm.in_strict_mode()));
        return {};
    }

    if (TRY(binding_object.has_property(name))) {
        CacheablePropertyMetadata cacheable_metadata;
        interpreter.accumulator() = js_undefined();
        interpreter.accumulator() = TRY(binding_object.internal_get(name, interpreter.accumulator(), &cacheable_metadata));
        if (cacheable_metadata.type == CacheablePropertyMetadata::Type::OwnProperty) {
            cache.shape = shape;
            cache.property_offset = cacheable_metadata.property_offset.value();
            cache.unique_shape_serial_number = shape.unique_shape_serial_number();
        }
        return {};
    }

    return vm.throw_completion<ReferenceError>(ErrorType::UnknownIdentifier, name);
}

ThrowCompletionOr<void> GetLocal::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    if (vm.running_execution_context().local_variables[m_index].is_empty()) {
        auto const& variable_name = vm.running_execution_context().function->local_variables_names()[m_index];
        return interpreter.vm().throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, variable_name);
    }
    interpreter.accumulator() = vm.running_execution_context().local_variables[m_index];
    return {};
}

ThrowCompletionOr<void> DeleteVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& string = interpreter.current_executable().get_identifier(m_identifier);
    auto reference = TRY(vm.resolve_binding(string));
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> CreateLexicalEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto make_and_swap_envs = [&](auto& old_environment) {
        GCPtr<Environment> environment = new_declarative_environment(*old_environment).ptr();
        swap(old_environment, environment);
        return environment;
    };
    interpreter.saved_lexical_environment_stack().append(make_and_swap_envs(interpreter.vm().running_execution_context().lexical_environment));
    return {};
}

ThrowCompletionOr<void> EnterObjectEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& old_environment = vm.running_execution_context().lexical_environment;
    interpreter.saved_lexical_environment_stack().append(old_environment);
    auto object = TRY(interpreter.accumulator().to_object(vm));
    vm.running_execution_context().lexical_environment = new_object_environment(object, true, old_environment);
    return {};
}

ThrowCompletionOr<void> CreateVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(m_identifier);

    if (m_mode == EnvironmentMode::Lexical) {
        VERIFY(!m_is_global);

        // Note: This is papering over an issue where "FunctionDeclarationInstantiation" creates these bindings for us.
        //       Instead of crashing in there, we'll just raise an exception here.
        if (TRY(vm.lexical_environment()->has_binding(name)))
            return vm.throw_completion<InternalError>(TRY_OR_THROW_OOM(vm, String::formatted("Lexical environment already has binding '{}'", name)));

        if (m_is_immutable)
            return vm.lexical_environment()->create_immutable_binding(vm, name, vm.in_strict_mode());
        else
            return vm.lexical_environment()->create_mutable_binding(vm, name, vm.in_strict_mode());
    } else {
        if (!m_is_global) {
            if (m_is_immutable)
                return vm.variable_environment()->create_immutable_binding(vm, name, vm.in_strict_mode());
            else
                return vm.variable_environment()->create_mutable_binding(vm, name, vm.in_strict_mode());
        } else {
            // NOTE: CreateVariable with m_is_global set to true is expected to only be used in GlobalDeclarationInstantiation currently, which only uses "false" for "can_be_deleted".
            //       The only area that sets "can_be_deleted" to true is EvalDeclarationInstantiation, which is currently fully implemented in C++ and not in Bytecode.
            return verify_cast<GlobalEnvironment>(vm.variable_environment())->create_global_var_binding(name, false);
        }
    }
    return {};
}

ThrowCompletionOr<void> SetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(m_identifier);
    auto environment = m_mode == EnvironmentMode::Lexical ? vm.running_execution_context().lexical_environment : vm.running_execution_context().variable_environment;
    auto reference = TRY(vm.resolve_binding(name, environment));
    switch (m_initialization_mode) {
    case InitializationMode::Initialize:
        TRY(reference.initialize_referenced_binding(vm, interpreter.accumulator()));
        break;
    case InitializationMode::Set:
        TRY(reference.put_value(vm, interpreter.accumulator()));
        break;
    case InitializationMode::InitializeOrSet:
        VERIFY(reference.is_environment_reference());
        VERIFY(reference.base_environment().is_declarative_environment());
        TRY(static_cast<DeclarativeEnvironment&>(reference.base_environment()).initialize_or_set_mutable_binding(vm, name, interpreter.accumulator()));
        break;
    }
    return {};
}

ThrowCompletionOr<void> SetLocal::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().running_execution_context().local_variables[m_index] = interpreter.accumulator();
    return {};
}

static ThrowCompletionOr<NonnullGCPtr<Object>> base_object_for_get(Bytecode::Interpreter& interpreter, Value base_value)
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

static ThrowCompletionOr<void> get_by_id(Bytecode::Interpreter& interpreter, IdentifierTableIndex property, Value base_value, Value this_value, u32 cache_index)
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(property);
    auto& cache = interpreter.current_executable().property_lookup_caches[cache_index];

    if (base_value.is_string()) {
        auto string_value = TRY(base_value.as_string().get(vm, name));
        if (string_value.has_value()) {
            interpreter.accumulator() = *string_value;
            return {};
        }
    }

    auto base_obj = TRY(base_object_for_get(interpreter, base_value));

    // OPTIMIZATION: If the shape of the object hasn't changed, we can use the cached property offset.
    // NOTE: Unique shapes don't change identity, so we compare their serial numbers instead.
    auto& shape = base_obj->shape();
    if (&shape == cache.shape
        && (!shape.is_unique() || shape.unique_shape_serial_number() == cache.unique_shape_serial_number)) {
        interpreter.accumulator() = base_obj->get_direct(cache.property_offset.value());
        return {};
    }

    CacheablePropertyMetadata cacheable_metadata;
    interpreter.accumulator() = TRY(base_obj->internal_get(name, this_value, &cacheable_metadata));

    if (cacheable_metadata.type == CacheablePropertyMetadata::Type::OwnProperty) {
        cache.shape = shape;
        cache.property_offset = cacheable_metadata.property_offset.value();
        cache.unique_shape_serial_number = shape.unique_shape_serial_number();
    }

    return {};
}

ThrowCompletionOr<void> GetById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.accumulator();
    return get_by_id(interpreter, m_property, base_value, base_value, m_cache_index);
}

ThrowCompletionOr<void> GetByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.accumulator();
    auto this_value = interpreter.reg(m_this_value);
    return get_by_id(interpreter, m_property, base_value, this_value, m_cache_index);
}

ThrowCompletionOr<void> GetPrivateById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(m_property);
    auto base_value = interpreter.accumulator();
    auto private_reference = make_private_reference(vm, base_value, name);
    interpreter.accumulator() = TRY(private_reference.get_value(vm));
    return {};
}

ThrowCompletionOr<void> HasPrivateId::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    if (!interpreter.accumulator().is_object())
        return vm.throw_completion<TypeError>(ErrorType::InOperatorWithObject);

    auto private_environment = vm.running_execution_context().private_environment;
    VERIFY(private_environment);
    auto private_name = private_environment->resolve_private_identifier(interpreter.current_executable().get_identifier(m_property));
    interpreter.accumulator() = Value(interpreter.accumulator().as_object().private_element_find(private_name) != nullptr);
    return {};
}

ThrowCompletionOr<void> PutById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();
    auto base = interpreter.reg(m_base);
    PropertyKey name = interpreter.current_executable().get_identifier(m_property);
    TRY(put_by_property_key(vm, base, base, value, name, m_kind));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> PutByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();
    auto base = interpreter.reg(m_base);
    PropertyKey name = interpreter.current_executable().get_identifier(m_property);
    TRY(put_by_property_key(vm, base, interpreter.reg(m_this_value), value, name, m_kind));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> PutPrivateById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();
    auto object = TRY(interpreter.reg(m_base).to_object(vm));
    auto name = interpreter.current_executable().get_identifier(m_property);
    auto private_reference = make_private_reference(vm, object, name);
    TRY(private_reference.put_value(vm, value));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> DeleteById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto base_value = interpreter.accumulator();
    auto const& identifier = interpreter.current_executable().get_identifier(m_property);
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, identifier, {}, strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> DeleteByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto base_value = interpreter.accumulator();
    auto const& identifier = interpreter.current_executable().get_identifier(m_property);
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, identifier, interpreter.reg(m_this_value), strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> Jump::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.jump(*m_true_target);
    return {};
}

ThrowCompletionOr<void> ResolveThisBinding::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (!interpreter.this_value().has_value()) {
        // OPTIMIZATION: Because the value of 'this' cannot be reassigned during a function execution, it's
        //               resolved once and then saved for subsequent use.
        auto& vm = interpreter.vm();
        interpreter.this_value() = TRY(vm.resolve_this_binding());
    }
    interpreter.accumulator() = interpreter.this_value().value();
    return {};
}

// https://tc39.es/ecma262/#sec-makesuperpropertyreference
ThrowCompletionOr<void> ResolveSuperBase::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // 1. Let env be GetThisEnvironment().
    auto& env = verify_cast<FunctionEnvironment>(*get_this_environment(vm));

    // 2. Assert: env.HasSuperBinding() is true.
    VERIFY(env.has_super_binding());

    // 3. Let baseValue be ? env.GetSuperBase().
    interpreter.accumulator() = TRY(env.get_super_base());

    return {};
}

ThrowCompletionOr<void> GetNewTarget::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().get_new_target();
    return {};
}

ThrowCompletionOr<void> GetImportMeta::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().get_import_meta();
    return {};
}

ThrowCompletionOr<void> JumpConditional::execute_impl(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_true_target.has_value());
    VERIFY(m_false_target.has_value());
    auto result = interpreter.accumulator();
    if (result.to_boolean())
        interpreter.jump(m_true_target.value());
    else
        interpreter.jump(m_false_target.value());
    return {};
}

ThrowCompletionOr<void> JumpNullish::execute_impl(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_true_target.has_value());
    VERIFY(m_false_target.has_value());
    auto result = interpreter.accumulator();
    if (result.is_nullish())
        interpreter.jump(m_true_target.value());
    else
        interpreter.jump(m_false_target.value());
    return {};
}

ThrowCompletionOr<void> JumpUndefined::execute_impl(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_true_target.has_value());
    VERIFY(m_false_target.has_value());
    auto result = interpreter.accumulator();
    if (result.is_undefined())
        interpreter.jump(m_true_target.value());
    else
        interpreter.jump(m_false_target.value());
    return {};
}

// 13.3.8.1 https://tc39.es/ecma262/#sec-runtime-semantics-argumentlistevaluation
static MarkedVector<Value> argument_list_evaluation(Bytecode::Interpreter& interpreter)
{
    // Note: Any spreading and actual evaluation is handled in preceding opcodes
    // Note: The spec uses the concept of a list, while we create a temporary array
    //       in the preceding opcodes, so we have to convert in a manner that is not
    //       visible to the user
    auto& vm = interpreter.vm();

    MarkedVector<Value> argument_values { vm.heap() };
    auto arguments = interpreter.accumulator();

    if (!(arguments.is_object() && is<Array>(arguments.as_object()))) {
        dbgln("[{}] Call arguments are not an array, but: {}", interpreter.debug_position(), MUST(arguments.to_string_without_side_effects()));
        interpreter.current_executable().dump();
        VERIFY_NOT_REACHED();
    }
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

static Completion throw_type_error_for_callee(Bytecode::Interpreter& interpreter, auto& call, StringView callee_type)
{
    auto& vm = interpreter.vm();
    auto callee = interpreter.reg(call.callee());

    if (call.expression_string().has_value())
        return vm.throw_completion<TypeError>(ErrorType::IsNotAEvaluatedFrom, TRY_OR_THROW_OOM(vm, callee.to_string_without_side_effects()), callee_type, interpreter.current_executable().get_string(call.expression_string()->value()));

    return vm.throw_completion<TypeError>(ErrorType::IsNotA, TRY_OR_THROW_OOM(vm, callee.to_string_without_side_effects()), callee_type);
}

static ThrowCompletionOr<void> throw_if_needed_for_call(Interpreter& interpreter, auto& call, Value callee)
{
    if (call.call_type() == CallType::Call && !callee.is_function())
        return throw_type_error_for_callee(interpreter, call, "function"sv);
    if (call.call_type() == CallType::Construct && !callee.is_constructor())
        return throw_type_error_for_callee(interpreter, call, "constructor"sv);
    return {};
}

static ThrowCompletionOr<void> perform_call(Interpreter& interpreter, auto& call, Value callee, MarkedVector<Value> argument_values)
{
    auto& vm = interpreter.vm();
    auto this_value = interpreter.reg(call.this_value());
    auto& function = callee.as_function();
    Value return_value;
    if (call.call_type() == CallType::DirectEval) {
        if (callee == interpreter.realm().intrinsics().eval_function())
            return_value = TRY(perform_eval(vm, !argument_values.is_empty() ? argument_values[0].value_or(JS::js_undefined()) : js_undefined(), vm.in_strict_mode() ? CallerMode::Strict : CallerMode::NonStrict, EvalMode::Direct));
        else
            return_value = TRY(JS::call(vm, function, this_value, move(argument_values)));
    } else if (call.call_type() == CallType::Call)
        return_value = TRY(JS::call(vm, function, this_value, move(argument_values)));
    else
        return_value = TRY(construct(vm, function, move(argument_values)));

    interpreter.accumulator() = return_value;
    return {};
}

ThrowCompletionOr<void> Call::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto callee = interpreter.reg(m_callee);

    TRY(throw_if_needed_for_call(interpreter, *this, callee));

    MarkedVector<Value> argument_values(vm.heap());
    argument_values.ensure_capacity(m_argument_count);
    for (u32 i = 0; i < m_argument_count; ++i) {
        argument_values.unchecked_append(interpreter.reg(Register { m_first_argument.index() + i }));
    }
    return perform_call(interpreter, *this, callee, move(argument_values));
}

ThrowCompletionOr<void> CallWithArgumentArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee = interpreter.reg(m_callee);
    TRY(throw_if_needed_for_call(interpreter, *this, callee));
    auto argument_values = argument_list_evaluation(interpreter);
    return perform_call(interpreter, *this, callee, move(argument_values));
}

// 13.3.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
ThrowCompletionOr<void> SuperCallWithArgumentArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    // 1. Let newTarget be GetNewTarget().
    auto new_target = vm.get_new_target();

    // 2. Assert: Type(newTarget) is Object.
    VERIFY(new_target.is_object());

    // 3. Let func be GetSuperConstructor().
    auto* func = get_super_constructor(vm);

    // 4. Let argList be ? ArgumentListEvaluation of Arguments.
    MarkedVector<Value> arg_list { vm.heap() };
    if (m_is_synthetic) {
        auto const& value = interpreter.accumulator();
        VERIFY(value.is_object() && is<Array>(value.as_object()));
        auto const& array_value = static_cast<Array const&>(value.as_object());
        auto length = MUST(length_of_array_like(vm, array_value));
        for (size_t i = 0; i < length; ++i)
            arg_list.append(array_value.get_without_side_effects(PropertyKey { i }));
    } else {
        arg_list = argument_list_evaluation(interpreter);
    }

    // 5. If IsConstructor(func) is false, throw a TypeError exception.
    if (!Value(func).is_constructor())
        return vm.throw_completion<TypeError>(ErrorType::NotAConstructor, "Super constructor");

    // 6. Let result be ? Construct(func, argList, newTarget).
    auto result = TRY(construct(vm, static_cast<FunctionObject&>(*func), move(arg_list), &new_target.as_function()));

    // 7. Let thisER be GetThisEnvironment().
    auto& this_environment = verify_cast<FunctionEnvironment>(*get_this_environment(vm));

    // 8. Perform ? thisER.BindThisValue(result).
    TRY(this_environment.bind_this_value(vm, result));

    // 9. Let F be thisER.[[FunctionObject]].
    auto& f = this_environment.function_object();

    // 10. Assert: F is an ECMAScript function object.
    // NOTE: This is implied by the strong C++ type.

    // 11. Perform ? InitializeInstanceElements(result, F).
    TRY(result->initialize_instance_elements(f));

    // 12. Return result.
    interpreter.accumulator() = result;
    return {};
}

ThrowCompletionOr<void> NewFunction::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    if (!m_function_node.has_name()) {
        DeprecatedFlyString name = {};
        if (m_lhs_name.has_value())
            name = interpreter.current_executable().get_identifier(m_lhs_name.value());
        interpreter.accumulator() = m_function_node.instantiate_ordinary_function_expression(vm, name);
    } else {
        interpreter.accumulator() = ECMAScriptFunctionObject::create(interpreter.realm(), m_function_node.name(), m_function_node.source_text(), m_function_node.body(), m_function_node.parameters(), m_function_node.function_length(), m_function_node.local_variables_names(), vm.lexical_environment(), vm.running_execution_context().private_environment, m_function_node.kind(), m_function_node.is_strict_mode(), m_function_node.might_need_arguments_object(), m_function_node.contains_direct_call_to_eval(), m_function_node.is_arrow_function());
    }

    if (m_home_object.has_value()) {
        auto home_object_value = interpreter.reg(m_home_object.value());
        static_cast<ECMAScriptFunctionObject&>(interpreter.accumulator().as_function()).set_home_object(&home_object_value.as_object());
    }
    return {};
}

ThrowCompletionOr<void> Return::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.do_return(interpreter.accumulator().value_or(js_undefined()));
    return {};
}

ThrowCompletionOr<void> Increment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = TRY(interpreter.accumulator().to_numeric(vm));

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() + 1);
    else
        interpreter.accumulator() = BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
    return {};
}

ThrowCompletionOr<void> Decrement::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = TRY(interpreter.accumulator().to_numeric(vm));

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() - 1);
    else
        interpreter.accumulator() = BigInt::create(vm, old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
    return {};
}

ThrowCompletionOr<void> Throw::execute_impl(Bytecode::Interpreter& interpreter) const
{
    return throw_completion(interpreter.accumulator());
}

ThrowCompletionOr<void> ThrowIfNotObject::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    if (!interpreter.accumulator().is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, TRY_OR_THROW_OOM(vm, interpreter.accumulator().to_string_without_side_effects()));
    return {};
}

ThrowCompletionOr<void> ThrowIfNullish::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.accumulator();
    if (value.is_nullish())
        return vm.throw_completion<TypeError>(ErrorType::NotObjectCoercible, TRY_OR_THROW_OOM(vm, value.to_string_without_side_effects()));
    return {};
}

ThrowCompletionOr<void> EnterUnwindContext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.enter_unwind_context(m_handler_target, m_finalizer_target);
    interpreter.jump(m_entry_point);
    return {};
}

ThrowCompletionOr<void> ScheduleJump::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.schedule_jump(m_target);
    return {};
}

ThrowCompletionOr<void> LeaveLexicalEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().running_execution_context().lexical_environment = interpreter.saved_lexical_environment_stack().take_last();
    return {};
}

ThrowCompletionOr<void> LeaveUnwindContext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.leave_unwind_context();
    return {};
}

ThrowCompletionOr<void> ContinuePendingUnwind::execute_impl(Bytecode::Interpreter& interpreter) const
{
    return interpreter.continue_pending_unwind(m_resume_target);
}

ThrowCompletionOr<void> PushDeclarativeEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto environment = interpreter.vm().heap().allocate_without_realm<DeclarativeEnvironment>(interpreter.vm().lexical_environment());
    interpreter.vm().running_execution_context().lexical_environment = environment;
    interpreter.vm().running_execution_context().variable_environment = environment;
    return {};
}

ThrowCompletionOr<void> Yield::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto yielded_value = interpreter.accumulator().value_or(js_undefined());
    auto object = Object::create(interpreter.realm(), nullptr);
    object->define_direct_property("result", yielded_value, JS::default_attributes);

    if (m_continuation_label.has_value())
        // FIXME: If we get a pointer, which is not accurately representable as a double
        //        will cause this to explode
        object->define_direct_property("continuation", Value(static_cast<double>(reinterpret_cast<u64>(&m_continuation_label->block()))), JS::default_attributes);
    else
        object->define_direct_property("continuation", Value(0), JS::default_attributes);

    object->define_direct_property("isAwait", Value(false), JS::default_attributes);
    interpreter.do_return(object);
    return {};
}

ThrowCompletionOr<void> Await::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto yielded_value = interpreter.accumulator().value_or(js_undefined());
    auto object = Object::create(interpreter.realm(), nullptr);
    object->define_direct_property("result", yielded_value, JS::default_attributes);
    // FIXME: If we get a pointer, which is not accurately representable as a double
    //        will cause this to explode
    object->define_direct_property("continuation", Value(static_cast<double>(reinterpret_cast<u64>(&m_continuation_label.block()))), JS::default_attributes);
    object->define_direct_property("isAwait", Value(true), JS::default_attributes);
    interpreter.do_return(object);
    return {};
}

ThrowCompletionOr<void> GetByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the property key from the accumulator before side effects have a chance to overwrite it.
    auto property_key_value = interpreter.accumulator();

    auto base_value = interpreter.reg(m_base);
    auto object = TRY(base_object_for_get(interpreter, base_value));
    auto property_key = TRY(property_key_value.to_property_key(vm));

    if (base_value.is_string()) {
        auto string_value = TRY(base_value.as_string().get(vm, property_key));
        if (string_value.has_value()) {
            interpreter.accumulator() = *string_value;
            return {};
        }
    }

    interpreter.accumulator() = TRY(object->internal_get(property_key, base_value));
    return {};
}

ThrowCompletionOr<void> GetByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the property key from the accumulator before side effects have a chance to overwrite it.
    auto property_key_value = interpreter.accumulator();

    auto object = TRY(interpreter.reg(m_base).to_object(vm));

    auto property_key = TRY(property_key_value.to_property_key(vm));

    interpreter.accumulator() = TRY(object->internal_get(property_key, interpreter.reg(m_this_value)));
    return {};
}

ThrowCompletionOr<void> PutByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();

    auto base = interpreter.reg(m_base);

    auto property_key = TRY(interpreter.reg(m_property).to_property_key(vm));
    TRY(put_by_property_key(vm, base, base, value, property_key, m_kind));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> PutByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();

    auto base = interpreter.reg(m_base);

    auto property_key = TRY(interpreter.reg(m_property).to_property_key(vm));
    TRY(put_by_property_key(vm, base, interpreter.reg(m_this_value), value, property_key, m_kind));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> DeleteByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the property key from the accumulator before side effects have a chance to overwrite it.
    auto property_key_value = interpreter.accumulator();

    auto base_value = interpreter.reg(m_base);
    auto property_key = TRY(property_key_value.to_property_key(vm));
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, property_key, {}, strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> DeleteByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the property key from the accumulator before side effects have a chance to overwrite it.
    auto property_key_value = interpreter.accumulator();

    auto base_value = interpreter.reg(m_base);
    auto property_key = TRY(property_key_value.to_property_key(vm));
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, property_key, interpreter.reg(m_this_value), strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> GetIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator = TRY(get_iterator(vm, interpreter.accumulator(), m_hint));
    interpreter.accumulator() = iterator_to_object(vm, iterator);
    return {};
}

ThrowCompletionOr<void> GetMethod::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto identifier = interpreter.current_executable().get_identifier(m_property);
    auto method = TRY(interpreter.accumulator().get_method(vm, identifier));
    interpreter.accumulator() = method ?: js_undefined();
    return {};
}

// 14.7.5.9 EnumerateObjectProperties ( O ), https://tc39.es/ecma262/#sec-enumerate-object-properties
ThrowCompletionOr<void> GetObjectPropertyIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    // While the spec does provide an algorithm, it allows us to implement it ourselves so long as we meet the following invariants:
    //    1- Returned property keys do not include keys that are Symbols
    //    2- Properties of the target object may be deleted during enumeration. A property that is deleted before it is processed by the iterator's next method is ignored
    //    3- If new properties are added to the target object during enumeration, the newly added properties are not guaranteed to be processed in the active enumeration
    //    4- A property name will be returned by the iterator's next method at most once in any enumeration.
    //    5- Enumerating the properties of the target object includes enumerating properties of its prototype, and the prototype of the prototype, and so on, recursively;
    //       but a property of a prototype is not processed if it has the same name as a property that has already been processed by the iterator's next method.
    //    6- The values of [[Enumerable]] attributes are not considered when determining if a property of a prototype object has already been processed.
    //    7- The enumerable property names of prototype objects must be obtained by invoking EnumerateObjectProperties passing the prototype object as the argument.
    //    8- EnumerateObjectProperties must obtain the own property keys of the target object by calling its [[OwnPropertyKeys]] internal method.
    //    9- Property attributes of the target object must be obtained by calling its [[GetOwnProperty]] internal method

    // Invariant 3 effectively allows the implementation to ignore newly added keys, and we do so (similar to other implementations).
    auto& vm = interpreter.vm();
    auto object = TRY(interpreter.accumulator().to_object(vm));
    // Note: While the spec doesn't explicitly require these to be ordered, it says that the values should be retrieved via OwnPropertyKeys,
    //       so we just keep the order consistent anyway.
    OrderedHashTable<PropertyKey> properties;
    OrderedHashTable<PropertyKey> non_enumerable_properties;
    HashTable<NonnullGCPtr<Object>> seen_objects;
    // Collect all keys immediately (invariant no. 5)
    for (auto object_to_check = GCPtr { object.ptr() }; object_to_check && !seen_objects.contains(*object_to_check); object_to_check = TRY(object_to_check->internal_get_prototype_of())) {
        seen_objects.set(*object_to_check);
        for (auto& key : TRY(object_to_check->internal_own_property_keys())) {
            if (key.is_symbol())
                continue;
            auto property_key = TRY(PropertyKey::from_value(vm, key));

            // If there is a non-enumerable property higher up the prototype chain with the same key,
            // we mustn't include this property even if it's enumerable (invariant no. 5 and 6)
            if (non_enumerable_properties.contains(property_key))
                continue;
            if (properties.contains(property_key))
                continue;

            auto descriptor = TRY(object_to_check->internal_get_own_property(property_key));
            if (!*descriptor->enumerable)
                non_enumerable_properties.set(move(property_key));
            else
                properties.set(move(property_key));
        }
    }
    IteratorRecord iterator {
        .iterator = object,
        .next_method = NativeFunction::create(
            interpreter.realm(),
            [items = move(properties)](VM& vm) mutable -> ThrowCompletionOr<Value> {
                auto& realm = *vm.current_realm();
                auto iterated_object_value = vm.this_value();
                if (!iterated_object_value.is_object())
                    return vm.throw_completion<InternalError>("Invalid state for GetObjectPropertyIterator.next"sv);

                auto& iterated_object = iterated_object_value.as_object();
                auto result_object = Object::create(realm, nullptr);
                while (true) {
                    if (items.is_empty()) {
                        result_object->define_direct_property(vm.names.done, JS::Value(true), default_attributes);
                        return result_object;
                    }

                    auto key = items.take_first();

                    // If the property is deleted, don't include it (invariant no. 2)
                    if (!TRY(iterated_object.has_property(key)))
                        continue;

                    result_object->define_direct_property(vm.names.done, JS::Value(false), default_attributes);

                    if (key.is_number())
                        result_object->define_direct_property(vm.names.value, PrimitiveString::create(vm, TRY_OR_THROW_OOM(vm, String::number(key.as_number()))), default_attributes);
                    else if (key.is_string())
                        result_object->define_direct_property(vm.names.value, PrimitiveString::create(vm, key.as_string()), default_attributes);
                    else
                        VERIFY_NOT_REACHED(); // We should not have non-string/number keys.

                    return result_object;
                }
            },
            1,
            vm.names.next),
        .done = false,
    };
    interpreter.accumulator() = iterator_to_object(vm, move(iterator));
    return {};
}

ThrowCompletionOr<void> IteratorClose::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, iterator_object);

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY(iterator_close(vm, iterator, Completion { m_completion_type, m_completion_value, {} }));
    return {};
}

ThrowCompletionOr<void> AsyncIteratorClose::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, iterator_object);

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY(async_iterator_close(vm, iterator, Completion { m_completion_type, m_completion_value, {} }));
    return {};
}

ThrowCompletionOr<void> IteratorNext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, iterator_object);

    interpreter.accumulator() = TRY(iterator_next(vm, iterator));
    return {};
}

ThrowCompletionOr<void> IteratorResultDone::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_result = TRY(interpreter.accumulator().to_object(vm));

    auto complete = TRY(iterator_complete(vm, iterator_result));
    interpreter.accumulator() = Value(complete);
    return {};
}

ThrowCompletionOr<void> IteratorResultValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_result = TRY(interpreter.accumulator().to_object(vm));

    interpreter.accumulator() = TRY(iterator_value(vm, iterator_result));
    return {};
}

ThrowCompletionOr<void> NewClass::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto name = m_class_expression.name();
    auto super_class = interpreter.accumulator();

    // NOTE: NewClass expects classEnv to be active lexical environment
    auto class_environment = vm.lexical_environment();
    vm.running_execution_context().lexical_environment = interpreter.saved_lexical_environment_stack().take_last();

    DeprecatedFlyString binding_name;
    DeprecatedFlyString class_name;
    if (!m_class_expression.has_name() && m_lhs_name.has_value()) {
        class_name = interpreter.current_executable().get_identifier(m_lhs_name.value());
    } else {
        binding_name = name;
        class_name = name.is_null() ? ""sv : name;
    }

    interpreter.accumulator() = TRY(m_class_expression.create_class_constructor(vm, class_environment, vm.lexical_environment(), super_class, binding_name, class_name));

    return {};
}

// 13.5.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-typeof-operator-runtime-semantics-evaluation
ThrowCompletionOr<void> TypeofVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // 1. Let val be the result of evaluating UnaryExpression.
    auto const& string = interpreter.current_executable().get_identifier(m_identifier);
    auto reference = TRY(vm.resolve_binding(string));

    // 2. If val is a Reference Record, then
    //    a. If IsUnresolvableReference(val) is true, return "undefined".
    if (reference.is_unresolvable()) {
        interpreter.accumulator() = MUST_OR_THROW_OOM(PrimitiveString::create(vm, "undefined"sv));
        return {};
    }

    // 3. Set val to ? GetValue(val).
    auto value = TRY(reference.get_value(vm));

    // 4. NOTE: This step is replaced in section B.3.6.3.
    // 5. Return a String according to Table 41.
    interpreter.accumulator() = MUST_OR_THROW_OOM(PrimitiveString::create(vm, value.typeof()));
    return {};
}

ThrowCompletionOr<void> TypeofLocal::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& value = vm.running_execution_context().local_variables[m_index];
    interpreter.accumulator() = MUST_OR_THROW_OOM(PrimitiveString::create(vm, value.typeof()));
    return {};
}

ThrowCompletionOr<void> ToNumeric::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(interpreter.accumulator().to_numeric(interpreter.vm()));
    return {};
}

ThrowCompletionOr<void> BlockDeclarationInstantiation::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_environment = vm.running_execution_context().lexical_environment;
    interpreter.saved_lexical_environment_stack().append(old_environment);
    vm.running_execution_context().lexical_environment = new_declarative_environment(*old_environment);
    m_scope_node.block_declaration_instantiation(vm, vm.running_execution_context().lexical_environment);
    return {};
}

DeprecatedString Load::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("Load {}", m_src);
}

DeprecatedString LoadImmediate::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("LoadImmediate {}", m_value);
}

DeprecatedString Store::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("Store {}", m_dst);
}

DeprecatedString NewBigInt::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("NewBigInt \"{}\"", m_bigint.to_base_deprecated(10));
}

DeprecatedString NewArray::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.append("NewArray"sv);
    if (m_element_count != 0) {
        builder.appendff(" [{}-{}]", m_elements[0], m_elements[1]);
    }
    return builder.to_deprecated_string();
}

DeprecatedString Append::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (m_is_spread)
        return DeprecatedString::formatted("Append lhs: **{}", m_lhs);
    return DeprecatedString::formatted("Append lhs: {}", m_lhs);
}

DeprecatedString IteratorToArray::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "IteratorToArray";
}

DeprecatedString NewString::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("NewString {} (\"{}\")", m_string, executable.string_table->get(m_string));
}

DeprecatedString NewObject::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "NewObject";
}

DeprecatedString NewRegExp::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("NewRegExp source:{} (\"{}\") flags:{} (\"{}\")", m_source_index, executable.get_string(m_source_index), m_flags_index, executable.get_string(m_flags_index));
}

DeprecatedString CopyObjectExcludingProperties::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.appendff("CopyObjectExcludingProperties from:{}", m_from_object);
    if (m_excluded_names_count != 0) {
        builder.append(" excluding:["sv);
        builder.join(", "sv, ReadonlySpan<Register>(m_excluded_names, m_excluded_names_count));
        builder.append(']');
    }
    return builder.to_deprecated_string();
}

DeprecatedString ConcatString::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("ConcatString {}", m_lhs);
}

DeprecatedString GetVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString GetGlobal::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetGlobal {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString GetLocal::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("GetLocal {}", m_index);
}

DeprecatedString DeleteVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("DeleteVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString CreateLexicalEnvironment::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "CreateLexicalEnvironment"sv;
}

DeprecatedString CreateVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return DeprecatedString::formatted("CreateVariable env:{} immutable:{} global:{} {} ({})", mode_string, m_is_immutable, m_is_global, m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString EnterObjectEnvironment::to_deprecated_string_impl(Executable const&) const
{
    return DeprecatedString::formatted("EnterObjectEnvironment");
}

DeprecatedString SetVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto initialization_mode_name = m_initialization_mode == InitializationMode ::Initialize ? "Initialize"
        : m_initialization_mode == InitializationMode::Set                                   ? "Set"
                                                                                             : "InitializeOrSet";
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return DeprecatedString::formatted("SetVariable env:{} init:{} {} ({})", mode_string, initialization_mode_name, m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString SetLocal::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("SetLocal {}", m_index);
}

DeprecatedString PutById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = m_kind == PropertyKind::Getter
        ? "getter"
        : m_kind == PropertyKind::Setter
        ? "setter"
        : "property";

    return DeprecatedString::formatted("PutById kind:{} base:{}, property:{} ({})", kind, m_base, m_property, executable.identifier_table->get(m_property));
}

DeprecatedString PutByIdWithThis::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = m_kind == PropertyKind::Getter
        ? "getter"
        : m_kind == PropertyKind::Setter
        ? "setter"
        : "property";

    return DeprecatedString::formatted("PutByIdWithThis kind:{} base:{}, property:{} ({}) this_value:{}", kind, m_base, m_property, executable.identifier_table->get(m_property), m_this_value);
}

DeprecatedString PutPrivateById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = m_kind == PropertyKind::Getter
        ? "getter"
        : m_kind == PropertyKind::Setter
        ? "setter"
        : "property";

    return DeprecatedString::formatted("PutPrivateById kind:{} base:{}, property:{} ({})", kind, m_base, m_property, executable.identifier_table->get(m_property));
}

DeprecatedString GetById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetById {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString GetByIdWithThis::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetByIdWithThis {} ({}) this_value:{}", m_property, executable.identifier_table->get(m_property), m_this_value);
}

DeprecatedString GetPrivateById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetPrivateById {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString HasPrivateId::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("HasPrivateId {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString DeleteById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("DeleteById {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString DeleteByIdWithThis::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("DeleteByIdWithThis {} ({}) this_value:{}", m_property, executable.identifier_table->get(m_property), m_this_value);
}

DeprecatedString Jump::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (m_true_target.has_value())
        return DeprecatedString::formatted("Jump {}", *m_true_target);
    return DeprecatedString::formatted("Jump <empty>");
}

DeprecatedString JumpConditional::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? DeprecatedString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? DeprecatedString::formatted("{}", *m_false_target) : "<empty>";
    return DeprecatedString::formatted("JumpConditional true:{} false:{}", true_string, false_string);
}

DeprecatedString JumpNullish::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? DeprecatedString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? DeprecatedString::formatted("{}", *m_false_target) : "<empty>";
    return DeprecatedString::formatted("JumpNullish null:{} nonnull:{}", true_string, false_string);
}

DeprecatedString JumpUndefined::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? DeprecatedString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? DeprecatedString::formatted("{}", *m_false_target) : "<empty>";
    return DeprecatedString::formatted("JumpUndefined undefined:{} not undefined:{}", true_string, false_string);
}

static StringView call_type_to_string(CallType type)
{
    switch (type) {
    case CallType::Call:
        return ""sv;
    case CallType::Construct:
        return " (Construct)"sv;
    case CallType::DirectEval:
        return " (DirectEval)"sv;
    }
    VERIFY_NOT_REACHED();
}

DeprecatedString Call::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto type = call_type_to_string(m_type);
    if (m_expression_string.has_value())
        return DeprecatedString::formatted("Call{} callee:{}, this:{}, first_arg:{} ({})", type, m_callee, m_this_value, m_first_argument, executable.get_string(m_expression_string.value()));
    return DeprecatedString::formatted("Call{} callee:{}, this:{}, first_arg:{}", type, m_callee, m_first_argument, m_this_value);
}

DeprecatedString CallWithArgumentArray::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto type = call_type_to_string(m_type);
    if (m_expression_string.has_value())
        return DeprecatedString::formatted("CallWithArgumentArray{} callee:{}, this:{}, arguments:[...acc] ({})", type, m_callee, m_this_value, executable.get_string(m_expression_string.value()));
    return DeprecatedString::formatted("CallWithArgumentArray{} callee:{}, this:{}, arguments:[...acc]", type, m_callee, m_this_value);
}

DeprecatedString SuperCallWithArgumentArray::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "SuperCallWithArgumentArray arguments:[...acc]"sv;
}

DeprecatedString NewFunction::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.append("NewFunction"sv);
    if (m_function_node.has_name())
        builder.appendff(" name:{}"sv, m_function_node.name());
    if (m_lhs_name.has_value())
        builder.appendff(" lhs_name:{}"sv, m_lhs_name.value());
    if (m_home_object.has_value())
        builder.appendff(" home_object:{}"sv, m_home_object.value());
    return builder.to_deprecated_string();
}

DeprecatedString NewClass::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    auto name = m_class_expression.name();
    builder.appendff("NewClass '{}'"sv, name.is_null() ? ""sv : name);
    if (m_lhs_name.has_value())
        builder.appendff(" lhs_name:{}"sv, m_lhs_name.value());
    return builder.to_deprecated_string();
}

DeprecatedString Return::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "Return";
}

DeprecatedString Increment::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "Increment";
}

DeprecatedString Decrement::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "Decrement";
}

DeprecatedString Throw::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "Throw";
}

DeprecatedString ThrowIfNotObject::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ThrowIfNotObject";
}

DeprecatedString ThrowIfNullish::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ThrowIfNullish";
}

DeprecatedString EnterUnwindContext::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto handler_string = m_handler_target.has_value() ? DeprecatedString::formatted("{}", *m_handler_target) : "<empty>";
    auto finalizer_string = m_finalizer_target.has_value() ? DeprecatedString::formatted("{}", *m_finalizer_target) : "<empty>";
    return DeprecatedString::formatted("EnterUnwindContext handler:{} finalizer:{} entry:{}", handler_string, finalizer_string, m_entry_point);
}

DeprecatedString ScheduleJump::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("ScheduleJump {}", m_target);
}

DeprecatedString LeaveLexicalEnvironment::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "LeaveLexicalEnvironment"sv;
}

DeprecatedString LeaveUnwindContext::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "LeaveUnwindContext";
}

DeprecatedString ContinuePendingUnwind::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("ContinuePendingUnwind resume:{}", m_resume_target);
}

DeprecatedString PushDeclarativeEnvironment::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    StringBuilder builder;
    builder.append("PushDeclarativeEnvironment"sv);
    if (!m_variables.is_empty()) {
        builder.append(" {"sv);
        Vector<DeprecatedString> names;
        for (auto& it : m_variables)
            names.append(executable.get_string(it.key));
        builder.append('}');
        builder.join(", "sv, names);
    }
    return builder.to_deprecated_string();
}

DeprecatedString Yield::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (m_continuation_label.has_value())
        return DeprecatedString::formatted("Yield continuation:@{}", m_continuation_label->block().name());
    return DeprecatedString::formatted("Yield return");
}

DeprecatedString Await::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("Await continuation:@{}", m_continuation_label.block().name());
}

DeprecatedString GetByValue::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("GetByValue base:{}", m_base);
}

DeprecatedString GetByValueWithThis::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("GetByValueWithThis base:{} this_value:{}", m_base, m_this_value);
}

DeprecatedString PutByValue::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto kind = m_kind == PropertyKind::Getter
        ? "getter"
        : m_kind == PropertyKind::Setter
        ? "setter"
        : "property";

    return DeprecatedString::formatted("PutByValue kind:{} base:{}, property:{}", kind, m_base, m_property);
}

DeprecatedString PutByValueWithThis::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto kind = m_kind == PropertyKind::Getter
        ? "getter"
        : m_kind == PropertyKind::Setter
        ? "setter"
        : "property";

    return DeprecatedString::formatted("PutByValueWithThis kind:{} base:{}, property:{} this_value:{}", kind, m_base, m_property, m_this_value);
}

DeprecatedString DeleteByValue::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("DeleteByValue base:{}", m_base);
}

DeprecatedString DeleteByValueWithThis::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("DeleteByValueWithThis base:{} this_value:{}", m_base, m_this_value);
}

DeprecatedString GetIterator::to_deprecated_string_impl(Executable const&) const
{
    auto hint = m_hint == IteratorHint::Sync ? "sync" : "async";
    return DeprecatedString::formatted("GetIterator hint:{}", hint);
}

DeprecatedString GetMethod::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetMethod {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString GetObjectPropertyIterator::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "GetObjectPropertyIterator";
}

DeprecatedString IteratorClose::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (!m_completion_value.has_value())
        return DeprecatedString::formatted("IteratorClose completion_type={} completion_value=<empty>", to_underlying(m_completion_type));

    auto completion_value_string = m_completion_value->to_string_without_side_effects().release_value_but_fixme_should_propagate_errors();
    return DeprecatedString::formatted("IteratorClose completion_type={} completion_value={}", to_underlying(m_completion_type), completion_value_string);
}

DeprecatedString AsyncIteratorClose::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (!m_completion_value.has_value())
        return DeprecatedString::formatted("AsyncIteratorClose completion_type={} completion_value=<empty>", to_underlying(m_completion_type));

    auto completion_value_string = m_completion_value->to_string_without_side_effects().release_value_but_fixme_should_propagate_errors();
    return DeprecatedString::formatted("AsyncIteratorClose completion_type={} completion_value={}", to_underlying(m_completion_type), completion_value_string);
}

DeprecatedString IteratorNext::to_deprecated_string_impl(Executable const&) const
{
    return "IteratorNext";
}

DeprecatedString IteratorResultDone::to_deprecated_string_impl(Executable const&) const
{
    return "IteratorResultDone";
}

DeprecatedString IteratorResultValue::to_deprecated_string_impl(Executable const&) const
{
    return "IteratorResultValue";
}

DeprecatedString ResolveThisBinding::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ResolveThisBinding"sv;
}

DeprecatedString ResolveSuperBase::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ResolveSuperBase"sv;
}

DeprecatedString GetNewTarget::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "GetNewTarget"sv;
}

DeprecatedString GetImportMeta::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "GetImportMeta"sv;
}

DeprecatedString TypeofVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("TypeofVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString TypeofLocal::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("TypeofLocal {}", m_index);
}

DeprecatedString ToNumeric::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ToNumeric"sv;
}

DeprecatedString BlockDeclarationInstantiation::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "BlockDeclarationInstantiation"sv;
}

DeprecatedString ImportCall::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("ImportCall specifier:{} options:{}"sv, m_specifier, m_options);
}

}
