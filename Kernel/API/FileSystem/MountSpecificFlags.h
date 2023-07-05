/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#define MOUNT_SPECIFIC_FLAG_KEY_STRING_MAX_LENGTH 64

#define MOUNT_SPECIFIC_FLAG_NON_ASCII_STRING_TYPE_MAX_LENGTH 64
#define MOUNT_SPECIFIC_FLAG_ASCII_STRING_TYPE_MAX_LENGTH 1024

struct MountSpecificFlag {
    u32 key_string_length;
    u32 value_length;

    enum class ValueType : u32 {
        Boolean = 0,
        UnsignedInteger,
        SignedInteger,
        ASCIIString,
    };

    ValueType value_type;
    unsigned char const* key_string_addr;
    void const* value_addr;
};
