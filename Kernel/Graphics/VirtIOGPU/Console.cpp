/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/Console.h>
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

NonnullRefPtr<Console> Console::initialize(VirtIODisplayConnector& parent_display_connector)
{
    auto current_resolution = parent_display_connector.current_mode_setting();
    return adopt_ref(*new Console(parent_display_connector, current_resolution));
}

Console::Console(VirtIODisplayConnector const& parent_display_connector, DisplayConnector::ModeSetting current_resolution)
    : GenericFramebufferConsole(current_resolution.horizontal_active, current_resolution.vertical_active, current_resolution.horizontal_stride)
    , m_parent_display_connector(parent_display_connector)
{
    enqueue_refresh_timer();
}

void Console::set_resolution(size_t, size_t, size_t)
{
    // FIXME: Update some values here?
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
        if (m_enabled.load() && rect.is_dirty()) {
            MUST(g_io_work->try_queue([this]() {
                {
                    MutexLocker locker(m_parent_display_connector->m_flushing_lock);
                    MUST(m_parent_display_connector->flush_first_surface());
                }
                m_dirty_rect.clear();
            }));
        }
        enqueue_refresh_timer();
    });
    TimerQueue::the().add_timer(move(refresh_timer));
}

void Console::enable()
{
    auto current_resolution = m_parent_display_connector->current_mode_setting();
    GenericFramebufferConsole::enable();
    m_width = current_resolution.horizontal_active;
    m_height = current_resolution.vertical_active;
    m_pitch = current_resolution.horizontal_stride;
    m_dirty_rect.union_rect(0, 0, m_width, m_height);
}

u8* Console::framebuffer_data()
{
    return m_parent_display_connector->framebuffer_data();
}

}
