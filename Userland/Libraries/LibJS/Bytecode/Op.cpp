/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
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
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode {

String Instruction::to_string(Bytecode::Executable const& executable) const
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op const&>(*this).to_string_impl(executable);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

}

namespace JS::Bytecode::Op {

static ThrowCompletionOr<void> put_by_property_key(Object* object, Value value, PropertyKey name, Bytecode::Interpreter& interpreter, PropertyKind kind)
{
    auto& vm = interpreter.vm();

    if (kind == PropertyKind::Getter || kind == PropertyKind::Setter) {
        // The generator should only pass us functions for getters and setters.
        VERIFY(value.is_function());
    }
    switch (kind) {
    case PropertyKind::Getter: {
        auto& function = value.as_function();
        if (function.name().is_empty() && is<ECMAScriptFunctionObject>(function))
            static_cast<ECMAScriptFunctionObject*>(&function)->set_name(String::formatted("get {}", name));
        object->define_direct_accessor(name, &function, nullptr, Attribute::Configurable | Attribute::Enumerable);
        break;
    }
    case PropertyKind::Setter: {
        auto& function = value.as_function();
        if (function.name().is_empty() && is<ECMAScriptFunctionObject>(function))
            static_cast<ECMAScriptFunctionObject*>(&function)->set_name(String::formatted("set {}", name));
        object->define_direct_accessor(name, nullptr, &function, Attribute::Configurable | Attribute::Enumerable);
        break;
    }
    case PropertyKind::KeyValue: {
        bool succeeded = TRY(object->internal_set(name, interpreter.accumulator(), object));
        if (!succeeded && vm.in_strict_mode())
            return vm.throw_completion<TypeError>(ErrorType::ReferenceNullishSetProperty, name, interpreter.accumulator().to_string_without_side_effects());
        break;
    }
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
    String OpTitleCase::to_string_impl(Bytecode::Executable const&) const                       \
    {                                                                                           \
        return String::formatted(#OpTitleCase " {}", m_lhs_reg);                                \
    }

JS_ENUMERATE_COMMON_BINARY_OPS(JS_DEFINE_COMMON_BINARY_OP)

static ThrowCompletionOr<Value> not_(VM&, Value value)
{
    return Value(!value.to_boolean());
}

static ThrowCompletionOr<Value> typeof_(VM& vm, Value value)
{
    return Value(js_string(vm, value.typeof()));
}

#define JS_DEFINE_COMMON_UNARY_OP(OpTitleCase, op_snake_case)                                   \
    ThrowCompletionOr<void> OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const \
    {                                                                                           \
        auto& vm = interpreter.vm();                                                            \
        interpreter.accumulator() = TRY(op_snake_case(vm, interpreter.accumulator()));          \
        return {};                                                                              \
    }                                                                                           \
    String OpTitleCase::to_string_impl(Bytecode::Executable const&) const                       \
    {                                                                                           \
        return #OpTitleCase;                                                                    \
    }

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DEFINE_COMMON_UNARY_OP)

ThrowCompletionOr<void> NewBigInt::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = js_bigint(interpreter.vm().heap(), m_bigint);
    return {};
}

ThrowCompletionOr<void> NewArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto* array = MUST(Array::create(interpreter.realm(), 0));
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

// FIXME: Since the accumulator is a Value, we store an object there and have to convert back and forth between that an Iterator records. Not great.
// Make sure to put this into the accumulator before the iterator object disappears from the stack to prevent the members from being GC'd.
static Object* iterator_to_object(VM& vm, Iterator iterator)
{
    auto& realm = *vm.current_realm();
    auto* object = Object::create(realm, nullptr);
    object->define_direct_property(vm.names.iterator, iterator.iterator, 0);
    object->define_direct_property(vm.names.next, iterator.next_method, 0);
    object->define_direct_property(vm.names.done, Value(iterator.done), 0);
    return object;
}

static Iterator object_to_iterator(VM& vm, Object& object)
{
    return Iterator {
        .iterator = &MUST(object.get(vm.names.iterator)).as_object(),
        .next_method = MUST(object.get(vm.names.next)),
        .done = MUST(object.get(vm.names.done)).as_bool()
    };
}

ThrowCompletionOr<void> IteratorToArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, *iterator_object);

