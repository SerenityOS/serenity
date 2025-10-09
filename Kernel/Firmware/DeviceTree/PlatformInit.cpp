/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>

#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/PlatformInit.h>
#include <LibDeviceTree/FlattenedDeviceTree.h>

#if ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/PlatformInit.h>
#endif

namespace Kernel::DeviceTree {

struct PlatformInitTableEntry {
    StringView compatible_string;
    void (*init_function)(StringView compatible_string);
};

static constinit auto const s_platform_init_table = to_array<PlatformInitTableEntry>({
#if ARCH(AARCH64)
    { "linux,dummy-virt"sv, virt_platform_init },
    { "raspberrypi,3-model-b"sv, raspberry_pi_3_4_platform_init },
    { "raspberrypi,4-model-b"sv, raspberry_pi_3_4_platform_init },
    { "raspberrypi,5-model-b"sv, raspberry_pi_5_platform_init },
#endif
});

void run_platform_init()
{
    auto& header = *bit_cast<::DeviceTree::FlattenedDeviceTreeHeader*>(flattened_devicetree().data());
    auto compatible_or_error = ::DeviceTree::slow_get_property("/compatible"sv, header, flattened_devicetree());

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
