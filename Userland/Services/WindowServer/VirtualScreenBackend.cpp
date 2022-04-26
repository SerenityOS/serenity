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

ErrorOr<void> VirtualScreenBackend::set_head_resolution(FBHeadResolution resolution)
{
    if (resolution.head_index != 0)
        return Error::from_string_literal("Only head index 0 is supported."sv);
    m_height = resolution.height;

    if (resolution.pitch == 0)
        resolution.pitch = static_cast<int>(resolution.width * sizeof(Gfx::ARGB32));
    m_pitch = resolution.pitch;
    if (static_cast<int>(resolution.width * sizeof(Gfx::ARGB32)) != resolution.pitch)
        return Error::from_string_literal("Unsupported pitch"sv);

    m_width = resolution.width;
    return {};
}

ErrorOr<FBHeadProperties> VirtualScreenBackend::get_head_properties()
{
    return FBHeadProperties {
        .head_index = 0,
        .pitch = static_cast<unsigned>(m_pitch),
        .width = static_cast<unsigned>(m_width),
        .height = static_cast<unsigned>(m_height),
        .offset = static_cast<unsigned>(m_first_buffer_active ? 0 : m_back_buffer_offset),
        .buffer_length = static_cast<unsigned>(m_size_in_bytes),
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