    auto* array = MUST(Array::create(interpreter.realm(), 0));
    size_t index = 0;

    while (true) {
        auto* iterator_result = TRY(iterator_next(vm, iterator));

        auto complete = TRY(iterator_complete(vm, *iterator_result));

        if (complete) {
            interpreter.accumulator() = array;
            return {};
        }

        auto value = TRY(iterator_value(vm, *iterator_result));

        MUST(array->create_data_property_or_throw(index, value));
        index++;
    }
    return {};
}

ThrowCompletionOr<void> NewString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = js_string(interpreter.vm(), interpreter.current_executable().get_string(m_string));
    return {};
}

ThrowCompletionOr<void> NewObject::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    interpreter.accumulator() = Object::create(realm, realm.intrinsics().object_prototype());
    return {};
}

ThrowCompletionOr<void> NewRegExp::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto source = interpreter.current_executable().get_string(m_source_index);
    auto flags = interpreter.current_executable().get_string(m_flags_index);

    interpreter.accumulator() = TRY(regexp_create(vm, js_string(vm, source), js_string(vm, flags)));
    return {};
}

ThrowCompletionOr<void> CopyObjectExcludingProperties::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    auto* from_object = TRY(interpreter.reg(m_from_object).to_object(vm));

    auto* to_object = Object::create(realm, realm.intrinsics().object_prototype());

    HashTable<Value, ValueTraits> excluded_names;
    for (size_t i = 0; i < m_excluded_names_count; ++i)
        excluded_names.set(interpreter.reg(m_excluded_names[i]));

    auto own_keys = TRY(from_object->internal_own_property_keys());

    for (auto& key : own_keys) {
        if (!excluded_names.contains(key)) {
            auto property_key = TRY(key.to_property_key(vm));
            auto property_value = TRY(from_object->get(property_key));
            to_object->define_direct_property(property_key, property_value, JS::default_attributes);
        }
    }

    interpreter.accumulator() = to_object;
    return {};
}

ThrowCompletionOr<void> ConcatString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.reg(m_lhs) = TRY(add(vm, interpreter.reg(m_lhs), interpreter.accumulator()));
    return {};
}

ThrowCompletionOr<void> GetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto get_reference = [&]() -> ThrowCompletionOr<Reference> {
        auto const& string = interpreter.current_executable().get_identifier(m_identifier);
        if (m_cached_environment_coordinate.has_value()) {
            auto* environment = vm.running_execution_context().lexical_environment;
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

ThrowCompletionOr<void> DeleteVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& string = interpreter.current_executable().get_identifier(m_identifier);
    auto reference = TRY(vm.resolve_binding(string));
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> CreateEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto make_and_swap_envs = [&](auto*& old_environment) {
        Environment* environment = new_declarative_environment(*old_environment);
        swap(old_environment, environment);
        return environment;
    };
    if (m_mode == EnvironmentMode::Lexical)
        interpreter.saved_lexical_environment_stack().append(make_and_swap_envs(interpreter.vm().running_execution_context().lexical_environment));
    else if (m_mode == EnvironmentMode::Var)
        interpreter.saved_variable_environment_stack().append(make_and_swap_envs(interpreter.vm().running_execution_context().variable_environment));
    return {};
}

ThrowCompletionOr<void> EnterObjectEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& old_environment = vm.running_execution_context().lexical_environment;
    interpreter.saved_lexical_environment_stack().append(old_environment);
    auto object = TRY(interpreter.accumulator().to_object(vm));
    vm.running_execution_context().lexical_environment = new_object_environment(*object, true, old_environment);
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
            return vm.throw_completion<InternalError>(String::formatted("Lexical environment already has binding '{}'", name));

        if (m_is_immutable)
            vm.lexical_environment()->create_immutable_binding(vm, name, vm.in_strict_mode());
        else
            vm.lexical_environment()->create_mutable_binding(vm, name, vm.in_strict_mode());
    } else {
        if (!m_is_global) {
            if (m_is_immutable)
                vm.variable_environment()->create_immutable_binding(vm, name, vm.in_strict_mode());
            else
                vm.variable_environment()->create_mutable_binding(vm, name, vm.in_strict_mode());
        } else {
            // NOTE: CreateVariable with m_is_global set to true is expected to only be used in GlobalDeclarationInstantiation currently, which only uses "false" for "can_be_deleted".
            //       The only area that sets "can_be_deleted" to true is EvalDeclarationInstantiation, which is currently fully implemented in C++ and not in Bytecode.
            verify_cast<GlobalEnvironment>(vm.variable_environment())->create_global_var_binding(name, false);
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

ThrowCompletionOr<void> GetById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* object = TRY(interpreter.accumulator().to_object(vm));
    interpreter.accumulator() = TRY(object->get(interpreter.current_executable().get_identifier(m_property)));
    return {};
}

ThrowCompletionOr<void> PutById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* object = TRY(interpreter.reg(m_base).to_object(vm));
    PropertyKey name = interpreter.current_executable().get_identifier(m_property);
    auto value = interpreter.accumulator();
    return put_by_property_key(object, value, name, interpreter, m_kind);
}

