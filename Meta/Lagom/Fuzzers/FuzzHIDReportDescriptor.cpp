/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibHID/ReportDescriptorParser.h>

extern "C" int LLVMFuzzerTestOneInput(u8 const* data, size_t size)
{
    AK::set_debug_enabled(false);
    HID::ReportDescriptorParser parser { { data, size } };
    (void)parser.parse();
    return 0;
}
