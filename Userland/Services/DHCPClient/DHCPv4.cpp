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

#include "DHCPv4.h"

//#define DHCPV4_DEBUG

ParsedDHCPv4Options DHCPv4Packet::parse_options() const
{
    ParsedDHCPv4Options options;
    for (size_t index = 4; index < DHCPV4_OPTION_FIELD_MAX_LENGTH; ++index) {
        auto opt_name = *(const DHCPOption*)&m_options[index];
        switch (opt_name) {
        case DHCPOption::Pad:
            continue;
        case DHCPOption::End:
            goto escape;
        default:
            ++index;
            auto length = m_options[index];
            if ((size_t)length > DHCPV4_OPTION_FIELD_MAX_LENGTH - index) {
                dbg() << "Bogus option length " << length << " assuming forgotten END";
                break;
            }
#ifdef DHCPV4_DEBUG
            dbg() << "DHCP Option " << (u8)opt_name << " with length " << length;
#endif
            ++index;
            options.options.set(opt_name, { length, &m_options[index] });
            index += length - 1;
            break;
        }
    }
escape:;
    return options;
}
