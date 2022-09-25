/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <AK/Try.h>
#if ARCH(I386) || ARCH(X86_64)
#    include <Kernel/Arch/x86/Hypervisor/BochsDisplayConnector.h>
#endif
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Bochs/GraphicsAdapter.h>
#include <Kernel/Graphics/Bochs/QEMUDisplayConnector.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<BochsGraphicsAdapter> BochsGraphicsAdapter::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    PCI::HardwareID id = pci_device_identifier.hardware_id();
    VERIFY((id.vendor_id == PCI::VendorID::QEMUOld && id.device_id == 0x1111) || (id.vendor_id == PCI::VendorID::VirtualBox && id.device_id == 0xbeef));
    auto adapter = adopt_lock_ref(*new BochsGraphicsAdapter(pci_device_identifier));
    MUST(adapter->initialize_adapter(pci_device_identifier));
    return adapter;
}

UNMAP_AFTER_INIT BochsGraphicsAdapter::BochsGraphicsAdapter(PCI::DeviceIdentifier const& pci_device_identifier)
    : PCI::Device(pci_device_identifier.address())
{
}

UNMAP_AFTER_INIT ErrorOr<void> BochsGraphicsAdapter::initialize_adapter(PCI::DeviceIdentifier const& pci_device_identifier)
{
    // Note: If we use VirtualBox graphics adapter (which is based on Bochs one), we need to use IO ports
    // Note: Bochs (the real bochs graphics adapter in the Bochs emulator) uses revision ID of 0x0
    // and doesn't support memory-mapped IO registers.

    // Note: In non x86-builds, we should never encounter VirtualBox hardware nor Pure Bochs VBE graphics,
    // so just assume we can use the QEMU BochsVBE-compatible graphics adapter only.
#if ARCH(I386) || ARCH(X86_64)
    bool virtual_box_hardware = (pci_device_identifier.hardware_id().vendor_id == 0x80ee && pci_device_identifier.hardware_id().device_id == 0xbeef);
    auto bar0_space_size = PCI::get_BAR_space_size(pci_device_identifier.address(), PCI::HeaderType0BaseRegister::BAR0);
    if (pci_device_identifier.revision_id().value() == 0x0 || virtual_box_hardware) {
        m_display_connector = BochsDisplayConnector::must_create(PhysicalAddress(PCI::get_BAR0(pci_device_identifier.address()) & 0xfffffff0), bar0_space_size, virtual_box_hardware);
    } else {
        auto registers_mapping = TRY(Memory::map_typed_writable<BochsDisplayMMIORegisters volatile>(PhysicalAddress(PCI::get_BAR2(pci_device_identifier.address()) & 0xfffffff0)));
        VERIFY(registers_mapping.region);
        m_display_connector = QEMUDisplayConnector::must_create(PhysicalAddress(PCI::get_BAR0(pci_device_identifier.address()) & 0xfffffff0), bar0_space_size, move(registers_mapping));
    }
#else
    auto registers_mapping = TRY(Memory::map_typed_writable<BochsDisplayMMIORegisters volatile>(PhysicalAddress(PCI::get_BAR2(pci_device_identifier.address()) & 0xfffffff0)));
    VERIFY(registers_mapping.region);
    m_display_connector = QEMUDisplayConnector::must_create(PhysicalAddress(PCI::get_BAR0(pci_device_identifier.address()) & 0xfffffff0), bar0_space_size, move(registers_mapping));
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
