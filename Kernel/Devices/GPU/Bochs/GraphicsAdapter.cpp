/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <AK/Try.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Hypervisor/BochsDisplayConnector.h>
#endif
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/GPU/Bochs/Definitions.h>
#include <Kernel/Devices/GPU/Bochs/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/Bochs/QEMUDisplayConnector.h>
#include <Kernel/Devices/GPU/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<BochsGraphicsAdapter>> BochsGraphicsAdapter::create(PCI::Device const& pci_device)
{
    auto adapter = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) BochsGraphicsAdapter(pci_device)));
    MUST(adapter->initialize_adapter());
    return adapter;
}

BochsGraphicsAdapter::BochsGraphicsAdapter(PCI::Device const& pci_device)
    : m_pci_device(pci_device)
{
}

ErrorOr<void> BochsGraphicsAdapter::initialize_adapter()
{
    // Note: If we use VirtualBox graphics adapter (which is based on Bochs one), we need to use IO ports
    // Note: Bochs (the real bochs graphics adapter in the Bochs emulator) uses revision ID of 0x0
    // and doesn't support memory-mapped IO registers.

    // Note: In non x86-builds, we should never encounter VirtualBox hardware nor Pure Bochs VBE graphics,
    // so just assume we can use the QEMU BochsVBE-compatible graphics adapter only.
    auto bar0_space_size = m_pci_device->resources()[0].length;
#if ARCH(X86_64)
    bool virtual_box_hardware = (m_pci_device->device_id().hardware_id().vendor_id == 0x80ee && m_pci_device->device_id().hardware_id().device_id == 0xbeef);
    if (m_pci_device->device_id().revision_id().value() == 0x0 || virtual_box_hardware) {
        m_display_connector = BochsDisplayConnector::must_create(PhysicalAddress(m_pci_device->resources()[0].physical_memory_address()), bar0_space_size, virtual_box_hardware);
    } else {
        auto registers_mapping = TRY(Memory::map_typed_writable<BochsDisplayMMIORegisters volatile>(PhysicalAddress(m_pci_device->resources()[2].physical_memory_address())));
        VERIFY(registers_mapping.region);
        m_display_connector = QEMUDisplayConnector::must_create(PhysicalAddress(m_pci_device->resources()[0].physical_memory_address()), bar0_space_size, move(registers_mapping));
    }
#else
    auto registers_mapping = TRY(Memory::map_typed_writable<BochsDisplayMMIORegisters volatile>(PhysicalAddress(m_pci_device->resources()[2].physical_memory_address())));
    VERIFY(registers_mapping.region);
    m_display_connector = QEMUDisplayConnector::must_create(PhysicalAddress(m_pci_device->resources()[0].physical_memory_address()), bar0_space_size, move(registers_mapping));
#endif

    // Note: According to Gerd Hoffmann - "The linux driver simply does
    // the unblank unconditionally. With bochs-display this is not needed but
    // it also has no bad side effect".
    // FIXME: If the error is ENOTIMPL, ignore it for now until we implement
    // unblank support for VBoxDisplayConnector class too.
    auto result = m_display_connector->unblank();
    if (result.is_error() && result.error().code() != ENOTIMPL)
        return result;

    TRY(m_display_connector->set_safe_mode_setting());

    return {};
}

}