ThrowCompletionOr<void> DeleteById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* object = TRY(interpreter.accumulator().to_object(vm));
    auto const& identifier = interpreter.current_executable().get_identifier(m_property);
    bool strict = vm.in_strict_mode();
    auto reference = Reference { object, identifier, {}, strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
};

ThrowCompletionOr<void> Jump::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.jump(*m_true_target);
    return {};
}

ThrowCompletionOr<void> ResolveThisBinding::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = TRY(vm.resolve_this_binding());
    return {};
}

ThrowCompletionOr<void> GetNewTarget::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().get_new_target();
    return {};
}

void Jump::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (m_true_target.has_value() && &m_true_target->block() == &from)
        m_true_target = Label { to };
    if (m_false_target.has_value() && &m_false_target->block() == &from)
        m_false_target = Label { to };
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

Completion Call::throw_type_error_for_callee(Bytecode::Interpreter& interpreter, StringView callee_type) const
{
    auto callee = interpreter.reg(m_callee);
    if (m_expression_string.has_value())
        return interpreter.vm().throw_completion<TypeError>(ErrorType::IsNotAEvaluatedFrom, callee.to_string_without_side_effects(), callee_type, interpreter.current_executable().get_string(m_expression_string->value()));

    return interpreter.vm().throw_completion<TypeError>(ErrorType::IsNotA, callee.to_string_without_side_effects(), callee_type);
}

ThrowCompletionOr<void> Call::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto callee = interpreter.reg(m_callee);

    if (m_type == CallType::Call && !callee.is_function())
        return throw_type_error_for_callee(interpreter, "function"sv);
    if (m_type == CallType::Construct && !callee.is_constructor())
        return throw_type_error_for_callee(interpreter, "constructor"sv);

    auto& function = callee.as_function();

    auto this_value = interpreter.reg(m_this_value);

    auto argument_values = argument_list_evaluation(interpreter);

    Value return_value;
    if (m_type == CallType::Call)
        return_value = TRY(call(vm, function, this_value, move(argument_values)));
    else
        return_value = TRY(construct(vm, function, move(argument_values)));

    interpreter.accumulator() = return_value;
    return {};
}

// 13.3.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
ThrowCompletionOr<void> SuperCall::execute_impl(Bytecode::Interpreter& interpreter) const
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
    auto* result = TRY(construct(vm, static_cast<FunctionObject&>(*func), move(arg_list), &new_target.as_function()));

    // 7. Let thisER be GetThisEnvironment().
    auto& this_environment = verify_cast<FunctionEnvironment>(get_this_environment(vm));

    // 8. Perform ? thisER.BindThisValue(result).
    TRY(this_environment.bind_this_value(vm, result));

    // 9. Let F be thisER.[[FunctionObject]].
    auto& f = this_environment.function_object();

    // 10. Assert: F is an ECMAScript function object.
    // NOTE: This is implied by the strong C++ type.

    // 11. Perform ? InitializeInstanceElements(result, F).
    TRY(vm.initialize_instance_elements(*result, f));

    // 12. Return result.
    interpreter.accumulator() = result;
    return {};
}

