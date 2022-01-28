/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <AK/Try.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Bochs/DisplayConnector.h>
#include <Kernel/Graphics/Bochs/GraphicsAdapter.h>
#include <Kernel/Graphics/Bochs/VBoxDisplayConnector.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<BochsGraphicsAdapter> BochsGraphicsAdapter::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    PCI::HardwareID id = pci_device_identifier.hardware_id();
    VERIFY((id.vendor_id == PCI::VendorID::QEMUOld && id.device_id == 0x1111) || (id.vendor_id == PCI::VendorID::VirtualBox && id.device_id == 0xbeef));
    auto adapter = adopt_ref(*new BochsGraphicsAdapter(pci_device_identifier));
    MUST(adapter->initialize_adapter(pci_device_identifier));
    return adapter;
}

UNMAP_AFTER_INIT BochsGraphicsAdapter::BochsGraphicsAdapter(PCI::DeviceIdentifier const& pci_device_identifier)
    : PCI::Device(pci_device_identifier.address())
{
    if (pci_device_identifier.class_code().value() == 0x3 && pci_device_identifier.subclass_code().value() == 0x0)
        m_is_vga_capable = true;
}

UNMAP_AFTER_INIT ErrorOr<void> BochsGraphicsAdapter::initialize_adapter(PCI::DeviceIdentifier const& pci_device_identifier)
{
    // Note: If we use VirtualBox graphics adapter (which is based on Bochs one), we need to use IO ports
    // Note: Bochs (the real bochs graphics adapter in the Bochs emulator) uses revision ID of 0x0
    // and doesn't support memory-mapped IO registers.
    if (pci_device_identifier.revision_id().value() == 0x0
        || (pci_device_identifier.hardware_id().vendor_id == 0x80ee && pci_device_identifier.hardware_id().device_id == 0xbeef)) {
        m_display_connector = VBoxDisplayConnector::must_create(PhysicalAddress(PCI::get_BAR0(pci_device_identifier.address()) & 0xfffffff0));
    } else {
        auto registers_mapping = TRY(Memory::map_typed_writable<BochsDisplayMMIORegisters volatile>(PhysicalAddress(PCI::get_BAR2(pci_device_identifier.address()) & 0xfffffff0)));
        VERIFY(registers_mapping.region);
        m_display_connector = BochsDisplayConnector::must_create(PhysicalAddress(PCI::get_BAR0(pci_device_identifier.address()) & 0xfffffff0), registers_mapping.region.release_nonnull(), registers_mapping.offset);
    }

    // Note: According to Gerd Hoffmann - "The linux driver simply does
    // the unblank unconditionally. With bochs-display this is not needed but
    // it also has no bad side effect".
    // FIXME: If the error is ENOTIMPL, ignore it for now until we implement
    // unblank support for VBoxDisplayConnector class too.
    auto result = m_display_connector->unblank();
    if (result.is_error() && result.error().code() != ENOTIMPL)
        return result;

    TRY(m_display_connector->set_safe_resolution());

    return {};
}

UNMAP_AFTER_INIT void BochsGraphicsAdapter::initialize_framebuffer_devices()
{
    // FIXME: Find a better way to determine default resolution...
    m_framebuffer_device = FramebufferDevice::create(*this, PhysicalAddress(PCI::get_BAR0(pci_address()) & 0xfffffff0), 1024, 768, 1024 * sizeof(u32));
    // While write-combine helps greatly on actual hardware, it greatly reduces performance in QEMU
    m_framebuffer_device->enable_write_combine(false);
    // FIXME: Would be nice to be able to return a ErrorOr<void> here.
    VERIFY(!m_framebuffer_device->try_to_initialize().is_error());
}

bool BochsGraphicsAdapter::vga_compatible() const
{
    return m_is_vga_capable;
}

ErrorOr<ByteBuffer> BochsGraphicsAdapter::get_edid(size_t output_port_index) const
{
    if (output_port_index != 0)
        return Error::from_errno(ENODEV);
    return m_display_connector->get_edid();
}

ErrorOr<void> BochsGraphicsAdapter::set_resolution(size_t output_port_index, size_t width, size_t height)
{
    if (output_port_index != 0)
        return Error::from_errno(ENODEV);
    return m_display_connector->set_resolution({ width, height, {} });
}
ErrorOr<void> BochsGraphicsAdapter::set_y_offset(size_t output_port_index, size_t y)
{
    if (output_port_index != 0)
        return Error::from_errno(ENODEV);
    if (m_console_enabled)
        return Error::from_errno(EBUSY);
    return m_display_connector->set_y_offset(y);
}

void BochsGraphicsAdapter::enable_consoles()
{
    SpinlockLocker lock(m_console_mode_switch_lock);
    m_console_enabled = true;
    MUST(m_display_connector->set_y_offset(0));
    if (m_framebuffer_device)
        m_framebuffer_device->deactivate_writes();
    m_display_connector->enable_console();
}
void BochsGraphicsAdapter::disable_consoles()
{
    SpinlockLocker lock(m_console_mode_switch_lock);
    VERIFY(m_framebuffer_device);
    m_console_enabled = false;
    MUST(m_display_connector->set_y_offset(0));
    m_display_connector->disable_console();
    m_framebuffer_device->activate_writes();
}

}
