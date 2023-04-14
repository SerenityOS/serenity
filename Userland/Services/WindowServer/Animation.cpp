/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Animation.h"
#include "Compositor.h"
#include <AK/Badge.h>

namespace WindowServer {

Animation::~Animation()
{
    if (m_running)
        Compositor::the().unregister_animation({}, *this);
}

void Animation::set_duration(int duration_in_ms)
{
    m_duration = duration_in_ms;
}

void Animation::start()
{
    if (m_running)
        return;
    m_running = true;
    m_timer.start();
    Compositor::the().register_animation({}, *this);
}

void Animation::stop()
{
    if (!m_running)
        return;
    m_running = false;
    Compositor::the().unregister_animation({}, *this);

    if (on_stop)
        on_stop();
}

void Animation::call_stop_handler(Badge<Compositor>)
{
    if (on_stop)
        on_stop();
}

void Animation::was_removed(Badge<Compositor>)
{
    m_running = false;
}

bool Animation::update(Gfx::Painter& painter, Screen& screen, Gfx::DisjointIntRectSet& flush_rects)
{
    i64 const elapsed_ms = m_timer.elapsed();
    float progress = min((float)elapsed_ms / (float)m_duration, 1.0f);

    if (on_update)
        on_update(progress, painter, screen, flush_rects);

    return progress < 1.0f;
}

}
