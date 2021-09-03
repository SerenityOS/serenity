/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/HashTable.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/OrdinaryFunctionObject.h>
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

static Value abstract_inequals(GlobalObject& global_object, Value src1, Value src2)
{
    return Value(!abstract_eq(global_object, src1, src2));
}

static Value abstract_equals(GlobalObject& global_object, Value src1, Value src2)
{
    return Value(abstract_eq(global_object, src1, src2));
}

static Value typed_inequals(GlobalObject&, Value src1, Value src2)
{
    return Value(!strict_eq(src1, src2));
}

static Value typed_equals(GlobalObject&, Value src1, Value src2)
{
    return Value(strict_eq(src1, src2));
}

#define JS_DEFINE_COMMON_BINARY_OP(OpTitleCase, op_snake_case)                            \
    void OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const              \
    {                                                                                     \
        auto lhs = interpreter.reg(m_lhs_reg);                                            \
        auto rhs = interpreter.accumulator();                                             \
        interpreter.accumulator() = op_snake_case(interpreter.global_object(), lhs, rhs); \
    }                                                                                     \
    String OpTitleCase::to_string_impl(Bytecode::Executable const&) const                 \
    {                                                                                     \
        return String::formatted(#OpTitleCase " {}", m_lhs_reg);                          \
    }

JS_ENUMERATE_COMMON_BINARY_OPS(JS_DEFINE_COMMON_BINARY_OP)

static Value not_(GlobalObject&, Value value)
{
    return Value(!value.to_boolean());
}

static Value typeof_(GlobalObject& global_object, Value value)
{
    return js_string(global_object.vm(), value.typeof());
}

#define JS_DEFINE_COMMON_UNARY_OP(OpTitleCase, op_snake_case)                                              \
    void OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const                               \
    {                                                                                                      \
        interpreter.accumulator() = op_snake_case(interpreter.global_object(), interpreter.accumulator()); \
    }                                                                                                      \
    String OpTitleCase::to_string_impl(Bytecode::Executable const&) const                                  \
    {                                                                                                      \
        return #OpTitleCase;                                                                               \
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

void IteratorToArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& global_object = interpreter.global_object();
    auto& vm = interpreter.vm();
    auto iterator = interpreter.accumulator().to_object(global_object);
    if (vm.exception())
        return;

    auto array = Array::create(global_object, 0);
    size_t index = 0;

    while (true) {
        auto iterator_result = iterator_next(*iterator);
        if (!iterator_result)
            return;

        auto complete = iterator_complete(global_object, *iterator_result);
        if (vm.exception())
            return;

        if (complete) {
            interpreter.accumulator() = array;
            return;
        }

        auto value = iterator_value(global_object, *iterator_result);
        if (vm.exception())
            return;

        array->create_data_property_or_throw(index, value);
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

    interpreter.accumulator() = regexp_create(interpreter.global_object(), js_string(interpreter.vm(), source), js_string(interpreter.vm(), flags));
}

void CopyObjectExcludingProperties::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto* from_object = interpreter.reg(m_from_object).to_object(interpreter.global_object());
    if (interpreter.vm().exception())
        return;

    auto* to_object = Object::create(interpreter.global_object(), interpreter.global_object().object_prototype());

    HashTable<Value, ValueTraits> excluded_names;
    for (size_t i = 0; i < m_excluded_names_count; ++i) {
        excluded_names.set(interpreter.reg(m_excluded_names[i]));
        if (interpreter.vm().exception())
            return;
    }

    auto own_keys = from_object->internal_own_property_keys();
    if (interpreter.vm().exception())
        return;

    for (auto& key : own_keys) {
        if (!excluded_names.contains(key)) {
            auto property_name = PropertyName(key.to_property_key(interpreter.global_object()));
            auto property_value = from_object->get(property_name);
            if (interpreter.vm().exception())
                return;
            to_object->define_direct_property(property_name, property_value, JS::default_attributes);
        }
    }

    interpreter.accumulator() = to_object;
}

void ConcatString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_lhs) = add(interpreter.global_object(), interpreter.reg(m_lhs), interpreter.accumulator());
}

void GetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().get_variable(interpreter.current_executable().get_string(m_identifier), interpreter.global_object());
}

void SetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().set_variable(interpreter.current_executable().get_string(m_identifier), interpreter.accumulator(), interpreter.global_object());
}

void GetById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.accumulator().to_object(interpreter.global_object()))
        interpreter.accumulator() = object->get(interpreter.current_executable().get_string(m_property));
}

void PutById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object()))
        object->set(interpreter.current_executable().get_string(m_property), interpreter.accumulator(), Object::ShouldThrowExceptions::Yes);
}

void Jump::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.jump(*m_true_target);
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
        TODO();
    }
    auto& function = callee.as_function();

    auto this_value = interpreter.reg(m_this_value);

    Value return_value;

    if (m_argument_count == 0 && m_type == CallType::Call) {
        return_value = interpreter.vm().call(function, this_value);
    } else {
        MarkedValueList argument_values { interpreter.vm().heap() };
        for (size_t i = 0; i < m_argument_count; ++i) {
            argument_values.append(interpreter.reg(m_arguments[i]));
        }
        if (m_type == CallType::Call)
            return_value = interpreter.vm().call(function, this_value, move(argument_values));
        else
            return_value = interpreter.vm().construct(function, function, move(argument_values));
    }

    interpreter.accumulator() = return_value;
}

void NewFunction::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = OrdinaryFunctionObject::create(interpreter.global_object(), m_function_node.name(), m_function_node.body(), m_function_node.parameters(), m_function_node.function_length(), vm.lexical_environment(), m_function_node.kind(), m_function_node.is_strict_mode(), m_function_node.is_arrow_function());
}

void Return::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.do_return(interpreter.accumulator().value_or(js_undefined()));
}

void Increment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto old_value = interpreter.accumulator().to_numeric(interpreter.global_object());
    if (interpreter.vm().exception())
        return;

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() + 1);
    else
        interpreter.accumulator() = js_bigint(interpreter.vm().heap(), old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
}

void Decrement::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto old_value = interpreter.accumulator().to_numeric(interpreter.global_object());
    if (interpreter.vm().exception())
        return;

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
    HashMap<FlyString, Variable> resolved_variables;
    for (auto& it : m_variables)
        resolved_variables.set(interpreter.current_executable().get_string(it.key), it.value);
    auto* environment = interpreter.vm().heap().allocate<DeclarativeEnvironment>(interpreter.global_object(), move(resolved_variables), interpreter.vm().lexical_environment());
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
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object())) {
        auto property_key = interpreter.accumulator().to_property_key(interpreter.global_object());
        if (interpreter.vm().exception())
            return;
        interpreter.accumulator() = object->get(property_key);
    }
}

void PutByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object())) {
        auto property_key = interpreter.reg(m_property).to_property_key(interpreter.global_object());
        if (interpreter.vm().exception())
            return;
        object->set(property_key, interpreter.accumulator(), Object::ShouldThrowExceptions::Yes);
    }
}

void GetIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = get_iterator(interpreter.global_object(), interpreter.accumulator());
}

void IteratorNext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.accumulator().to_object(interpreter.global_object()))
        interpreter.accumulator() = iterator_next(*object);
}

void IteratorResultDone::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (auto* iterator_result = interpreter.accumulator().to_object(interpreter.global_object()))
        interpreter.accumulator() = Value(iterator_complete(interpreter.global_object(), *iterator_result));
}

void IteratorResultValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (auto* iterator_result = interpreter.accumulator().to_object(interpreter.global_object()))
        interpreter.accumulator() = iterator_value(interpreter.global_object(), *iterator_result);
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
    return String::formatted("GetVariable {} ({})", m_identifier, executable.string_table->get(m_identifier));
}

String SetVariable::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("SetVariable {} ({})", m_identifier, executable.string_table->get(m_identifier));
}

String PutById::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("PutById base:{}, property:{} ({})", m_base, m_property, executable.string_table->get(m_property));
}

String GetById::to_string_impl(Bytecode::Executable const& executable) const
{
    return String::formatted("GetById {} ({})", m_property, executable.string_table->get(m_property));
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

}
