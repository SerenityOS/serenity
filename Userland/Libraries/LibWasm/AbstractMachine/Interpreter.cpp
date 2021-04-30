/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/AbstractMachine/Configuration.h>
#include <LibWasm/AbstractMachine/Interpreter.h>
#include <LibWasm/Opcode.h>
#include <LibWasm/Printer/Printer.h>

namespace Wasm {

void Interpreter::interpret(Configuration& configuration)
{
    auto& instructions = configuration.frame()->expression().instructions();
    auto max_ip_value = InstructionPointer { instructions.size() };
    auto& current_ip_value = configuration.ip();

    while (current_ip_value < max_ip_value) {
        auto& instruction = instructions[current_ip_value.value()];
        interpret(configuration, current_ip_value, instruction);
        ++current_ip_value;
    }
}

void Interpreter::branch_to_label(Configuration& configuration, LabelIndex index)
{
    auto label = configuration.nth_label(index.value());
    VERIFY(label.has_value());
    NonnullOwnPtrVector<Value> results;
    // Pop results in order
    for (size_t i = 0; i < label->arity(); ++i)
        results.append(move(configuration.stack().pop().get<NonnullOwnPtr<Value>>()));

    size_t drop_count = index.value() + 1;
    if (label->continuation() < configuration.ip())
        --drop_count;

    for (; !configuration.stack().is_empty();) {
        auto entry = configuration.stack().pop();
        if (entry.has<NonnullOwnPtr<Label>>()) {
            if (drop_count-- == 0)
                break;
        }
    }

    // Push results in reverse
    for (size_t i = results.size(); i > 0; --i)
        configuration.stack().push(move(static_cast<Vector<NonnullOwnPtr<Value>>&>(results)[i - 1]));

    configuration.ip() = label->continuation() + 1;
}

ReadonlyBytes Interpreter::load_from_memory(Configuration& configuration, const Instruction& instruction, size_t size)
{
    auto& address = configuration.frame()->module().memories().first();
    auto memory = configuration.store().get(address);
    VERIFY(memory);
    auto& arg = instruction.arguments().get<Instruction::MemoryArgument>();
    auto base = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
    VERIFY(base.has_value());
    auto instance_address = base.value() + static_cast<i64>(arg.offset);
    if (instance_address < 0 || static_cast<u64>(instance_address + size) > memory->size()) {
        dbgln("LibWasm: Memory access out of bounds (expected 0 > {} and {} > {})", instance_address, instance_address + size, memory->size());
        return {};
    }
    return memory->data().bytes().slice(instance_address, size);
}

void Interpreter::interpret(Configuration& configuration, InstructionPointer& ip, const Instruction& instruction)
{
    dbgln_if(WASM_TRACE_DEBUG, "Executing instruction {} at ip {}", instruction_name(instruction.opcode()), ip.value());
    if constexpr (WASM_TRACE_DEBUG)
        configuration.dump_stack();
    switch (instruction.opcode().value()) {
    case Instructions::unreachable.value():
        VERIFY_NOT_REACHED(); // FIXME: This is definitely not right :)
    case Instructions::nop.value():
        return;
    case Instructions::local_get.value():
        configuration.stack().push(make<Value>(configuration.frame()->locals()[instruction.arguments().get<LocalIndex>().value()]));
        return;
    case Instructions::local_set.value(): {
        auto entry = configuration.stack().pop();
        configuration.frame()->locals()[instruction.arguments().get<LocalIndex>().value()] = move(*entry.get<NonnullOwnPtr<Value>>());
        return;
    }
    case Instructions::i32_const.value():
        configuration.stack().push(make<Value>(ValueType { ValueType::I32 }, static_cast<i64>(instruction.arguments().get<i32>())));
        return;
    case Instructions::i64_const.value():
        configuration.stack().push(make<Value>(ValueType { ValueType::I64 }, instruction.arguments().get<i64>()));
        return;
    case Instructions::f32_const.value():
        configuration.stack().push(make<Value>(ValueType { ValueType::F32 }, static_cast<double>(instruction.arguments().get<float>())));
        return;
    case Instructions::f64_const.value():
        configuration.stack().push(make<Value>(ValueType { ValueType::F64 }, instruction.arguments().get<double>()));
        return;
    case Instructions::block.value(): {
        size_t arity = 0;
        auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
        if (args.block_type.kind() != BlockType::Empty)
            arity = 1;
        configuration.stack().push(make<Label>(arity, args.end_ip));
        return;
    }
    case Instructions::loop.value(): {
        size_t arity = 0;
        auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
        if (args.block_type.kind() != BlockType::Empty)
            arity = 1;
        configuration.stack().push(make<Label>(arity, ip.value() + 1));
        return;
    }
    case Instructions::if_.value(): {
        size_t arity = 0;
        auto& args = instruction.arguments().get<Instruction::StructuredInstructionArgs>();
        if (args.block_type.kind() != BlockType::Empty)
            arity = 1;

        auto entry = configuration.stack().pop();
        auto value = entry.get<NonnullOwnPtr<Value>>()->to<i32>();
        VERIFY(value.has_value());
        configuration.stack().push(make<Label>(arity, args.end_ip));
        if (value.value() == 0) {
            if (args.else_ip.has_value()) {
                configuration.ip() = args.else_ip.value();
            } else {
                configuration.ip() = args.end_ip;
                configuration.stack().pop();
            }
        }
        return;
    }
    case Instructions::structured_end.value():
        return;
    case Instructions::structured_else.value(): {
        auto label = configuration.nth_label(0);
        VERIFY(label.has_value());
        NonnullOwnPtrVector<Value> results;
        // Pop results in order
        for (size_t i = 0; i < label->arity(); ++i)
            results.append(move(configuration.stack().pop().get<NonnullOwnPtr<Value>>()));

        // drop all locals
        for (; !configuration.stack().is_empty();) {
            auto entry = configuration.stack().pop();
            if (entry.has<NonnullOwnPtr<Label>>())
                break;
        }

        // Push results in reverse
        for (size_t i = 1; i < results.size() + 1; ++i)
            configuration.stack().push(move(static_cast<Vector<NonnullOwnPtr<Value>>&>(results)[results.size() - i]));

        if (instruction.opcode() == Instructions::structured_end)
            return;

        // Jump to the end label
        configuration.ip() = label->continuation();
        return;
    }

    case Instructions::br.value():
        return branch_to_label(configuration, instruction.arguments().get<LabelIndex>());
    case Instructions::br_if.value(): {
        if (configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>().value_or(0) == 0)
            return;
        return branch_to_label(configuration, instruction.arguments().get<LabelIndex>());
    }
    case Instructions::br_table.value():
    case Instructions::call.value():
    case Instructions::call_indirect.value():
    case Instructions::i32_load.value():
    case Instructions::i64_load.value():
    case Instructions::f32_load.value():
    case Instructions::f64_load.value():
    case Instructions::i32_load8_s.value():
        goto unimplemented;
    case Instructions::i32_load8_u.value(): {
        auto slice = load_from_memory(configuration, instruction, 1);
        VERIFY(slice.size() == 1);
        configuration.stack().push(make<Value>(static_cast<i32>(slice[0])));
        return;
    }
    case Instructions::i32_load16_s.value():
    case Instructions::i32_load16_u.value():
    case Instructions::i64_load8_s.value():
    case Instructions::i64_load8_u.value():
    case Instructions::i64_load16_s.value():
    case Instructions::i64_load16_u.value():
    case Instructions::i64_load32_s.value():
    case Instructions::i64_load32_u.value():
    case Instructions::i32_store.value():
    case Instructions::i64_store.value():
    case Instructions::f32_store.value():
    case Instructions::f64_store.value():
    case Instructions::i32_store8.value():
    case Instructions::i32_store16.value():
    case Instructions::i64_store8.value():
    case Instructions::i64_store16.value():
    case Instructions::i64_store32.value():
    case Instructions::local_tee.value():
    case Instructions::global_get.value():
    case Instructions::global_set.value():
    case Instructions::memory_size.value():
    case Instructions::memory_grow.value():
    case Instructions::table_get.value():
    case Instructions::table_set.value():
    case Instructions::select_typed.value():
    case Instructions::ref_null.value():
    case Instructions::ref_func.value():
    case Instructions::ref_is_null.value():
    case Instructions::return_.value():
    case Instructions::drop.value():
    case Instructions::select.value():
    case Instructions::i32_eqz.value():
    case Instructions::i32_eq.value():
        goto unimplemented;
    case Instructions::i32_ne.value(): {
        auto lhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        auto rhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        VERIFY(lhs.has_value());
        VERIFY(rhs.has_value());
        configuration.stack().push(make<Value>(lhs.value() != rhs.value()));
        return;
    }
    case Instructions::i32_lts.value():
    case Instructions::i32_ltu.value():
    case Instructions::i32_gts.value():
    case Instructions::i32_gtu.value():
    case Instructions::i32_les.value():
    case Instructions::i32_leu.value():
    case Instructions::i32_ges.value():
    case Instructions::i32_geu.value():
    case Instructions::i64_eqz.value():
    case Instructions::i64_eq.value():
    case Instructions::i64_ne.value():
    case Instructions::i64_lts.value():
    case Instructions::i64_ltu.value():
    case Instructions::i64_gts.value():
    case Instructions::i64_gtu.value():
    case Instructions::i64_les.value():
    case Instructions::i64_leu.value():
    case Instructions::i64_ges.value():
    case Instructions::i64_geu.value():
    case Instructions::f32_eq.value():
    case Instructions::f32_ne.value():
    case Instructions::f32_lt.value():
    case Instructions::f32_gt.value():
    case Instructions::f32_le.value():
    case Instructions::f32_ge.value():
    case Instructions::f64_eq.value():
    case Instructions::f64_ne.value():
    case Instructions::f64_lt.value():
    case Instructions::f64_gt.value():
    case Instructions::f64_le.value():
    case Instructions::f64_ge.value():
    case Instructions::i32_clz.value():
    case Instructions::i32_ctz.value():
    case Instructions::i32_popcnt.value():
        goto unimplemented;
    case Instructions::i32_add.value(): {
        auto lhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        auto rhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        VERIFY(lhs.has_value());
        VERIFY(rhs.has_value());
        configuration.stack().push(make<Value>(lhs.value() + rhs.value()));
        return;
    }
    case Instructions::i32_sub.value():
    case Instructions::i32_mul.value():
    case Instructions::i32_divs.value():
    case Instructions::i32_divu.value():
    case Instructions::i32_rems.value():
    case Instructions::i32_remu.value():
        goto unimplemented;
    case Instructions::i32_and.value(): {
        auto lhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        auto rhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        VERIFY(lhs.has_value());
        VERIFY(rhs.has_value());
        configuration.stack().push(make<Value>(lhs.value() & rhs.value()));
        return;
    }
    case Instructions::i32_or.value(): {
        auto lhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        auto rhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        VERIFY(lhs.has_value());
        VERIFY(rhs.has_value());
        configuration.stack().push(make<Value>(lhs.value() | rhs.value()));
        return;
    }
    case Instructions::i32_xor.value(): {
        auto lhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        auto rhs = configuration.stack().pop().get<NonnullOwnPtr<Value>>()->to<i32>();
        VERIFY(lhs.has_value());
        VERIFY(rhs.has_value());
        configuration.stack().push(make<Value>(lhs.value() ^ rhs.value()));
        return;
    }
    case Instructions::i32_shl.value():
    case Instructions::i32_shrs.value():
    case Instructions::i32_shru.value():
    case Instructions::i32_rotl.value():
    case Instructions::i32_rotr.value():
    case Instructions::i64_clz.value():
    case Instructions::i64_ctz.value():
    case Instructions::i64_popcnt.value():
    case Instructions::i64_add.value():
    case Instructions::i64_sub.value():
    case Instructions::i64_mul.value():
    case Instructions::i64_divs.value():
    case Instructions::i64_divu.value():
    case Instructions::i64_rems.value():
    case Instructions::i64_remu.value():
    case Instructions::i64_and.value():
    case Instructions::i64_or.value():
    case Instructions::i64_xor.value():
    case Instructions::i64_shl.value():
    case Instructions::i64_shrs.value():
    case Instructions::i64_shru.value():
    case Instructions::i64_rotl.value():
    case Instructions::i64_rotr.value():
    case Instructions::f32_abs.value():
    case Instructions::f32_neg.value():
    case Instructions::f32_ceil.value():
    case Instructions::f32_floor.value():
    case Instructions::f32_trunc.value():
    case Instructions::f32_nearest.value():
    case Instructions::f32_sqrt.value():
    case Instructions::f32_add.value():
    case Instructions::f32_sub.value():
    case Instructions::f32_mul.value():
    case Instructions::f32_div.value():
    case Instructions::f32_min.value():
    case Instructions::f32_max.value():
    case Instructions::f32_copysign.value():
    case Instructions::f64_abs.value():
    case Instructions::f64_neg.value():
    case Instructions::f64_ceil.value():
    case Instructions::f64_floor.value():
    case Instructions::f64_trunc.value():
    case Instructions::f64_nearest.value():
    case Instructions::f64_sqrt.value():
    case Instructions::f64_add.value():
    case Instructions::f64_sub.value():
    case Instructions::f64_mul.value():
    case Instructions::f64_div.value():
    case Instructions::f64_min.value():
    case Instructions::f64_max.value():
    case Instructions::f64_copysign.value():
    case Instructions::i32_wrap_i64.value():
    case Instructions::i32_trunc_sf32.value():
    case Instructions::i32_trunc_uf32.value():
    case Instructions::i32_trunc_sf64.value():
    case Instructions::i32_trunc_uf64.value():
    case Instructions::i64_extend_si32.value():
    case Instructions::i64_extend_ui32.value():
    case Instructions::i64_trunc_sf32.value():
    case Instructions::i64_trunc_uf32.value():
    case Instructions::i64_trunc_sf64.value():
    case Instructions::i64_trunc_uf64.value():
    case Instructions::f32_convert_si32.value():
    case Instructions::f32_convert_ui32.value():
    case Instructions::f32_convert_si64.value():
    case Instructions::f32_convert_ui64.value():
    case Instructions::f32_demote_f64.value():
    case Instructions::f64_convert_si32.value():
    case Instructions::f64_convert_ui32.value():
    case Instructions::f64_convert_si64.value():
    case Instructions::f64_convert_ui64.value():
    case Instructions::f64_promote_f32.value():
    case Instructions::i32_reinterpret_f32.value():
    case Instructions::i64_reinterpret_f64.value():
    case Instructions::f32_reinterpret_i32.value():
    case Instructions::f64_reinterpret_i64.value():
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
    default:
    unimplemented:;
        dbgln("Instruction '{}' not implemented", instruction_name(instruction.opcode()));
        return;
    }
}
}
