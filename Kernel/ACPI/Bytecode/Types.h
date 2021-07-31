/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <Kernel/ACPI/Bytecode/Package.h>

namespace Kernel::ACPI {

enum class ConstObjectType {
    One,
    Ones,
    Zero
};

struct ByteBufferPackage {
    Package::DecodingResult size;
    ByteBuffer data;
};

union ConstantData {
    u8 byte_data;
    u16 word_data;
    u32 dword_data;
    u64 qword_data;
    ConstObjectType const_opcode;
};

}
