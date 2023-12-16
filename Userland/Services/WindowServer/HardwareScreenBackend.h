/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ScreenBackend.h"
#include "ScreenLayout.h"
#include <AK/ByteString.h>
#include <AK/Error.h>
#include <AK/Span.h>
#include <sys/ioctl.h>

namespace WindowServer {
class HardwareScreenBackend : public ScreenBackend {
public:
    virtual ~HardwareScreenBackend();

    explicit HardwareScreenBackend(ByteString device);

    virtual ErrorOr<void> open() override;

    virtual void set_head_buffer(int index) override;

    virtual ErrorOr<void> flush_framebuffer_rects(int buffer_index, ReadonlySpan<FBRect> rects) override;

    virtual ErrorOr<void> flush_framebuffer() override;

    virtual ErrorOr<void> unmap_framebuffer() override;
    virtual ErrorOr<void> map_framebuffer() override;

    virtual ErrorOr<void> set_safe_head_mode_setting() override;

    virtual ErrorOr<void> set_head_mode_setting(GraphicsHeadModeSetting) override;
    virtual ErrorOr<GraphicsHeadModeSetting> get_head_mode_setting() override;

    ByteString m_device {};
    int m_display_connector_fd { -1 };

    Gfx::ARGB32* scanline(int buffer_index, int y) const
    {
        size_t buffer_offset = buffer_index == 1 ? m_back_buffer_offset : 0;
        return reinterpret_cast<Gfx::ARGB32*>(((u8*)m_framebuffer) + buffer_offset + (y * m_pitch));
    }
};

}
