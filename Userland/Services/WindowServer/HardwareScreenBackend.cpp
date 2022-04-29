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

HardwareScreenBackend::HardwareScreenBackend(String device, bool display_connector_device_backed)
    : m_device(move(device))
    , display_connector_device_backed(display_connector_device_backed)
{
}

ErrorOr<void> HardwareScreenBackend::open()
{
    m_framebuffer_fd = TRY(Core::System::open(m_device.characters(), O_RDWR | O_CLOEXEC));

    FBProperties properties;
    if (fb_get_properties(m_framebuffer_fd, &properties) < 0)
        return Error::from_syscall(String::formatted("failed to ioctl {}", m_device), errno);

    m_can_device_flush_buffers = (properties.partial_flushing_support != 0);
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
        if (!display_connector_device_backed)
            MUST(Core::System::munmap(m_framebuffer, m_size_in_bytes));
        else
            free(m_framebuffer);

        m_framebuffer = nullptr;
        m_size_in_bytes = 0;
    }
}

ErrorOr<void> HardwareScreenBackend::set_head_resolution(FBHeadResolution resolution)
{
    if (!display_connector_device_backed) {
        auto rc = fb_set_resolution(m_framebuffer_fd, &resolution);
        if (rc != 0)
            return Error::from_syscall("fb_set_resolution", rc);
    } else {
        FBHeadModeSetting mode_setting;
        memset(&mode_setting, 0, sizeof(FBHeadModeSetting));
        mode_setting.horizontal_active = resolution.width;
        mode_setting.vertical_active = resolution.height;
        mode_setting.horizontal_stride = resolution.pitch;
        auto rc = fb_set_head_mode_setting(m_framebuffer_fd, &mode_setting);
        if (rc != 0) {
            dbgln("Failed to set backend mode setting: falling back to safe resolution");
            rc = fb_set_safe_head_mode_setting(m_framebuffer_fd);
            if (rc != 0) {
                dbgln("Failed to set backend safe mode setting: aborting");
                return Error::from_syscall("fb_set_safe_head_mode_setting", rc);
            }
            dbgln("Failed to set backend mode setting: falling back to safe resolution - success.");
        }
    }

    return {};
}

ErrorOr<void> HardwareScreenBackend::unmap_framebuffer()
{
    if (m_framebuffer) {
        if (!display_connector_device_backed) {
            size_t previous_size_in_bytes = m_size_in_bytes;
            return Core::System::munmap(m_framebuffer, previous_size_in_bytes);
        } else {
            free(m_framebuffer);
        }
    }
    return {};
}

ErrorOr<void> HardwareScreenBackend::write_all_contents(Gfx::IntRect const& virtual_rect)
{
    if (!display_connector_device_backed)
        return {};
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
    if (!display_connector_device_backed) {
        FBHeadProperties properties;
        properties.head_index = 0;
        int rc = fb_get_head_properties(m_framebuffer_fd, &properties);
        if (rc != 0)
            return Error::from_syscall("fb_get_head_properties", rc);
        m_size_in_bytes = properties.buffer_length;

        m_framebuffer = (Gfx::ARGB32*)TRY(Core::System::mmap(nullptr, m_size_in_bytes, PROT_READ | PROT_WRITE, MAP_SHARED, m_framebuffer_fd, 0));

        if (m_can_set_head_buffer) {
            // Note: fall back to assuming the second buffer starts right after the last line of the first
            // Note: for now, this calculation works quite well, so need to defer it to another function
            // that does ioctl to figure out the correct offset. If a Framebuffer device ever happens to
            // to set the second buffer at different location than this, we might need to consider bringing
            // back a function with ioctl to check this.
            m_back_buffer_offset = static_cast<size_t>(properties.pitch) * properties.height;
        } else {
            m_back_buffer_offset = 0;
        }
    } else {
        FBHeadModeSetting mode_setting {};
        memset(&mode_setting, 0, sizeof(FBHeadModeSetting));
        int rc = fb_get_head_mode_setting(m_framebuffer_fd, &mode_setting);
        if (rc != 0) {
            return Error::from_syscall("fb_get_head_mode_setting", rc);
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
    }

    return {};
}

ErrorOr<FBHeadProperties> HardwareScreenBackend::get_head_properties()
{
    if (!display_connector_device_backed) {
        FBHeadProperties properties;
        properties.head_index = 0;
        int rc = fb_get_head_properties(m_framebuffer_fd, &properties);
        if (rc != 0)
            return Error::from_syscall("fb_get_head_properties", rc);
        m_pitch = static_cast<int>(properties.pitch);
        return properties;
    } else {
        FBHeadModeSetting mode_setting {};
        memset(&mode_setting, 0, sizeof(FBHeadModeSetting));
        int rc = fb_get_head_mode_setting(m_framebuffer_fd, &mode_setting);
        if (rc != 0) {
            return Error::from_syscall("fb_get_head_mode_setting", rc);
        }
        m_pitch = mode_setting.horizontal_stride;
        // Note: We translate (for now, until Framebuffer devices are removed) the FBHeadModeSetting
        // structure to FBHeadProperties.
        FBHeadProperties properties;
        properties.head_index = 0;
        properties.pitch = mode_setting.horizontal_stride;
        properties.width = mode_setting.horizontal_active;
        properties.height = mode_setting.vertical_active;
        properties.offset = 0;
        properties.buffer_length = mode_setting.horizontal_stride * mode_setting.vertical_active * 2;
        return properties;
    }
    VERIFY_NOT_REACHED();
}

void HardwareScreenBackend::set_head_buffer(int head_index)
{
    VERIFY(m_can_set_head_buffer);
    VERIFY(head_index <= 1 && head_index >= 0);
    FBHeadVerticalOffset offset { 0, 0 };
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

}
