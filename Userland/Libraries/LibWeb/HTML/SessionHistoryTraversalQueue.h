/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibCore/Timer.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

struct SessionHistoryTraversalQueueEntry {
    JS::SafeFunction<void()> steps;
    JS::GCPtr<HTML::Navigable> target_navigable;
};

// https://html.spec.whatwg.org/multipage/document-sequences.html#tn-session-history-traversal-queue
class SessionHistoryTraversalQueue {
public:
    SessionHistoryTraversalQueue()
    {
        m_timer = Core::Timer::create_single_shot(0, [this] {
            while (m_queue.size() > 0) {
                auto entry = m_queue.take_first();
                entry.steps();
            }
        }).release_value_but_fixme_should_propagate_errors();
    }

    void append(JS::SafeFunction<void()> steps)
    {
        m_queue.append({ move(steps), nullptr });
        if (!m_timer->is_active()) {
            m_timer->start();
        }
    }

    void append_sync(JS::SafeFunction<void()> steps, JS::GCPtr<Navigable> target_navigable)
    {
        m_queue.append({ move(steps), target_navigable });
        if (!m_timer->is_active()) {
            m_timer->start();
        }
    }

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#sync-navigations-jump-queue
    SessionHistoryTraversalQueueEntry first_synchronous_navigation_steps_with_target_navigable_not_contained_in(Vector<JS::GCPtr<Navigable>> const& list)
    {
        auto index = m_queue.find_first_index_if([&list](auto const& entry) -> bool {
            return (entry.target_navigable != nullptr) && !list.contains_slow(entry.target_navigable);
        });
        return index.has_value() ? m_queue.take(*index) : SessionHistoryTraversalQueueEntry {};
    }

    void process()
    {
        while (m_queue.size() > 0) {
            auto entry = m_queue.take_first();
            entry.steps();
        }
    }

private:
    Vector<SessionHistoryTraversalQueueEntry> m_queue;
    RefPtr<Core::Timer> m_timer;
};

}
