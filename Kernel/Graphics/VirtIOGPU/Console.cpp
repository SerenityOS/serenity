/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/FramebufferDevice.h>
#include <Kernel/WorkQueue.h>

namespace Kernel::Graphics::VirtIOGPU {

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

NonnullRefPtr<Console> Console::initialize(RefPtr<FramebufferDevice> const& framebuffer_device)
{
    return adopt_ref(*new Console(framebuffer_device));
}

Console::Console(RefPtr<FramebufferDevice> const& framebuffer_device)
    : GenericFramebufferConsole(framebuffer_device->width(), framebuffer_device->height(), framebuffer_device->pitch())
    , m_framebuffer_device(framebuffer_device)
{
    enqueue_refresh_timer();
}

void Console::set_resolution(size_t width, size_t height, size_t pitch)
{
    auto did_set_resolution = m_framebuffer_device->set_head_resolution(0, width, height, pitch);
    VERIFY(!did_set_resolution.is_error());
}

void Console::flush(size_t x, size_t y, size_t width, size_t height)
{
    m_dirty_rect.union_rect(x, y, width, height);
}

void Console::enqueue_refresh_timer()
{
    NonnullRefPtr<Timer> refresh_timer = adopt_ref(*new Timer());
    refresh_timer->setup(CLOCK_MONOTONIC, refresh_interval, [this]() {
        auto rect = m_dirty_rect;
        if (rect.is_dirty()) {
            Protocol::Rect dirty_rect {
                .x = (u32)rect.x(),
                .y = (u32)rect.y(),
                .width = (u32)rect.width(),
                .height = (u32)rect.height(),
            };
            g_io_work->queue([this, dirty_rect]() {
                m_framebuffer_device->flush_dirty_window(dirty_rect, m_framebuffer_device->current_buffer());
                m_dirty_rect.clear();
            });
        }
        enqueue_refresh_timer();
    });
    TimerQueue::the().add_timer(move(refresh_timer));
}

void Console::enable()
{
    GenericFramebufferConsole::enable();
    m_width = m_framebuffer_device->width();
    m_height = m_framebuffer_device->height();
    m_pitch = m_framebuffer_device->pitch();
    m_dirty_rect.union_rect(0, 0, m_width, m_height);
}

u8* Console::framebuffer_data()
{
    return m_framebuffer_device->framebuffer_data();
}

}
