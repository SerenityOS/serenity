/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibDNS/Packet.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    auto packet_or_error = DNS::Packet::from_raw_packet({ data, size });
    if (packet_or_error.is_error())
        return 0;

    (void)packet_or_error.release_value().to_byte_buffer();
    return 0;
}
