/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>

#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/PlatformInit.h>
#include <LibDeviceTree/FlattenedDeviceTree.h>

namespace Kernel::DeviceTree {

struct PlatformInitTableEntry {
    StringView compatible_string;
    void (*init_function)(StringView compatible_string);
};

static constinit auto const s_platform_init_table = to_array<PlatformInitTableEntry>({});

void run_platform_init()
{
    auto& header = *bit_cast<::DeviceTree::FlattenedDeviceTreeHeader*>(&s_fdt_storage[0]);
    auto fdt = ReadonlyBytes { s_fdt_storage, header.totalsize };
    auto compatible_or_error = ::DeviceTree::slow_get_property("/compatible"sv, header, fdt);

    if (compatible_or_error.is_error())
        return;

    compatible_or_error.value().for_each_string([](StringView compatible_entry) -> IterationDecision {
        for (auto [platform_compatible_string, platform_init_function] : s_platform_init_table) {
            if (platform_compatible_string == compatible_entry) {
                platform_init_function(compatible_entry);
                return IterationDecision::Break;
            }
        }

        return IterationDecision::Continue;
    });
}

}
