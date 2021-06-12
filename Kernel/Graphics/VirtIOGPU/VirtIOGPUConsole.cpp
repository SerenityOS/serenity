/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/VirtIOGPUConsole.h>
#include <Kernel/WorkQueue.h>

namespace Kernel::Graphics {

constexpr static AK::Time refresh_interval = AK::Time::from_milliseconds(16);

void DirtyRect::union_rect(size_t x, size_t y, size_t width, size_t height)
{
    if (width == 0 || height == 0)
        return;
    if (m_is_dirty) {
        m_x0 = min(x, m_x0);
        m_y0 = min(y, m_y0);
        m_x1 = max(x + width, m_x1);
        m_y1 = max(y + height, m_y1);
    } else {
        m_is_dirty = true;
        m_x0 = x;
        m_y0 = y;
        m_x1 = x + width;
        m_y1 = y + height;
    }
}

NonnullRefPtr<VirtIOGPUConsole> VirtIOGPUConsole::initialize(RefPtr<VirtIOGPU> gpu)
{
    return adopt_ref(*new VirtIOGPUConsole(gpu));
}

VirtIOGPUConsole::VirtIOGPUConsole(RefPtr<VirtIOGPU> gpu)
    : GenericFramebufferConsole(gpu->framebuffer_width(), gpu->framebuffer_height(), gpu->framebuffer_pitch())
    , m_gpu(gpu)
{
    m_framebuffer_region = gpu->framebuffer_region();
    enqueue_refresh_timer();
}

void VirtIOGPUConsole::set_resolution(size_t width, size_t height, size_t)
{
    auto did_set_resolution = m_gpu->try_to_set_resolution(width, height);
    VERIFY(did_set_resolution);
}

void VirtIOGPUConsole::flush(size_t x, size_t y, size_t width, size_t height)
{
    m_dirty_rect.union_rect(x, y, width, height);
}

void VirtIOGPUConsole::enqueue_refresh_timer()
{
    NonnullRefPtr<Timer> refresh_timer = adopt_ref(*new Timer());
    refresh_timer->setup(CLOCK_MONOTONIC, refresh_interval, [this]() {
        auto rect = m_dirty_rect;
        if (rect.is_dirty()) {
            VirtIOGPURect dirty_rect {
                .x = (u32)rect.x(),
                .y = (u32)rect.y(),
                .width = (u32)rect.width(),
                .height = (u32)rect.height(),
            };
            g_io_work->queue([this, dirty_rect]() {
                m_gpu->flush_dirty_window(dirty_rect);
                m_dirty_rect.clear();
            });
        }
        enqueue_refresh_timer();
    });
    TimerQueue::the().add_timer(move(refresh_timer));
}

void VirtIOGPUConsole::enable()
{
    GenericFramebufferConsole::enable();
    m_width = m_gpu->framebuffer_width();
    m_height = m_gpu->framebuffer_height();
    m_pitch = m_gpu->framebuffer_pitch();
    m_dirty_rect.union_rect(0, 0, m_width, m_height);
}

}
