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
    auto current_resolution = MUST(parent_display_connector.get_resolution());
    return adopt_ref_if_nonnull(new (nothrow) VMWareFramebufferConsole(parent_display_connector, current_resolution)).release_nonnull();
}

VMWareFramebufferConsole::VMWareFramebufferConsole(VMWareDisplayConnector const& parent_display_connector, DisplayConnector::Resolution current_resolution)
    : GenericFramebufferConsole(current_resolution.width, current_resolution.height, current_resolution.pitch)
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
    NonnullRefPtr<Timer> refresh_timer = adopt_ref_if_nonnull(new (nothrow) Timer()).release_nonnull();
    refresh_timer->setup(CLOCK_MONOTONIC, refresh_interval, [this]() {
        if (m_enabled.load() && m_dirty) {
            g_io_work->queue([this]() {
                MUST(m_parent_display_connector->flush_first_surface());
                m_dirty = false;
            });
        }
        enqueue_refresh_timer();
    });
    TimerQueue::the().add_timer(move(refresh_timer));
}

void VMWareFramebufferConsole::enable()
{
    auto current_resolution = MUST(m_parent_display_connector->get_resolution());
    GenericFramebufferConsole::enable();
    m_width = current_resolution.width;
    m_height = current_resolution.height;
    m_pitch = current_resolution.pitch;
}

u8* VMWareFramebufferConsole::framebuffer_data()
{
    return m_parent_display_connector->framebuffer_data();
}

}
