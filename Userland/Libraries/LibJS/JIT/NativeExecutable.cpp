/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/JIT/NativeExecutable.h>
#include <LibJS/Runtime/VM.h>
#include <LibX86/Disassembler.h>
#include <sys/mman.h>

namespace JS::JIT {

NativeExecutable::NativeExecutable(void* code, size_t size)
    : m_code(code)
    , m_size(size)
{
}

NativeExecutable::~NativeExecutable()
{
    munmap(m_code, m_size);
}

void NativeExecutable::run(VM& vm) const
{
    typedef void (*JITCode)(VM&, Value* registers, Value* locals);
    ((JITCode)m_code)(vm,
        vm.bytecode_interpreter().registers().data(),
        vm.running_execution_context().local_variables.data());
}

void NativeExecutable::dump_disassembly() const
{
#if ARCH(X86_64)
    auto const* code_bytes = static_cast<u8 const*>(m_code);
    auto stream = X86::SimpleInstructionStream { code_bytes, m_size };
    auto disassembler = X86::Disassembler(stream);

    while (true) {
        auto offset = stream.offset();
        auto virtual_offset = bit_cast<size_t>(m_code) + offset;
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
        builder.append(insn.value().to_deprecated_string(virtual_offset, nullptr));
        dbgln("{}", builder.string_view());

        for (size_t bytes_printed = 7; bytes_printed < length; bytes_printed += 7) {
            builder.clear();
            builder.appendff("{:p} ", virtual_offset + bytes_printed);
            for (size_t i = bytes_printed; i < bytes_printed + 7 && i < length; i++)
                builder.appendff(" {:02x}", code_bytes[offset + i]);
            dbgln("{}", builder.string_view());
        }
    }
#endif
}

}
