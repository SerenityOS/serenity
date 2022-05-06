/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VMWare/Console.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

constexpr static AK::Time refresh_interval = AK::Time::from_milliseconds(16);

NonnullRefPtr<VMWareFramebufferConsole> VMWareFramebufferConsole::initialize(VMWareDisplayConnector& parent_display_connector)
{
    auto current_resolution = parent_display_connector.current_mode_setting();
    return adopt_ref(*new (nothrow) VMWareFramebufferConsole(parent_display_connector, current_resolution));
}

VMWareFramebufferConsole::VMWareFramebufferConsole(VMWareDisplayConnector const& parent_display_connector, DisplayConnector::ModeSetting current_resolution)
    : GenericFramebufferConsole(current_resolution.horizontal_active, current_resolution.vertical_active, current_resolution.horizontal_stride)
    , m_parent_display_connector(parent_display_connector)
{
    enqueue_refresh_timer();
}

void VMWareFramebufferConsole::set_resolution(size_t width, size_t height, size_t pitch)
{
    m_width = width;
    m_height = height;
    m_pitch = pitch;
    m_dirty = true;
}

void VMWareFramebufferConsole::flush(size_t, size_t, size_t, size_t)
{
    m_dirty = true;
}

void VMWareFramebufferConsole::enqueue_refresh_timer()
{
    NonnullRefPtr<Timer> refresh_timer = adopt_ref(*new (nothrow) Timer());
    refresh_timer->setup(CLOCK_MONOTONIC, refresh_interval, [this]() {
        if (m_enabled.load() && m_dirty) {
            MUST(g_io_work->try_queue([this]() {
                MUST(m_parent_display_connector->flush_first_surface());
                m_dirty = false;
            }));
        }
        enqueue_refresh_timer();
    });
    TimerQueue::the().add_timer(move(refresh_timer));
}

void VMWareFramebufferConsole::enable()
{
    auto current_resolution = m_parent_display_connector->current_mode_setting();
    GenericFramebufferConsole::enable();
    m_width = current_resolution.horizontal_active;
    m_height = current_resolution.vertical_active;
    m_pitch = current_resolution.horizontal_stride;
}

u8* VMWareFramebufferConsole::framebuffer_data()
{
    return m_parent_display_connector->framebuffer_data();
}

}
