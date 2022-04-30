/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HardwareScreenBackend.h"
#include "ScreenBackend.h"
#include <AK/Try.h>
#include <Kernel/API/FB.h>
#include <LibCore/System.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>

namespace WindowServer {

HardwareScreenBackend::HardwareScreenBackend(String device)
    : m_device(move(device))
{
}

ErrorOr<void> HardwareScreenBackend::open()
{
    m_framebuffer_fd = TRY(Core::System::open(m_device.characters(), O_RDWR | O_CLOEXEC));

    GraphicsConnectorProperties properties;
    if (graphics_connector_get_properties(m_framebuffer_fd, &properties) < 0)
        return Error::from_syscall(String::formatted("failed to ioctl {}", m_device), errno);

    m_can_device_flush_buffers = (properties.partial_flushing_support != 0);
    m_can_device_flush_entire_framebuffer = (properties.flushing_support != 0);
    m_can_set_head_buffer = (properties.doublebuffer_support != 0);
    return {};
}

HardwareScreenBackend::~HardwareScreenBackend()
{
    if (m_framebuffer_fd >= 0) {
        close(m_framebuffer_fd);
        m_framebuffer_fd = -1;
    }
    if (m_framebuffer) {
        free(m_framebuffer);

        m_framebuffer = nullptr;
        m_size_in_bytes = 0;
    }
}

ErrorOr<void> HardwareScreenBackend::set_head_mode_setting(GraphicsHeadModeSetting mode_setting)
{

    GraphicsHeadModeSetting requested_mode_setting = mode_setting;
    auto rc = graphics_connector_set_head_mode_setting(m_framebuffer_fd, &requested_mode_setting);
    if (rc != 0) {
        dbgln("Failed to set backend mode setting: falling back to safe resolution");
        rc = graphics_connector_set_safe_head_mode_setting(m_framebuffer_fd);
        if (rc != 0) {
            dbgln("Failed to set backend safe mode setting: aborting");
            return Error::from_syscall("graphics_connector_set_safe_head_mode_setting", rc);
        }
        dbgln("Failed to set backend mode setting: falling back to safe resolution - success.");
    }

    return {};
}

ErrorOr<void> HardwareScreenBackend::unmap_framebuffer()
{
    if (m_framebuffer) {
        free(m_framebuffer);
    }
    return {};
}

ErrorOr<void> HardwareScreenBackend::write_all_contents(Gfx::IntRect const& virtual_rect)
{
    lseek(m_framebuffer_fd, 0, SEEK_SET);
    write(m_framebuffer_fd, scanline(0, 0), virtual_rect.height() * m_pitch);
    if (m_can_set_head_buffer) {
        if (lseek(m_framebuffer_fd, virtual_rect.height() * m_pitch, SEEK_SET) < 0) {
            VERIFY_NOT_REACHED();
        }

        if (write(m_framebuffer_fd, scanline(0, 0), virtual_rect.height() * m_pitch) < 0)
            VERIFY_NOT_REACHED();
    }
    return {};
}

ErrorOr<void> HardwareScreenBackend::map_framebuffer()
{
    GraphicsHeadModeSetting mode_setting {};
    memset(&mode_setting, 0, sizeof(GraphicsHeadModeSetting));
    int rc = graphics_connector_get_head_mode_setting(m_framebuffer_fd, &mode_setting);
    if (rc != 0) {
        return Error::from_syscall("graphics_connector_get_head_mode_setting", rc);
    }
    m_size_in_bytes = mode_setting.horizontal_stride * mode_setting.vertical_active * 2;
    m_framebuffer = (Gfx::ARGB32*)malloc(m_size_in_bytes);
    if (m_can_set_head_buffer) {
        // Note: fall back to assuming the second buffer starts right after the last line of the first
        // Note: for now, this calculation works quite well, so need to defer it to another function
        // that does ioctl to figure out the correct offset. If a Framebuffer device ever happens to
        // to set the second buffer at different location than this, we might need to consider bringing
        // back a function with ioctl to check this.
        m_back_buffer_offset = static_cast<size_t>(mode_setting.horizontal_stride) * mode_setting.vertical_active;
    } else {
        m_back_buffer_offset = 0;
    }

    return {};
}

ErrorOr<GraphicsHeadModeSetting> HardwareScreenBackend::get_head_mode_setting()
{
    GraphicsHeadModeSetting mode_setting {};
    memset(&mode_setting, 0, sizeof(GraphicsHeadModeSetting));
    int rc = graphics_connector_get_head_mode_setting(m_framebuffer_fd, &mode_setting);
    if (rc != 0) {
        return Error::from_syscall("graphics_connector_get_head_mode_setting", rc);
    }
    m_pitch = mode_setting.horizontal_stride;
    return mode_setting;
}

void HardwareScreenBackend::set_head_buffer(int head_index)
{
    VERIFY(m_can_set_head_buffer);
    VERIFY(head_index <= 1 && head_index >= 0);
    GraphicsHeadVerticalOffset offset { 0, 0 };
    if (head_index == 1)
        offset.offsetted = 1;
    int rc = fb_set_head_vertical_offset_buffer(m_framebuffer_fd, &offset);
    VERIFY(rc == 0);
}

ErrorOr<void> HardwareScreenBackend::flush_framebuffer_rects(int buffer_index, Span<FBRect const> flush_rects)
{
    int rc = fb_flush_buffers(m_framebuffer_fd, buffer_index, flush_rects.data(), (unsigned)flush_rects.size());
    if (rc == -ENOTSUP)
        m_can_device_flush_buffers = false;
    else
        return Error::from_syscall("fb_flush_buffers", rc);
    return {};
}

ErrorOr<void> HardwareScreenBackend::flush_framebuffer()
{
    int rc = fb_flush_head(m_framebuffer_fd);
    if (rc == -ENOTSUP)
        m_can_device_flush_entire_framebuffer = false;
    else
        return Error::from_syscall("fb_flush_head", rc);
    return {};
}
}
