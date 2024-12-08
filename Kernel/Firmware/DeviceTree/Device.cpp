/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/DeviceTree/Device.h>

namespace Kernel::DeviceTree {

ErrorOr<Device::Resource> Device::get_resource(size_t index) const
{
    auto reg_entry = TRY(TRY(node().reg()).entry(index));
    return Resource {
        .paddr = PhysicalAddress { TRY(TRY(reg_entry.resolve_root_address()).as_flatptr()) },
        .size = TRY(reg_entry.length().as_size_t()),
    };
}

}
