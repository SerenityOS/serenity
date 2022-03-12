/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

namespace Graphics::VirtIOGPU {

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

}

namespace Graphics {

NonnullRefPtr<VirtIOGPUConsole> VirtIOGPUConsole::initialize(VirtIODisplayConnector& parent_display_connector)
{
    auto current_resolution = MUST(parent_display_connector.get_resolution());
    return adopt_ref(*new VirtIOGPUConsole(parent_display_connector, current_resolution));
}

VirtIOGPUConsole::VirtIOGPUConsole(VirtIODisplayConnector const& parent_display_connector, DisplayConnector::Resolution current_resolution)
    : GenericFramebufferConsole(current_resolution.width, current_resolution.height, current_resolution.pitch)
    , m_parent_display_connector(parent_display_connector)
{
    enqueue_refresh_timer();
}

void VirtIOGPUConsole::set_resolution(size_t, size_t, size_t)
{
    // FIXME: Update some values here?
}

void VirtIOGPUConsole::flush(size_t x, size_t y, size_t width, size_t height)
{
    m_dirty_rect.union_rect(x, y, width, height);
}

void VirtIOGPUConsole::enqueue_refresh_timer()
{
    NonnullRefPtr<Timer> refresh_timer = adopt_ref(*new Timer());
    refresh_timer->setup(CLOCK_MONOTONIC, Graphics::VirtIOGPU::refresh_interval, [this]() {
        auto rect = m_dirty_rect;
        if (m_enabled.load() && rect.is_dirty()) {
            g_io_work->queue([this]() {
                {
                    MutexLocker locker(m_parent_display_connector->m_flushing_lock);
                    MUST(m_parent_display_connector->flush_first_surface());
                }
                m_dirty_rect.clear();
            });
        }
        enqueue_refresh_timer();
    });
    TimerQueue::the().add_timer(move(refresh_timer));
}

void VirtIOGPUConsole::enable()
{
    auto current_resolution = MUST(m_parent_display_connector->get_resolution());
    GenericFramebufferConsole::enable();
    m_width = current_resolution.width;
    m_height = current_resolution.height;
    m_pitch = current_resolution.pitch;
    m_dirty_rect.union_rect(0, 0, m_width, m_height);
}

u8* VirtIOGPUConsole::framebuffer_data()
{
    return m_parent_display_connector->framebuffer_data();
}

}
}
