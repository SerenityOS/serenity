/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/document-sequences.html#tn-session-history-traversal-queue
class SessionHistoryTraversalQueue {
public:
    SessionHistoryTraversalQueue()
    {
        m_timer = Core::Timer::create_single_shot(0, [this] {
            while (m_queue.size() > 0) {
                auto steps = m_queue.take_first();
                steps();
            }
        }).release_value_but_fixme_should_propagate_errors();
    }

    void append(JS::SafeFunction<void()> steps)
    {
        m_queue.append(move(steps));
        if (!m_timer->is_active()) {
            m_timer->start();
        }
    }

private:
    Vector<JS::SafeFunction<void()>> m_queue;
    RefPtr<Core::Timer> m_timer;
};

}
