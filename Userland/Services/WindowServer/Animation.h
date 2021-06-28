/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGfx/Forward.h>

namespace WindowServer {

class Compositor;
class Screen;

class Animation : public RefCounted<Animation> {
public:
    static NonnullRefPtr<Animation> create() { return adopt_ref(*new Animation); }

    ~Animation();

    bool is_running() const { return m_running; }

    void start();
    void stop();

    void set_duration(int duration_in_ms);
    int duration() const { return m_duration; }

    void update(Badge<Compositor>, Gfx::Painter&, Screen&, Gfx::DisjointRectSet& flush_rects);

    Function<void(float progress, Gfx::Painter&, Screen&, Gfx::DisjointRectSet& flush_rects)> on_update;
    Function<void()> on_stop;

private:
    Animation();

    Core::ElapsedTimer m_timer;
    int m_duration { 0 };
    bool m_running { false };
};

}
