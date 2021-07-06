/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/TemporaryChange.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/Printer/Printer.h>

namespace Wasm {

struct Names {
    static HashMap<OpCode, String> instruction_names;
};

String instruction_name(OpCode const& opcode)
{
    return Names::instruction_names.get(opcode).value_or("<unknown>");
}

void Printer::print_indent()
{
    for (size_t i = 0; i < m_indent; ++i)
        m_stream.write_or_error("  "sv.bytes());
}

void Printer::print(Wasm::BlockType const& type)
{
    print_indent();
    print("(type block ");
    switch (type.kind()) {
    case Wasm::BlockType::Kind::Index:
        print("index {})\n", type.type_index().value());
        return;
    case Wasm::BlockType::Kind::Type: {
        print("type\n");
        {
            TemporaryChange change { m_indent, m_indent + 1 };
            print(type.value_type());
        }
        print_indent();
        print(")\n");
        return;
    }
    case Wasm::BlockType::Kind ::Empty:
        print("empty)\n");
        return;
    }
    VERIFY_NOT_REACHED();
}

void Printer::print(Wasm::CodeSection const& section)
{
    print_indent();
    print("(section code\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& code : section.functions())
            print(code);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::CodeSection::Code const& code)
{
    print(code.func());
}

void Printer::print(Wasm::CustomSection const& section)
{
    print_indent();
    print("(section custom\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print_indent();
        print("(name `{}')\n", section.name());
        print_indent();
        print("(contents {} bytes)\n", section.contents().size());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::DataCountSection const& section)
{
    print_indent();
    print("(section data count\n");
    if (section.count().has_value()) {
        TemporaryChange change { m_indent, m_indent + 1 };
        print_indent();
        print("(count `{}')\n", *section.count());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::DataSection const& section)
{
    print_indent();
    print("(section data\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& entry : section.data())
            print(entry);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::DataSection::Data const& data)
{
    print_indent();
    print("(data with value\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        data.value().visit(
            [this](DataSection::Data::Passive const& value) {
                print_indent();
                print("(passive init {}xu8 (", value.init.size());
                bool first = true;
                for (auto v : value.init) {
                    if (first)
                        print("{:x}", v);
                    else
                        print(" {:x}", v);
                    first = false;
                }
                print(")\n");
            },
            [this](DataSection::Data::Active const& value) {
                print_indent();
                print("(active init {}xu8 (", value.init.size());
                bool first = true;
                for (auto v : value.init) {
                    if (first)
                        print("{:x}", v);
                    else
                        print(" {:x}", v);
                    first = false;
                }
                print("\n");
                {
                    TemporaryChange change { m_indent, m_indent + 1 };
                    print_indent();
                    print("(offset\n");
                    {
                        TemporaryChange change { m_indent, m_indent + 1 };
                        print(value.offset);
                    }
                    print_indent();
                    print(")\n");
                }
                {
                    TemporaryChange change { m_indent, m_indent + 1 };
                    print_indent();
                    print("(index {})\n", value.index.value());
                }
            });
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::ElementSection const& section)
{
    print_indent();
    print("(section element\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& entry : section.segments())
            print(entry);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::ElementSection::Element const& element)
{
    print_indent();
    print("(element ");
    {
        TemporaryChange<size_t> change { m_indent, 0 };
        print(element.type);
    }
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print_indent();
        print("(init\n");
        {
            TemporaryChange change { m_indent, m_indent + 1 };
            for (auto& entry : element.init)
                print(entry);
        }
        print_indent();
        print(")\n");
        print_indent();
        print("(mode ");
        element.mode.visit(
            [this](ElementSection::Active const& active) {
                print("\n");
                {
                    TemporaryChange change { m_indent, m_indent + 1 };
                    print_indent();
                    print("(active index {}\n", active.index.value());
                    {
                        print(active.expression);
                    }
                    print_indent();
                    print(")\n");
                }
                print_indent();
            },
            [this](ElementSection::Passive const&) { print("passive"); },
            [this](ElementSection::Declarative const&) { print("declarative"); });
        print(")\n");
    }
}

void Printer::print(Wasm::ExportSection const& section)
{
    print_indent();
    print("(section export\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& entry : section.entries())
            print(entry);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::ExportSection::Export const& entry)
{
    print_indent();
    print("(export `{}' as\n", entry.name());
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print_indent();
        entry.description().visit(
            [this](FunctionIndex const& index) { print("(function index {})\n", index.value()); },
            [this](TableIndex const& index) { print("(table index {})\n", index.value()); },
            [this](MemoryIndex const& index) { print("(memory index {})\n", index.value()); },
            [this](GlobalIndex const& index) { print("(global index {})\n", index.value()); });
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::Expression const& expression)
{
    TemporaryChange change { m_indent, m_indent + 1 };
    for (auto& instr : expression.instructions())
        print(instr);
}

void Printer::print(Wasm::CodeSection::Func const& func)
{
    print_indent();
    print("(function\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        {
            print_indent();
            print("(locals\n");
            {
                TemporaryChange change { m_indent, m_indent + 1 };
                for (auto& locals : func.locals())
                    print(locals);
            }
            print_indent();
            print(")\n");
        }
        print_indent();
        print("(body\n");
        print(func.body());
        print_indent();
        print(")\n");
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::FunctionSection const& section)
{
    print_indent();
    print("(section function\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& index : section.types()) {
            print_indent();
            print("(type index {})\n", index.value());
        }
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::FunctionType const& type)
{
    print_indent();
    print("(type function\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print_indent();
        print("(parameters\n");
        {
            TemporaryChange change { m_indent, m_indent + 1 };
            for (auto& param : type.parameters())
                print(param);
        }
        print_indent();
        print(")\n");
    }
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print_indent();
        print("(results\n");
        {
            TemporaryChange change { m_indent, m_indent + 1 };
            for (auto& type : type.results())
                print(type);
        }
        print_indent();
        print(")\n");
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::GlobalSection const& section)
{
    print_indent();
    print("(section global\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& entry : section.entries())
            print(entry);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::GlobalSection::Global const& entry)
{
    print_indent();
    print("(global\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print_indent();
        print("(type\n");
        {
            TemporaryChange change { m_indent, m_indent + 1 };
            print(entry.type());
        }
        print_indent();
        print(")\n");
    }
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print_indent();
        print("(init\n");
        {
            TemporaryChange change { m_indent, m_indent + 1 };
            print(entry.expression());
        }
        print_indent();
        print(")\n");
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::GlobalType const& type)
{
    print_indent();
    print("(type global {}mutable\n", type.is_mutable() ? "" : "im");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print(type.type());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::ImportSection const& section)
{
    print_indent();
    print("(section import\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& import : section.imports())
            print(import);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::ImportSection::Import const& import)
{
    print_indent();
    print("(import `{}' from `{}' as\n", import.name(), import.module());
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        import.description().visit(
            [this](auto const& type) { print(type); },
            [this](TypeIndex const& index) {
                print_indent();
                print("(type index {})\n", index.value());
            });
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::Instruction const& instruction)
{
    print_indent();
    print("({}", instruction_name(instruction.opcode()));
    if (instruction.arguments().has<u8>()) {
        print(")\n");
    } else {
        print(" ");
        instruction.arguments().visit(
            [&](BlockType const& type) { print(type); },
            [&](DataIndex const& index) { print("(data index {})", index.value()); },
            [&](ElementIndex const& index) { print("(element index {})", index.value()); },
            [&](FunctionIndex const& index) { print("(function index {})", index.value()); },
            [&](GlobalIndex const& index) { print("(global index {})", index.value()); },
            [&](LabelIndex const& index) { print("(label index {})", index.value()); },
            [&](LocalIndex const& index) { print("(local index {})", index.value()); },
            [&](TableIndex const& index) { print("(table index {})", index.value()); },
            [&](Instruction::IndirectCallArgs const& args) { print("(indirect (type index {}) (table index {}))", args.type.value(), args.table.value()); },
            [&](Instruction::MemoryArgument const& args) { print("(memory (align {}) (offset {}))", args.align, args.offset); },
            [&](Instruction::StructuredInstructionArgs const& args) { print("(structured (else {}) (end {}))", args.else_ip.has_value() ? String::number(args.else_ip->value()) : "(none)", args.end_ip.value()); },
            [&](Instruction::TableBranchArgs const& args) {
                print("(table_branch");
                for (auto& label : args.labels)
                    print(" (label {})", label.value());
                print(" (label {}))", args.default_.value());
            },
            [&](Instruction::TableElementArgs const& args) { print("(table_element (table index {}) (element index {}))", args.table_index.value(), args.element_index.value()); },
            [&](Instruction::TableTableArgs const& args) { print("(table_table (table index {}) (table index {}))", args.lhs.value(), args.rhs.value()); },
            [&](ValueType const& type) { print(type); },
            [&](Vector<ValueType> const&) { print("(types...)"); },
            [&](auto const& value) { print("{}", value); });

        print(")\n");
    }
}

void Printer::print(Wasm::Limits const& limits)
{
    print_indent();
    print("(limits min={}", limits.min());
    if (limits.max().has_value())
        print(" max={}", limits.max().value());
    else
        print(" unbounded");
    print(")\n");
}

void Printer::print(Wasm::Locals const& local)
{
    print_indent();
    print("(local x{} of type\n", local.n());
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print(local.type());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::MemorySection const& section)
{
    print_indent();
    print("(section memory\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& memory : section.memories())
            print(memory);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::MemorySection::Memory const& memory)
{
    print_indent();
    print("(memory\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print(memory.type());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::MemoryType const& type)
{
    print_indent();
    print("(type memory\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print(type.limits());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::Module const& module)
{
    print_indent();
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print("(module\n");
        for (auto& section : module.sections())
            section.visit([this](auto const& value) { print(value); });
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::Module::Function const& func)
{
    print_indent();
    print("(function\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        {
            print_indent();
            print("(locals\n");
            {
                TemporaryChange change { m_indent, m_indent + 1 };
                for (auto& locals : func.locals())
                    print(locals);
            }
            print_indent();
            print(")\n");
        }
        print_indent();
        print("(body\n");
        print(func.body());
        print_indent();
        print(")\n");
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::StartSection const& section)
{
    print_indent();
    print("(section start\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print(section.function());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::StartSection::StartFunction const& function)
{
    print_indent();
    print("(start function index {})\n", function.index().value());
}

void Printer::print(Wasm::TableSection const& section)
{
    print_indent();
    print("(section table\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& table : section.tables())
            print(table);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::TableSection::Table const& table)
{
    print_indent();
    print("(table\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print(table.type());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::TableType const& type)
{
    print_indent();
    print("(type table min:{}", type.limits().min());
    if (type.limits().max().has_value())
        print(" max:{}", type.limits().max().value());
    print("\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        print(type.element_type());
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::TypeSection const& section)
{
    print_indent();
    print("(section type\n");
    {
        TemporaryChange change { m_indent, m_indent + 1 };
        for (auto& type : section.types())
            print(type);
    }
    print_indent();
    print(")\n");
}

void Printer::print(Wasm::ValueType const& type)
{
    print_indent();
    print("(type {})\n", ValueType::kind_name(type.kind()));
}

void Printer::print(Wasm::Value const& value)
{
    print_indent();
    print("{} ", value.value().visit([&]<typename T>(T const& value) {
        if constexpr (IsSame<Wasm::Reference, T>)
            return String::formatted(
                "addr({})",
                value.ref().visit(
                    [](Wasm::Reference::Null const&) { return String("null"); },
                    [](auto const& ref) { return String::number(ref.address.value()); }));
        else
            return String::formatted("{}", value);
    }));
    TemporaryChange<size_t> change { m_indent, 0 };
    print(value.type());
}

void Printer::print(Wasm::Reference const& value)
{
    print_indent();
    print(
        "addr({})\n",
        value.ref().visit(
            [](Wasm::Reference::Null const&) { return String("null"); },
            [](auto const& ref) { return String::number(ref.address.value()); }));
}

}

HashMap<Wasm::OpCode, String> Wasm::Names::instruction_names {
    { Instructions::unreachable, "unreachable" },
    { Instructions::nop, "nop" },
    { Instructions::block, "block" },
    { Instructions::loop, "loop" },
    { Instructions::if_, "if" },
    { Instructions::br, "br" },
    { Instructions::br_if, "br.if" },
    { Instructions::br_table, "br.table" },
    { Instructions::return_, "return." },
    { Instructions::call, "call" },
    { Instructions::call_indirect, "call.indirect" },
    { Instructions::drop, "drop" },
    { Instructions::select, "select" },
    { Instructions::select_typed, "select.typed" },
    { Instructions::local_get, "local.get" },
    { Instructions::local_set, "local.set" },
    { Instructions::local_tee, "local.tee" },
    { Instructions::global_get, "global.get" },
    { Instructions::global_set, "global.set" },
    { Instructions::table_get, "table.get" },
    { Instructions::table_set, "table.set" },
    { Instructions::i32_load, "i32.load" },
    { Instructions::i64_load, "i64.load" },
    { Instructions::f32_load, "f32.load" },
    { Instructions::f64_load, "f64.load" },
    { Instructions::i32_load8_s, "i32.load8.s" },
    { Instructions::i32_load8_u, "i32.load8.u" },
    { Instructions::i32_load16_s, "i32.load16.s" },
    { Instructions::i32_load16_u, "i32.load16.u" },
    { Instructions::i64_load8_s, "i64.load8.s" },
    { Instructions::i64_load8_u, "i64.load8.u" },
    { Instructions::i64_load16_s, "i64.load16.s" },
    { Instructions::i64_load16_u, "i64.load16.u" },
    { Instructions::i64_load32_s, "i64.load32.s" },
    { Instructions::i64_load32_u, "i64.load32.u" },
    { Instructions::i32_store, "i32.store" },
    { Instructions::i64_store, "i64.store" },
    { Instructions::f32_store, "f32.store" },
    { Instructions::f64_store, "f64.store" },
    { Instructions::i32_store8, "i32.store8" },
    { Instructions::i32_store16, "i32.store16" },
    { Instructions::i64_store8, "i64.store8" },
    { Instructions::i64_store16, "i64.store16" },
    { Instructions::i64_store32, "i64.store32" },
    { Instructions::memory_size, "memory.size" },
    { Instructions::memory_grow, "memory.grow" },
    { Instructions::i32_const, "i32.const" },
    { Instructions::i64_const, "i64.const" },
    { Instructions::f32_const, "f32.const" },
    { Instructions::f64_const, "f64.const" },
    { Instructions::i32_eqz, "i32.eqz" },
    { Instructions::i32_eq, "i32.eq" },
    { Instructions::i32_ne, "i32.ne" },
    { Instructions::i32_lts, "i32.lts" },
    { Instructions::i32_ltu, "i32.ltu" },
    { Instructions::i32_gts, "i32.gts" },
    { Instructions::i32_gtu, "i32.gtu" },
    { Instructions::i32_les, "i32.les" },
    { Instructions::i32_leu, "i32.leu" },
    { Instructions::i32_ges, "i32.ges" },
    { Instructions::i32_geu, "i32.geu" },
    { Instructions::i64_eqz, "i64.eqz" },
    { Instructions::i64_eq, "i64.eq" },
    { Instructions::i64_ne, "i64.ne" },
    { Instructions::i64_lts, "i64.lts" },
    { Instructions::i64_ltu, "i64.ltu" },
    { Instructions::i64_gts, "i64.gts" },
    { Instructions::i64_gtu, "i64.gtu" },
    { Instructions::i64_les, "i64.les" },
    { Instructions::i64_leu, "i64.leu" },
    { Instructions::i64_ges, "i64.ges" },
    { Instructions::i64_geu, "i64.geu" },
    { Instructions::f32_eq, "f32.eq" },
    { Instructions::f32_ne, "f32.ne" },
    { Instructions::f32_lt, "f32.lt" },
    { Instructions::f32_gt, "f32.gt" },
    { Instructions::f32_le, "f32.le" },
    { Instructions::f32_ge, "f32.ge" },
    { Instructions::f64_eq, "f64.eq" },
    { Instructions::f64_ne, "f64.ne" },
    { Instructions::f64_lt, "f64.lt" },
    { Instructions::f64_gt, "f64.gt" },
    { Instructions::f64_le, "f64.le" },
    { Instructions::f64_ge, "f64.ge" },
    { Instructions::i32_clz, "i32.clz" },
    { Instructions::i32_ctz, "i32.ctz" },
    { Instructions::i32_popcnt, "i32.popcnt" },
    { Instructions::i32_add, "i32.add" },
    { Instructions::i32_sub, "i32.sub" },
    { Instructions::i32_mul, "i32.mul" },
    { Instructions::i32_divs, "i32.divs" },
    { Instructions::i32_divu, "i32.divu" },
    { Instructions::i32_rems, "i32.rems" },
    { Instructions::i32_remu, "i32.remu" },
    { Instructions::i32_and, "i32.and" },
    { Instructions::i32_or, "i32.or" },
    { Instructions::i32_xor, "i32.xor" },
    { Instructions::i32_shl, "i32.shl" },
    { Instructions::i32_shrs, "i32.shrs" },
    { Instructions::i32_shru, "i32.shru" },
    { Instructions::i32_rotl, "i32.rotl" },
    { Instructions::i32_rotr, "i32.rotr" },
    { Instructions::i64_clz, "i64.clz" },
    { Instructions::i64_ctz, "i64.ctz" },
    { Instructions::i64_popcnt, "i64.popcnt" },
    { Instructions::i64_add, "i64.add" },
    { Instructions::i64_sub, "i64.sub" },
    { Instructions::i64_mul, "i64.mul" },
    { Instructions::i64_divs, "i64.divs" },
    { Instructions::i64_divu, "i64.divu" },
    { Instructions::i64_rems, "i64.rems" },
    { Instructions::i64_remu, "i64.remu" },
    { Instructions::i64_and, "i64.and" },
    { Instructions::i64_or, "i64.or" },
    { Instructions::i64_xor, "i64.xor" },
    { Instructions::i64_shl, "i64.shl" },
    { Instructions::i64_shrs, "i64.shrs" },
    { Instructions::i64_shru, "i64.shru" },
    { Instructions::i64_rotl, "i64.rotl" },
    { Instructions::i64_rotr, "i64.rotr" },
    { Instructions::f32_abs, "f32.abs" },
    { Instructions::f32_neg, "f32.neg" },
    { Instructions::f32_ceil, "f32.ceil" },
    { Instructions::f32_floor, "f32.floor" },
    { Instructions::f32_trunc, "f32.trunc" },
    { Instructions::f32_nearest, "f32.nearest" },
    { Instructions::f32_sqrt, "f32.sqrt" },
    { Instructions::f32_add, "f32.add" },
    { Instructions::f32_sub, "f32.sub" },
    { Instructions::f32_mul, "f32.mul" },
    { Instructions::f32_div, "f32.div" },
    { Instructions::f32_min, "f32.min" },
    { Instructions::f32_max, "f32.max" },
    { Instructions::f32_copysign, "f32.copysign" },
    { Instructions::f64_abs, "f64.abs" },
    { Instructions::f64_neg, "f64.neg" },
    { Instructions::f64_ceil, "f64.ceil" },
    { Instructions::f64_floor, "f64.floor" },
    { Instructions::f64_trunc, "f64.trunc" },
    { Instructions::f64_nearest, "f64.nearest" },
    { Instructions::f64_sqrt, "f64.sqrt" },
    { Instructions::f64_add, "f64.add" },
    { Instructions::f64_sub, "f64.sub" },
    { Instructions::f64_mul, "f64.mul" },
    { Instructions::f64_div, "f64.div" },
    { Instructions::f64_min, "f64.min" },
    { Instructions::f64_max, "f64.max" },
    { Instructions::f64_copysign, "f64.copysign" },
    { Instructions::i32_wrap_i64, "i32.wrap.i64" },
    { Instructions::i32_trunc_sf32, "i32.trunc.sf32" },
    { Instructions::i32_trunc_uf32, "i32.trunc.uf32" },
    { Instructions::i32_trunc_sf64, "i32.trunc.sf64" },
    { Instructions::i32_trunc_uf64, "i32.trunc.uf64" },
    { Instructions::i64_extend_si32, "i64.extend.si32" },
    { Instructions::i64_extend_ui32, "i64.extend.ui32" },
    { Instructions::i64_trunc_sf32, "i64.trunc.sf32" },
    { Instructions::i64_trunc_uf32, "i64.trunc.uf32" },
    { Instructions::i64_trunc_sf64, "i64.trunc.sf64" },
    { Instructions::i64_trunc_uf64, "i64.trunc.uf64" },
    { Instructions::f32_convert_si32, "f32.convert.si32" },
    { Instructions::f32_convert_ui32, "f32.convert.ui32" },
    { Instructions::f32_convert_si64, "f32.convert.si64" },
    { Instructions::f32_convert_ui64, "f32.convert.ui64" },
    { Instructions::f32_demote_f64, "f32.demote.f64" },
    { Instructions::f64_convert_si32, "f64.convert.si32" },
    { Instructions::f64_convert_ui32, "f64.convert.ui32" },
    { Instructions::f64_convert_si64, "f64.convert.si64" },
    { Instructions::f64_convert_ui64, "f64.convert.ui64" },
    { Instructions::f64_promote_f32, "f64.promote.f32" },
    { Instructions::i32_reinterpret_f32, "i32.reinterpret.f32" },
    { Instructions::i64_reinterpret_f64, "i64.reinterpret.f64" },
    { Instructions::f32_reinterpret_i32, "f32.reinterpret.i32" },
    { Instructions::f64_reinterpret_i64, "f64.reinterpret.i64" },
    { Instructions::i32_extend8_s, "i32.extend8_s" },
    { Instructions::i32_extend16_s, "i32.extend16_s" },
    { Instructions::i64_extend8_s, "i64.extend8_s" },
    { Instructions::i64_extend16_s, "i64.extend16_s" },
    { Instructions::i64_extend32_s, "i64.extend32_s" },
    { Instructions::ref_null, "ref.null" },
    { Instructions::ref_is_null, "ref.is.null" },
    { Instructions::ref_func, "ref.func" },
    { Instructions::i32_trunc_sat_f32_s, "i32.trunc.sat.f32.s" },
    { Instructions::i32_trunc_sat_f32_u, "i32.trunc.sat.f32.u" },
    { Instructions::i32_trunc_sat_f64_s, "i32.trunc.sat.f64.s" },
    { Instructions::i32_trunc_sat_f64_u, "i32.trunc.sat.f64.u" },
    { Instructions::i64_trunc_sat_f32_s, "i64.trunc.sat.f32.s" },
    { Instructions::i64_trunc_sat_f32_u, "i64.trunc.sat.f32.u" },
    { Instructions::i64_trunc_sat_f64_s, "i64.trunc.sat.f64.s" },
    { Instructions::i64_trunc_sat_f64_u, "i64.trunc.sat.f64.u" },
    { Instructions::memory_init, "memory.init" },
    { Instructions::data_drop, "data.drop" },
    { Instructions::memory_copy, "memory.copy" },
    { Instructions::memory_fill, "memory.fill" },
    { Instructions::table_init, "table.init" },
    { Instructions::elem_drop, "elem.drop" },
    { Instructions::table_copy, "table.copy" },
    { Instructions::table_grow, "table.grow" },
    { Instructions::table_size, "table.size" },
    { Instructions::table_fill, "table.fill" },
    { Instructions::structured_else, "synthetic:else" },
    { Instructions::structured_end, "synthetic:end" },
};
