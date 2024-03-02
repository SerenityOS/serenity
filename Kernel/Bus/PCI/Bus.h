/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/IntrusiveListRelaxedConst.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Device.h>

namespace Kernel::PCI {

class Bus final : public RefCounted<Bus> {
    AK_MAKE_NONCOPYABLE(Bus);

public:
    using DevicesList = IntrusiveListRelaxedConst<&Device::m_bus_list_node>;

    static ErrorOr<NonnullRefPtr<Bus>> create(BusNumber bus_number, NonnullRefPtr<Device> self_device, RefPtr<Bus> parent_bus);

    // FIXME: Add methods to detach a child bus/device.
    void attach_child_bus(Bus& child_bus);
    void attach_child_device(Device& child_device);

    BusNumber number() const { return m_bus_number; }

    Device& self_device() { return *m_self_device; }

private:
    Bus(BusNumber bus_number, NonnullRefPtr<Device> self_device, RefPtr<Bus> parent_bus);

    BusNumber m_bus_number { 0 };
    NonnullRefPtr<Device> m_self_device;
    RefPtr<Bus> const m_parent_bus;

    IntrusiveListNode<Bus, NonnullRefPtr<Bus>> m_bus_list_node;
    using ChildrenBusList = IntrusiveList<&Bus::m_bus_list_node>;

    DevicesList m_devices_list;
    ChildrenBusList m_children_bus_list;
};
}
