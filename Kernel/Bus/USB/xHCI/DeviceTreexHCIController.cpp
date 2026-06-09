/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Bus/USB/xHCI/DeviceTreexHCIController.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::USB::xHCI {

ErrorOr<NonnullLockRefPtr<DeviceTreexHCIController>> DeviceTreexHCIController::try_to_initialize(DeviceTree::Device::Resource registers_resource, StringView node_name, InterruptNumber interrupt_number)
{
    auto registers_mapping = TRY(Memory::map_typed<u8>(registers_resource.paddr, registers_resource.size, Memory::Region::Access::ReadWrite));

    auto controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) DeviceTreexHCIController(move(registers_mapping), node_name, interrupt_number)));
    TRY(controller->initialize());
    return controller;
}

UNMAP_AFTER_INIT DeviceTreexHCIController::DeviceTreexHCIController(Memory::TypedMapping<u8> registers_mapping, StringView node_name, InterruptNumber interrupt_number)
    : xHCIController(move(registers_mapping))
    , m_node_name(node_name)
    , m_interrupt_number(interrupt_number)
{
}

ErrorOr<OwnPtr<xHCIInterrupter>> DeviceTreexHCIController::create_interrupter(u16 interrupter_id)
{
    return TRY(xHCIDeviceTreeInterrupter::create(*this, m_interrupt_number, interrupter_id));
}

ErrorOr<NonnullOwnPtr<xHCIDeviceTreeInterrupter>> xHCIDeviceTreeInterrupter::create(DeviceTreexHCIController& controller, InterruptNumber irq, u16 interrupter_id)
{
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) xHCIDeviceTreeInterrupter(controller, interrupter_id, irq)));
}

xHCIDeviceTreeInterrupter::xHCIDeviceTreeInterrupter(DeviceTreexHCIController& controller, u16 interrupter_id, InterruptNumber irq)
    : xHCIInterrupter(controller, interrupter_id)
    , IRQHandler(irq)
{
    enable_irq();
}

bool xHCIDeviceTreeInterrupter::handle_irq()
{
    xHCIInterrupter::handle_interrupt();
    return true;
}

static constinit Array const compatibles_array = {
    "generic-xhci"sv,
};

DEVICETREE_DRIVER(DeviceTreexHCIControllerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/usb/generic-xhci.yaml
ErrorOr<void> DeviceTreexHCIControllerDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto registers_resource = TRY(device.get_resource(0));
    auto interrupt_number = TRY(device.get_interrupt_number(0));

    auto controller = TRY(DeviceTreexHCIController::try_to_initialize(registers_resource, device.node_name(), interrupt_number));
    USB::USBManagement::the().add_controller(controller);

    return {};
}

}
