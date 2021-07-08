/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/VirtIOFrameBufferDevice.h>
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

NonnullRefPtr<VirtIOGPUConsole> VirtIOGPUConsole::initialize(RefPtr<VirtIOFrameBufferDevice> const& framebuffer_device)
{
    return adopt_ref(*new VirtIOGPUConsole(framebuffer_device));
}

VirtIOGPUConsole::VirtIOGPUConsole(RefPtr<VirtIOFrameBufferDevice> const& framebuffer_device)
    : GenericFramebufferConsole(framebuffer_device->width(), framebuffer_device->height(), framebuffer_device->pitch())
    , m_framebuffer_device(framebuffer_device)
{
    enqueue_refresh_timer();
}

void VirtIOGPUConsole::set_resolution(size_t width, size_t height, size_t)
{
    auto did_set_resolution = m_framebuffer_device->try_to_set_resolution(width, height);
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
            Processor::deferred_call_queue([this, dirty_rect]() {
                m_framebuffer_device->flush_dirty_window(dirty_rect, m_framebuffer_device->current_buffer());
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
    m_width = m_framebuffer_device->width();
    m_height = m_framebuffer_device->height();
    m_pitch = m_framebuffer_device->pitch();
    m_dirty_rect.union_rect(0, 0, m_width, m_height);
}

u8* VirtIOGPUConsole::framebuffer_data()
{
    return m_framebuffer_device->framebuffer_data();
}

}
