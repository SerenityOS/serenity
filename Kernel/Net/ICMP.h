/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/MACAddress.h>

struct ICMPType {
    enum {
        EchoReply = 0,
        EchoRequest = 8,
    };
};

class [[gnu::packed]] ICMPHeader
{
public:
    ICMPHeader() {}
    ~ICMPHeader() {}

    u8 type() const { return m_type; }
    void set_type(u8 b) { m_type = b; }

    u8 code() const { return m_code; }
    void set_code(u8 b) { m_code = b; }

    u16 checksum() const { return m_checksum; }
    void set_checksum(u16 w) { m_checksum = w; }

    const void* payload() const { return this + 1; }
    void* payload() { return this + 1; }

private:
    u8 m_type { 0 };
    u8 m_code { 0 };
    NetworkOrdered<u16> m_checksum { 0 };
    // NOTE: The rest of the header is 4 bytes
};

static_assert(sizeof(ICMPHeader) == 4);

struct [[gnu::packed]] ICMPEchoPacket
{
    ICMPHeader header;
    NetworkOrdered<u16> identifier;
    NetworkOrdered<u16> sequence_number;
    void* payload() { return this + 1; }
    const void* payload() const { return this + 1; }
};
