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
        MUST(Core::System::munmap(m_framebuffer, m_size_in_bytes));

        m_framebuffer = nullptr;
        m_size_in_bytes = 0;
    }
}

ErrorOr<void> HardwareScreenBackend::set_head_resolution(FBHeadResolution resolution)
{
    auto rc = fb_set_resolution(m_framebuffer_fd, &resolution);
    if (rc != 0)
        return Error::from_syscall("fb_set_resolution", rc);
    return {};
}

ErrorOr<void> HardwareScreenBackend::unmap_framebuffer()
{
    if (m_framebuffer) {
        size_t previous_size_in_bytes = m_size_in_bytes;
        return Core::System::munmap(m_framebuffer, previous_size_in_bytes);
    }
    return {};
}

ErrorOr<void> HardwareScreenBackend::map_framebuffer()
{
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

    return {};
}

ErrorOr<FBHeadProperties> HardwareScreenBackend::get_head_properties()
{
    FBHeadProperties properties;
    properties.head_index = 0;
    int rc = fb_get_head_properties(m_framebuffer_fd, &properties);
    if (rc != 0)
        return Error::from_syscall("fb_get_head_properties", rc);
    m_pitch = static_cast<int>(properties.pitch);
    return properties;
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
