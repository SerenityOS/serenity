/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DHCPv4.h"
#include <AK/Debug.h>

ParsedDHCPv4Options DHCPv4Packet::parse_options() const
{
    ParsedDHCPv4Options options;
    for (size_t index = 4; index < DHCPV4_OPTION_FIELD_MAX_LENGTH; ++index) {
        auto opt_name = *(DHCPOption const*)&m_options[index];
        switch (opt_name) {
        case DHCPOption::Pad:
            continue;
        case DHCPOption::End:
            goto escape;
        default:
            ++index;
            auto length = m_options[index];
            if ((size_t)length > DHCPV4_OPTION_FIELD_MAX_LENGTH - index) {
                dbgln("Bogus option length {} assuming forgotten END", length);
                break;
            }
            dbgln_if(DHCPV4_DEBUG, "DHCP Option {} with length {}", (u8)opt_name, length);
            ++index;
            options.options.set(opt_name, { length, &m_options[index] });
            index += length - 1;
            break;
        }
    }
escape:;
    return options;
}
