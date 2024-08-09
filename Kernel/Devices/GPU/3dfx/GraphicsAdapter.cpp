/*
 * Copyright (c) 2023, Edwin Rijkee <edwin@virtualparadise.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/GPU/3dfx/Definitions.h>
#include <Kernel/Devices/GPU/3dfx/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/3dfx/VoodooDisplayConnector.h>

namespace Kernel {

static constexpr u16 supported_models[] {
    // 0x0003, // Banshee (untested)
    0x0005, // Voodoo 3
    // 0x0009 // Voodoo 4 / Voodoo 5 (untested)
};

static bool is_supported_model(u16 device_id)
{
    for (auto& id : supported_models) {
        if (id == device_id)
            return true;
    }
    return false;
}

UNMAP_AFTER_INIT ErrorOr<bool> VoodooGraphicsAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    PCI::HardwareID id = pci_device_identifier.hardware_id();
    return id.vendor_id == PCI::VendorID::Tdfx && is_supported_model(id.device_id);
}

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<GPUDevice>> VoodooGraphicsAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto adapter = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) VoodooGraphicsAdapter(pci_device_identifier)));
    MUST(adapter->initialize_adapter(pci_device_identifier));
    return adapter;
}

UNMAP_AFTER_INIT VoodooGraphicsAdapter::VoodooGraphicsAdapter(PCI::DeviceIdentifier const& device_identifier)
    : PCI::Device(const_cast<PCI::DeviceIdentifier&>(device_identifier))
{
}

UNMAP_AFTER_INIT ErrorOr<void> VoodooGraphicsAdapter::initialize_adapter(PCI::DeviceIdentifier const& pci_device_identifier)
{
    PCI::enable_io_space(device_identifier());
    PCI::enable_memory_space(device_identifier());

    auto mmio_mapping = TRY(PCI::map_bar<VoodooGraphics::RegisterMap volatile>(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR0));
    dbgln_if(TDFX_DEBUG, "3dfx mmio addr {} size {}", mmio_mapping.paddr, mmio_mapping.length);

    auto vmem_addr = TRY(PCI::get_bar_address(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR1));
    auto vmem_size = PCI::get_BAR_space_size(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR1);
    dbgln_if(TDFX_DEBUG, "3dfx vmem addr {} size {}", vmem_addr, vmem_size);

    auto io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR2));

    m_display_connector = TRY(VoodooGraphics::VoodooDisplayConnector::create(vmem_addr, vmem_size, move(mmio_mapping), move(io_window)));
    TRY(m_display_connector->set_safe_mode_setting());

    return {};
}

}
