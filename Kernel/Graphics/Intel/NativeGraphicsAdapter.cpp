/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/Intel/Definitions.h>
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
    auto adapter = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) IntelNativeGraphicsAdapter(pci_device_identifier)));
    TRY(adapter->initialize_adapter());
    return adapter;
}

ErrorOr<void> IntelNativeGraphicsAdapter::reset_gen4_graphics_device()
{
    using namespace IntelGraphics;
    SpinlockLocker locker(device_identifier().operation_lock());
    PCI::write8_locked(device_identifier(), static_cast<PCI::RegisterOffset>(pci_gen4_reset_register_offset), pci_gen4_reset_register_value);
    for (int retry = 0; retry < 50; retry++) {
        auto status = PCI::read8_locked(device_identifier(), static_cast<PCI::RegisterOffset>(pci_gen4_reset_register_offset));
        if (!(status & pci_gen4_reset_register_value))
            return {};
        microseconds_delay(1000);
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> IntelNativeGraphicsAdapter::initialize_adapter()
{
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Native Graphics Adapter @ {}", device_identifier().address());
    auto bar0_space_size = PCI::get_BAR_space_size(device_identifier(), PCI::HeaderType0BaseRegister::BAR0);
    auto bar2_space_size = PCI::get_BAR_space_size(device_identifier(), PCI::HeaderType0BaseRegister::BAR2);
    dmesgln_pci(*this, "MMIO @ {}, space size is {:x} bytes", PhysicalAddress(PCI::get_BAR0(device_identifier())), bar0_space_size);
    dmesgln_pci(*this, "framebuffer @ {}", PhysicalAddress(PCI::get_BAR2(device_identifier())));

    using MMIORegion = IntelGraphics::MMIORegion;
    MMIORegion first_region { PhysicalAddress(PCI::get_BAR0(device_identifier()) & 0xfffffff0), bar0_space_size };
    MMIORegion second_region { PhysicalAddress(PCI::get_BAR2(device_identifier()) & 0xfffffff0), bar2_space_size };

    PCI::enable_bus_mastering(device_identifier());
    PCI::enable_io_space(device_identifier());
    PCI::enable_memory_space(device_identifier());

    switch (device_identifier().hardware_id().device_id) {
    case 0x29c2:
        TRY(reset_gen4_graphics_device());
        m_connector_group = TRY(IntelDisplayConnectorGroup::try_create({}, IntelGraphics::Generation::Gen4, first_region, second_region));
        return {};
    default:
        return Error::from_errno(ENODEV);
    }
}

IntelNativeGraphicsAdapter::IntelNativeGraphicsAdapter(PCI::DeviceIdentifier const& pci_device_identifier)
    : GenericGraphicsAdapter()
    , PCI::Device(const_cast<PCI::DeviceIdentifier&>(pci_device_identifier))
{
}

}
