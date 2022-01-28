/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Bochs/VBoxDisplayConnector.h>

namespace Kernel {

NonnullOwnPtr<VBoxDisplayConnector> VBoxDisplayConnector::must_create(PhysicalAddress framebuffer_address)
{
    auto connector = adopt_own_if_nonnull(new (nothrow) VBoxDisplayConnector(framebuffer_address)).release_nonnull();
    MUST(connector->create_attached_framebuffer_console());
    return connector;
}

VBoxDisplayConnector::VBoxDisplayConnector(PhysicalAddress framebuffer_address)
    : BochsDisplayConnector(framebuffer_address)
{
}

static void set_register_with_io(u16 index, u16 data)
{
    IO::out16(VBE_DISPI_IOPORT_INDEX, index);
    IO::out16(VBE_DISPI_IOPORT_DATA, data);
}

static u16 get_register_with_io(u16 index)
{
    IO::out16(VBE_DISPI_IOPORT_INDEX, index);
    return IO::in16(VBE_DISPI_IOPORT_DATA);
}

ErrorOr<ByteBuffer> VBoxDisplayConnector::get_edid() const
{
    return Error::from_errno(ENOTIMPL);
}

BochsDisplayConnector::IndexID VBoxDisplayConnector::index_id() const
{
    return get_register_with_io(0);
}

ErrorOr<void> VBoxDisplayConnector::set_resolution(Resolution const& resolution)
{
    MutexLocker locker(m_modeset_lock);
    size_t width = resolution.width;
    size_t height = resolution.height;

    dbgln_if(BXVGA_DEBUG, "VBoxDisplayConnector resolution registers set to - {}x{}", width, height);

    set_register_with_io(to_underlying(BochsDISPIRegisters::ENABLE), 0);
    set_register_with_io(to_underlying(BochsDISPIRegisters::XRES), (u16)width);
    set_register_with_io(to_underlying(BochsDISPIRegisters::YRES), (u16)height);
    set_register_with_io(to_underlying(BochsDISPIRegisters::VIRT_WIDTH), (u16)width);
    set_register_with_io(to_underlying(BochsDISPIRegisters::VIRT_HEIGHT), (u16)height * 2);
    set_register_with_io(to_underlying(BochsDISPIRegisters::BPP), 32);
    set_register_with_io(to_underlying(BochsDISPIRegisters::ENABLE), to_underlying(BochsFramebufferSettings::Enabled) | to_underlying(BochsFramebufferSettings::LinearFramebuffer));
    set_register_with_io(to_underlying(BochsDISPIRegisters::BANK), 0);
    if ((u16)width != get_register_with_io(to_underlying(BochsDISPIRegisters::XRES)) || (u16)height != get_register_with_io(to_underlying(BochsDISPIRegisters::YRES))) {
        return Error::from_errno(ENOTIMPL);
    }
    return {};
}

ErrorOr<DisplayConnector::Resolution> VBoxDisplayConnector::get_resolution()
{
    MutexLocker locker(m_modeset_lock);
    return Resolution { get_register_with_io(to_underlying(BochsDISPIRegisters::XRES)), get_register_with_io(to_underlying(BochsDISPIRegisters::YRES)), {} };
}

ErrorOr<void> VBoxDisplayConnector::set_y_offset(size_t y_offset)
{
    MutexLocker locker(m_modeset_lock);
    set_register_with_io(to_underlying(BochsDISPIRegisters::Y_OFFSET), (u16)y_offset);
    return {};
}

ErrorOr<void> VBoxDisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

}