ThrowCompletionOr<void> NewFunction::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = ECMAScriptFunctionObject::create(interpreter.realm(), m_function_node.name(), m_function_node.source_text(), m_function_node.body(), m_function_node.parameters(), m_function_node.function_length(), vm.lexical_environment(), vm.running_execution_context().private_environment, m_function_node.kind(), m_function_node.is_strict_mode(), m_function_node.might_need_arguments_object(), m_function_node.contains_direct_call_to_eval(), m_function_node.is_arrow_function());
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
        interpreter.accumulator() = js_bigint(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
    return {};
}

ThrowCompletionOr<void> Decrement::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = TRY(interpreter.accumulator().to_numeric(vm));

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() - 1);
    else
        interpreter.accumulator() = js_bigint(vm, old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
    return {};
}

ThrowCompletionOr<void> Throw::execute_impl(Bytecode::Interpreter& interpreter) const
{
    return throw_completion(interpreter.accumulator());
}

ThrowCompletionOr<void> EnterUnwindContext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.enter_unwind_context(m_handler_target, m_finalizer_target);
    interpreter.jump(m_entry_point);
    return {};
}

void EnterUnwindContext::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (&m_entry_point.block() == &from)
        m_entry_point = Label { to };
    if (m_handler_target.has_value() && &m_handler_target->block() == &from)
        m_handler_target = Label { to };
    if (m_finalizer_target.has_value() && &m_finalizer_target->block() == &from)
        m_finalizer_target = Label { to };
}

ThrowCompletionOr<void> FinishUnwind::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.leave_unwind_context();
    interpreter.jump(m_next_target);
    return {};
}

void FinishUnwind::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (&m_next_target.block() == &from)
        m_next_target = Label { to };
}

ThrowCompletionOr<void> LeaveEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (m_mode == EnvironmentMode::Lexical)
        interpreter.vm().running_execution_context().lexical_environment = interpreter.saved_lexical_environment_stack().take_last();
    if (m_mode == EnvironmentMode::Var)
        interpreter.vm().running_execution_context().variable_environment = interpreter.saved_variable_environment_stack().take_last();
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

void ContinuePendingUnwind::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (&m_resume_target.block() == &from)
        m_resume_target = Label { to };
}

ThrowCompletionOr<void> PushDeclarativeEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto* environment = interpreter.vm().heap().allocate_without_realm<DeclarativeEnvironment>(interpreter.vm().lexical_environment());
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
        object->define_direct_property("continuation", Value(static_cast<double>(reinterpret_cast<u64>(&m_continuation_label->block()))), JS::default_attributes);
    else
        object->define_direct_property("continuation", Value(0), JS::default_attributes);
    interpreter.do_return(object);
    return {};
}

void Yield::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (m_continuation_label.has_value() && &m_continuation_label->block() == &from)
        m_continuation_label = Label { to };
}

ThrowCompletionOr<void> GetByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* object = TRY(interpreter.reg(m_base).to_object(vm));

    auto property_key = TRY(interpreter.accumulator().to_property_key(vm));

    interpreter.accumulator() = TRY(object->get(property_key));
    return {};
}

ThrowCompletionOr<void> PutByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* object = TRY(interpreter.reg(m_base).to_object(vm));

    auto property_key = TRY(interpreter.reg(m_property).to_property_key(vm));
    return put_by_property_key(object, interpreter.accumulator(), property_key, interpreter, m_kind);
}

