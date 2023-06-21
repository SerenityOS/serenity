/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/VirtIO/DisplayConnector.h>
#include <Kernel/Time/TimerQueue.h>

namespace Kernel::Graphics::VirtIOGPU {

class Console final : public GenericFramebufferConsole {
public:
    static NonnullLockRefPtr<Console> initialize(VirtIODisplayConnector& parent_display_connector);

    virtual void set_resolution(size_t width, size_t height, size_t pitch) override;
    virtual void flush(size_t x, size_t y, size_t width, size_t height) override;
    virtual void enable() override;

    virtual void set_cursor(size_t x, size_t y) override;

private:
    void enqueue_refresh_timer();
    virtual u8* framebuffer_data() override;

    virtual void hide_cursor() override;
    virtual void show_cursor() override;

    Console(VirtIODisplayConnector const& parent_display_connector, DisplayConnector::ModeSetting current_resolution);
    NonnullLockRefPtr<VirtIODisplayConnector> m_parent_display_connector;
    bool m_dirty { false };
};

}
