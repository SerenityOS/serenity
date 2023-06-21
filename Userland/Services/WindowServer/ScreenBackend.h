/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ScreenLayout.h"
#include <AK/Error.h>
#include <AK/Span.h>
#include <LibGfx/Color.h>
#include <sys/ioctl.h>

namespace WindowServer {

// Handles low-level device interfacing for the screen.
// ScreenBackend is a thin transparent wrapper around framebuffer-related data which is responsible for setting up this data,
// tearing it down, changing its properties like size, and performing flushes.
// The screen is intended to directly access the members to perform its function, but it only ever reads from anything
// except the data in the framebuffer memory.
class ScreenBackend {
public:
    virtual ~ScreenBackend() = default;

    virtual ErrorOr<void> open() = 0;

    virtual void set_head_buffer(int index) = 0;

    virtual ErrorOr<void> flush_framebuffer_rects(int buffer_index, ReadonlySpan<FBRect> rects) = 0;

    virtual ErrorOr<void> unmap_framebuffer() = 0;
    virtual ErrorOr<void> map_framebuffer() = 0;

    virtual ErrorOr<void> flush_framebuffer() = 0;

    virtual ErrorOr<void> set_head_mode_setting(GraphicsHeadModeSetting) = 0;
    virtual ErrorOr<void> set_safe_head_mode_setting() = 0;
    virtual ErrorOr<GraphicsHeadModeSetting> get_head_mode_setting() = 0;

    bool m_can_device_flush_buffers { true };
    bool m_can_device_flush_entire_framebuffer { true };
    bool m_can_set_head_buffer { false };

    Gfx::ARGB32* m_framebuffer { nullptr };
    size_t m_size_in_bytes { 0 };
    size_t m_max_size_in_bytes { 0 };
    size_t m_back_buffer_offset { 0 };

    int m_pitch { 0 };
};

}
