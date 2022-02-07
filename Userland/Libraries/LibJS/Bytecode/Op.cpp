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
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/IteratorOperations.h>
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

void Load::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.reg(m_src);
}

void LoadImmediate::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = m_value;
}

void Store::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = interpreter.accumulator();
}

static ThrowCompletionOr<Value> abstract_inequals(GlobalObject& global_object, Value src1, Value src2)
{
    return Value(!TRY(is_loosely_equal(global_object, src1, src2)));
}

static ThrowCompletionOr<Value> abstract_equals(GlobalObject& global_object, Value src1, Value src2)
{
    return Value(TRY(is_loosely_equal(global_object, src1, src2)));
}

static ThrowCompletionOr<Value> typed_inequals(GlobalObject&, Value src1, Value src2)
{
    return Value(!is_strictly_equal(src1, src2));
}

static ThrowCompletionOr<Value> typed_equals(GlobalObject&, Value src1, Value src2)
{
    return Value(is_strictly_equal(src1, src2));
}

#define JS_DEFINE_COMMON_BINARY_OP(OpTitleCase, op_snake_case)                       \
    void OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const         \
    {                                                                                \
        auto lhs = interpreter.reg(m_lhs_reg);                                       \
        auto rhs = interpreter.accumulator();                                        \
        auto result_or_error = op_snake_case(interpreter.global_object(), lhs, rhs); \
        if (result_or_error.is_error())                                              \
            return;                                                                  \
        interpreter.accumulator() = result_or_error.release_value();                 \
    }                                                                                \
    String OpTitleCase::to_string_impl(Bytecode::Executable const&) const            \
    {                                                                                \
        return String::formatted(#OpTitleCase " {}", m_lhs_reg);                     \
    }

JS_ENUMERATE_COMMON_BINARY_OPS(JS_DEFINE_COMMON_BINARY_OP)

static ThrowCompletionOr<Value> not_(GlobalObject&, Value value)
{
    return Value(!value.to_boolean());
}

static ThrowCompletionOr<Value> typeof_(GlobalObject& global_object, Value value)
{
    return Value(js_string(global_object.vm(), value.typeof()));
}

#define JS_DEFINE_COMMON_UNARY_OP(OpTitleCase, op_snake_case)                                         \
    void OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const                          \
    {                                                                                                 \
        auto result_or_error = op_snake_case(interpreter.global_object(), interpreter.accumulator()); \
        if (result_or_error.is_error())                                                               \
            return;                                                                                   \
        interpreter.accumulator() = result_or_error.release_value();                                  \
    }                                                                                                 \
    String OpTitleCase::to_string_impl(Bytecode::Executable const&) const                             \
    {                                                                                                 \
        return #OpTitleCase;                                                                          \
    }

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DEFINE_COMMON_UNARY_OP)

void NewBigInt::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = js_bigint(interpreter.vm().heap(), m_bigint);
}

void NewArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    Vector<Value> elements;
    elements.ensure_capacity(m_element_count);
    for (size_t i = 0; i < m_element_count; i++)
        elements.append(interpreter.reg(m_elements[i]));
    interpreter.accumulator() = Array::create_from(interpreter.global_object(), elements);
}

// FIXME: Since the accumulator is a Value, we store an object there and have to convert back and forth between that an Iterator records. Not great.
// Make sure to put this into the accumulator before the iterator object disappears from the stack to prevent the members from being GC'd.
static Object* iterator_to_object(GlobalObject& global_object, Iterator iterator)
{
    auto& vm = global_object.vm();
    auto* object = Object::create(global_object, nullptr);
    object->define_direct_property(vm.names.iterator, iterator.iterator, 0);
    object->define_direct_property(vm.names.next, iterator.next_method, 0);
    object->define_direct_property(vm.names.done, Value(iterator.done), 0);
    return object;
}

static Iterator object_to_iterator(GlobalObject& global_object, Object& object)
{
    auto& vm = global_object.vm();
    return Iterator {
        .iterator = &MUST(object.get(vm.names.iterator)).as_object(),
        .next_method = MUST(object.get(vm.names.next)),
        .done = MUST(object.get(vm.names.done)).as_bool()
    };
}

void IteratorToArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& global_object = interpreter.global_object();
    auto iterator_object_or_error = interpreter.accumulator().to_object(global_object);
    if (iterator_object_or_error.is_error())
        return;
    auto iterator = object_to_iterator(global_object, *iterator_object_or_error.release_value());

    auto* array = MUST(Array::create(global_object, 0));
    size_t index = 0;

    while (true) {
        auto iterator_result_or_error = iterator_next(global_object, iterator);
        if (iterator_result_or_error.is_error())
            return;
        auto* iterator_result = iterator_result_or_error.release_value();

        auto complete_or_error = iterator_complete(global_object, *iterator_result);
        if (complete_or_error.is_error())
            return;
        auto complete = complete_or_error.release_value();

        if (complete) {
            interpreter.accumulator() = array;
            return;
        }

        auto value_or_error = iterator_value(global_object, *iterator_result);
        if (value_or_error.is_error())
            return;
        auto value = value_or_error.release_value();

        MUST(array->create_data_property_or_throw(index, value));
        index++;
    }
}

void NewString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = js_string(interpreter.vm(), interpreter.current_executable().get_string(m_string));
}

void NewObject::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = Object::create(interpreter.global_object(), interpreter.global_object().object_prototype());
}

void NewRegExp::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto source = interpreter.current_executable().get_string(m_source_index);
    auto flags = interpreter.current_executable().get_string(m_flags_index);

    auto regexp_or_error = regexp_create(interpreter.global_object(), js_string(interpreter.vm(), source), js_string(interpreter.vm(), flags));
    if (regexp_or_error.is_error())
        return;
    interpreter.accumulator() = regexp_or_error.value();
}

void CopyObjectExcludingProperties::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto from_object_or_error = interpreter.reg(m_from_object).to_object(interpreter.global_object());
    if (from_object_or_error.is_error())
        return;
    auto* from_object = from_object_or_error.release_value();

    auto* to_object = Object::create(interpreter.global_object(), interpreter.global_object().object_prototype());

    HashTable<Value, ValueTraits> excluded_names;
    for (size_t i = 0; i < m_excluded_names_count; ++i) {
        excluded_names.set(interpreter.reg(m_excluded_names[i]));
        if (interpreter.vm().exception())
            return;
    }

    auto own_keys_or_error = from_object->internal_own_property_keys();
    if (own_keys_or_error.is_error())
        return;
    auto own_keys = own_keys_or_error.release_value();

    for (auto& key : own_keys) {
        if (!excluded_names.contains(key)) {
            auto property_key_or_error = key.to_property_key(interpreter.global_object());
            if (property_key_or_error.is_error())
                return;
            PropertyKey property_key = property_key_or_error.release_value();
            auto property_value_or_error = from_object->get(property_key);
            if (property_value_or_error.is_error())
                return;
            auto property_value = property_value_or_error.release_value();
            to_object->define_direct_property(property_key, property_value, JS::default_attributes);
        }
    }

    interpreter.accumulator() = to_object;
}

void ConcatString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto result_or_error = add(interpreter.global_object(), interpreter.reg(m_lhs), interpreter.accumulator());
    if (result_or_error.is_error())
        return;
    interpreter.reg(m_lhs) = result_or_error.release_value();
}

void GetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto reference = [&] {
        auto const& string = interpreter.current_executable().get_identifier(m_identifier);
        if (m_cached_environment_coordinate.has_value()) {
            auto* environment = interpreter.vm().running_execution_context().lexical_environment;
            for (size_t i = 0; i < m_cached_environment_coordinate->hops; ++i)
                environment = environment->outer_environment();
            VERIFY(environment);
            VERIFY(environment->is_declarative_environment());
            if (!environment->is_permanently_screwed_by_eval()) {
                return Reference { *environment, string, interpreter.vm().in_strict_mode(), m_cached_environment_coordinate };
            }
            m_cached_environment_coordinate = {};
        }

        auto reference_or_error = interpreter.vm().resolve_binding(string);
        if (reference_or_error.is_throw_completion()) {
            interpreter.vm().throw_exception(interpreter.global_object(), *reference_or_error.release_error().value());
            return Reference {};
        }

        auto reference = reference_or_error.release_value();
        if (reference.environment_coordinate().has_value())
            m_cached_environment_coordinate = reference.environment_coordinate();
        return reference;
    }();

    if (interpreter.vm().exception())
        return;

    auto value_or_error = reference.get_value(interpreter.global_object());
    if (value_or_error.is_error())
        return;
    interpreter.accumulator() = value_or_error.release_value();
}

void SetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto reference_or_error = vm.resolve_binding(interpreter.current_executable().get_identifier(m_identifier));
    if (reference_or_error.is_throw_completion()) {
        interpreter.vm().throw_exception(interpreter.global_object(), *reference_or_error.release_error().value());
        return;
    }

    auto reference = reference_or_error.release_value();
    // TODO: ThrowCompletionOr<void> return
    (void)reference.put_value(interpreter.global_object(), interpreter.accumulator());
}

void GetById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto object_or_error = interpreter.accumulator().to_object(interpreter.global_object());
    if (object_or_error.is_error())
        return;
    auto* object = object_or_error.release_value();
    auto value_or_error = object->get(interpreter.current_executable().get_identifier(m_property));
    if (value_or_error.is_error())
        return;
    interpreter.accumulator() = value_or_error.release_value();
}

void PutById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto object_or_error = interpreter.reg(m_base).to_object(interpreter.global_object());
    if (object_or_error.is_error())
        return;
    auto* object = object_or_error.release_value();
    MUST(object->set(interpreter.current_executable().get_identifier(m_property), interpreter.accumulator(), Object::ShouldThrowExceptions::Yes));
}

void Jump::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.jump(*m_true_target);
}

void ResolveThisBinding::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto value_or_error = interpreter.vm().resolve_this_binding(interpreter.global_object());
    if (value_or_error.is_error())
        return;
    interpreter.accumulator() = value_or_error.release_value();
}

void Jump::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (m_true_target.has_value() && &m_true_target->block() == &from)
        m_true_target = Label { to };
    if (m_false_target.has_value() && &m_false_target->block() == &from)
        m_false_target = Label { to };
}

void JumpConditional::execute_impl(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_true_target.has_value());
    VERIFY(m_false_target.has_value());
    auto result = interpreter.accumulator();
    if (result.to_boolean())
        interpreter.jump(m_true_target.value());
    else
        interpreter.jump(m_false_target.value());
}

void JumpNullish::execute_impl(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_true_target.has_value());
    VERIFY(m_false_target.has_value());
    auto result = interpreter.accumulator();
    if (result.is_nullish())
        interpreter.jump(m_true_target.value());
    else
        interpreter.jump(m_false_target.value());
}

void JumpUndefined::execute_impl(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_true_target.has_value());
    VERIFY(m_false_target.has_value());
    auto result = interpreter.accumulator();
    if (result.is_undefined())
        interpreter.jump(m_true_target.value());
    else
        interpreter.jump(m_false_target.value());
}

void Call::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee = interpreter.reg(m_callee);
    if (!callee.is_function()) {
        interpreter.vm().throw_exception<TypeError>(interpreter.global_object(), ErrorType::IsNotA, callee.to_string_without_side_effects(), "function"sv);
        return;
    }
    auto& function = callee.as_function();

    auto this_value = interpreter.reg(m_this_value);

    Value return_value;

    if (m_argument_count == 0 && m_type == CallType::Call) {
        auto return_value_or_error = call(interpreter.global_object(), function, this_value);
        if (!return_value_or_error.is_error())
            return_value = return_value_or_error.release_value();
    } else {
        MarkedValueList argument_values { interpreter.vm().heap() };
        for (size_t i = 0; i < m_argument_count; ++i) {
            argument_values.append(interpreter.reg(m_arguments[i]));
        }
        if (m_type == CallType::Call) {
            auto return_value_or_error = call(interpreter.global_object(), function, this_value, move(argument_values));
            if (return_value_or_error.is_error())
                return;
            return_value = return_value_or_error.release_value();
        } else {
            auto return_value_or_error = construct(interpreter.global_object(), function, move(argument_values));
            if (return_value_or_error.is_error())
                return;
            return_value = return_value_or_error.release_value();
        }
    }

    interpreter.accumulator() = return_value;
}

void NewFunction::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = ECMAScriptFunctionObject::create(interpreter.global_object(), m_function_node.name(), m_function_node.source_text(), m_function_node.body(), m_function_node.parameters(), m_function_node.function_length(), vm.lexical_environment(), vm.running_execution_context().private_environment, m_function_node.kind(), m_function_node.is_strict_mode(), m_function_node.might_need_arguments_object(), m_function_node.is_arrow_function());
}

