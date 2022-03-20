/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Animation.h"
#include "Compositor.h"
#include <AK/Badge.h>

namespace WindowServer {

Animation::Animation()
{
    Compositor::the().register_animation({}, *this);
}

Animation::~Animation()
{
    if (!m_was_removed)
        Compositor::the().unregister_animation({}, *this);
}

void Animation::set_duration(int duration_in_ms)
{
    m_duration = duration_in_ms;
}

void Animation::start()
{
    m_running = true;
    m_timer.start();
    Compositor::the().animation_started({});
}

void Animation::stop()
{
    m_running = false;
    if (on_stop)
        on_stop();
}

void Animation::was_removed(Badge<Compositor>)
{
    m_was_removed = true;
}

bool Animation::update(Badge<Compositor>, Gfx::Painter& painter, Screen& screen, Gfx::DisjointRectSet& flush_rects)
{
    int elapsed_ms = m_timer.elapsed();
    float progress = min((float)elapsed_ms / (float)m_duration, 1.0f);

    if (on_update)
        on_update(progress, painter, screen, flush_rects);

    return progress < 1.0f;
}

}
