/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibDNS/Packet.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    AK::set_debug_enabled(false);
    auto maybe_packet = DNS::Packet::from_raw_packet({ data, size });
    if (!maybe_packet.has_value())
        return 0;

    (void)maybe_packet.value().to_byte_buffer();
    return 0;
}
