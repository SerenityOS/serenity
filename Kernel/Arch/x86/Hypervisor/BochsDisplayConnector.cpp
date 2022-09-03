/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <Kernel/Arch/x86/Hypervisor/BochsDisplayConnector.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>

namespace Kernel {

NonnullLockRefPtr<BochsDisplayConnector> BochsDisplayConnector::must_create(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, bool virtual_box_hardware)
{
    auto device_or_error = DeviceManagement::try_create_device<BochsDisplayConnector>(framebuffer_address, framebuffer_resource_size);
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    MUST(connector->create_attached_framebuffer_console());
    if (virtual_box_hardware)
        MUST(connector->initialize_edid_for_generic_monitor(Array<u8, 3> { 'V', 'B', 'X' }));
    else
        MUST(connector->initialize_edid_for_generic_monitor({}));
    return connector;
}

BochsDisplayConnector::BochsDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size)
    : DisplayConnector(framebuffer_address, framebuffer_resource_size, false)
{
}

ErrorOr<void> BochsDisplayConnector::create_attached_framebuffer_console()
{
    // We assume safe resolution is 1024x768x32
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(m_framebuffer_address.value(), 1024, 768, 1024 * sizeof(u32));
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
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

BochsDisplayConnector::IndexID BochsDisplayConnector::index_id() const
{
    return get_register_with_io(0);
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

ErrorOr<void> BochsDisplayConnector::set_safe_mode_setting()
{
    DisplayConnector::ModeSetting safe_mode_set {
        .horizontal_stride = 1024 * sizeof(u32),
        .pixel_clock_in_khz = 0, // Note: There's no pixel clock in paravirtualized hardware
        .horizontal_active = 1024,
        .horizontal_front_porch_pixels = 0, // Note: There's no horizontal_front_porch_pixels in paravirtualized hardware
        .horizontal_sync_time_pixels = 0,   // Note: There's no horizontal_sync_time_pixels in paravirtualized hardware
        .horizontal_blank_pixels = 0,       // Note: There's no horizontal_blank_pixels in paravirtualized hardware
        .vertical_active = 768,
        .vertical_front_porch_lines = 0, // Note: There's no vertical_front_porch_lines in paravirtualized hardware
        .vertical_sync_time_lines = 0,   // Note: There's no vertical_sync_time_lines in paravirtualized hardware
        .vertical_blank_lines = 0,       // Note: There's no vertical_blank_lines in paravirtualized hardware
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };
    return set_mode_setting(safe_mode_set);
}

ErrorOr<void> BochsDisplayConnector::set_mode_setting(ModeSetting const& mode_setting)
{
    SpinlockLocker locker(m_modeset_lock);
    size_t width = mode_setting.horizontal_active;
    size_t height = mode_setting.vertical_active;

    dbgln_if(BXVGA_DEBUG, "BochsDisplayConnector resolution registers set to - {}x{}", width, height);

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
    auto current_horizontal_active = get_register_with_io(to_underlying(BochsDISPIRegisters::XRES));
    auto current_vertical_active = get_register_with_io(to_underlying(BochsDISPIRegisters::YRES));
    DisplayConnector::ModeSetting mode_set {
        .horizontal_stride = current_horizontal_active * sizeof(u32),
        .pixel_clock_in_khz = 0, // Note: There's no pixel clock in paravirtualized hardware
        .horizontal_active = current_horizontal_active,
        .horizontal_front_porch_pixels = 0, // Note: There's no horizontal_front_porch_pixels in paravirtualized hardware
        .horizontal_sync_time_pixels = 0,   // Note: There's no horizontal_sync_time_pixels in paravirtualized hardware
        .horizontal_blank_pixels = 0,       // Note: There's no horizontal_blank_pixels in paravirtualized hardware
        .vertical_active = current_vertical_active,
        .vertical_front_porch_lines = 0, // Note: There's no vertical_front_porch_lines in paravirtualized hardware
        .vertical_sync_time_lines = 0,   // Note: There's no vertical_sync_time_lines in paravirtualized hardware
        .vertical_blank_lines = 0,       // Note: There's no vertical_blank_lines in paravirtualized hardware
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };
    m_current_mode_setting = mode_set;
    return {};
}

ErrorOr<void> BochsDisplayConnector::set_y_offset(size_t)
{
    // Note: Although when using this device on QEMU we can actually set the horizontal and vertical offsets
    // with IO ports, this class is meant to be used for plain old Bochs graphics which might not support
    // this feature at all.
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<void> BochsDisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

}
