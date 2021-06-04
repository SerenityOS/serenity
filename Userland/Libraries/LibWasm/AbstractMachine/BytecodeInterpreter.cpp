/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/AbstractMachine/BytecodeInterpreter.h>
#include <LibWasm/AbstractMachine/Configuration.h>
#include <LibWasm/Opcode.h>
#include <LibWasm/Printer/Printer.h>
#include <limits.h>
#include <math.h>

namespace Wasm {

#define TRAP_IF_NOT(x)                                                                         \
    do {                                                                                       \
        if (trap_if_not(x)) {                                                                  \
            dbgln_if(WASM_TRACE_DEBUG, "Trapped because {} failed, at line {}", #x, __LINE__); \
            return;                                                                            \
        }                                                                                      \
    } while (false)

#define TRAP_IF_NOT_NORETURN(x)                                                                \
    do {                                                                                       \
        if (trap_if_not(x)) {                                                                  \
            dbgln_if(WASM_TRACE_DEBUG, "Trapped because {} failed, at line {}", #x, __LINE__); \
        }                                                                                      \
    } while (false)

void BytecodeInterpreter::interpret(Configuration& configuration)
{
    m_do_trap = false;
    auto& instructions = configuration.frame().expression().instructions();
    auto max_ip_value = InstructionPointer { instructions.size() };
    auto& current_ip_value = configuration.ip();

    while (current_ip_value < max_ip_value) {
        auto& instruction = instructions[current_ip_value.value()];
        auto old_ip = current_ip_value;
        interpret(configuration, current_ip_value, instruction);
        if (m_do_trap)
            return;
        if (current_ip_value == old_ip) // If no jump occurred
            ++current_ip_value;
    }
}

void BytecodeInterpreter::branch_to_label(Configuration& configuration, LabelIndex index)
{
    dbgln_if(WASM_TRACE_DEBUG, "Branch to label with index {}...", index.value());
    auto label = configuration.nth_label(index.value());
    TRAP_IF_NOT(label.has_value());
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
        m_do_trap = true;
        return;
    }
    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    auto base = configuration.stack().peek().get<Value>().to<i32>();
    if (!base.has_value()) {
        m_do_trap = true;
        return;
    }
    auto instance_address = base.value() + static_cast<i64>(arg.offset);
    if (instance_address < 0 || static_cast<u64>(instance_address + sizeof(ReadType)) > memory->size()) {
        m_do_trap = true;
        dbgln("LibWasm: Memory access out of bounds (expected 0 <= {} and {} <= {})", instance_address, instance_address + sizeof(ReadType), memory->size());
        return;
    }
    dbgln_if(WASM_TRACE_DEBUG, "load({} : {}) -> stack", instance_address, sizeof(ReadType));
    auto slice = memory->data().bytes().slice(instance_address, sizeof(ReadType));
    configuration.stack().peek() = Value(static_cast<PushType>(read_value<ReadType>(slice)));
}

void BytecodeInterpreter::store_to_memory(Configuration& configuration, Instruction const& instruction, ReadonlyBytes data)
{
    auto& address = configuration.frame().module().memories().first();
    auto memory = configuration.store().get(address);
    TRAP_IF_NOT(memory);
    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    auto base = configuration.stack().pop().get<Value>().to<i32>();
    TRAP_IF_NOT(base.has_value());
    auto instance_address = base.value() + static_cast<i64>(arg.offset);
    if (instance_address < 0 || static_cast<u64>(instance_address + data.size()) > memory->size()) {
        m_do_trap = true;
        dbgln("LibWasm: Memory access out of bounds (expected 0 <= {} and {} <= {})", instance_address, instance_address + data.size(), memory->size());
        return;
    }
    dbgln_if(WASM_TRACE_DEBUG, "tempoaray({}b) -> store({})", data.size(), instance_address);
    data.copy_to(memory->data().bytes().slice(instance_address, data.size()));
}

void BytecodeInterpreter::call_address(Configuration& configuration, FunctionAddress address)
{
    auto instance = configuration.store().get(address);
    TRAP_IF_NOT(instance);
    FunctionType const* type { nullptr };
    instance->visit([&](auto const& function) { type = &function.type(); });
    TRAP_IF_NOT(type);
    TRAP_IF_NOT(configuration.stack().entries().size() > type->parameters().size());
    Vector<Value> args;
    args.ensure_capacity(type->parameters().size());
    auto span = configuration.stack().entries().span().slice_from_end(type->parameters().size());
    for (auto& entry : span) {
        auto* ptr = entry.get_pointer<Value>();
        TRAP_IF_NOT(ptr != nullptr);
        args.unchecked_append(*ptr);
    }

    configuration.stack().entries().remove(configuration.stack().size() - span.size(), span.size());

    Result result { Trap {} };
    {
        CallFrameHandle handle { *this, configuration };
        result = configuration.call(*this, address, move(args));
    }

    if (result.is_trap()) {
        m_do_trap = true;
        return;
    }

    configuration.stack().entries().ensure_capacity(configuration.stack().size() + result.values().size());
    for (auto& entry : result.values())
        configuration.stack().entries().unchecked_append(move(entry));
}

#define BINARY_NUMERIC_OPERATION(type, operator, cast, ...)                                       \
    do {                                                                                          \
        auto rhs = configuration.stack().pop().get<Value>().to<type>();                           \
        auto lhs = configuration.stack().peek().get<Value>().to<type>();                          \
        TRAP_IF_NOT(lhs.has_value());                                                             \
        TRAP_IF_NOT(rhs.has_value());                                                             \
        __VA_ARGS__;                                                                              \
        auto result = lhs.value() operator rhs.value();                                           \
        dbgln_if(WASM_TRACE_DEBUG, "{} {} {} = {}", lhs.value(), #operator, rhs.value(), result); \
        configuration.stack().peek() = Value(cast(result));                                       \
        return;                                                                                   \
    } while (false)

#define OVF_CHECKED_BINARY_NUMERIC_OPERATION(type, operator, cast, ...)                            \
    do {                                                                                           \
        auto rhs = configuration.stack().pop().get<Value>().to<type>();                            \
        auto ulhs = configuration.stack().peek().get<Value>().to<type>();                          \
        TRAP_IF_NOT(ulhs.has_value());                                                             \
        TRAP_IF_NOT(rhs.has_value());                                                              \
        dbgln_if(WASM_TRACE_DEBUG, "{} {} {} = ??", ulhs.value(), #operator, rhs.value());         \
        __VA_ARGS__;                                                                               \
        Checked lhs = ulhs.value();                                                                \
        lhs operator##= rhs.value();                                                               \
        TRAP_IF_NOT(!lhs.has_overflow());                                                          \
        auto result = lhs.value();                                                                 \
        dbgln_if(WASM_TRACE_DEBUG, "{} {} {} = {}", ulhs.value(), #operator, rhs.value(), result); \
        configuration.stack().peek() = Value(cast(result));                                        \
        return;                                                                                    \
    } while (false)

#define BINARY_PREFIX_NUMERIC_OPERATION(type, operation, cast, ...)                                 \
    do {                                                                                            \
        auto rhs = configuration.stack().pop().get<Value>().to<type>();                             \
        auto lhs = configuration.stack().peek().get<Value>().to<type>();                            \
        TRAP_IF_NOT(lhs.has_value());                                                               \
        TRAP_IF_NOT(rhs.has_value());                                                               \
        auto result = operation(lhs.value(), rhs.value());                                          \
        dbgln_if(WASM_TRACE_DEBUG, "{}({} {}) = {}", #operation, lhs.value(), rhs.value(), result); \
        configuration.stack().peek() = Value(cast(result));                                         \
        return;                                                                                     \
    } while (false)

#define UNARY_MAP(pop_type, operation, ...)                                               \
    do {                                                                                  \
        auto value = configuration.stack().peek().get<Value>().to<pop_type>();            \
        TRAP_IF_NOT(value.has_value());                                                   \
        auto result = operation(value.value());                                           \
        dbgln_if(WASM_TRACE_DEBUG, "map({}) {} = {}", #operation, value.value(), result); \
        configuration.stack().peek() = Value(__VA_ARGS__(result));                        \
        return;                                                                           \
    } while (false)

#define UNARY_NUMERIC_OPERATION(type, operation) \
    UNARY_MAP(type, operation, type)

#define LOAD_AND_PUSH(read_type, push_type)                                     \
    do {                                                                        \
        return load_and_push<read_type, push_type>(configuration, instruction); \
    } while (false)

#define POP_AND_STORE(pop_type, store_type)                                                                 \
    do {                                                                                                    \
        auto value = ConvertToRaw<store_type> {}(*configuration.stack().pop().get<Value>().to<pop_type>()); \
        dbgln_if(WASM_TRACE_DEBUG, "stack({}) -> temporary({}b)", value, sizeof(store_type));               \
        store_to_memory(configuration, instruction, { &value, sizeof(store_type) });                        \
        return;                                                                                             \
    } while (false)

template<typename T>
T BytecodeInterpreter::read_value(ReadonlyBytes data)
{
    LittleEndian<T> value;
    InputMemoryStream stream { data };
    stream >> value;
    if (stream.handle_any_error()) {
        dbgln("Read from {} failed", data.data());
        m_do_trap = true;
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
        m_do_trap = true;
    return bit_cast<float>(static_cast<u32>(raw_value));
}

template<>
double BytecodeInterpreter::read_value<double>(ReadonlyBytes data)
{
    InputMemoryStream stream { data };
    LittleEndian<u64> raw_value;
    stream >> raw_value;
    if (stream.handle_any_error())
        m_do_trap = true;
    return bit_cast<double>(static_cast<u64>(raw_value));
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

template<typename V, typename T>
MakeSigned<T> BytecodeInterpreter::checked_signed_truncate(V value)
{
    if (isnan(value) || isinf(value)) { // "undefined", let's just trap.
        m_do_trap = true;
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
    m_do_trap = true;
    return true;
}

template<typename V, typename T>
MakeUnsigned<T> BytecodeInterpreter::checked_unsigned_truncate(V value)
{
    if (isnan(value) || isinf(value)) { // "undefined", let's just trap.
        m_do_trap = true;
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
    m_do_trap = true;
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

template<typename T, typename R>
ALWAYS_INLINE static T rotl(T value, R shift)
{
    // generates a single 'rol' instruction if shift is positive
    // otherwise generate a `ror`
    auto const mask = CHAR_BIT * sizeof(T) - 1;
    shift &= mask;
    return (value << shift) | (value >> ((-shift) & mask));
}

template<typename T, typename R>
ALWAYS_INLINE static T rotr(T value, R shift)
{
    // generates a single 'ror' instruction if shift is positive
    // otherwise generate a `rol`
    auto const mask = CHAR_BIT * sizeof(T) - 1;
    shift &= mask;
    return (value >> shift) | (value << ((-shift) & mask));
}

template<typename T>
ALWAYS_INLINE static i32 clz(T value)
{
    if (value == 0)
        return sizeof(T) * CHAR_BIT;

    if constexpr (sizeof(T) == 4)
        return __builtin_clz(value);
    else if constexpr (sizeof(T) == 8)
        return __builtin_clzll(value);
    else
        VERIFY_NOT_REACHED();
}

template<typename T>
ALWAYS_INLINE static i32 ctz(T value)
{
    if (value == 0)
        return sizeof(T) * CHAR_BIT;

    if constexpr (sizeof(T) == 4)
        return __builtin_ctz(value);
    else if constexpr (sizeof(T) == 8)
        return __builtin_ctzll(value);
    else
        VERIFY_NOT_REACHED();
}

void BytecodeInterpreter::interpret(Configuration& configuration, InstructionPointer& ip, Instruction const& instruction)
{
    dbgln_if(WASM_TRACE_DEBUG, "Executing instruction {} at ip {}", instruction_name(instruction.opcode()), ip.value());

    switch (instruction.opcode().value()) {
    case Instructions::unreachable.value():
        m_do_trap = true;
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
        TRAP_IF_NOT(value.has_value());
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
        auto label = configuration.nth_label(0);
        TRAP_IF_NOT(label.has_value());
        size_t end = configuration.stack().size() - label->arity() - 1;
        size_t start = end;
        while (start > 0 && start < configuration.stack().size() && !configuration.stack().entries()[start].has<Label>())
            --start;

        configuration.stack().entries().remove(start, end - start + 1);

        if (instruction.opcode() == Instructions::structured_end)
            return;

        // Jump to the end label
        configuration.ip() = label->continuation();
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
        if (configuration.stack().pop().get<Value>().to<i32>().value_or(0) == 0)
            return;
        return branch_to_label(configuration, instruction.arguments().get<LabelIndex>());
    }
    case Instructions::br_table.value(): {
        auto& arguments = instruction.arguments().get<Instruction::TableBranchArgs>();
        auto entry = configuration.stack().pop();
        TRAP_IF_NOT(entry.has<Value>());
        auto maybe_i = entry.get<Value>().to<i32>();
        TRAP_IF_NOT(maybe_i.has_value());
        TRAP_IF_NOT(maybe_i.value() >= 0);
        size_t i = *maybe_i;
        if (i < arguments.labels.size())
            return branch_to_label(configuration, arguments.labels[i]);
        return branch_to_label(configuration, arguments.default_);
    }
    case Instructions::call.value(): {
        auto index = instruction.arguments().get<FunctionIndex>();
        TRAP_IF_NOT(index.value() < configuration.frame().module().functions().size());
        auto address = configuration.frame().module().functions()[index.value()];
        dbgln_if(WASM_TRACE_DEBUG, "call({})", address.value());
        call_address(configuration, address);
        return;
    }
    case Instructions::call_indirect.value(): {
        auto& args = instruction.arguments().get<Instruction::IndirectCallArgs>();
        TRAP_IF_NOT(args.table.value() < configuration.frame().module().tables().size());
        auto table_address = configuration.frame().module().tables()[args.table.value()];
        auto table_instance = configuration.store().get(table_address);
        auto index = configuration.stack().pop().get<Value>().to<i32>();
        TRAP_IF_NOT(index.has_value());
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
        LOAD_AND_PUSH(i32, i32);
    case Instructions::i64_load.value():
        LOAD_AND_PUSH(i64, i64);
    case Instructions::f32_load.value():
        LOAD_AND_PUSH(float, float);
    case Instructions::f64_load.value():
        LOAD_AND_PUSH(double, double);
    case Instructions::i32_load8_s.value():
        LOAD_AND_PUSH(i8, i32);
    case Instructions::i32_load8_u.value():
        LOAD_AND_PUSH(u8, i32);
    case Instructions::i32_load16_s.value():
        LOAD_AND_PUSH(i16, i32);
    case Instructions::i32_load16_u.value():
        LOAD_AND_PUSH(u16, i32);
    case Instructions::i64_load8_s.value():
        LOAD_AND_PUSH(i8, i64);
    case Instructions::i64_load8_u.value():
        LOAD_AND_PUSH(u8, i64);
    case Instructions::i64_load16_s.value():
        LOAD_AND_PUSH(i16, i64);
    case Instructions::i64_load16_u.value():
        LOAD_AND_PUSH(u16, i64);
    case Instructions::i64_load32_s.value():
        LOAD_AND_PUSH(i32, i64);
    case Instructions::i64_load32_u.value():
        LOAD_AND_PUSH(u32, i64);
    case Instructions::i32_store.value():
        POP_AND_STORE(i32, i32);
    case Instructions::i64_store.value():
        POP_AND_STORE(i64, i64);
    case Instructions::f32_store.value():
        POP_AND_STORE(float, float);
    case Instructions::f64_store.value():
        POP_AND_STORE(double, double);
    case Instructions::i32_store8.value():
        POP_AND_STORE(i32, i8);
    case Instructions::i32_store16.value():
        POP_AND_STORE(i32, i16);
    case Instructions::i64_store8.value():
        POP_AND_STORE(i64, i8);
    case Instructions::i64_store16.value():
        POP_AND_STORE(i64, i16);
    case Instructions::i64_store32.value():
        POP_AND_STORE(i64, i32);
    case Instructions::local_tee.value(): {
        auto value = configuration.stack().peek().get<Value>();
        auto local_index = instruction.arguments().get<LocalIndex>();
        TRAP_IF_NOT(configuration.frame().locals().size() > local_index.value());
        dbgln_if(WASM_TRACE_DEBUG, "stack:peek -> locals({})", local_index.value());
        configuration.frame().locals()[local_index.value()] = move(value);
        return;
    }
    case Instructions::global_get.value(): {
        auto global_index = instruction.arguments().get<GlobalIndex>();
        TRAP_IF_NOT(configuration.frame().module().globals().size() > global_index.value());
        auto address = configuration.frame().module().globals()[global_index.value()];
        dbgln_if(WASM_TRACE_DEBUG, "global({}) -> stack", address.value());
        auto global = configuration.store().get(address);
        configuration.stack().push(Value(global->value()));
        return;
    }
    case Instructions::global_set.value(): {
        auto global_index = instruction.arguments().get<GlobalIndex>();
        TRAP_IF_NOT(configuration.frame().module().globals().size() > global_index.value());
        auto address = configuration.frame().module().globals()[global_index.value()];
        auto value = configuration.stack().pop().get<Value>();
        dbgln_if(WASM_TRACE_DEBUG, "stack -> global({})", address.value());
        auto global = configuration.store().get(address);
        global->set_value(move(value));
        return;
    }
    case Instructions::memory_size.value(): {
        TRAP_IF_NOT(configuration.frame().module().memories().size() > 0);
        auto address = configuration.frame().module().memories()[0];
        auto instance = configuration.store().get(address);
        auto pages = instance->size() / Constants::page_size;
        dbgln_if(WASM_TRACE_DEBUG, "memory.size -> stack({})", pages);
        configuration.stack().push(Value((i32)pages));
        return;
    }
    case Instructions::memory_grow.value(): {
        TRAP_IF_NOT(configuration.frame().module().memories().size() > 0);
        auto address = configuration.frame().module().memories()[0];
        auto instance = configuration.store().get(address);
        i32 old_pages = instance->size() / Constants::page_size;
        auto new_pages = configuration.stack().peek().get<Value>().to<i32>();
        TRAP_IF_NOT(new_pages.has_value());
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
        TRAP_IF_NOT(type.is_reference());
        configuration.stack().push(Value(Reference(Reference::Null { type })));
        return;
    };
    case Instructions::ref_func.value(): {
        auto index = instruction.arguments().get<FunctionIndex>().value();
        auto& functions = configuration.frame().module().functions();
        TRAP_IF_NOT(functions.size() > index);
        auto address = functions[index];
        configuration.stack().push(Value(ValueType(ValueType::FunctionReference), address.value()));
        return;
    }
    case Instructions::ref_is_null.value(): {
        auto top = configuration.stack().peek().get_pointer<Value>();
        TRAP_IF_NOT(top);
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
        auto value = configuration.stack().pop().get<Value>().to<i32>();
        TRAP_IF_NOT(value.has_value());
        dbgln_if(WASM_TRACE_DEBUG, "select({})", value.value());
        auto rhs = move(configuration.stack().pop().get<Value>());
        auto lhs = move(configuration.stack().peek().get<Value>());
        configuration.stack().peek() = value.value() != 0 ? move(lhs) : move(rhs);
        return;
    }
    case Instructions::i32_eqz.value():
        UNARY_NUMERIC_OPERATION(i32, 0 ==);
    case Instructions::i32_eq.value():
        BINARY_NUMERIC_OPERATION(i32, ==, i32);
    case Instructions::i32_ne.value():
        BINARY_NUMERIC_OPERATION(i32, !=, i32);
    case Instructions::i32_lts.value():
        BINARY_NUMERIC_OPERATION(i32, <, i32);
    case Instructions::i32_ltu.value():
        BINARY_NUMERIC_OPERATION(u32, <, i32);
    case Instructions::i32_gts.value():
        BINARY_NUMERIC_OPERATION(i32, >, i32);
    case Instructions::i32_gtu.value():
        BINARY_NUMERIC_OPERATION(u32, >, i32);
    case Instructions::i32_les.value():
        BINARY_NUMERIC_OPERATION(i32, <=, i32);
    case Instructions::i32_leu.value():
        BINARY_NUMERIC_OPERATION(u32, <=, i32);
    case Instructions::i32_ges.value():
        BINARY_NUMERIC_OPERATION(i32, >=, i32);
    case Instructions::i32_geu.value():
        BINARY_NUMERIC_OPERATION(u32, >=, i32);
    case Instructions::i64_eqz.value():
        UNARY_NUMERIC_OPERATION(i64, 0ull ==);
    case Instructions::i64_eq.value():
        BINARY_NUMERIC_OPERATION(i64, ==, i32);
    case Instructions::i64_ne.value():
        BINARY_NUMERIC_OPERATION(i64, !=, i32);
    case Instructions::i64_lts.value():
        BINARY_NUMERIC_OPERATION(i64, <, i32);
    case Instructions::i64_ltu.value():
        BINARY_NUMERIC_OPERATION(u64, <, i32);
    case Instructions::i64_gts.value():
        BINARY_NUMERIC_OPERATION(i64, >, i32);
    case Instructions::i64_gtu.value():
        BINARY_NUMERIC_OPERATION(u64, >, i32);
    case Instructions::i64_les.value():
        BINARY_NUMERIC_OPERATION(i64, <=, i32);
    case Instructions::i64_leu.value():
        BINARY_NUMERIC_OPERATION(u64, <=, i32);
    case Instructions::i64_ges.value():
        BINARY_NUMERIC_OPERATION(i64, >=, i32);
    case Instructions::i64_geu.value():
        BINARY_NUMERIC_OPERATION(u64, >=, i32);
    case Instructions::f32_eq.value():
        BINARY_NUMERIC_OPERATION(float, ==, i32);
    case Instructions::f32_ne.value():
        BINARY_NUMERIC_OPERATION(float, !=, i32);
    case Instructions::f32_lt.value():
        BINARY_NUMERIC_OPERATION(float, <, i32);
    case Instructions::f32_gt.value():
        BINARY_NUMERIC_OPERATION(float, >, i32);
    case Instructions::f32_le.value():
        BINARY_NUMERIC_OPERATION(float, <=, i32);
    case Instructions::f32_ge.value():
        BINARY_NUMERIC_OPERATION(float, >=, i32);
    case Instructions::f64_eq.value():
        BINARY_NUMERIC_OPERATION(double, ==, i32);
    case Instructions::f64_ne.value():
        BINARY_NUMERIC_OPERATION(double, !=, i32);
    case Instructions::f64_lt.value():
        BINARY_NUMERIC_OPERATION(double, <, i32);
    case Instructions::f64_gt.value():
        BINARY_NUMERIC_OPERATION(double, >, i32);
    case Instructions::f64_le.value():
        BINARY_NUMERIC_OPERATION(double, <=, i32);
    case Instructions::f64_ge.value():
        BINARY_NUMERIC_OPERATION(double, >, i32);
    case Instructions::i32_clz.value():
        UNARY_NUMERIC_OPERATION(i32, clz);
    case Instructions::i32_ctz.value():
        UNARY_NUMERIC_OPERATION(i32, ctz);
    case Instructions::i32_popcnt.value():
        UNARY_NUMERIC_OPERATION(i32, __builtin_popcount);
    case Instructions::i32_add.value():
        BINARY_NUMERIC_OPERATION(i32, +, i32);
    case Instructions::i32_sub.value():
        BINARY_NUMERIC_OPERATION(i32, -, i32);
    case Instructions::i32_mul.value():
        BINARY_NUMERIC_OPERATION(i32, *, i32);
    case Instructions::i32_divs.value():
        BINARY_NUMERIC_OPERATION(i32, /, i32, TRAP_IF_NOT(!(Checked<i32>(lhs.value()) /= rhs.value()).has_overflow()));
    case Instructions::i32_divu.value():
        BINARY_NUMERIC_OPERATION(u32, /, i32, TRAP_IF_NOT(rhs.value() != 0));
    case Instructions::i32_rems.value():
        BINARY_NUMERIC_OPERATION(i32, %, i32, TRAP_IF_NOT(!(Checked<i32>(lhs.value()) /= rhs.value()).has_overflow()));
    case Instructions::i32_remu.value():
        BINARY_NUMERIC_OPERATION(u32, %, i32, TRAP_IF_NOT(rhs.value() != 0));
    case Instructions::i32_and.value():
        BINARY_NUMERIC_OPERATION(i32, &, i32);
    case Instructions::i32_or.value():
        BINARY_NUMERIC_OPERATION(i32, |, i32);
    case Instructions::i32_xor.value():
        BINARY_NUMERIC_OPERATION(i32, ^, i32);
    case Instructions::i32_shl.value():
        BINARY_NUMERIC_OPERATION(i32, <<, i32);
    case Instructions::i32_shrs.value():
        BINARY_NUMERIC_OPERATION(i32, >>, i32);
    case Instructions::i32_shru.value():
        BINARY_NUMERIC_OPERATION(u32, >>, i32);
    case Instructions::i32_rotl.value():
        BINARY_PREFIX_NUMERIC_OPERATION(u32, rotl, i32);
    case Instructions::i32_rotr.value():
        BINARY_PREFIX_NUMERIC_OPERATION(u32, rotr, i32);
    case Instructions::i64_clz.value():
        UNARY_NUMERIC_OPERATION(i64, clz);
    case Instructions::i64_ctz.value():
        UNARY_NUMERIC_OPERATION(i64, ctz);
    case Instructions::i64_popcnt.value():
        UNARY_NUMERIC_OPERATION(i64, __builtin_popcountll);
    case Instructions::i64_add.value():
        OVF_CHECKED_BINARY_NUMERIC_OPERATION(i64, +, i64);
    case Instructions::i64_sub.value():
        OVF_CHECKED_BINARY_NUMERIC_OPERATION(i64, -, i64);
    case Instructions::i64_mul.value():
        OVF_CHECKED_BINARY_NUMERIC_OPERATION(i64, *, i64);
    case Instructions::i64_divs.value():
        OVF_CHECKED_BINARY_NUMERIC_OPERATION(i64, /, i64, TRAP_IF_NOT(rhs.value() != 0));
    case Instructions::i64_divu.value():
        OVF_CHECKED_BINARY_NUMERIC_OPERATION(u64, /, i64, TRAP_IF_NOT(rhs.value() != 0));
    case Instructions::i64_rems.value():
        BINARY_NUMERIC_OPERATION(i64, %, i64, TRAP_IF_NOT(!(Checked<i32>(lhs.value()) /= rhs.value()).has_overflow()));
    case Instructions::i64_remu.value():
        BINARY_NUMERIC_OPERATION(u64, %, i64, TRAP_IF_NOT(rhs.value() != 0));
    case Instructions::i64_and.value():
        BINARY_NUMERIC_OPERATION(i64, &, i64);
    case Instructions::i64_or.value():
        BINARY_NUMERIC_OPERATION(i64, |, i64);
    case Instructions::i64_xor.value():
        BINARY_NUMERIC_OPERATION(i64, ^, i64);
    case Instructions::i64_shl.value():
        BINARY_NUMERIC_OPERATION(i64, <<, i64);
    case Instructions::i64_shrs.value():
        BINARY_NUMERIC_OPERATION(i64, >>, i64);
    case Instructions::i64_shru.value():
        BINARY_NUMERIC_OPERATION(u64, >>, i64);
    case Instructions::i64_rotl.value():
        BINARY_PREFIX_NUMERIC_OPERATION(u64, rotl, i64);
    case Instructions::i64_rotr.value():
        BINARY_PREFIX_NUMERIC_OPERATION(u64, rotr, i64);
    case Instructions::f32_abs.value():
        UNARY_NUMERIC_OPERATION(float, fabsf);
    case Instructions::f32_neg.value():
        UNARY_NUMERIC_OPERATION(float, -);
    case Instructions::f32_ceil.value():
        UNARY_NUMERIC_OPERATION(float, ceilf);
    case Instructions::f32_floor.value():
        UNARY_NUMERIC_OPERATION(float, floorf);
    case Instructions::f32_trunc.value():
        UNARY_NUMERIC_OPERATION(float, truncf);
    case Instructions::f32_nearest.value():
        UNARY_NUMERIC_OPERATION(float, roundf);
    case Instructions::f32_sqrt.value():
        UNARY_NUMERIC_OPERATION(float, sqrtf);
    case Instructions::f32_add.value():
        BINARY_NUMERIC_OPERATION(float, +, float);
    case Instructions::f32_sub.value():
        BINARY_NUMERIC_OPERATION(float, -, float);
    case Instructions::f32_mul.value():
        BINARY_NUMERIC_OPERATION(float, *, float);
    case Instructions::f32_div.value():
        BINARY_NUMERIC_OPERATION(float, /, float);
    case Instructions::f32_min.value():
        BINARY_PREFIX_NUMERIC_OPERATION(float, min, float);
    case Instructions::f32_max.value():
        BINARY_PREFIX_NUMERIC_OPERATION(float, max, float);
    case Instructions::f32_copysign.value():
        BINARY_PREFIX_NUMERIC_OPERATION(float, copysignf, float);
    case Instructions::f64_abs.value():
        UNARY_NUMERIC_OPERATION(double, fabs);
    case Instructions::f64_neg.value():
        UNARY_NUMERIC_OPERATION(double, -);
    case Instructions::f64_ceil.value():
        UNARY_NUMERIC_OPERATION(double, ceil);
    case Instructions::f64_floor.value():
        UNARY_NUMERIC_OPERATION(double, floor);
    case Instructions::f64_trunc.value():
        UNARY_NUMERIC_OPERATION(double, trunc);
    case Instructions::f64_nearest.value():
        UNARY_NUMERIC_OPERATION(double, round);
    case Instructions::f64_sqrt.value():
        UNARY_NUMERIC_OPERATION(double, sqrt);
    case Instructions::f64_add.value():
        BINARY_NUMERIC_OPERATION(double, +, double);
    case Instructions::f64_sub.value():
        BINARY_NUMERIC_OPERATION(double, -, double);
    case Instructions::f64_mul.value():
        BINARY_NUMERIC_OPERATION(double, *, double);
    case Instructions::f64_div.value():
        BINARY_NUMERIC_OPERATION(double, /, double);
    case Instructions::f64_min.value():
        BINARY_PREFIX_NUMERIC_OPERATION(double, min, double);
    case Instructions::f64_max.value():
        BINARY_PREFIX_NUMERIC_OPERATION(double, max, double);
    case Instructions::f64_copysign.value():
        BINARY_PREFIX_NUMERIC_OPERATION(double, copysign, double);
    case Instructions::i32_wrap_i64.value():
        UNARY_MAP(i64, i32, i32);
    case Instructions::i32_trunc_sf32.value(): {
        auto fn = [this](auto& v) { return checked_signed_truncate<float, i32>(v); };
        UNARY_MAP(float, fn, i32);
    }
    case Instructions::i32_trunc_uf32.value(): {
        auto fn = [this](auto& value) { return checked_unsigned_truncate<float, i32>(value); };
        UNARY_MAP(float, fn, i32);
    }
    case Instructions::i32_trunc_sf64.value(): {
        auto fn = [this](auto& value) { return checked_signed_truncate<double, i32>(value); };
        UNARY_MAP(double, fn, i32);
    }
    case Instructions::i32_trunc_uf64.value(): {
        auto fn = [this](auto& value) { return checked_unsigned_truncate<double, i32>(value); };
        UNARY_MAP(double, fn, i32);
    }
    case Instructions::i64_trunc_sf32.value(): {
        auto fn = [this](auto& value) { return checked_signed_truncate<float, i64>(value); };
        UNARY_MAP(float, fn, i64);
    }
    case Instructions::i64_trunc_uf32.value(): {
        auto fn = [this](auto& value) { return checked_unsigned_truncate<float, i64>(value); };
        UNARY_MAP(float, fn, i64);
    }
    case Instructions::i64_trunc_sf64.value(): {
        auto fn = [this](auto& value) { return checked_signed_truncate<double, i64>(value); };
        UNARY_MAP(double, fn, i64);
    }
    case Instructions::i64_trunc_uf64.value(): {
        auto fn = [this](auto& value) { return checked_unsigned_truncate<double, i64>(value); };
        UNARY_MAP(double, fn, i64);
    }
    case Instructions::i64_extend_si32.value():
        UNARY_MAP(i32, i64, i64);
    case Instructions::i64_extend_ui32.value():
        UNARY_MAP(u32, i64, i64);
    case Instructions::f32_convert_si32.value():
        UNARY_MAP(i32, float, float);
    case Instructions::f32_convert_ui32.value():
        UNARY_MAP(u32, float, float);
    case Instructions::f32_convert_si64.value():
        UNARY_MAP(i64, float, float);
    case Instructions::f32_convert_ui64.value():
        UNARY_MAP(u32, float, float);
    case Instructions::f32_demote_f64.value():
        UNARY_MAP(double, float, float);
    case Instructions::f64_convert_si32.value():
        UNARY_MAP(i32, double, double);
    case Instructions::f64_convert_ui32.value():
        UNARY_MAP(u32, double, double);
    case Instructions::f64_convert_si64.value():
        UNARY_MAP(i64, double, double);
    case Instructions::f64_convert_ui64.value():
        UNARY_MAP(u64, double, double);
    case Instructions::f64_promote_f32.value():
        UNARY_MAP(float, double, double);
    case Instructions::i32_reinterpret_f32.value():
        UNARY_MAP(float, bit_cast<i32>, i32);
    case Instructions::i64_reinterpret_f64.value():
        UNARY_MAP(double, bit_cast<i64>, i64);
    case Instructions::f32_reinterpret_i32.value():
        UNARY_MAP(i32, bit_cast<float>, float);
    case Instructions::f64_reinterpret_i64.value():
        UNARY_MAP(i64, bit_cast<double>, double);
    case Instructions::i32_trunc_sat_f32_s.value():
    case Instructions::i32_trunc_sat_f32_u.value():
    case Instructions::i32_trunc_sat_f64_s.value():
    case Instructions::i32_trunc_sat_f64_u.value():
    case Instructions::i64_trunc_sat_f32_s.value():
    case Instructions::i64_trunc_sat_f32_u.value():
    case Instructions::i64_trunc_sat_f64_s.value():
    case Instructions::i64_trunc_sat_f64_u.value():
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
    case Instructions::i32_extend8_s.value():
    case Instructions::i32_extend16_s.value():
    case Instructions::i64_extend8_s.value():
    case Instructions::i64_extend16_s.value():
    case Instructions::i64_extend32_s.value():
    default:
    unimplemented:;
        dbgln("Instruction '{}' not implemented", instruction_name(instruction.opcode()));
        m_do_trap = true;
        return;
    }
}

void DebuggerBytecodeInterpreter::interpret(Configuration& configuration, InstructionPointer& ip, Instruction const& instruction)
{
    if (pre_interpret_hook) {
        auto result = pre_interpret_hook(configuration, ip, instruction);
        if (!result) {
            m_do_trap = true;
            return;
        }
    }

    ScopeGuard guard { [&] {
        if (post_interpret_hook) {
            auto result = post_interpret_hook(configuration, ip, instruction, *this);
            if (!result) {
                m_do_trap = true;
                return;
            }
        }
    } };

    BytecodeInterpreter::interpret(configuration, ip, instruction);
}

}
