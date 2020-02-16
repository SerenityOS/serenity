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

namespace Kernel {

class [[gnu::packed]] UDPPacket
{
public:
    UDPPacket() {}
    ~UDPPacket() {}

    u16 source_port() const { return m_source_port; }
    void set_source_port(u16 port) { m_source_port = port; }

    u16 destination_port() const { return m_destination_port; }
    void set_destination_port(u16 port) { m_destination_port = port; }

    u16 length() const { return m_length; }
    void set_length(u16 length) { m_length = length; }

    u16 checksum() const { return m_checksum; }
    void set_checksum(u16 checksum) { m_checksum = checksum; }

    const void* payload() const { return this + 1; }
    void* payload() { return this + 1; }

private:
    NetworkOrdered<u16> m_source_port;
    NetworkOrdered<u16> m_destination_port;
    NetworkOrdered<u16> m_length;
    NetworkOrdered<u16> m_checksum;
};

static_assert(sizeof(UDPPacket) == 8);

}
