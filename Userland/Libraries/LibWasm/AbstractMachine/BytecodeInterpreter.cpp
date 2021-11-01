/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/AbstractMachine/BytecodeInterpreter.h>
#include <LibWasm/AbstractMachine/Configuration.h>
#include <LibWasm/AbstractMachine/Operators.h>
#include <LibWasm/Opcode.h>
#include <LibWasm/Printer/Printer.h>

namespace Wasm {

#define TRAP_IF_NOT(x)                                                                         \
    do {                                                                                       \
        if (trap_if_not(x, #x##sv)) {                                                          \
            dbgln_if(WASM_TRACE_DEBUG, "Trapped because {} failed, at line {}", #x, __LINE__); \
            return;                                                                            \
        }                                                                                      \
    } while (false)

#define TRAP_IF_NOT_NORETURN(x)                                                                \
    do {                                                                                       \
        if (trap_if_not(x, #x##sv)) {                                                          \
            dbgln_if(WASM_TRACE_DEBUG, "Trapped because {} failed, at line {}", #x, __LINE__); \
        }                                                                                      \
    } while (false)

void BytecodeInterpreter::interpret(Configuration& configuration)
{
    m_trap.clear();
    auto& instructions = configuration.frame().expression().instructions();
    auto max_ip_value = InstructionPointer { instructions.size() };
    auto& current_ip_value = configuration.ip();
    auto const should_limit_instruction_count = configuration.should_limit_instruction_count();
    u64 executed_instructions = 0;

    while (current_ip_value < max_ip_value) {
        if (should_limit_instruction_count) {
            if (executed_instructions++ >= Constants::max_allowed_executed_instructions_per_call) [[unlikely]] {
                m_trap = Trap { "Exceeded maximum allowed number of instructions" };
                return;
            }
        }
        auto& instruction = instructions[current_ip_value.value()];
        auto old_ip = current_ip_value;
        interpret(configuration, current_ip_value, instruction);
        if (m_trap.has_value())
            return;
        if (current_ip_value == old_ip) // If no jump occurred
            ++current_ip_value;
    }
}

void BytecodeInterpreter::branch_to_label(Configuration& configuration, LabelIndex index)
{
    dbgln_if(WASM_TRACE_DEBUG, "Branch to label with index {}...", index.value());
    auto label = configuration.nth_label(index.value());
    dbgln_if(WASM_TRACE_DEBUG, "...which is actually IP {}, and has {} result(s)", label->continuation().value(), label->arity());
    auto results = pop_values(configuration, label->arity());

    size_t drop_count = index.value() + 1;
    for (; !configuration.stack().is_empty();) {
        auto& entry = configuration.stack().peek();
        if (entry.has<Label>()) {
            if (--drop_count == 0)
                break;
        }
        configuration.stack().pop();
    }

    for (auto& result : results)
        configuration.stack().push(move(result));

    configuration.ip() = label->continuation();
}

template<typename ReadType, typename PushType>
void BytecodeInterpreter::load_and_push(Configuration& configuration, Instruction const& instruction)
{
    auto& address = configuration.frame().module().memories().first();
    auto memory = configuration.store().get(address);
    if (!memory) {
        m_trap = Trap { "Nonexistent memory" };
        return;
    }
    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    auto& entry = configuration.stack().peek();
    auto base = entry.get<Value>().to<i32>();
    if (!base.has_value()) {
        m_trap = Trap { "Memory access out of bounds" };
        return;
    }
    u64 instance_address = static_cast<u64>(bit_cast<u32>(base.value())) + arg.offset;
    Checked addition { instance_address };
    addition += sizeof(ReadType);
    if (addition.has_overflow() || addition.value() > memory->size()) {
        m_trap = Trap { "Memory access out of bounds" };
        dbgln("LibWasm: Memory access out of bounds (expected {} to be less than or equal to {})", instance_address + sizeof(ReadType), memory->size());
        return;
    }
    dbgln_if(WASM_TRACE_DEBUG, "load({} : {}) -> stack", instance_address, sizeof(ReadType));
    auto slice = memory->data().bytes().slice(instance_address, sizeof(ReadType));
    configuration.stack().peek() = Value(static_cast<PushType>(read_value<ReadType>(slice)));
}

void BytecodeInterpreter::call_address(Configuration& configuration, FunctionAddress address)
{
    TRAP_IF_NOT(m_stack_info.size_free() >= Constants::minimum_stack_space_to_keep_free);

    auto instance = configuration.store().get(address);
    FunctionType const* type { nullptr };
    instance->visit([&](auto const& function) { type = &function.type(); });
    TRAP_IF_NOT(configuration.stack().entries().size() > type->parameters().size());
    Vector<Value> args;
    args.ensure_capacity(type->parameters().size());
    auto span = configuration.stack().entries().span().slice_from_end(type->parameters().size());
    for (auto& entry : span) {
        auto* call_argument = entry.get_pointer<Value>();
        TRAP_IF_NOT(call_argument);
        args.unchecked_append(move(*call_argument));
    }

    configuration.stack().entries().remove(configuration.stack().size() - span.size(), span.size());

    Result result { Trap { ""sv } };
    {
        CallFrameHandle handle { *this, configuration };
        result = configuration.call(*this, address, move(args));
    }

    if (result.is_trap()) {
        m_trap = move(result.trap());
        return;
    }

    configuration.stack().entries().ensure_capacity(configuration.stack().size() + result.values().size());
    for (auto& entry : result.values())
        configuration.stack().entries().unchecked_append(move(entry));
}

template<typename PopType, typename PushType, typename Operator>
void BytecodeInterpreter::binary_numeric_operation(Configuration& configuration)
{
    auto rhs_entry = configuration.stack().pop();
    auto& lhs_entry = configuration.stack().peek();
    auto rhs_ptr = rhs_entry.get_pointer<Value>();
    auto lhs_ptr = lhs_entry.get_pointer<Value>();
    auto rhs = rhs_ptr->to<PopType>();
    auto lhs = lhs_ptr->to<PopType>();
    PushType result;
    auto call_result = Operator {}(lhs.value(), rhs.value());
    if constexpr (IsSpecializationOf<decltype(call_result), AK::Result>) {
        if (call_result.is_error()) {
            trap_if_not(false, call_result.error());
            return;
        }
        result = call_result.release_value();
    } else {
        result = call_result;
    }
    dbgln_if(WASM_TRACE_DEBUG, "{} {} {} = {}", lhs.value(), Operator::name(), rhs.value(), result);
    lhs_entry = Value(result);
}

template<typename PopType, typename PushType, typename Operator>
void BytecodeInterpreter::unary_operation(Configuration& configuration)
{
    auto& entry = configuration.stack().peek();
    auto entry_ptr = entry.get_pointer<Value>();
    auto value = entry_ptr->to<PopType>();
    auto call_result = Operator {}(*value);
    PushType result;
    if constexpr (IsSpecializationOf<decltype(call_result), AK::Result>) {
        if (call_result.is_error()) {
            trap_if_not(false, call_result.error());
            return;
        }
        result = call_result.release_value();
    } else {
        result = call_result;
    }
    dbgln_if(WASM_TRACE_DEBUG, "map({}) {} = {}", Operator::name(), *value, result);
    entry = Value(result);
}

template<typename T>
struct ConvertToRaw {
    T operator()(T value)
    {
        return LittleEndian<T>(value);
    }
};

template<>
struct ConvertToRaw<float> {
    u32 operator()(float value)
    {
        LittleEndian<u32> res;
        ReadonlyBytes bytes { &value, sizeof(float) };
        InputMemoryStream stream { bytes };
        stream >> res;
        VERIFY(!stream.has_any_error());
        return static_cast<u32>(res);
    }
};

template<>
struct ConvertToRaw<double> {
    u64 operator()(double value)
    {
        LittleEndian<u64> res;
        ReadonlyBytes bytes { &value, sizeof(double) };
        InputMemoryStream stream { bytes };
        stream >> res;
        VERIFY(!stream.has_any_error());
        return static_cast<u64>(res);
    }
};

template<typename PopT, typename StoreT>
void BytecodeInterpreter::pop_and_store(Configuration& configuration, Instruction const& instruction)
{
    auto entry = configuration.stack().pop();
    auto value = ConvertToRaw<StoreT> {}(*entry.get<Value>().to<PopT>());
    dbgln_if(WASM_TRACE_DEBUG, "stack({}) -> temporary({}b)", value, sizeof(StoreT));
    store_to_memory(configuration, instruction, { &value, sizeof(StoreT) });
}

void BytecodeInterpreter::store_to_memory(Configuration& configuration, Instruction const& instruction, ReadonlyBytes data)
{
    auto& address = configuration.frame().module().memories().first();
    auto memory = configuration.store().get(address);
    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    auto entry = configuration.stack().pop();
    auto base = entry.get<Value>().to<i32>();
    u64 instance_address = static_cast<u64>(bit_cast<u32>(base.value())) + arg.offset;
    Checked addition { instance_address };
    addition += data.size();
    if (addition.has_overflow() || addition.value() > memory->size()) {
        m_trap = Trap { "Memory access out of bounds" };
        dbgln("LibWasm: Memory access out of bounds (expected 0 <= {} and {} <= {})", instance_address, instance_address + data.size(), memory->size());
        return;
    }
    dbgln_if(WASM_TRACE_DEBUG, "tempoaray({}b) -> store({})", data.size(), instance_address);
    data.copy_to(memory->data().bytes().slice(instance_address, data.size()));
}

template<typename T>
T BytecodeInterpreter::read_value(ReadonlyBytes data)
{
    LittleEndian<T> value;
    InputMemoryStream stream { data };
    stream >> value;
    if (stream.handle_any_error()) {
        dbgln("Read from {} failed", data.data());
        m_trap = Trap { "Read from memory failed" };
    }
    return value;
}

template<>
float BytecodeInterpreter::read_value<float>(ReadonlyBytes data)
{
    InputMemoryStream stream { data };
    LittleEndian<u32> raw_value;
    stream >> raw_value;
    if (stream.handle_any_error())
        m_trap = Trap { "Read from memory failed" };
    return bit_cast<float>(static_cast<u32>(raw_value));
}

template<>
double BytecodeInterpreter::read_value<double>(ReadonlyBytes data)
{
    InputMemoryStream stream { data };
    LittleEndian<u64> raw_value;
    stream >> raw_value;
    if (stream.handle_any_error())
        m_trap = Trap { "Read from memory failed" };
    return bit_cast<double>(static_cast<u64>(raw_value));
}

template<typename V, typename T>
MakeSigned<T> BytecodeInterpreter::checked_signed_truncate(V value)
{
    if (isnan(value) || isinf(value)) { // "undefined", let's just trap.
        m_trap = Trap { "Signed truncation undefined behavior" };
        return 0;
    }

    double truncated;
    if constexpr (IsSame<float, V>)
        truncated = truncf(value);
    else
        truncated = trunc(value);

    using SignedT = MakeSigned<T>;
    if (NumericLimits<SignedT>::min() <= truncated && static_cast<double>(NumericLimits<SignedT>::max()) >= truncated)
        return static_cast<SignedT>(truncated);

    dbgln_if(WASM_TRACE_DEBUG, "Truncate out of range error");
    m_trap = Trap { "Signed truncation out of range" };
    return true;
}

template<typename V, typename T>
MakeUnsigned<T> BytecodeInterpreter::checked_unsigned_truncate(V value)
{
    if (isnan(value) || isinf(value)) { // "undefined", let's just trap.
        m_trap = Trap { "Unsigned truncation undefined behavior" };
        return 0;
    }
    double truncated;
    if constexpr (IsSame<float, V>)
        truncated = truncf(value);
    else
        truncated = trunc(value);

    using UnsignedT = MakeUnsigned<T>;
    if (NumericLimits<UnsignedT>::min() <= truncated && static_cast<double>(NumericLimits<UnsignedT>::max()) >= truncated)
        return static_cast<UnsignedT>(truncated);

    dbgln_if(WASM_TRACE_DEBUG, "Truncate out of range error");
    m_trap = Trap { "Unsigned truncation out of range" };
    return true;
}

Vector<Value> BytecodeInterpreter::pop_values(Configuration& configuration, size_t count)
{
    Vector<Value> results;
    results.resize(count);

    for (size_t i = 0; i < count; ++i) {
        auto top_of_stack = configuration.stack().pop();
        if (auto value = top_of_stack.get_pointer<Value>())
            results[i] = move(*value);
        else
            TRAP_IF_NOT_NORETURN(value);
    }
    return results;
}

void BytecodeInterpreter::interpret(Configuration& configuration, InstructionPointer& ip, Instruction const& instruction)
{
    dbgln_if(WASM_TRACE_DEBUG, "Executing instruction {} at ip {}", instruction_name(instruction.opcode()), ip.value());

    switch (instruction.opcode().value()) {
    case Instructions::unreachable.value():
        m_trap = Trap { "Unreachable" };
        return;
    case Instructions::nop.value():
        return;
    case Instructions::local_get.value():
        configuration.stack().push(Value(configuration.frame().locals()[instruction.arguments().get<LocalIndex>().value()]));
        return;
    case Instructions::local_set.value(): {
        auto entry = configuration.stack().pop();
        configuration.frame().locals()[instruction.arguments().get<LocalIndex>().value()] = move(entry.get<Value>());
        return;
    }
    case Instructions::i32_const.value():
        configuration.stack().push(Value(ValueType { ValueType::I32 }, static_cast<i64>(instruction.arguments().get<i32>())));
        return;
    case Instructions::i64_const.value():
        configuration.stack().push(Value(ValueType { ValueType::I64 }, instruction.arguments().get<i64>()));
        return;
    case Instructions::f32_const.value():
        configuration.stack().push(Value(ValueType { ValueType::F32 }, static_cast<double>(instruction.arguments().get<float>())));
        return;
    case Instructions::f64_const.value():
        configuration.stack().push(Value(ValueType { ValueType::F64 }, instruction.arguments().get<double>()));
        return;
    case Instructions::block.value(): {
        size_t arity = 0;
        auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
        if (args.block_type.kind() != BlockType::Empty)
            arity = 1;
        configuration.stack().push(Label(arity, args.end_ip));
        return;
    }
    case Instructions::loop.value(): {
        size_t arity = 0;
        auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
        if (args.block_type.kind() != BlockType::Empty)
            arity = 1;
        configuration.stack().push(Label(arity, ip.value() + 1));
        return;
    }
    case Instructions::if_.value(): {
        size_t arity = 0;
        auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
        if (args.block_type.kind() != BlockType::Empty)
            arity = 1;

        auto entry = configuration.stack().pop();
        auto value = entry.get<Value>().to<i32>();
        auto end_label = Label(arity, args.end_ip.value());
        if (value.value() == 0) {
            if (args.else_ip.has_value()) {
                configuration.ip() = args.else_ip.value();
                configuration.stack().push(move(end_label));
            } else {
                configuration.ip() = args.end_ip.value() + 1;
            }
        } else {
            configuration.stack().push(move(end_label));
        }
        return;
    }
    case Instructions::structured_end.value():
    case Instructions::structured_else.value(): {
        auto index = configuration.nth_label_index(0);
        auto label = configuration.stack().entries()[*index].get<Label>();
        configuration.stack().entries().remove(*index, 1);

        if (instruction.opcode() == Instructions::structured_end)
            return;

        // Jump to the end label
        configuration.ip() = label.continuation();
        return;
    }
    case Instructions::return_.value(): {
        auto& frame = configuration.frame();
        size_t end = configuration.stack().size() - frame.arity();
        size_t start = end;
        for (; start + 1 > 0 && start < configuration.stack().size(); --start) {
            auto& entry = configuration.stack().entries()[start];
            if (entry.has<Frame>()) {
                // Leave the frame, _and_ its label.
                start += 2;
                break;
            }
        }

        configuration.stack().entries().remove(start, end - start);

        // Jump past the call/indirect instruction
        configuration.ip() = configuration.frame().expression().instructions().size();
        return;
    }
    case Instructions::br.value():
        return branch_to_label(configuration, instruction.arguments().get<LabelIndex>());
    case Instructions::br_if.value(): {
        auto entry = configuration.stack().pop();
        if (entry.get<Value>().to<i32>().value_or(0) == 0)
            return;
        return branch_to_label(configuration, instruction.arguments().get<LabelIndex>());
    }
    case Instructions::br_table.value(): {
        auto& arguments = instruction.arguments().get<Instruction::TableBranchArgs>();
        auto entry = configuration.stack().pop();
        auto maybe_i = entry.get<Value>().to<i32>();
        if (0 <= *maybe_i) {
            size_t i = *maybe_i;
            if (i < arguments.labels.size())
                return branch_to_label(configuration, arguments.labels[i]);
        }
        return branch_to_label(configuration, arguments.default_);
    }
    case Instructions::call.value(): {
        auto index = instruction.arguments().get<FunctionIndex>();
        auto address = configuration.frame().module().functions()[index.value()];
        dbgln_if(WASM_TRACE_DEBUG, "call({})", address.value());
        call_address(configuration, address);
        return;
    }
    case Instructions::call_indirect.value(): {
        auto& args = instruction.arguments().get<Instruction::IndirectCallArgs>();
        auto table_address = configuration.frame().module().tables()[args.table.value()];
        auto table_instance = configuration.store().get(table_address);
        auto entry = configuration.stack().pop();
        auto index = entry.get<Value>().to<i32>();
        TRAP_IF_NOT(index.value() >= 0);
        TRAP_IF_NOT(static_cast<size_t>(index.value()) < table_instance->elements().size());
        auto element = table_instance->elements()[index.value()];
        TRAP_IF_NOT(element.has_value());
        TRAP_IF_NOT(element->ref().has<Reference::Func>());
        auto address = element->ref().get<Reference::Func>().address;
        dbgln_if(WASM_TRACE_DEBUG, "call_indirect({} -> {})", index.value(), address.value());
        call_address(configuration, address);
        return;
    }
    case Instructions::i32_load.value():
        return load_and_push<i32, i32>(configuration, instruction);
    case Instructions::i64_load.value():
        return load_and_push<i64, i64>(configuration, instruction);
    case Instructions::f32_load.value():
        return load_and_push<float, float>(configuration, instruction);
    case Instructions::f64_load.value():
        return load_and_push<double, double>(configuration, instruction);
    case Instructions::i32_load8_s.value():
        return load_and_push<i8, i32>(configuration, instruction);
    case Instructions::i32_load8_u.value():
        return load_and_push<u8, i32>(configuration, instruction);
    case Instructions::i32_load16_s.value():
        return load_and_push<i16, i32>(configuration, instruction);
    case Instructions::i32_load16_u.value():
        return load_and_push<u16, i32>(configuration, instruction);
    case Instructions::i64_load8_s.value():
        return load_and_push<i8, i64>(configuration, instruction);
    case Instructions::i64_load8_u.value():
        return load_and_push<u8, i64>(configuration, instruction);
    case Instructions::i64_load16_s.value():
        return load_and_push<i16, i64>(configuration, instruction);
    case Instructions::i64_load16_u.value():
        return load_and_push<u16, i64>(configuration, instruction);
    case Instructions::i64_load32_s.value():
        return load_and_push<i32, i64>(configuration, instruction);
    case Instructions::i64_load32_u.value():
        return load_and_push<u32, i64>(configuration, instruction);
    case Instructions::i32_store.value():
        return pop_and_store<i32, i32>(configuration, instruction);
    case Instructions::i64_store.value():
        return pop_and_store<i64, i64>(configuration, instruction);
    case Instructions::f32_store.value():
        return pop_and_store<float, float>(configuration, instruction);
    case Instructions::f64_store.value():
        return pop_and_store<double, double>(configuration, instruction);
    case Instructions::i32_store8.value():
        return pop_and_store<i32, i8>(configuration, instruction);
    case Instructions::i32_store16.value():
        return pop_and_store<i32, i16>(configuration, instruction);
    case Instructions::i64_store8.value():
        return pop_and_store<i64, i8>(configuration, instruction);
    case Instructions::i64_store16.value():
        return pop_and_store<i64, i16>(configuration, instruction);
    case Instructions::i64_store32.value():
        return pop_and_store<i64, i32>(configuration, instruction);
    case Instructions::local_tee.value(): {
        auto& entry = configuration.stack().peek();
        auto value = entry.get<Value>();
        auto local_index = instruction.arguments().get<LocalIndex>();
        dbgln_if(WASM_TRACE_DEBUG, "stack:peek -> locals({})", local_index.value());
        configuration.frame().locals()[local_index.value()] = move(value);
        return;
    }
    case Instructions::global_get.value(): {
        auto global_index = instruction.arguments().get<GlobalIndex>();
        auto address = configuration.frame().module().globals()[global_index.value()];
        dbgln_if(WASM_TRACE_DEBUG, "global({}) -> stack", address.value());
        auto global = configuration.store().get(address);
        configuration.stack().push(Value(global->value()));
        return;
    }
    case Instructions::global_set.value(): {
        auto global_index = instruction.arguments().get<GlobalIndex>();
        auto address = configuration.frame().module().globals()[global_index.value()];
        auto entry = configuration.stack().pop();
        auto value = entry.get<Value>();
        dbgln_if(WASM_TRACE_DEBUG, "stack -> global({})", address.value());
        auto global = configuration.store().get(address);
        global->set_value(move(value));
        return;
    }
    case Instructions::memory_size.value(): {
        auto address = configuration.frame().module().memories()[0];
        auto instance = configuration.store().get(address);
        auto pages = instance->size() / Constants::page_size;
        dbgln_if(WASM_TRACE_DEBUG, "memory.size -> stack({})", pages);
        configuration.stack().push(Value((i32)pages));
        return;
    }
    case Instructions::memory_grow.value(): {
        auto address = configuration.frame().module().memories()[0];
        auto instance = configuration.store().get(address);
        i32 old_pages = instance->size() / Constants::page_size;
        auto& entry = configuration.stack().peek();
        auto new_pages = entry.get<Value>().to<i32>();
        dbgln_if(WASM_TRACE_DEBUG, "memory.grow({}), previously {} pages...", *new_pages, old_pages);
        if (instance->grow(new_pages.value() * Constants::page_size))
            configuration.stack().peek() = Value((i32)old_pages);
        else
            configuration.stack().peek() = Value((i32)-1);
        return;
    }
    case Instructions::table_get.value():
    case Instructions::table_set.value():
        goto unimplemented;
    case Instructions::ref_null.value(): {
        auto type = instruction.arguments().get<ValueType>();
        configuration.stack().push(Value(Reference(Reference::Null { type })));
        return;
    };
    case Instructions::ref_func.value(): {
        auto index = instruction.arguments().get<FunctionIndex>().value();
        auto& functions = configuration.frame().module().functions();
        auto address = functions[index];
        configuration.stack().push(Value(ValueType(ValueType::FunctionReference), address.value()));
        return;
    }
    case Instructions::ref_is_null.value(): {
        auto top = configuration.stack().peek().get_pointer<Value>();
        TRAP_IF_NOT(top->type().is_reference());
        auto is_null = top->to<Reference::Null>().has_value();
        configuration.stack().peek() = Value(ValueType(ValueType::I32), static_cast<u64>(is_null ? 1 : 0));
        return;
    }
    case Instructions::drop.value():
        configuration.stack().pop();
        return;
    case Instructions::select.value():
    case Instructions::select_typed.value(): {
        // Note: The type seems to only be used for validation.
        auto entry = configuration.stack().pop();
        auto value = entry.get<Value>().to<i32>();
        dbgln_if(WASM_TRACE_DEBUG, "select({})", value.value());
        auto rhs_entry = configuration.stack().pop();
        auto& lhs_entry = configuration.stack().peek();
        auto rhs = move(rhs_entry.get<Value>());
        auto lhs = move(lhs_entry.get<Value>());
        configuration.stack().peek() = value.value() != 0 ? move(lhs) : move(rhs);
        return;
    }
    case Instructions::i32_eqz.value():
        return unary_operation<i32, i32, Operators::EqualsZero>(configuration);
    case Instructions::i32_eq.value():
        return binary_numeric_operation<i32, i32, Operators::Equals>(configuration);
    case Instructions::i32_ne.value():
        return binary_numeric_operation<i32, i32, Operators::NotEquals>(configuration);
    case Instructions::i32_lts.value():
        return binary_numeric_operation<i32, i32, Operators::LessThan>(configuration);
    case Instructions::i32_ltu.value():
        return binary_numeric_operation<u32, i32, Operators::LessThan>(configuration);
    case Instructions::i32_gts.value():
        return binary_numeric_operation<i32, i32, Operators::GreaterThan>(configuration);
    case Instructions::i32_gtu.value():
        return binary_numeric_operation<u32, i32, Operators::GreaterThan>(configuration);
    case Instructions::i32_les.value():
        return binary_numeric_operation<i32, i32, Operators::LessThanOrEquals>(configuration);
    case Instructions::i32_leu.value():
        return binary_numeric_operation<u32, i32, Operators::LessThanOrEquals>(configuration);
    case Instructions::i32_ges.value():
        return binary_numeric_operation<i32, i32, Operators::GreaterThanOrEquals>(configuration);
    case Instructions::i32_geu.value():
        return binary_numeric_operation<u32, i32, Operators::GreaterThanOrEquals>(configuration);
    case Instructions::i64_eqz.value():
        return unary_operation<i64, i32, Operators::EqualsZero>(configuration);
    case Instructions::i64_eq.value():
        return binary_numeric_operation<i64, i32, Operators::Equals>(configuration);
    case Instructions::i64_ne.value():
        return binary_numeric_operation<i64, i32, Operators::NotEquals>(configuration);
    case Instructions::i64_lts.value():
        return binary_numeric_operation<i64, i32, Operators::LessThan>(configuration);
    case Instructions::i64_ltu.value():
        return binary_numeric_operation<u64, i32, Operators::LessThan>(configuration);
    case Instructions::i64_gts.value():
        return binary_numeric_operation<i64, i32, Operators::GreaterThan>(configuration);
    case Instructions::i64_gtu.value():
        return binary_numeric_operation<u64, i32, Operators::GreaterThan>(configuration);
    case Instructions::i64_les.value():
        return binary_numeric_operation<i64, i32, Operators::LessThanOrEquals>(configuration);
    case Instructions::i64_leu.value():
        return binary_numeric_operation<u64, i32, Operators::LessThanOrEquals>(configuration);
    case Instructions::i64_ges.value():
        return binary_numeric_operation<i64, i32, Operators::GreaterThanOrEquals>(configuration);
    case Instructions::i64_geu.value():
        return binary_numeric_operation<u64, i32, Operators::GreaterThanOrEquals>(configuration);
    case Instructions::f32_eq.value():
        return binary_numeric_operation<float, i32, Operators::Equals>(configuration);
    case Instructions::f32_ne.value():
        return binary_numeric_operation<float, i32, Operators::NotEquals>(configuration);
    case Instructions::f32_lt.value():
        return binary_numeric_operation<float, i32, Operators::LessThan>(configuration);
    case Instructions::f32_gt.value():
        return binary_numeric_operation<float, i32, Operators::GreaterThan>(configuration);
    case Instructions::f32_le.value():
        return binary_numeric_operation<float, i32, Operators::LessThanOrEquals>(configuration);
    case Instructions::f32_ge.value():
        return binary_numeric_operation<float, i32, Operators::GreaterThanOrEquals>(configuration);
    case Instructions::f64_eq.value():
        return binary_numeric_operation<double, i32, Operators::Equals>(configuration);
    case Instructions::f64_ne.value():
        return binary_numeric_operation<double, i32, Operators::NotEquals>(configuration);
    case Instructions::f64_lt.value():
        return binary_numeric_operation<double, i32, Operators::LessThan>(configuration);
    case Instructions::f64_gt.value():
        return binary_numeric_operation<double, i32, Operators::GreaterThan>(configuration);
    case Instructions::f64_le.value():
        return binary_numeric_operation<double, i32, Operators::LessThanOrEquals>(configuration);
    case Instructions::f64_ge.value():
        return binary_numeric_operation<double, i32, Operators::GreaterThanOrEquals>(configuration);
    case Instructions::i32_clz.value():
        return unary_operation<i32, i32, Operators::CountLeadingZeros>(configuration);
    case Instructions::i32_ctz.value():
        return unary_operation<i32, i32, Operators::CountTrailingZeros>(configuration);
    case Instructions::i32_popcnt.value():
        return unary_operation<i32, i32, Operators::PopCount>(configuration);
    case Instructions::i32_add.value():
        return binary_numeric_operation<u32, i32, Operators::Add>(configuration);
    case Instructions::i32_sub.value():
        return binary_numeric_operation<u32, i32, Operators::Subtract>(configuration);
    case Instructions::i32_mul.value():
        return binary_numeric_operation<u32, i32, Operators::Multiply>(configuration);
    case Instructions::i32_divs.value():
        return binary_numeric_operation<i32, i32, Operators::Divide>(configuration);
    case Instructions::i32_divu.value():
        return binary_numeric_operation<u32, i32, Operators::Divide>(configuration);
    case Instructions::i32_rems.value():
        return binary_numeric_operation<i32, i32, Operators::Modulo>(configuration);
    case Instructions::i32_remu.value():
        return binary_numeric_operation<u32, i32, Operators::Modulo>(configuration);
    case Instructions::i32_and.value():
        return binary_numeric_operation<i32, i32, Operators::BitAnd>(configuration);
    case Instructions::i32_or.value():
        return binary_numeric_operation<i32, i32, Operators::BitOr>(configuration);
    case Instructions::i32_xor.value():
        return binary_numeric_operation<i32, i32, Operators::BitXor>(configuration);
    case Instructions::i32_shl.value():
        return binary_numeric_operation<u32, i32, Operators::BitShiftLeft>(configuration);
    case Instructions::i32_shrs.value():
        return binary_numeric_operation<i32, i32, Operators::BitShiftRight>(configuration);
    case Instructions::i32_shru.value():
        return binary_numeric_operation<u32, i32, Operators::BitShiftRight>(configuration);
    case Instructions::i32_rotl.value():
        return binary_numeric_operation<u32, i32, Operators::BitRotateLeft>(configuration);
    case Instructions::i32_rotr.value():
        return binary_numeric_operation<u32, i32, Operators::BitRotateRight>(configuration);
    case Instructions::i64_clz.value():
        return unary_operation<i64, i64, Operators::CountLeadingZeros>(configuration);
    case Instructions::i64_ctz.value():
        return unary_operation<i64, i64, Operators::CountTrailingZeros>(configuration);
    case Instructions::i64_popcnt.value():
        return unary_operation<i64, i64, Operators::PopCount>(configuration);
    case Instructions::i64_add.value():
        return binary_numeric_operation<u64, i64, Operators::Add>(configuration);
    case Instructions::i64_sub.value():
        return binary_numeric_operation<u64, i64, Operators::Subtract>(configuration);
    case Instructions::i64_mul.value():
        return binary_numeric_operation<u64, i64, Operators::Multiply>(configuration);
    case Instructions::i64_divs.value():
        return binary_numeric_operation<i64, i64, Operators::Divide>(configuration);
    case Instructions::i64_divu.value():
        return binary_numeric_operation<u64, i64, Operators::Divide>(configuration);
    case Instructions::i64_rems.value():
        return binary_numeric_operation<i64, i64, Operators::Modulo>(configuration);
    case Instructions::i64_remu.value():
        return binary_numeric_operation<u64, i64, Operators::Modulo>(configuration);
    case Instructions::i64_and.value():
        return binary_numeric_operation<i64, i64, Operators::BitAnd>(configuration);
    case Instructions::i64_or.value():
        return binary_numeric_operation<i64, i64, Operators::BitOr>(configuration);
    case Instructions::i64_xor.value():
        return binary_numeric_operation<i64, i64, Operators::BitXor>(configuration);
    case Instructions::i64_shl.value():
        return binary_numeric_operation<u64, i64, Operators::BitShiftLeft>(configuration);
    case Instructions::i64_shrs.value():
        return binary_numeric_operation<i64, i64, Operators::BitShiftRight>(configuration);
    case Instructions::i64_shru.value():
        return binary_numeric_operation<u64, i64, Operators::BitShiftRight>(configuration);
    case Instructions::i64_rotl.value():
        return binary_numeric_operation<u64, i64, Operators::BitRotateLeft>(configuration);
    case Instructions::i64_rotr.value():
        return binary_numeric_operation<u64, i64, Operators::BitRotateRight>(configuration);
    case Instructions::f32_abs.value():
        return unary_operation<float, float, Operators::Absolute>(configuration);
    case Instructions::f32_neg.value():
        return unary_operation<float, float, Operators::Negate>(configuration);
    case Instructions::f32_ceil.value():
        return unary_operation<float, float, Operators::Ceil>(configuration);
    case Instructions::f32_floor.value():
        return unary_operation<float, float, Operators::Floor>(configuration);
    case Instructions::f32_trunc.value():
        return unary_operation<float, float, Operators::Truncate>(configuration);
    case Instructions::f32_nearest.value():
        return unary_operation<float, float, Operators::NearbyIntegral>(configuration);
    case Instructions::f32_sqrt.value():
        return unary_operation<float, float, Operators::SquareRoot>(configuration);
    case Instructions::f32_add.value():
        return binary_numeric_operation<float, float, Operators::Add>(configuration);
    case Instructions::f32_sub.value():
        return binary_numeric_operation<float, float, Operators::Subtract>(configuration);
    case Instructions::f32_mul.value():
        return binary_numeric_operation<float, float, Operators::Multiply>(configuration);
    case Instructions::f32_div.value():
        return binary_numeric_operation<float, float, Operators::Divide>(configuration);
    case Instructions::f32_min.value():
        return binary_numeric_operation<float, float, Operators::Minimum>(configuration);
    case Instructions::f32_max.value():
        return binary_numeric_operation<float, float, Operators::Maximum>(configuration);
    case Instructions::f32_copysign.value():
        return binary_numeric_operation<float, float, Operators::CopySign>(configuration);
    case Instructions::f64_abs.value():
        return unary_operation<double, double, Operators::Absolute>(configuration);
    case Instructions::f64_neg.value():
        return unary_operation<double, double, Operators::Negate>(configuration);
    case Instructions::f64_ceil.value():
        return unary_operation<double, double, Operators::Ceil>(configuration);
    case Instructions::f64_floor.value():
        return unary_operation<double, double, Operators::Floor>(configuration);
    case Instructions::f64_trunc.value():
        return unary_operation<double, double, Operators::Truncate>(configuration);
    case Instructions::f64_nearest.value():
        return unary_operation<double, double, Operators::NearbyIntegral>(configuration);
    case Instructions::f64_sqrt.value():
        return unary_operation<double, double, Operators::SquareRoot>(configuration);
    case Instructions::f64_add.value():
        return binary_numeric_operation<double, double, Operators::Add>(configuration);
    case Instructions::f64_sub.value():
        return binary_numeric_operation<double, double, Operators::Subtract>(configuration);
    case Instructions::f64_mul.value():
        return binary_numeric_operation<double, double, Operators::Multiply>(configuration);
    case Instructions::f64_div.value():
        return binary_numeric_operation<double, double, Operators::Divide>(configuration);
    case Instructions::f64_min.value():
        return binary_numeric_operation<double, double, Operators::Minimum>(configuration);
    case Instructions::f64_max.value():
        return binary_numeric_operation<double, double, Operators::Maximum>(configuration);
    case Instructions::f64_copysign.value():
        return binary_numeric_operation<double, double, Operators::CopySign>(configuration);
    case Instructions::i32_wrap_i64.value():
        return unary_operation<i64, i32, Operators::Wrap<i32>>(configuration);
    case Instructions::i32_trunc_sf32.value():
        return unary_operation<float, i32, Operators::CheckedTruncate<i32>>(configuration);
    case Instructions::i32_trunc_uf32.value():
        return unary_operation<float, i32, Operators::CheckedTruncate<u32>>(configuration);
    case Instructions::i32_trunc_sf64.value():
        return unary_operation<double, i32, Operators::CheckedTruncate<i32>>(configuration);
    case Instructions::i32_trunc_uf64.value():
        return unary_operation<double, i32, Operators::CheckedTruncate<u32>>(configuration);
    case Instructions::i64_trunc_sf32.value():
        return unary_operation<float, i64, Operators::CheckedTruncate<i64>>(configuration);
    case Instructions::i64_trunc_uf32.value():
        return unary_operation<float, i64, Operators::CheckedTruncate<u64>>(configuration);
    case Instructions::i64_trunc_sf64.value():
        return unary_operation<double, i64, Operators::CheckedTruncate<i64>>(configuration);
    case Instructions::i64_trunc_uf64.value():
        return unary_operation<double, i64, Operators::CheckedTruncate<u64>>(configuration);
    case Instructions::i64_extend_si32.value():
        return unary_operation<i32, i64, Operators::Extend<i64>>(configuration);
    case Instructions::i64_extend_ui32.value():
        return unary_operation<u32, i64, Operators::Extend<i64>>(configuration);
    case Instructions::f32_convert_si32.value():
        return unary_operation<i32, float, Operators::Convert<float>>(configuration);
    case Instructions::f32_convert_ui32.value():
        return unary_operation<u32, float, Operators::Convert<float>>(configuration);
    case Instructions::f32_convert_si64.value():
        return unary_operation<i64, float, Operators::Convert<float>>(configuration);
    case Instructions::f32_convert_ui64.value():
        return unary_operation<u64, float, Operators::Convert<float>>(configuration);
    case Instructions::f32_demote_f64.value():
        return unary_operation<double, float, Operators::Demote>(configuration);
    case Instructions::f64_convert_si32.value():
        return unary_operation<i32, double, Operators::Convert<double>>(configuration);
    case Instructions::f64_convert_ui32.value():
        return unary_operation<u32, double, Operators::Convert<double>>(configuration);
    case Instructions::f64_convert_si64.value():
        return unary_operation<i64, double, Operators::Convert<double>>(configuration);
    case Instructions::f64_convert_ui64.value():
        return unary_operation<u64, double, Operators::Convert<double>>(configuration);
    case Instructions::f64_promote_f32.value():
        return unary_operation<float, double, Operators::Promote>(configuration);
    case Instructions::i32_reinterpret_f32.value():
        return unary_operation<float, i32, Operators::Reinterpret<i32>>(configuration);
    case Instructions::i64_reinterpret_f64.value():
        return unary_operation<double, i64, Operators::Reinterpret<i64>>(configuration);
    case Instructions::f32_reinterpret_i32.value():
        return unary_operation<i32, float, Operators::Reinterpret<float>>(configuration);
    case Instructions::f64_reinterpret_i64.value():
        return unary_operation<i64, double, Operators::Reinterpret<double>>(configuration);
    case Instructions::i32_extend8_s.value():
        return unary_operation<i32, i32, Operators::SignExtend<i8>>(configuration);
    case Instructions::i32_extend16_s.value():
        return unary_operation<i32, i32, Operators::SignExtend<i16>>(configuration);
    case Instructions::i64_extend8_s.value():
        return unary_operation<i64, i64, Operators::SignExtend<i8>>(configuration);
    case Instructions::i64_extend16_s.value():
        return unary_operation<i64, i64, Operators::SignExtend<i16>>(configuration);
    case Instructions::i64_extend32_s.value():
        return unary_operation<i64, i64, Operators::SignExtend<i32>>(configuration);
    case Instructions::i32_trunc_sat_f32_s.value():
        return unary_operation<float, i32, Operators::SaturatingTruncate<i32>>(configuration);
    case Instructions::i32_trunc_sat_f32_u.value():
        return unary_operation<float, i32, Operators::SaturatingTruncate<u32>>(configuration);
    case Instructions::i32_trunc_sat_f64_s.value():
        return unary_operation<double, i32, Operators::SaturatingTruncate<i32>>(configuration);
    case Instructions::i32_trunc_sat_f64_u.value():
        return unary_operation<double, i32, Operators::SaturatingTruncate<u32>>(configuration);
    case Instructions::i64_trunc_sat_f32_s.value():
        return unary_operation<float, i64, Operators::SaturatingTruncate<i64>>(configuration);
    case Instructions::i64_trunc_sat_f32_u.value():
        return unary_operation<float, i64, Operators::SaturatingTruncate<u64>>(configuration);
    case Instructions::i64_trunc_sat_f64_s.value():
        return unary_operation<double, i64, Operators::SaturatingTruncate<i64>>(configuration);
    case Instructions::i64_trunc_sat_f64_u.value():
        return unary_operation<double, i64, Operators::SaturatingTruncate<u64>>(configuration);
    case Instructions::memory_init.value():
    case Instructions::data_drop.value():
    case Instructions::memory_copy.value():
    case Instructions::memory_fill.value():
    case Instructions::table_init.value():
    case Instructions::elem_drop.value():
    case Instructions::table_copy.value():
    case Instructions::table_grow.value():
    case Instructions::table_size.value():
    case Instructions::table_fill.value():
    default:
    unimplemented:;
        dbgln("Instruction '{}' not implemented", instruction_name(instruction.opcode()));
        m_trap = Trap { String::formatted("Unimplemented instruction {}", instruction_name(instruction.opcode())) };
        return;
    }
}

void DebuggerBytecodeInterpreter::interpret(Configuration& configuration, InstructionPointer& ip, Instruction const& instruction)
{
    if (pre_interpret_hook) {
        auto result = pre_interpret_hook(configuration, ip, instruction);
        if (!result) {
            m_trap = Trap { "Trapped by user request" };
            return;
        }
    }

    BytecodeInterpreter::interpret(configuration, ip, instruction);

    if (post_interpret_hook) {
        auto result = post_interpret_hook(configuration, ip, instruction, *this);
        if (!result) {
            m_trap = Trap { "Trapped by user request" };
            return;
        }
    }
}
}
