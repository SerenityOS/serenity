/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2024, sdomi <ja@sdomi.pl>
 * Copyright (c) 2024, Wanda <wanda@phinode.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

// IPv4 "Protocol" field or IPv6 "Next Header" field.
// Included in this enum are only transport layer protocols, not IPv6 extended headers.
// https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml
enum class TransportProtocol : u8 {
    ICMP = 1,
    TCP = 6,
    UDP = 17,
    ICMPv6 = 58,
};

}
