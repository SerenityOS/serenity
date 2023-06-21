/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "VirtualScreenBackend.h"
#include "ScreenBackend.h"
#include <AK/Try.h>
#include <AK/kmalloc.h>
#include <LibGfx/Color.h>

namespace WindowServer {

VirtualScreenBackend::~VirtualScreenBackend()
{
    MUST(unmap_framebuffer());
}

ErrorOr<void> VirtualScreenBackend::open()
{
    m_can_device_flush_buffers = true;
    m_can_set_head_buffer = true;
    return {};
}

void VirtualScreenBackend::set_head_buffer(int index)
{
    VERIFY(index <= 1 && index >= 0);
    m_first_buffer_active = index == 0;
}

ErrorOr<void> VirtualScreenBackend::set_safe_head_mode_setting()
{
    return {};
}

ErrorOr<void> VirtualScreenBackend::set_head_mode_setting(GraphicsHeadModeSetting mode_setting)
{
    m_height = mode_setting.vertical_active;

    if (mode_setting.horizontal_stride == 0)
        mode_setting.horizontal_stride = static_cast<int>(mode_setting.horizontal_active * sizeof(Gfx::ARGB32));
    m_pitch = mode_setting.horizontal_stride;
    if (static_cast<int>(mode_setting.horizontal_active * sizeof(Gfx::ARGB32)) != mode_setting.horizontal_stride)
        return Error::from_string_literal("Unsupported pitch");

    m_width = mode_setting.horizontal_active;
    return {};
}

ErrorOr<GraphicsHeadModeSetting> VirtualScreenBackend::get_head_mode_setting()
{
    return GraphicsHeadModeSetting {
        .horizontal_stride = m_pitch,
        .pixel_clock_in_khz = 0,
        .horizontal_active = m_width,
        .horizontal_front_porch_pixels = 0,
        .horizontal_sync_time_pixels = 0,
        .horizontal_blank_pixels = 0,
        .vertical_active = m_height,
        .vertical_front_porch_lines = 0,
        .vertical_sync_time_lines = 0,
        .vertical_blank_lines = 0,
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };
}

ErrorOr<void> VirtualScreenBackend::map_framebuffer()
{
    m_size_in_bytes = static_cast<unsigned long>(m_pitch) * m_height * 2;
    m_framebuffer = new Gfx::ARGB32[static_cast<unsigned long>(m_width) * m_height * 2];
    if (m_framebuffer == nullptr)
        return Error::from_errno(ENOMEM);

    m_back_buffer_offset = m_size_in_bytes / 2;
    m_first_buffer_active = true;

    return {};
}

ErrorOr<void> VirtualScreenBackend::unmap_framebuffer()
{
    delete[] m_framebuffer;
    return {};
}

}
