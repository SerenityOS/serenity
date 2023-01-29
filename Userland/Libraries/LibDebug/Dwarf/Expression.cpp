/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Expression.h"

#include <AK/Format.h>
#include <LibCore/MemoryStream.h>
#include <sys/arch/regs.h>

namespace Debug::Dwarf::Expression {

ErrorOr<Value> evaluate(ReadonlyBytes bytes, [[maybe_unused]] PtraceRegisters const& regs)
{
    auto stream = TRY(Core::Stream::FixedMemoryStream::construct(bytes));

    while (!stream->is_eof()) {
        auto opcode = TRY(stream->read_value<u8>());

        switch (static_cast<Operations>(opcode)) {

        default:
            dbgln("DWARF expr addr: {:p}", bytes.data());
            dbgln("unsupported opcode: {}", opcode);
            VERIFY_NOT_REACHED();
        }
    }
    VERIFY_NOT_REACHED();
}

}
