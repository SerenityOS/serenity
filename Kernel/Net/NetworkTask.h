/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

enum class INetProtocol : u16 {
    ICMPv4 = 1,
    IPv4 = 4,
    TCP = 6,
    UDP = 17,
    IPv6 = 41,
    ICMPv6 = 58,
};

class NetworkTask {
public:
    static void spawn();
    static bool is_current();
};
}