void Return::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.do_return(interpreter.accumulator().value_or(js_undefined()));
}

void Increment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto old_value_or_error = interpreter.accumulator().to_numeric(interpreter.global_object());
    if (old_value_or_error.is_error())
        return;
    auto old_value = old_value_or_error.release_value();

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() + 1);
    else
        interpreter.accumulator() = js_bigint(interpreter.vm().heap(), old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
}

void Decrement::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto old_value_or_error = interpreter.accumulator().to_numeric(interpreter.global_object());
    if (old_value_or_error.is_error())
        return;
    auto old_value = old_value_or_error.release_value();

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() - 1);
    else
        interpreter.accumulator() = js_bigint(interpreter.vm().heap(), old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
}

void Throw::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().throw_exception(interpreter.global_object(), interpreter.accumulator());
}

void EnterUnwindContext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.enter_unwind_context(m_handler_target, m_finalizer_target);
    interpreter.jump(m_entry_point);
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

void FinishUnwind::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.leave_unwind_context();
    interpreter.jump(m_next_target);
}

void FinishUnwind::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (&m_next_target.block() == &from)
        m_next_target = Label { to };
}

void LeaveUnwindContext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.leave_unwind_context();
}

void ContinuePendingUnwind::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.continue_pending_unwind(m_resume_target);
}

void ContinuePendingUnwind::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (&m_resume_target.block() == &from)
        m_resume_target = Label { to };
}

void PushDeclarativeEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto* environment = interpreter.vm().heap().allocate_without_global_object<DeclarativeEnvironment>(interpreter.vm().lexical_environment());
    interpreter.vm().running_execution_context().lexical_environment = environment;
    interpreter.vm().running_execution_context().variable_environment = environment;
}

void Yield::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto yielded_value = interpreter.accumulator().value_or(js_undefined());
    auto object = JS::Object::create(interpreter.global_object(), nullptr);
    object->define_direct_property("result", yielded_value, JS::default_attributes);
    if (m_continuation_label.has_value())
        object->define_direct_property("continuation", Value(static_cast<double>(reinterpret_cast<u64>(&m_continuation_label->block()))), JS::default_attributes);
    else
        object->define_direct_property("continuation", Value(0), JS::default_attributes);
    interpreter.do_return(object);
}

void Yield::replace_references_impl(BasicBlock const& from, BasicBlock const& to)
{
    if (m_continuation_label.has_value() && &m_continuation_label->block() == &from)
        m_continuation_label = Label { to };
}

void GetByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto object_or_error = interpreter.reg(m_base).to_object(interpreter.global_object());
    if (object_or_error.is_error())
        return;
    auto* object = object_or_error.release_value();
    auto property_key_or_error = interpreter.accumulator().to_property_key(interpreter.global_object());
    if (property_key_or_error.is_error())
        return;
    auto value_or_error = object->get(property_key_or_error.release_value());
    if (value_or_error.is_error())
        return;
    interpreter.accumulator() = value_or_error.release_value();
}

void PutByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto object_or_error = interpreter.reg(m_base).to_object(interpreter.global_object());
    if (object_or_error.is_error())
        return;
    auto* object = object_or_error.release_value();
    auto property_key_or_error = interpreter.reg(m_property).to_property_key(interpreter.global_object());
    if (property_key_or_error.is_error())
        return;
    MUST(object->set(property_key_or_error.release_value(), interpreter.accumulator(), Object::ShouldThrowExceptions::Yes));
}

void GetIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto iterator_or_error = get_iterator(interpreter.global_object(), interpreter.accumulator());
    if (iterator_or_error.is_error())
        return;
    interpreter.accumulator() = iterator_to_object(interpreter.global_object(), iterator_or_error.release_value());
}

void IteratorNext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto iterator_object_or_error = interpreter.accumulator().to_object(interpreter.global_object());
    if (iterator_object_or_error.is_error())
        return;
    auto iterator = object_to_iterator(interpreter.global_object(), *iterator_object_or_error.release_value());

    auto iterator_result_or_error = iterator_next(interpreter.global_object(), iterator);
    if (iterator_result_or_error.is_error())
        return;
    auto* iterator_result = iterator_result_or_error.release_value();

    interpreter.accumulator() = iterator_result;
}

