/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGA/DisplayConnector.h>

namespace Kernel {

NonnullRefPtr<GenericDisplayConnector> GenericDisplayConnector::must_create_with_preset_resolution(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
{
    auto device_or_error = DeviceManagement::try_create_device<GenericDisplayConnector>(framebuffer_address, width, height, pitch);
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    MUST(connector->create_attached_framebuffer_console());
    MUST(connector->initialize_edid_for_generic_monitor());
    return connector;
}

GenericDisplayConnector::GenericDisplayConnector(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
    : DisplayConnector()
    , m_framebuffer_address(framebuffer_address)
{
    m_current_mode_setting.horizontal_active = width;
    m_current_mode_setting.vertical_active = height;
    m_current_mode_setting.horizontal_stride = pitch;
}

ErrorOr<void> GenericDisplayConnector::create_attached_framebuffer_console()
{
    auto width = m_current_mode_setting.horizontal_active;
    auto height = m_current_mode_setting.vertical_active;
    auto pitch = m_current_mode_setting.horizontal_stride;

    auto rounded_size = TRY(Memory::page_round_up(pitch * height));
    m_framebuffer_region = TRY(MM.allocate_kernel_region(m_framebuffer_address.page_base(), rounded_size, "Framebuffer"sv, Memory::Region::Access::ReadWrite));
    [[maybe_unused]] auto result = m_framebuffer_region->set_write_combine(true);
    m_framebuffer_data = m_framebuffer_region->vaddr().offset(m_framebuffer_address.offset_in_page()).as_ptr();
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(m_framebuffer_address, width, height, pitch);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

ErrorOr<size_t> GenericDisplayConnector::write_to_first_surface(u64 offset, UserOrKernelBuffer const& buffer, size_t length)
{
    VERIFY(m_control_lock.is_locked());
    if (offset + length > m_framebuffer_region->size())
        return Error::from_errno(EOVERFLOW);
    TRY(buffer.read(m_framebuffer_data + offset, 0, length));
    return length;
}

void GenericDisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}

void GenericDisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
}

ErrorOr<void> GenericDisplayConnector::flush_first_surface()
{
    return Error::from_errno(ENOTSUP);
}

}
