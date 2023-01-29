/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct EtherType {
    enum : u16 {
        ARP = 0x0806,
        IPv4 = 0x0800,
        IPv6 = 0x86DD,
    };
};
