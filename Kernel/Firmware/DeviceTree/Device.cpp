/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/DeviceTree/Device.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::DeviceTree {

ErrorOr<Device::Resource> Device::get_resource(size_t index) const
{
    auto reg_entry = TRY(TRY(node().reg()).entry(index));
    return Resource {
        .paddr = PhysicalAddress { TRY(TRY(reg_entry.resolve_root_address()).as_flatptr()) },
        .size = TRY(reg_entry.length().as_size_t()),
    };
}

ErrorOr<size_t> Device::get_interrupt_number(size_t index) const
{
    auto interrupts = TRY(node().interrupts(DeviceTree::get()));
    auto maybe_interrupt = interrupts.get(index);
    if (!maybe_interrupt.has_value())
        return EINVAL;

    return Management::the().resolve_interrupt_number(*maybe_interrupt);
}

}
