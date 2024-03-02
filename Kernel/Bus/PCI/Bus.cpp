/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Bus.h>

namespace Kernel::PCI {

ErrorOr<NonnullRefPtr<Bus>> Bus::create(BusNumber bus_number, NonnullRefPtr<Device> self_device, RefPtr<Bus> parent_bus)
{
    auto bus = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Bus(bus_number, self_device, parent_bus)));
    if (parent_bus)
        parent_bus->attach_child_bus(*bus);
    return bus;
}

void Bus::attach_child_device(Device& child_device)
{
    m_devices_list.append(child_device);
}

void Bus::attach_child_bus(Bus& child_bus)
{
    m_children_bus_list.append(child_bus);
}

Bus::Bus(BusNumber bus_number, NonnullRefPtr<Device> self_device, RefPtr<Bus> parent_bus)
    : m_bus_number(bus_number)
    , m_self_device(self_device)
    , m_parent_bus(parent_bus)
{
}

}
