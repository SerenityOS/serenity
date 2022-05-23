/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/VMWare/DisplayConnector.h>
#include <Kernel/TimerQueue.h>

namespace Kernel {

class VMWareFramebufferConsole final : public Graphics::GenericFramebufferConsole {
public:
    static NonnullRefPtr<VMWareFramebufferConsole> initialize(VMWareDisplayConnector& parent_display_connector);

    virtual void set_resolution(size_t width, size_t height, size_t pitch) override;
    virtual void flush(size_t x, size_t y, size_t width, size_t height) override;
    virtual void enable() override;

private:
    void enqueue_refresh_timer();
    virtual u8* framebuffer_data() override;

    VMWareFramebufferConsole(VMWareDisplayConnector const& parent_display_connector, DisplayConnector::ModeSetting current_resolution);
    RefPtr<VMWareDisplayConnector> m_parent_display_connector;
    bool m_dirty { false };
};

}
