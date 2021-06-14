/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/ScopeObject.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode {

String Instruction::to_string(Bytecode::Executable const& executable) const
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op const&>(*this).to_string(executable);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

}

namespace JS::Bytecode::Op {

void Load::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.reg(m_src);
}

void LoadImmediate::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = m_value;
}

void Store::execute(Bytecode::Interpreter& interpreter) const
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
    void OpTitleCase::execute(Bytecode::Interpreter& interpreter) const                   \
    {                                                                                     \
        auto lhs = interpreter.reg(m_lhs_reg);                                            \
        auto rhs = interpreter.accumulator();                                             \
        interpreter.accumulator() = op_snake_case(interpreter.global_object(), lhs, rhs); \
    }                                                                                     \
    String OpTitleCase::to_string(Bytecode::Executable const&) const                      \
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
    void OpTitleCase::execute(Bytecode::Interpreter& interpreter) const                                    \
    {                                                                                                      \
        interpreter.accumulator() = op_snake_case(interpreter.global_object(), interpreter.accumulator()); \
    }                                                                                                      \
    String OpTitleCase::to_string(Bytecode::Executable const&) const                                       \
    {                                                                                                      \
        return #OpTitleCase;                                                                               \
    }

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DEFINE_COMMON_UNARY_OP)

void NewBigInt::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = js_bigint(interpreter.vm().heap(), m_bigint);
}

void NewArray::execute(Bytecode::Interpreter& interpreter) const
{
    Vector<Value> elements;
    elements.ensure_capacity(m_element_count);
    for (size_t i = 0; i < m_element_count; i++)
        elements.append(interpreter.reg(m_elements[i]));
    interpreter.accumulator() = Array::create_from(interpreter.global_object(), elements);
}

void NewString::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = js_string(interpreter.vm(), interpreter.current_executable().get_string(m_string));
}

void NewObject::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = Object::create_empty(interpreter.global_object());
}

void ConcatString::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_lhs) = add(interpreter.global_object(), interpreter.reg(m_lhs), interpreter.accumulator());
}

void GetVariable::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().get_variable(interpreter.current_executable().get_string(m_identifier), interpreter.global_object());
}

void SetVariable::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().set_variable(interpreter.current_executable().get_string(m_identifier), interpreter.accumulator(), interpreter.global_object());
}

void GetById::execute(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.accumulator().to_object(interpreter.global_object()))
        interpreter.accumulator() = object->get(interpreter.current_executable().get_string(m_property));
}

void PutById::execute(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object()))
        object->put(interpreter.current_executable().get_string(m_property), interpreter.accumulator());
}

void Jump::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.jump(*m_true_target);
}

void JumpConditional::execute(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_true_target.has_value());
    VERIFY(m_false_target.has_value());
    auto result = interpreter.accumulator();
    if (result.to_boolean())
        interpreter.jump(m_true_target.value());
    else
        interpreter.jump(m_false_target.value());
}

void JumpNullish::execute(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_true_target.has_value());
    VERIFY(m_false_target.has_value());
    auto result = interpreter.accumulator();
    if (result.is_nullish())
        interpreter.jump(m_true_target.value());
    else
        interpreter.jump(m_false_target.value());
}

void Call::execute(Bytecode::Interpreter& interpreter) const
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

void NewFunction::execute(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = ScriptFunction::create(interpreter.global_object(), m_function_node.name(), m_function_node.body(), m_function_node.parameters(), m_function_node.function_length(), vm.current_scope(), m_function_node.kind(), m_function_node.is_strict_mode(), m_function_node.is_arrow_function());
}

void Return::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.do_return(interpreter.accumulator().value_or(js_undefined()));
}

void Increment::execute(Bytecode::Interpreter& interpreter) const
{
    auto old_value = interpreter.accumulator().to_numeric(interpreter.global_object());
    if (interpreter.vm().exception())
        return;

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() + 1);
    else
        interpreter.accumulator() = js_bigint(interpreter.vm().heap(), old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
}

void Decrement::execute(Bytecode::Interpreter& interpreter) const
{
    auto old_value = interpreter.accumulator().to_numeric(interpreter.global_object());
    if (interpreter.vm().exception())
        return;

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() - 1);
    else
        interpreter.accumulator() = js_bigint(interpreter.vm().heap(), old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
}

void Throw::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().throw_exception(interpreter.global_object(), interpreter.accumulator());
}

void EnterUnwindContext::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.enter_unwind_context(m_handler_target, m_finalizer_target);
}

void LeaveUnwindContext::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.leave_unwind_context();
}

void ContinuePendingUnwind::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.continue_pending_unwind(m_resume_target);
}

void PushLexicalEnvironment::execute(Bytecode::Interpreter& interpreter) const
{
    HashMap<FlyString, Variable> resolved_variables;
    for (auto& it : m_variables)
        resolved_variables.set(interpreter.current_executable().get_string(it.key), it.value);
    auto* block_lexical_environment = interpreter.vm().heap().allocate<LexicalEnvironment>(interpreter.global_object(), move(resolved_variables), interpreter.vm().current_scope());
    interpreter.vm().call_frame().scope = block_lexical_environment;
}

