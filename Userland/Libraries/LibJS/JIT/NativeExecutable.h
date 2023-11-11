/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/Noncopyable.h>
#include <AK/Types.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Runtime/Completion.h>

namespace JS::JIT {

struct BytecodeMapping {
    size_t native_offset;
    size_t block_index;
    size_t bytecode_offset;

    // Special block index for labels outside any blocks.
    static constexpr auto EXECUTABLE = NumericLimits<size_t>::max();
    static constexpr auto EXECUTABLE_LABELS = AK::Array { "entry"sv, "common_exit"sv };
};

class NativeExecutable {
    AK_MAKE_NONCOPYABLE(NativeExecutable);
    AK_MAKE_NONMOVABLE(NativeExecutable);

public:
    NativeExecutable(void* code, size_t size, Vector<BytecodeMapping>, Optional<FixedArray<u8>> gdb_object = {});
    ~NativeExecutable();

    void run(VM&, size_t entry_point) const;
    void dump_disassembly(Bytecode::Executable const& executable) const;
    BytecodeMapping const& find_mapping_entry(size_t native_offset) const;
    Optional<UnrealizedSourceRange> get_source_range(Bytecode::Executable const& executable, FlatPtr address) const;

    ReadonlyBytes code_bytes() const { return { m_code, m_size }; }

private:
    void* m_code { nullptr };
    size_t m_size { 0 };
    Vector<BytecodeMapping> m_mapping;
    Vector<FlatPtr> m_block_entry_points;
    mutable OwnPtr<Bytecode::InstructionStreamIterator> m_instruction_stream_iterator;
    Optional<FixedArray<u8>> m_gdb_object;
};

}
