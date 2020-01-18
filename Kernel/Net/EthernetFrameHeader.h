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

#include <AK/NetworkOrdered.h>
#include <Kernel/Net/MACAddress.h>

#pragma GCC diagnostic ignored "-Warray-bounds"

class [[gnu::packed]] EthernetFrameHeader
{
public:
    EthernetFrameHeader() {}
    ~EthernetFrameHeader() {}

    MACAddress destination() const { return m_destination; }
    void set_destination(const MACAddress& address) { m_destination = address; }

    MACAddress source() const { return m_source; }
    void set_source(const MACAddress& address) { m_source = address; }

    u16 ether_type() const { return m_ether_type; }
    void set_ether_type(u16 ether_type) { m_ether_type = ether_type; }

    const void* payload() const { return &m_payload[0]; }
    void* payload() { return &m_payload[0]; }

private:
    MACAddress m_destination;
    MACAddress m_source;
    NetworkOrdered<u16> m_ether_type;
    u32 m_payload[0];
};

static_assert(sizeof(EthernetFrameHeader) == 14);
