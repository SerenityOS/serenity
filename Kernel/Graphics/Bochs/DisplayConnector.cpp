/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Bochs/DisplayConnector.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>

namespace Kernel {

NonnullRefPtr<BochsDisplayConnector> BochsDisplayConnector::must_create(PhysicalAddress framebuffer_address, NonnullOwnPtr<Memory::Region> registers_region, size_t registers_region_offset)
{
    auto device_or_error = DeviceManagement::try_create_device<BochsDisplayConnector>(framebuffer_address, move(registers_region), registers_region_offset);
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    MUST(connector->create_attached_framebuffer_console());
    return connector;
}

ErrorOr<void> BochsDisplayConnector::create_attached_framebuffer_console()
{
    auto rounded_size = TRY(Memory::page_round_up(1024 * sizeof(u32) * 768 * 2));
    m_framebuffer_region = TRY(MM.allocate_kernel_region(m_framebuffer_address.page_base(), rounded_size, "Framebuffer"sv, Memory::Region::Access::ReadWrite));
    [[maybe_unused]] auto result = m_framebuffer_region->set_write_combine(true);
    m_framebuffer_data = m_framebuffer_region->vaddr().offset(m_framebuffer_address.offset_in_page()).as_ptr();
    // We assume safe resolution is 1024x768x32
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(m_framebuffer_address, 1024, 768, 1024 * sizeof(u32));
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

BochsDisplayConnector::BochsDisplayConnector(PhysicalAddress framebuffer_address)
    : DisplayConnector()
    , m_framebuffer_address(framebuffer_address)
{
}

BochsDisplayConnector::BochsDisplayConnector(PhysicalAddress framebuffer_address, NonnullOwnPtr<Memory::Region> registers_region, size_t registers_region_offset)
    : DisplayConnector()
    , m_framebuffer_address(framebuffer_address)
{
    m_registers.region = move(registers_region);
    m_registers.offset = registers_region_offset;
}

BochsDisplayConnector::IndexID BochsDisplayConnector::index_id() const
{
    return m_registers->bochs_regs.index_id;
}

void BochsDisplayConnector::set_framebuffer_to_big_endian_format()
{
    MutexLocker locker(m_modeset_lock);
    dbgln_if(BXVGA_DEBUG, "BochsDisplayConnector set_framebuffer_to_big_endian_format");
    full_memory_barrier();
    if (m_registers->extension_regs.region_size == 0xFFFFFFFF || m_registers->extension_regs.region_size == 0)
        return;
    full_memory_barrier();
    m_registers->extension_regs.framebuffer_byteorder = BOCHS_DISPLAY_BIG_ENDIAN;
    full_memory_barrier();
}

void BochsDisplayConnector::set_framebuffer_to_little_endian_format()
{
    MutexLocker locker(m_modeset_lock);
    dbgln_if(BXVGA_DEBUG, "BochsDisplayConnector set_framebuffer_to_little_endian_format");
    full_memory_barrier();
    if (m_registers->extension_regs.region_size == 0xFFFFFFFF || m_registers->extension_regs.region_size == 0)
        return;
    full_memory_barrier();
    m_registers->extension_regs.framebuffer_byteorder = BOCHS_DISPLAY_LITTLE_ENDIAN;
    full_memory_barrier();
}

ErrorOr<void> BochsDisplayConnector::set_safe_resolution()
{
    DisplayConnector::Resolution safe_resolution { 1024, 768, 32, 1024 * sizeof(u32), {} };
    return set_resolution(safe_resolution);
}

ErrorOr<void> BochsDisplayConnector::unblank()
{
    MutexLocker locker(m_modeset_lock);
    full_memory_barrier();
    m_registers->vga_ioports[0] = 0x20;
    full_memory_barrier();
    return {};
}

ErrorOr<size_t> BochsDisplayConnector::write_to_first_surface(u64 offset, UserOrKernelBuffer const& buffer, size_t length)
{
    VERIFY(m_control_lock.is_locked());
    if (offset + length > m_framebuffer_region->size())
        return Error::from_errno(EOVERFLOW);
    TRY(buffer.read(m_framebuffer_data + offset, 0, length));
    return length;
}

void BochsDisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}

void BochsDisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
}

ErrorOr<void> BochsDisplayConnector::flush_first_surface()
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> BochsDisplayConnector::set_y_offset(size_t y_offset)
{
    MutexLocker locker(m_modeset_lock);
    m_registers->bochs_regs.y_offset = y_offset;
    return {};
}

ErrorOr<DisplayConnector::Resolution> BochsDisplayConnector::get_resolution()
{
    MutexLocker locker(m_modeset_lock);
    return Resolution { m_registers->bochs_regs.xres, m_registers->bochs_regs.yres, 32, m_registers->bochs_regs.xres * sizeof(u32), {} };
}

ErrorOr<void> BochsDisplayConnector::set_resolution(Resolution const& resolution)
{
    MutexLocker locker(m_modeset_lock);
    VERIFY(m_framebuffer_console);
    size_t width = resolution.width;
    size_t height = resolution.height;
    size_t bpp = resolution.bpp;
    if (bpp != 32) {
        dbgln_if(BXVGA_DEBUG, "BochsDisplayConnector - no support for non-32bpp resolutions");
        return Error::from_errno(ENOTSUP);
    }

    if (Checked<size_t>::multiplication_would_overflow(width, height, sizeof(u32)))
        return EOVERFLOW;

    dbgln_if(BXVGA_DEBUG, "BochsDisplayConnector resolution registers set to - {}x{}", width, height);
    m_registers->bochs_regs.enable = 0;
    full_memory_barrier();
    m_registers->bochs_regs.xres = width;
    m_registers->bochs_regs.yres = height;
    m_registers->bochs_regs.virt_width = width;
    m_registers->bochs_regs.virt_height = height * 2;
    m_registers->bochs_regs.bpp = 32;
    full_memory_barrier();
    m_registers->bochs_regs.enable = to_underlying(BochsFramebufferSettings::Enabled) | to_underlying(BochsFramebufferSettings::LinearFramebuffer);
    full_memory_barrier();
    m_registers->bochs_regs.bank = 0;
    if (index_id().value() == VBE_DISPI_ID5) {
        set_framebuffer_to_little_endian_format();
    }

    if ((u16)width != m_registers->bochs_regs.xres || (u16)height != m_registers->bochs_regs.yres) {
        return Error::from_errno(ENOTIMPL);
    }
    auto rounded_size = TRY(Memory::page_round_up(width * sizeof(u32) * height * 2));
    m_framebuffer_region = TRY(MM.allocate_kernel_region(m_framebuffer_address.page_base(), rounded_size, "Framebuffer"sv, Memory::Region::Access::ReadWrite));
    [[maybe_unused]] auto result = m_framebuffer_region->set_write_combine(true);
    m_framebuffer_data = m_framebuffer_region->vaddr().offset(m_framebuffer_address.offset_in_page()).as_ptr();
    m_framebuffer_console->set_resolution(width, height, width * sizeof(u32));
    return {};
}

ErrorOr<ByteBuffer> BochsDisplayConnector::get_edid() const
{
    return ByteBuffer::copy(const_cast<u8 const*>(m_registers->edid_data), sizeof(m_registers->edid_data));
}

}
