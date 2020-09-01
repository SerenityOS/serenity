/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include "Expression.h"

#include <AK/MemoryStream.h>

#include <sys/arch/i386/regs.h>

namespace Debug::Dwarf::Expression {

Value evaluate(ReadonlyBytes bytes, const PtraceRegisters& regs)
{
    InputMemoryStream stream(bytes);

    while (!stream.eof()) {
        u8 opcode = 0;
        stream >> opcode;

        switch (static_cast<Operations>(opcode)) {
        case Operations::RegEbp: {
            ssize_t offset = 0;
            stream.read_LEB128_signed(offset);
            return Value { Type::UnsignedIntetger, regs.ebp + offset };
        }

        case Operations::FbReg: {
            ssize_t offset = 0;
            stream.read_LEB128_signed(offset);
            return Value { Type::UnsignedIntetger, regs.ebp + 2 * sizeof(size_t) + offset };
        }

        default:
            dbg() << "DWARF expr addr: " << (const void*)bytes.data();
            dbg() << "unsupported opcode: " << (u8)opcode;
            ASSERT_NOT_REACHED();
        }
    }
    ASSERT_NOT_REACHED();
}

}