ThrowCompletionOr<void> DeleteByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* object = TRY(interpreter.reg(m_base).to_object(vm));
    auto property_key = TRY(interpreter.accumulator().to_property_key(vm));
    bool strict = vm.in_strict_mode();
    auto reference = Reference { object, property_key, {}, strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> GetIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator = TRY(get_iterator(vm, interpreter.accumulator()));
    interpreter.accumulator() = iterator_to_object(vm, iterator);
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
    // Invariants 1 and 6 through 9 are implemented in `enumerable_own_property_names`, which implements the EnumerableOwnPropertyNames AO.
    auto& vm = interpreter.vm();
    auto* object = TRY(interpreter.accumulator().to_object(vm));
    // Note: While the spec doesn't explicitly require these to be ordered, it says that the values should be retrieved via OwnPropertyKeys,
    //       so we just keep the order consistent anyway.
    OrderedHashTable<PropertyKey> properties;
    HashTable<Object*> seen_objects;
    // Collect all keys immediately (invariant no. 5)
    for (auto* object_to_check = object; object_to_check && !seen_objects.contains(object_to_check); object_to_check = TRY(object_to_check->internal_get_prototype_of())) {
        seen_objects.set(object_to_check);
        for (auto& key : TRY(object_to_check->enumerable_own_property_names(Object::PropertyKind::Key))) {
            properties.set(TRY(PropertyKey::from_value(vm, key)));
        }
    }
    Iterator iterator {
        .iterator = object,
        .next_method = NativeFunction::create(
            interpreter.realm(),
            [seen_items = HashTable<PropertyKey>(), items = move(properties)](VM& vm) mutable -> ThrowCompletionOr<Value> {
                auto& realm = *vm.current_realm();
                auto iterated_object_value = vm.this_value();
                if (!iterated_object_value.is_object())
                    return vm.throw_completion<InternalError>("Invalid state for GetObjectPropertyIterator.next");

                auto& iterated_object = iterated_object_value.as_object();
                auto* result_object = Object::create(realm, nullptr);
                while (true) {
                    if (items.is_empty()) {
                        result_object->define_direct_property(vm.names.done, JS::Value(true), default_attributes);
                        return result_object;
                    }

                    auto it = items.begin();
                    auto key = *it;
                    items.remove(it);

                    // If the key was already seen, skip over it (invariant no. 4)
                    auto result = seen_items.set(key);
                    if (result != AK::HashSetResult::InsertedNewEntry)
                        continue;

                    // If the property is deleted, don't include it (invariant no. 2)
                    if (!TRY(iterated_object.has_property(key)))
                        continue;

                    result_object->define_direct_property(vm.names.done, JS::Value(false), default_attributes);

                    if (key.is_number())
                        result_object->define_direct_property(vm.names.value, JS::Value(key.as_number()), default_attributes);
                    else if (key.is_string())
                        result_object->define_direct_property(vm.names.value, js_string(vm, key.as_string()), default_attributes);
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

ThrowCompletionOr<void> IteratorNext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, *iterator_object);

    interpreter.accumulator() = TRY(iterator_next(vm, iterator));
    return {};
}

ThrowCompletionOr<void> IteratorResultDone::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* iterator_result = TRY(interpreter.accumulator().to_object(vm));

    auto complete = TRY(iterator_complete(vm, *iterator_result));
    interpreter.accumulator() = Value(complete);
    return {};
}

ThrowCompletionOr<void> IteratorResultValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto* iterator_result = TRY(interpreter.accumulator().to_object(vm));

    interpreter.accumulator() = TRY(iterator_value(vm, *iterator_result));
    return {};
}

ThrowCompletionOr<void> NewClass::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto name = m_class_expression.name();
    auto scope = interpreter.ast_interpreter_scope();
    auto& ast_interpreter = scope.interpreter();

    auto* class_object = TRY(m_class_expression.class_definition_evaluation(ast_interpreter, name, name.is_null() ? ""sv : name));
    class_object->set_source_text(m_class_expression.source_text());

    interpreter.accumulator() = class_object;
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
        interpreter.accumulator() = js_string(vm, "undefined"sv);
        return {};
    }

    // 3. Set val to ? GetValue(val).
    auto value = TRY(reference.get_value(vm));

    // 4. NOTE: This step is replaced in section B.3.6.3.
    // 5. Return a String according to Table 41.
    interpreter.accumulator() = js_string(vm, value.typeof());
    return {};
}

String Load::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("Load {}", m_src);
}

String LoadImmediate::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("LoadImmediate {}", m_value);
}

String Store::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("Store {}", m_dst);
}

String NewBigInt::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("NewBigInt \"{}\"", m_bigint.to_base(10));
}