void Yield::execute(Bytecode::Interpreter& interpreter) const
{
    auto yielded_value = interpreter.accumulator().value_or(js_undefined());
    auto object = JS::Object::create_empty(interpreter.global_object());
    object->put("result", yielded_value);
    if (m_continuation_label.has_value())
        object->put("continuation", Value(static_cast<double>(reinterpret_cast<u64>(&m_continuation_label->block()))));
    else
        object->put("continuation", Value(0));
    interpreter.do_return(object);
}

void GetByValue::execute(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object())) {
        auto property_key = interpreter.accumulator().to_property_key(interpreter.global_object());
        if (interpreter.vm().exception())
            return;
        interpreter.accumulator() = object->get(property_key);
    }
}

void PutByValue::execute(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object())) {
        auto property_key = interpreter.reg(m_property).to_property_key(interpreter.global_object());
        if (interpreter.vm().exception())
            return;
        object->put(property_key, interpreter.accumulator());
    }
}

void LoadArgument::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().argument(m_index);
}

String Load::to_string(Bytecode::Executable const&) const
{
    return String::formatted("Load {}", m_src);
}

String LoadImmediate::to_string(Bytecode::Executable const&) const
{
    return String::formatted("LoadImmediate {}", m_value);
}

String Store::to_string(Bytecode::Executable const&) const
{
    return String::formatted("Store {}", m_dst);
}

String NewBigInt::to_string(Bytecode::Executable const&) const
{
    return String::formatted("NewBigInt \"{}\"", m_bigint.to_base10());
}

String NewArray::to_string(Bytecode::Executable const&) const
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

String NewString::to_string(Bytecode::Executable const& executable) const
{
    return String::formatted("NewString {} (\"{}\")", m_string, executable.string_table->get(m_string));
}

String NewObject::to_string(Bytecode::Executable const&) const
{
    return "NewObject";
}

String ConcatString::to_string(Bytecode::Executable const&) const
{
    return String::formatted("ConcatString {}", m_lhs);
}

String GetVariable::to_string(Bytecode::Executable const& executable) const
{
    return String::formatted("GetVariable {} ({})", m_identifier, executable.string_table->get(m_identifier));
}

String SetVariable::to_string(Bytecode::Executable const& executable) const
{
    return String::formatted("SetVariable {} ({})", m_identifier, executable.string_table->get(m_identifier));
}

String PutById::to_string(Bytecode::Executable const& executable) const
{
    return String::formatted("PutById base:{}, property:{} ({})", m_base, m_property, executable.string_table->get(m_property));
}

String GetById::to_string(Bytecode::Executable const& executable) const
{
    return String::formatted("GetById {} ({})", m_property, executable.string_table->get(m_property));
}

String Jump::to_string(Bytecode::Executable const&) const
{
    if (m_true_target.has_value())
        return String::formatted("Jump {}", *m_true_target);
    return String::formatted("Jump <empty>");
}

String JumpConditional::to_string(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? String::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? String::formatted("{}", *m_false_target) : "<empty>";
    return String::formatted("JumpConditional true:{} false:{}", true_string, false_string);
}

String JumpNullish::to_string(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? String::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? String::formatted("{}", *m_false_target) : "<empty>";
    return String::formatted("JumpNullish null:{} nonnull:{}", true_string, false_string);
}

String Call::to_string(Bytecode::Executable const&) const
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

String NewFunction::to_string(Bytecode::Executable const&) const
{
    return "NewFunction";
}

String Return::to_string(Bytecode::Executable const&) const
{
    return "Return";
}

String Increment::to_string(Bytecode::Executable const&) const
{
    return "Increment";
}

String Decrement::to_string(Bytecode::Executable const&) const
{
    return "Decrement";
}

String Throw::to_string(Bytecode::Executable const&) const
{
    return "Throw";
}

String EnterUnwindContext::to_string(Bytecode::Executable const&) const
{
    auto handler_string = m_handler_target.has_value() ? String::formatted("{}", *m_handler_target) : "<empty>";
    auto finalizer_string = m_finalizer_target.has_value() ? String::formatted("{}", *m_finalizer_target) : "<empty>";
    return String::formatted("EnterUnwindContext handler:{} finalizer:{}", handler_string, finalizer_string);
}

String LeaveUnwindContext::to_string(Bytecode::Executable const&) const
{
    return "LeaveUnwindContext";
}

String ContinuePendingUnwind::to_string(Bytecode::Executable const&) const
{
    return String::formatted("ContinuePendingUnwind resume:{}", m_resume_target);
}

String PushLexicalEnvironment::to_string(const Bytecode::Executable& executable) const
{
    StringBuilder builder;
    builder.append("PushLexicalEnvironment");
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

String Yield::to_string(Bytecode::Executable const&) const
{
    if (m_continuation_label.has_value())
        return String::formatted("Yield continuation:@{}", m_continuation_label->block().name());
    return String::formatted("Yield return");
}

String GetByValue::to_string(const Bytecode::Executable&) const
{
    return String::formatted("GetByValue base:{}", m_base);
}

String PutByValue::to_string(const Bytecode::Executable&) const
{
    return String::formatted("PutByValue base:{}, property:{}", m_base, m_property);
}

String LoadArgument::to_string(const Bytecode::Executable&) const
{
    return String::formatted("LoadArgument {}", m_index);
}

}
