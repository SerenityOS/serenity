/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOGPU.h>
#include <Kernel/TimerQueue.h>

namespace Kernel::Graphics {

class DirtyRect {
public:
    void union_rect(size_t x, size_t y, size_t width, size_t height);
    bool is_dirty() const { return m_is_dirty; }
    size_t x() const { return m_x0; }
    size_t y() const { return m_y0; }
    size_t width() const { return m_x1 - m_x0; }
    size_t height() const { return m_y1 - m_y0; }
    void clear() { m_is_dirty = false; }

private:
    bool m_is_dirty { false };
    size_t m_x0 { 0 };
    size_t m_y0 { 0 };
    size_t m_x1 { 0 };
    size_t m_y1 { 0 };
};

class VirtIOGPUConsole final : public GenericFramebufferConsole {
public:
    static NonnullRefPtr<VirtIOGPUConsole> initialize(RefPtr<VirtIOGPU>);

    virtual void set_resolution(size_t width, size_t height, size_t pitch) override;
    virtual void flush(size_t x, size_t y, size_t width, size_t height) override;
    virtual void enable() override;

private:
    void enqueue_refresh_timer();
    virtual u8* framebuffer_data() override
    {
        return m_framebuffer_region.unsafe_ptr()->vaddr().as_ptr();
    }

    VirtIOGPUConsole(RefPtr<VirtIOGPU>);
    WeakPtr<Region> m_framebuffer_region;
    RefPtr<VirtIOGPU> m_gpu;
    DirtyRect m_dirty_rect;
};

}
