/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Expression.h"

#include <AK/Format.h>
#include <AK/MemoryStream.h>
#include <sys/arch/i386/regs.h>

namespace Debug::Dwarf::Expression {

Value evaluate(ReadonlyBytes bytes, [[maybe_unused]] PtraceRegisters const& regs)
{
    InputMemoryStream stream(bytes);

    while (!stream.eof()) {
        u8 opcode = 0;
        stream >> opcode;

        switch (static_cast<Operations>(opcode)) {
#if ARCH(I386)
        case Operations::RegEbp: {
            ssize_t offset = 0;
            stream.read_LEB128_signed(offset);
            return Value { Type::UnsignedInteger, { regs.ebp + offset } };
        }

        case Operations::FbReg: {
            ssize_t offset = 0;
            stream.read_LEB128_signed(offset);
            return Value { Type::UnsignedInteger, { regs.ebp + 2 * sizeof(size_t) + offset } };
        }
#endif

        default:
            dbgln("DWARF expr addr: {}", (const void*)bytes.data());
            dbgln("unsupported opcode: {}", (u8)opcode);
            VERIFY_NOT_REACHED();
        }
    }
    VERIFY_NOT_REACHED();
}

}
