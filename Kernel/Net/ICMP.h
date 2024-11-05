/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MACAddress.h>
#include <Kernel/Net/IP/IPv4.h>

enum class ICMPType : u8 {
    EchoReply = 0,
    EchoRequest = 8,
};

struct [[gnu::packed]] ICMPHeader {
    ICMPType type { 0 };
    u8 code { 0 };
    NetworkOrdered<u16> checksum { 0 };
};

static_assert(AssertSize<ICMPHeader, 4>());

struct [[gnu::packed]] ICMPEchoPacket {
    ICMPHeader header;
    NetworkOrdered<u16> identifier;
    NetworkOrdered<u16> sequence_number;
    u8 payload[];
};
