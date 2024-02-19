/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <LibJIT/GDB.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/JIT/NativeExecutable.h>
#include <LibJS/Runtime/VM.h>
#include <LibX86/Disassembler.h>
#include <sys/mman.h>

namespace JS::JIT {

NativeExecutable::NativeExecutable(void* code, size_t size, Vector<BytecodeMapping> mapping, Optional<FixedArray<u8>> gdb_object)
    : m_code(code)
    , m_size(size)
    , m_mapping(move(mapping))
    , m_gdb_object(move(gdb_object))
{
    // Translate block index to instruction address, so the native code can just jump to it.
    for (auto const& entry : m_mapping) {
        if (entry.block_index == BytecodeMapping::EXECUTABLE)
            continue;
        if (entry.bytecode_offset == 0) {
            VERIFY(entry.block_index == m_block_entry_points.size());
            m_block_entry_points.append(bit_cast<FlatPtr>(m_code) + entry.native_offset);
        }
    }
    if (m_gdb_object.has_value())
        ::JIT::GDB::register_into_gdb(m_gdb_object.value().span());
}

NativeExecutable::~NativeExecutable()
{
    if (m_gdb_object.has_value())
        ::JIT::GDB::unregister_from_gdb(m_gdb_object.value().span());
    munmap(m_code, m_size);
}

void NativeExecutable::run(VM& vm, size_t entry_point) const
{
    FlatPtr entry_point_address = 0;
    if (entry_point != 0) {
        entry_point_address = m_block_entry_points[entry_point];
        VERIFY(entry_point_address != 0);
    }

    typedef void (*JITCode)(VM&, Value* registers, Value* locals, FlatPtr entry_point_address, ExecutionContext&);
    ((JITCode)m_code)(vm,
        vm.bytecode_interpreter().registers().data(),
        vm.running_execution_context().locals.data(),
        entry_point_address,
        vm.running_execution_context());
}

#if ARCH(X86_64)
class JITSymbolProvider : public X86::SymbolProvider {
public:
    JITSymbolProvider(NativeExecutable const& executable)
        : m_executable(executable)
    {
    }

    virtual ~JITSymbolProvider() override = default;

    virtual ByteString symbolicate(FlatPtr address, u32* offset = nullptr) const override
    {
        auto base = bit_cast<FlatPtr>(m_executable.code_bytes().data());
        auto native_offset = static_cast<u32>(address - base);
        if (native_offset >= m_executable.code_bytes().size())
            return {};

        auto const& entry = m_executable.find_mapping_entry(native_offset);

        if (offset)
            *offset = native_offset - entry.native_offset;

        if (entry.block_index == BytecodeMapping::EXECUTABLE)
            return BytecodeMapping::EXECUTABLE_LABELS[entry.bytecode_offset];

        if (entry.bytecode_offset == 0)
            return ByteString::formatted("Block {}", entry.block_index + 1);

        return ByteString::formatted("{}:{:x}", entry.block_index + 1, entry.bytecode_offset);
    }

private:
    NativeExecutable const& m_executable;
};
#endif

void NativeExecutable::dump_disassembly([[maybe_unused]] Bytecode::Executable const& executable) const
{
#if ARCH(X86_64)
    auto const* code_bytes = static_cast<u8 const*>(m_code);
    auto stream = X86::SimpleInstructionStream { code_bytes, m_size };
    auto disassembler = X86::Disassembler(stream);
    auto symbol_provider = JITSymbolProvider(*this);
    auto mapping = m_mapping.begin();

    if (!executable.basic_blocks.is_empty() && executable.basic_blocks[0]->size() != 0) {
        auto first_instruction = Bytecode::InstructionStreamIterator { executable.basic_blocks[0]->instruction_stream(), &executable };
        auto source_range = first_instruction.source_range().realize();
        dbgln("Disassembly of '{}' ({}:{}:{}):", executable.name, source_range.filename(), source_range.start.line, source_range.start.column);
    } else {
        dbgln("Disassembly of '{}':", executable.name);
    }

    while (true) {
        auto offset = stream.offset();
        auto virtual_offset = bit_cast<size_t>(m_code) + offset;

        while (!mapping.is_end() && offset > mapping->native_offset)
            ++mapping;
        if (!mapping.is_end() && offset == mapping->native_offset) {
            if (mapping->block_index == BytecodeMapping::EXECUTABLE) {
                dbgln("{}:", BytecodeMapping::EXECUTABLE_LABELS[mapping->bytecode_offset]);
            } else {
                auto const& block = *executable.basic_blocks[mapping->block_index];
                if (mapping->bytecode_offset == 0)
                    dbgln("\nBlock {}:", mapping->block_index + 1);

                if (block.size() != 0) {
                    VERIFY(mapping->bytecode_offset < block.size());
                    auto const& instruction = *reinterpret_cast<Bytecode::Instruction const*>(block.data() + mapping->bytecode_offset);
                    dbgln("{}:{:x} {}:", mapping->block_index + 1, mapping->bytecode_offset, instruction.to_byte_string(executable));
                }
            }
        }

        auto insn = disassembler.next();
        if (!insn.has_value())
            break;

        StringBuilder builder;
        builder.appendff("{:p}  ", virtual_offset);
        auto length = insn.value().length();
        for (size_t i = 0; i < 7; i++) {
            if (i < length)
                builder.appendff("{:02x} ", code_bytes[offset + i]);
            else
                builder.append("   "sv);
        }
        builder.append(" "sv);
        builder.append(insn.value().to_byte_string(virtual_offset, &symbol_provider));
        dbgln("{}", builder.string_view());

        for (size_t bytes_printed = 7; bytes_printed < length; bytes_printed += 7) {
            builder.clear();
            builder.appendff("{:p} ", virtual_offset + bytes_printed);
            for (size_t i = bytes_printed; i < bytes_printed + 7 && i < length; i++)
                builder.appendff(" {:02x}", code_bytes[offset + i]);
            dbgln("{}", builder.string_view());
        }
    }

    dbgln();
#endif
}

BytecodeMapping const& NativeExecutable::find_mapping_entry(size_t native_offset) const
{
    size_t nearby_index = 0;
    AK::binary_search(
        m_mapping,
        native_offset,
        &nearby_index,
        [](FlatPtr needle, BytecodeMapping const& mapping_entry) {
            if (needle > mapping_entry.native_offset)
                return 1;
            if (needle == mapping_entry.native_offset)
                return 0;
            return -1;
        });
    return m_mapping[nearby_index];
}

Optional<UnrealizedSourceRange> NativeExecutable::get_source_range(Bytecode::Executable const& executable, FlatPtr address) const
{
    auto start = bit_cast<FlatPtr>(m_code);
    auto end = start + m_size;
    if (address < start || address >= end)
        return {};
    auto const& entry = find_mapping_entry(address - start - 1);
    if (entry.block_index < executable.basic_blocks.size()) {
        auto const& block = *executable.basic_blocks[entry.block_index];
        if (entry.bytecode_offset < block.size()) {
            auto iterator = Bytecode::InstructionStreamIterator { block.instruction_stream(), &executable, entry.bytecode_offset };
            return iterator.source_range();
        }
    }
    return {};
}

}
