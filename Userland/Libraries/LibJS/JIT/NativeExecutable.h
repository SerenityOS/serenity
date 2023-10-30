/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/Types.h>
#include <LibJS/Runtime/Completion.h>

namespace JS::JIT {

struct BytecodeMapping {
    size_t native_offset;
    size_t block_index;
    size_t bytecode_offset;

    // Special block index for labels outside any blocks.
    static constexpr auto EXECUTABLE = NumericLimits<size_t>::max();
};

class NativeExecutable {
    AK_MAKE_NONCOPYABLE(NativeExecutable);
    AK_MAKE_NONMOVABLE(NativeExecutable);

public:
    NativeExecutable(void* code, size_t size, Vector<BytecodeMapping>);
    ~NativeExecutable();

    void run(VM&) const;
    void dump_disassembly() const;

private:
    void* m_code { nullptr };
    size_t m_size { 0 };
    Vector<BytecodeMapping> m_mapping;
};

}
