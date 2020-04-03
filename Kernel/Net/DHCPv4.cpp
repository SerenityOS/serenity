/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <Kernel/Net/DHCPv4.h>

//#define DHCPV4_DEBUG

namespace Kernel {
ParsedDHCPv4Options DHCPv4Packet::parse_options() const
{
    ParsedDHCPv4Options options;
    for (auto idx = 4; idx < 312; ++idx) {
        auto opt_name = *(const DHCPOptions*)&m_options[idx];
        switch (opt_name) {
        case DHCPOptions::Pad:
            continue;
        case DHCPOptions::End:
            goto escape;
        default:
            ++idx;
            auto len = m_options[idx];
#ifdef DHCPV4_DEBUG
            dbg() << "DHCP Option " << (u8)opt_name << " with length " << len;
#endif
            ++idx;
            options.options.set(opt_name, { len, &m_options[idx] });
            idx += len - 1;
            break;
        }
    }
escape:;
    return options;
}
}