void IteratorResultDone::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto iterator_result_or_error = interpreter.accumulator().to_object(interpreter.global_object());
    if (iterator_result_or_error.is_error())
        return;
    auto* iterator_result = iterator_result_or_error.release_value();

    auto complete_or_error = iterator_complete(interpreter.global_object(), *iterator_result);
    if (complete_or_error.is_error())
        return;
    auto complete = complete_or_error.release_value();

    interpreter.accumulator() = Value(complete);
}

void IteratorResultValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto iterator_result_or_error = interpreter.accumulator().to_object(interpreter.global_object());
    if (iterator_result_or_error.is_error())
        return;
    auto* iterator_result = iterator_result_or_error.release_value();

    auto value_or_error = iterator_value(interpreter.global_object(), *iterator_result);
    if (value_or_error.is_error())
        return;
    auto value = value_or_error.release_value();

    interpreter.accumulator() = value;
}

void NewClass::execute_impl(Bytecode::Interpreter&) const
{
    (void)m_class_expression;
    TODO();
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
    builder.append("NewArray");
    if (m_element_count != 0) {
        builder.append(" [");
        for (size_t i = 0; i < m_element_count; ++i) {
            builder.appendff("{}", m_elements[i]);
            if (i != m_element_count - 1)
                builder.append(',');
        }
        builder.append(']');
    }
    return builder.to_string();
}

String IteratorToArray::to_string_impl(const Bytecode::Executable&) const
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

String CopyObjectExcludingProperties::to_string_impl(const Bytecode::Executable&) const
{
    StringBuilder builder;
    builder.appendff("CopyObjectExcludingProperties from:{}", m_from_object);
    if (m_excluded_names_count != 0) {
        builder.append(" excluding:[");
        for (size_t i = 0; i < m_excluded_names_count; ++i) {
            builder.appendff("{}", m_excluded_names[i]);
            if (i != m_excluded_names_count - 1)
                builder.append(',');
        }
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

String SetVariable::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("SetVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

String PutById::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("PutById base:{}, property:{} ({})", m_base, m_property, executable.identifier_table->get(m_property));
}

String GetById::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("GetById {} ({})", m_property, executable.identifier_table->get(m_property));
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

String Call::to_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.appendff("Call callee:{}, this:{}", m_callee, m_this_value);
    if (m_argument_count != 0) {
        builder.append(", arguments:[");
        for (size_t i = 0; i < m_argument_count; ++i) {
            builder.appendff("{}", m_arguments[i]);
            if (i != m_argument_count - 1)
                builder.append(',');
        }
        builder.append(']');
    }
    return builder.to_string();
}

String NewFunction::to_string_impl(Bytecode::Executable const&) const
{
    return "NewFunction";
}

String NewClass::to_string_impl(Bytecode::Executable const&) const
{
    return "NewClass";
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

String FinishUnwind::to_string_impl(const Bytecode::Executable&) const
{
    return String::formatted("FinishUnwind next:{}", m_next_target);
}

String LeaveUnwindContext::to_string_impl(Bytecode::Executable const&) const
{
    return "LeaveUnwindContext";
}

String ContinuePendingUnwind::to_string_impl(Bytecode::Executable const&) const
{
    return String::formatted("ContinuePendingUnwind resume:{}", m_resume_target);
}

String PushDeclarativeEnvironment::to_string_impl(const Bytecode::Executable& executable) const
{
    StringBuilder builder;
    builder.append("PushDeclarativeEnvironment");
    if (!m_variables.is_empty()) {
        builder.append(" {");
        Vector<String> names;
        for (auto& it : m_variables)
            names.append(executable.get_string(it.key));
        builder.join(", ", names);
        builder.append("}");
    }
    return builder.to_string();
}

String Yield::to_string_impl(Bytecode::Executable const&) const
{
    if (m_continuation_label.has_value())
        return String::formatted("Yield continuation:@{}", m_continuation_label->block().name());
    return String::formatted("Yield return");
}

String GetByValue::to_string_impl(const Bytecode::Executable&) const
{
    return String::formatted("GetByValue base:{}", m_base);
}

String PutByValue::to_string_impl(const Bytecode::Executable&) const
{
    return String::formatted("PutByValue base:{}, property:{}", m_base, m_property);
}

String GetIterator::to_string_impl(Executable const&) const
{
    return "GetIterator";
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

}
