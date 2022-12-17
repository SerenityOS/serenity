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

ErrorOr<bool> IntelNativeGraphicsAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    return is_supported_model(pci_device_identifier.hardware_id().device_id);
}

ErrorOr<NonnullLockRefPtr<GenericGraphicsAdapter>> IntelNativeGraphicsAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto adapter = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) IntelNativeGraphicsAdapter(pci_device_identifier.address())));
    TRY(adapter->initialize_adapter());
    return adapter;
}

ErrorOr<void> IntelNativeGraphicsAdapter::initialize_adapter()
{
    auto address = pci_address();
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Native Graphics Adapter @ {}", address);
    auto bar0_space_size = PCI::get_BAR_space_size(address, PCI::HeaderType0BaseRegister::BAR0);
    VERIFY(bar0_space_size == 0x80000);
    auto bar2_space_size = PCI::get_BAR_space_size(address, PCI::HeaderType0BaseRegister::BAR2);
    dmesgln_pci(*this, "MMIO @ {}, space size is {:x} bytes", PhysicalAddress(PCI::get_BAR0(address)), bar0_space_size);
    dmesgln_pci(*this, "framebuffer @ {}", PhysicalAddress(PCI::get_BAR2(address)));
    PCI::enable_bus_mastering(address);

    m_display_connector = TRY(IntelNativeDisplayConnector::try_create(PhysicalAddress(PCI::get_BAR2(address) & 0xfffffff0), bar2_space_size, PhysicalAddress(PCI::get_BAR0(address) & 0xfffffff0), bar0_space_size));
    return {};
}

IntelNativeGraphicsAdapter::IntelNativeGraphicsAdapter(PCI::Address address)
    : GenericGraphicsAdapter()
    , PCI::Device(address)
{
}

}
