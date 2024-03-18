/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/GPU/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Devices/GPU/Definitions.h>
#include <Kernel/Devices/GPU/Intel/NativeGraphicsAdapter.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<IntelNativeGraphicsAdapter>> IntelNativeGraphicsAdapter::create(PCI::Device& pci_device)
{
    auto adapter = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) IntelNativeGraphicsAdapter(pci_device)));
    TRY(adapter->initialize_adapter());
    return adapter;
}

ErrorOr<void> IntelNativeGraphicsAdapter::initialize_adapter()
{
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Intel Native Graphics Adapter @ {}", m_pci_device->device_id().address());
    auto bar0_space_size = m_pci_device->resources()[0].length;
    auto bar2_space_size = m_pci_device->resources()[2].length;
    dmesgln_pci(*m_pci_device, "MMIO @ {}, space size is {:x} bytes", PhysicalAddress(m_pci_device->resources()[0].physical_memory_address()), bar0_space_size);
    dmesgln_pci(*m_pci_device, "framebuffer @ {}", PhysicalAddress(m_pci_device->resources()[2].physical_memory_address()));

    using MMIORegion = IntelDisplayConnectorGroup::MMIORegion;
    MMIORegion first_region { MMIORegion::BARAssigned::BAR0, PhysicalAddress(m_pci_device->resources()[0].physical_memory_address()), bar0_space_size };
    MMIORegion second_region { MMIORegion::BARAssigned::BAR2, PhysicalAddress(m_pci_device->resources()[2].physical_memory_address()), bar2_space_size };

    m_pci_device->enable_bus_mastering();
    m_pci_device->enable_io_space();
    m_pci_device->enable_memory_space();

    switch (m_pci_device->device_id().hardware_id().device_id) {
    case 0x29c2:
        m_connector_group = TRY(IntelDisplayConnectorGroup::try_create({}, IntelGraphics::Generation::Gen4, first_region, second_region));
        return {};
    default:
        return Error::from_errno(ENODEV);
    }
}

IntelNativeGraphicsAdapter::IntelNativeGraphicsAdapter(PCI::Device& pci_device)
    : GPUDevice()
    , m_pci_device(pci_device)
{
}

}
