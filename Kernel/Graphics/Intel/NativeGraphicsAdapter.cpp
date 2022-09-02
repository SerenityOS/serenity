/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/Intel/NativeGraphicsAdapter.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

static constexpr u16 supported_models[] {
    0x29c2, // Intel G35 Adapter
};

static bool is_supported_model(u16 device_id)
{
    for (auto& id : supported_models) {
        if (id == device_id)
            return true;
    }
    return false;
}

LockRefPtr<IntelNativeGraphicsAdapter> IntelNativeGraphicsAdapter::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    VERIFY(pci_device_identifier.hardware_id().vendor_id == 0x8086);
    if (!is_supported_model(pci_device_identifier.hardware_id().device_id))
        return {};
    auto adapter = adopt_lock_ref(*new IntelNativeGraphicsAdapter(pci_device_identifier.address()));
    MUST(adapter->initialize_adapter());
    return adapter;
}

ErrorOr<void> IntelNativeGraphicsAdapter::initialize_adapter()
{
    auto address = pci_address();
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Native Graphics Adapter @ {}", address);
    auto bar0_space_size = PCI::get_BAR_space_size(address, 0);
    VERIFY(bar0_space_size == 0x80000);
    auto bar2_space_size = PCI::get_BAR_space_size(address, 2);
    dmesgln("Intel Native Graphics Adapter @ {}, MMIO @ {}, space size is {:x} bytes", address, PhysicalAddress(PCI::get_BAR0(address)), bar0_space_size);
    dmesgln("Intel Native Graphics Adapter @ {}, framebuffer @ {}", address, PhysicalAddress(PCI::get_BAR2(address)));
    PCI::enable_bus_mastering(address);

    m_display_connector = IntelNativeDisplayConnector::must_create(PhysicalAddress(PCI::get_BAR2(address) & 0xfffffff0), bar2_space_size, PhysicalAddress(PCI::get_BAR0(address) & 0xfffffff0), bar0_space_size);
    return {};
}

IntelNativeGraphicsAdapter::IntelNativeGraphicsAdapter(PCI::Address address)
    : GenericGraphicsAdapter()
    , PCI::Device(address)
{
}

}
