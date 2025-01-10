/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Bus/USB/xHCI/DeviceTreexHCIController.h>
#include <Kernel/Bus/USB/xHCI/xHCIInterrupter.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::USB {

ErrorOr<NonnullLockRefPtr<DeviceTreexHCIController>> DeviceTreexHCIController::try_to_initialize(DeviceTree::Device::Resource registers_resource, StringView node_name, size_t interrupt_number)
{
    auto registers_mapping = TRY(Memory::map_typed_writable<u8>(registers_resource.paddr));

    auto controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) DeviceTreexHCIController(move(registers_mapping), node_name, interrupt_number)));
    TRY(controller->initialize());
    return controller;
}

UNMAP_AFTER_INIT DeviceTreexHCIController::DeviceTreexHCIController(Memory::TypedMapping<u8> registers_mapping, StringView node_name, size_t interrupt_number)
    : xHCIController(move(registers_mapping))
    , m_node_name(node_name)
    , m_interrupt_number(interrupt_number)
{
}

ErrorOr<NonnullOwnPtr<GenericInterruptHandler>> DeviceTreexHCIController::create_interrupter(u16 interrupter_id)
{
    return TRY(xHCIDeviceTreeInterrupter::create(*this, m_interrupt_number, interrupter_id));
}

static constinit Array const compatibles_array = {
    "generic-xhci"sv,
};

DEVICETREE_DRIVER(DeviceTreexHCIControllerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/usb/generic-xhci.yaml
ErrorOr<void> DeviceTreexHCIControllerDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto registers_resource = TRY(device.get_resource(0));
    auto interrupt = TRY(device.node().interrupts(DeviceTree::get()))[0];

    // FIXME: Don't depend on a specific interrupt descriptor format and implement proper devicetree interrupt mapping/translation.
    if (!interrupt.domain_root->is_compatible_with("arm,gic-400"sv) && !interrupt.domain_root->is_compatible_with("arm,cortex-a15-gic"sv))
        return ENOTSUP;
    if (interrupt.interrupt_identifier.size() != 3 * sizeof(BigEndian<u32>))
        return ENOTSUP;

    // The interrupt type is in the first cell. It should be 0 for SPIs.
    if (reinterpret_cast<BigEndian<u32> const*>(interrupt.interrupt_identifier.data())[0] != 0)
        return ENOTSUP;

    // The interrupt number is in the second cell.
    // GIC interrupts 32-1019 are for SPIs, so add 32 to get the GIC interrupt ID.
    auto interrupt_number = (reinterpret_cast<BigEndian<u32> const*>(interrupt.interrupt_identifier.data())[1]) + 32;

    DeviceTree::DeviceRecipe<NonnullLockRefPtr<USBController>> recipe {
        name(),
        device.node_name(),
        [registers_resource, node_name = device.node_name(), interrupt_number] {
            return DeviceTreexHCIController::try_to_initialize(registers_resource, node_name, interrupt_number);
        },
    };

    USBManagement::add_recipe(move(recipe));

    return {};
}

}
