/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/TTY/ConsoleManagement.h>
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
    // NOTE: Clear the framebuffer, in case it's left with some garbage.
    memset(framebuffer_data(), 0, current_resolution.horizontal_stride * current_resolution.vertical_active);
    enqueue_refresh_timer();
}

void Console::set_resolution(size_t width, size_t height, size_t pitch)
{
    m_width = width;
    m_height = height;
    m_pitch = pitch;

    // Just to start cleanly, we clean the entire framebuffer
    memset(framebuffer_data(), 0, pitch * height);

    ConsoleManagement::the().resolution_was_changed();
}

void Console::set_cursor(size_t x, size_t y)
{
    GenericFramebufferConsole::hide_cursor();
    m_x = x;
    m_y = y;
    GenericFramebufferConsole::show_cursor();
    m_dirty = true;
}

void Console::hide_cursor()
{
    GenericFramebufferConsole::hide_cursor();
    m_dirty = true;
}

void Console::show_cursor()
{
    GenericFramebufferConsole::show_cursor();
    m_dirty = true;
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
    // FIXME: Do we need some locking here to ensure the resolution doesn't change
    // while we enable the console?
    auto current_resolution = m_parent_display_connector->current_mode_setting();
    m_width = current_resolution.horizontal_active;
    m_height = current_resolution.vertical_active;
    m_pitch = current_resolution.horizontal_stride;
    GenericFramebufferConsole::enable();
    m_dirty = true;
}

u8* Console::framebuffer_data()
{
    return m_parent_display_connector->framebuffer_data();
}

}