String NewArray::to_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.append("NewArray"sv);
    if (m_element_count != 0) {
        builder.appendff(" [{}-{}]", m_elements[0], m_elements[1]);
    }
    return builder.to_string();
}

String Append::to_string_impl(Bytecode::Executable const&) const
{
    if (m_is_spread)
        return String::formatted("Append lhs: **{}", m_lhs);
    return String::formatted("Append lhs: {}", m_lhs);
}

String IteratorToArray::to_string_impl(Bytecode::Executable const&) const
{
    return "IteratorToArray";
}

String NewString::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("NewString {} (\"{}\")", m_string, executable.string_table->get(m_string));
}

String NewObject::to_string_impl(Bytecode::Executable const&) const
{
    return "NewObject";
}

String NewRegExp::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("NewRegExp source:{} (\"{}\") flags:{} (\"{}\")", m_source_index, executable.get_string(m_source_index), m_flags_index, executable.get_string(m_flags_index));
}

String CopyObjectExcludingProperties::to_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.appendff("CopyObjectExcludingProperties from:{}", m_from_object);
    if (m_excluded_names_count != 0) {
        builder.append(" excluding:["sv);
        builder.join(", "sv, Span<Register const>(m_excluded_names, m_excluded_names_count));
        builder.append(']');
    }
    return builder.to_string();
}

String ConcatString::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("ConcatString {}", m_lhs);
}

String GetVariable::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("GetVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

String DeleteVariable::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("DeleteVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

String CreateEnvironment::to_string_impl(Bytecode::Executable const&) const
{
    auto mode_string = m_mode == EnvironmentMode::Lexical
        ? "Lexical"
        : "Variable";
    return String::formatted("CreateEnvironment mode:{}", mode_string);
}

String CreateVariable::to_string_impl(Bytecode::Executable const& executable) const
{
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return String::formatted("CreateVariable env:{} immutable:{} global:{} {} ({})", mode_string, m_is_immutable, m_is_global, m_identifier, executable.identifier_table->get(m_identifier));
}

String EnterObjectEnvironment::to_string_impl(Executable const&) const
{
    return String::formatted("EnterObjectEnvironment");
}

String SetVariable::to_string_impl(Bytecode::Executable const& executable) const
{
    auto initialization_mode_name = m_initialization_mode == InitializationMode ::Initialize ? "Initialize"
        : m_initialization_mode == InitializationMode::Set                                   ? "Set"
                                                                                             : "InitializeOrSet";
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return String::formatted("SetVariable env:{} init:{} {} ({})", mode_string, initialization_mode_name, m_identifier, executable.identifier_table->get(m_identifier));
}

String PutById::to_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = m_kind == PropertyKind::Getter
        ? "getter"
        : m_kind == PropertyKind::Setter
        ? "setter"
        : "property";

    return String::formatted("PutById kind:{} base:{}, property:{} ({})", kind, m_base, m_property, executable.identifier_table->get(m_property));
}

String GetById::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("GetById {} ({})", m_property, executable.identifier_table->get(m_property));
}

String DeleteById::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("DeleteById {} ({})", m_property, executable.identifier_table->get(m_property));
}

String Jump::to_string_impl(Bytecode::Executable const&) const
{
    if (m_true_target.has_value())
        return String::formatted("Jump {}", *m_true_target);
    return String::formatted("Jump <empty>");
}

String JumpConditional::to_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? String::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? String::formatted("{}", *m_false_target) : "<empty>";
    return String::formatted("JumpConditional true:{} false:{}", true_string, false_string);
}

String JumpNullish::to_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? String::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? String::formatted("{}", *m_false_target) : "<empty>";
    return String::formatted("JumpNullish null:{} nonnull:{}", true_string, false_string);
}

String JumpUndefined::to_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? String::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? String::formatted("{}", *m_false_target) : "<empty>";
    return String::formatted("JumpUndefined undefined:{} not undefined:{}", true_string, false_string);
}

