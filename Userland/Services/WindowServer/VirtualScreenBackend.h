/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ScreenBackend.h"
#include "ScreenLayout.h"
#include <AK/Error.h>
#include <AK/Span.h>

namespace WindowServer {

class VirtualScreenBackend : public ScreenBackend {
public:
    virtual ~VirtualScreenBackend();

    VirtualScreenBackend() = default;

private:
    friend class Screen;

    virtual ErrorOr<void> open() override;

    virtual void set_head_buffer(int index) override;

    virtual ErrorOr<void> flush_framebuffer_rects(int, ReadonlySpan<FBRect>) override { return {}; }

    virtual ErrorOr<void> flush_framebuffer() override { return {}; }

    virtual ErrorOr<void> unmap_framebuffer() override;
    virtual ErrorOr<void> map_framebuffer() override;

    virtual ErrorOr<void> set_safe_head_mode_setting() override;

    virtual ErrorOr<void> set_head_mode_setting(GraphicsHeadModeSetting) override;
    virtual ErrorOr<GraphicsHeadModeSetting> get_head_mode_setting() override;

    int m_height { 0 };
    int m_width { 0 };
    bool m_first_buffer_active { true };
};

}
