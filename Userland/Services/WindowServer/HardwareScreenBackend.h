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
#include <AK/String.h>
#include <sys/ioctl_numbers.h>

namespace WindowServer {
class HardwareScreenBackend : public ScreenBackend {
public:
    virtual ~HardwareScreenBackend();

    HardwareScreenBackend(String device);

    virtual ErrorOr<void> open() override;

    virtual void set_head_buffer(int index) override;

    virtual ErrorOr<void> flush_framebuffer_rects(int buffer_index, Span<FBRect const> rects) override;

    virtual ErrorOr<void> unmap_framebuffer() override;
    virtual ErrorOr<void> map_framebuffer() override;

    virtual ErrorOr<void> set_head_resolution(FBHeadResolution) override;
    virtual ErrorOr<FBHeadProperties> get_head_properties() override;

    String m_device {};
    int m_framebuffer_fd { -1 };
};

}
