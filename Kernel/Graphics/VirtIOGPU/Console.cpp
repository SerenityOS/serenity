/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/WorkQueue.h>

namespace Kernel::Graphics::VirtIOGPU {

constexpr static AK::Time refresh_interval = AK::Time::from_milliseconds(16);

NonnullLockRefPtr<Console> Console::initialize(VirtIODisplayConnector& parent_display_connector)
{
    auto current_resolution = parent_display_connector.current_mode_setting();
    return adopt_lock_ref(*new Console(parent_display_connector, current_resolution));
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

void Console::flush(size_t, size_t, size_t, size_t)
{
    m_dirty = true;
}

void Console::enqueue_refresh_timer()
{
    NonnullLockRefPtr<Timer> refresh_timer = adopt_lock_ref(*new Timer());
    refresh_timer->setup(CLOCK_MONOTONIC, refresh_interval, [this]() {
        if (m_enabled.load() && m_dirty) {
            MUST(g_io_work->try_queue([this]() {
                {
                    MutexLocker locker(m_parent_display_connector->m_flushing_lock);
                    MUST(m_parent_display_connector->flush_first_surface());
                }
                m_dirty = false;
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
    m_dirty = true;
}

u8* Console::framebuffer_data()
{
    return m_parent_display_connector->framebuffer_data();
}

}
