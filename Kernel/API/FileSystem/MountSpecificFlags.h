/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct [[gnu::packed]] mount_specific_flag {
    u32 key_string_length;
    u32 value_length;
    u32 value_type;
    unsigned char const* key_string_addr;
    void const* value_addr;

    enum ValueType {
        None,
        UnsignedInteger,
        SignedInteger,
        ASCIIString,
    };
};