String Call::to_string_impl(Bytecode::Executable const& executable) const
{
    if (m_expression_string.has_value())
        return String::formatted("Call callee:{}, this:{}, arguments:[...acc] ({})", m_callee, m_this_value, executable.get_string(m_expression_string.value()));

    return String::formatted("Call callee:{}, this:{}, arguments:[...acc]", m_callee, m_this_value);
}

String SuperCall::to_string_impl(Bytecode::Executable const&) const
{
    return "SuperCall arguments:[...acc]"sv;
}

String NewFunction::to_string_impl(Bytecode::Executable const&) const
{
    return "NewFunction";
}

String NewClass::to_string_impl(Bytecode::Executable const&) const
{
    auto name = m_class_expression.name();
    return String::formatted("NewClass '{}'", name.is_null() ? ""sv : name);
}

String Return::to_string_impl(Bytecode::Executable const&) const
{
    return "Return";
}

String Increment::to_string_impl(Bytecode::Executable const&) const
{
    return "Increment";
}

String Decrement::to_string_impl(Bytecode::Executable const&) const
{
    return "Decrement";
}

String Throw::to_string_impl(Bytecode::Executable const&) const
{
    return "Throw";
}

String EnterUnwindContext::to_string_impl(Bytecode::Executable const&) const
{
    auto handler_string = m_handler_target.has_value() ? String::formatted("{}", *m_handler_target) : "<empty>";
    auto finalizer_string = m_finalizer_target.has_value() ? String::formatted("{}", *m_finalizer_target) : "<empty>";
    return String::formatted("EnterUnwindContext handler:{} finalizer:{} entry:{}", handler_string, finalizer_string, m_entry_point);
}

String FinishUnwind::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("FinishUnwind next:{}", m_next_target);
}

String LeaveEnvironment::to_string_impl(Bytecode::Executable const&) const
{
    auto mode_string = m_mode == EnvironmentMode::Lexical
        ? "Lexical"
        : "Variable";
    return String::formatted("LeaveEnvironment env:{}", mode_string);
}

String LeaveUnwindContext::to_string_impl(Bytecode::Executable const&) const
{
    return "LeaveUnwindContext";
}

String ContinuePendingUnwind::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("ContinuePendingUnwind resume:{}", m_resume_target);
}

String PushDeclarativeEnvironment::to_string_impl(Bytecode::Executable const& executable) const
{
    StringBuilder builder;
    builder.append("PushDeclarativeEnvironment"sv);
    if (!m_variables.is_empty()) {
        builder.append(" {"sv);
        Vector<String> names;
        for (auto& it : m_variables)
            names.append(executable.get_string(it.key));
        builder.append('}');
        builder.join(", "sv, names);
    }
    return builder.to_string();
}

String Yield::to_string_impl(Bytecode::Executable const&) const
{
    if (m_continuation_label.has_value())
        return String::formatted("Yield continuation:@{}", m_continuation_label->block().name());
    return String::formatted("Yield return");
}

String GetByValue::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("GetByValue base:{}", m_base);
}

String PutByValue::to_string_impl(Bytecode::Executable const&) const
{
    auto kind = m_kind == PropertyKind::Getter
        ? "getter"
        : m_kind == PropertyKind::Setter
        ? "setter"
        : "property";

    return String::formatted("PutByValue kind:{} base:{}, property:{}", kind, m_base, m_property);
}

String DeleteByValue::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("DeleteByValue base:{}", m_base);
}

String GetIterator::to_string_impl(Executable const&) const
{
    return "GetIterator";
}

String GetObjectPropertyIterator::to_string_impl(Bytecode::Executable const&) const
{
    return "GetObjectPropertyIterator";
}

String IteratorNext::to_string_impl(Executable const&) const
{
    return "IteratorNext";
}

String IteratorResultDone::to_string_impl(Executable const&) const
{
    return "IteratorResultDone";
}

String IteratorResultValue::to_string_impl(Executable const&) const
{
    return "IteratorResultValue";
}

String ResolveThisBinding::to_string_impl(Bytecode::Executable const&) const
{
    return "ResolveThisBinding"sv;
}

String GetNewTarget::to_string_impl(Bytecode::Executable const&) const
{
    return "GetNewTarget"sv;
}

String TypeofVariable::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("TypeofVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

}
