/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

// https://www.seagate.com/files/staticfiles/support/docs/manual/Interface%20manuals/100293068j.pdf

namespace Kernel::SCSI {
// 3.22.1
struct ReadCapacity10 {
    u8 opcode { 0x25 };
    u8 reserved1 { 0 };
    BigEndian<u32> oboslete_logical_block_address { 0 };
    u16 reserved2 { 0 };
    u8 reserved3 { 0 };
    u8 control { 0 };
};
static_assert(AssertSize<ReadCapacity10, 10>());
// 3.22.2
struct ReadCapacity10Parameters {
    BigEndian<u32> block_count;
    BigEndian<u32> block_size;
};
static_assert(AssertSize<ReadCapacity10Parameters, 8>());

// 3.16
struct Read10 {
    u8 operation_code { 0x28 };
    union {
        u8 settings { 0 };
        struct {
            u8 obsolete : 2;
            u8 rarc : 1;
            u8 fua : 1;
            u8 dpo : 1;
            u8 rdprotect : 3;
        };
    };
    BigEndian<u32> logical_block_address;
    u8 group_number { 0 }; // only bottom 5 bits
    BigEndian<u16> transfer_length;
    u8 control { 0 };
};
static_assert(AssertSize<Read10, 10>());

// 3.60
struct Write10 {
    u8 operation_code { 0x2A };
    union {
        u8 settings { 0 };
        struct {
            u8 obsolete : 2;
            u8 reserved : 1;
            u8 fua : 1;
            u8 dpo : 1;
            u8 wrprotect : 3;
        };
    };
    BigEndian<u32> logical_block_address;
    u8 group_number { 0 }; // only bottom 5 bits
    BigEndian<u16> transfer_length;
    u8 control { 0 };
};
static_assert(AssertSize<Read10, 10>());
}
