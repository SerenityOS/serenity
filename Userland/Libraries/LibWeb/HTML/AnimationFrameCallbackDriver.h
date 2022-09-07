/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/IDAllocator.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/Platform/Timer.h>

namespace Web::HTML {

struct AnimationFrameCallbackDriver {
    using Callback = Function<void(i32)>;

    AnimationFrameCallbackDriver()
    {
        m_timer = Platform::Timer::create_single_shot(16, [] {
            HTML::main_thread_event_loop().schedule();
        });
    }

    i32 add(Callback handler)
    {
        auto id = m_id_allocator.allocate();
        m_callbacks.set(id, move(handler));
        if (!m_timer->is_active())
            m_timer->start();
        return id;
    }

    bool remove(i32 id)
    {
        auto it = m_callbacks.find(id);
        if (it == m_callbacks.end())
            return false;
        m_callbacks.remove(it);
        m_id_allocator.deallocate(id);
        return true;
    }

    void run()
    {
        auto taken_callbacks = move(m_callbacks);
        for (auto& [id, callback] : taken_callbacks)
            callback(id);
    }

    bool has_callbacks() const
    {
        return !m_callbacks.is_empty();
    }

private:
    HashMap<i32, Callback> m_callbacks;
    IDAllocator m_id_allocator;
    RefPtr<Platform::Timer> m_timer;
};

}
