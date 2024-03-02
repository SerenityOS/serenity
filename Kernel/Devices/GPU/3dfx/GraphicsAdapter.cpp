/*
 * Copyright (c) 2023, Edwin Rijkee <edwin@virtualparadise.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/GPU/3dfx/Definitions.h>
#include <Kernel/Devices/GPU/3dfx/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/3dfx/VoodooDisplayConnector.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<VoodooGraphicsAdapter>> VoodooGraphicsAdapter::create(PCI::Device& pci_device)
{
    auto adapter = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) VoodooGraphicsAdapter(pci_device)));
    MUST(adapter->initialize_adapter());
    return adapter;
}

VoodooGraphicsAdapter::VoodooGraphicsAdapter(PCI::Device& pci_device)
    : m_pci_device(pci_device)
{
}

ErrorOr<void> VoodooGraphicsAdapter::initialize_adapter()
{
    m_pci_device->enable_io_space();
    m_pci_device->enable_memory_space();

    auto const& resource0 = m_pci_device->resources()[0];
    auto mmio_addr = PhysicalAddress(resource0.physical_memory_address());
    dbgln_if(TDFX_DEBUG, "3dfx mmio addr {} size {}", mmio_addr, resource0.length);
    auto mmio_mapping = TRY(Memory::map_typed<VoodooGraphics::RegisterMap volatile>(mmio_addr, resource0.length, Memory::Region::Access::Read | Memory::Region::Access::Write));

    auto const& resource1 = m_pci_device->resources()[1];
    auto vmem_addr = PhysicalAddress(resource1.physical_memory_address());
    dbgln_if(TDFX_DEBUG, "3dfx vmem addr {} size {}", vmem_addr, resource1.length);

    auto io_window = TRY(IOWindow::create_for_pci_device_bar(*m_pci_device, PCI::HeaderType0BaseRegister::BAR2));

    m_display_connector = VoodooGraphics::VoodooDisplayConnector::must_create(vmem_addr, resource1.length, move(mmio_mapping), move(io_window));
    TRY(m_display_connector->set_safe_mode_setting());

    return {};
}

}
