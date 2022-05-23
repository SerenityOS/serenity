/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/Bochs/QEMUDisplayConnector.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <LibEDID/Definitions.h>

namespace Kernel {

NonnullRefPtr<QEMUDisplayConnector> QEMUDisplayConnector::must_create(PhysicalAddress framebuffer_address, Memory::TypedMapping<BochsDisplayMMIORegisters volatile> registers_mapping)
{
    auto device_or_error = DeviceManagement::try_create_device<QEMUDisplayConnector>(framebuffer_address, move(registers_mapping));
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    MUST(connector->create_attached_framebuffer_console());
    MUST(connector->fetch_and_initialize_edid());
    return connector;
}

ErrorOr<void> QEMUDisplayConnector::fetch_and_initialize_edid()
{
    Array<u8, 128> bochs_edid;
    static_assert(sizeof(BochsDisplayMMIORegisters::edid_data) >= sizeof(EDID::Definitions::EDID));
    memcpy(bochs_edid.data(), (u8 const*)(m_registers.base_address().offset(__builtin_offsetof(BochsDisplayMMIORegisters, edid_data)).as_ptr()), sizeof(bochs_edid));
    set_edid_bytes(bochs_edid);
    return {};
}

QEMUDisplayConnector::QEMUDisplayConnector(PhysicalAddress framebuffer_address, Memory::TypedMapping<BochsDisplayMMIORegisters volatile> registers_mapping)
    : BochsDisplayConnector(framebuffer_address)
    , m_registers(move(registers_mapping))
{
}

QEMUDisplayConnector::IndexID QEMUDisplayConnector::index_id() const
{
    return m_registers->bochs_regs.index_id;
}

void QEMUDisplayConnector::set_framebuffer_to_big_endian_format()
{
    VERIFY(m_modeset_lock.is_locked());
    dbgln_if(BXVGA_DEBUG, "QEMUDisplayConnector set_framebuffer_to_big_endian_format");
    full_memory_barrier();
    if (m_registers->extension_regs.region_size == 0xFFFFFFFF || m_registers->extension_regs.region_size == 0)
        return;
    full_memory_barrier();
    m_registers->extension_regs.framebuffer_byteorder = BOCHS_DISPLAY_BIG_ENDIAN;
    full_memory_barrier();
}

void QEMUDisplayConnector::set_framebuffer_to_little_endian_format()
{
    VERIFY(m_modeset_lock.is_locked());
    dbgln_if(BXVGA_DEBUG, "QEMUDisplayConnector set_framebuffer_to_little_endian_format");
    full_memory_barrier();
    if (m_registers->extension_regs.region_size == 0xFFFFFFFF || m_registers->extension_regs.region_size == 0)
        return;
    full_memory_barrier();
    m_registers->extension_regs.framebuffer_byteorder = BOCHS_DISPLAY_LITTLE_ENDIAN;
    full_memory_barrier();
}

ErrorOr<void> QEMUDisplayConnector::unblank()
{
    SpinlockLocker locker(m_modeset_lock);
    full_memory_barrier();
    m_registers->vga_ioports[0] = 0x20;
    full_memory_barrier();
    return {};
}

ErrorOr<void> QEMUDisplayConnector::set_y_offset(size_t y_offset)
{
    VERIFY(m_modeset_lock.is_locked());
    m_registers->bochs_regs.y_offset = y_offset;
    return {};
}

ErrorOr<void> QEMUDisplayConnector::set_mode_setting(ModeSetting const& mode_setting)
{
    SpinlockLocker locker(m_modeset_lock);
    VERIFY(m_framebuffer_console);
    size_t width = mode_setting.horizontal_active;
    size_t height = mode_setting.vertical_active;

    if (Checked<size_t>::multiplication_would_overflow(width, height, sizeof(u32)))
        return EOVERFLOW;

    dbgln_if(BXVGA_DEBUG, "QEMUDisplayConnector resolution registers set to - {}x{}", width, height);
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

    DisplayConnector::ModeSetting mode_set {
        .horizontal_stride = m_registers->bochs_regs.xres * sizeof(u32),
        .pixel_clock_in_khz = 0, // Note: There's no pixel clock in paravirtualized hardware
        .horizontal_active = m_registers->bochs_regs.xres,
        .horizontal_front_porch_pixels = 0, // Note: There's no horizontal_front_porch_pixels in paravirtualized hardware
        .horizontal_sync_time_pixels = 0,   // Note: There's no horizontal_sync_time_pixels in paravirtualized hardware
        .horizontal_blank_pixels = 0,       // Note: There's no horizontal_blank_pixels in paravirtualized hardware
        .vertical_active = m_registers->bochs_regs.yres,
        .vertical_front_porch_lines = 0, // Note: There's no vertical_front_porch_lines in paravirtualized hardware
        .vertical_sync_time_lines = 0,   // Note: There's no vertical_sync_time_lines in paravirtualized hardware
        .vertical_blank_lines = 0,       // Note: There's no vertical_blank_lines in paravirtualized hardware
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };

    m_current_mode_setting = mode_set;
    return {};
}

}
