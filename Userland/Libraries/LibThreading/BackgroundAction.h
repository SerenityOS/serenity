/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/Queue.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>
#include <LibThreading/Thread.h>

namespace Threading {

template<typename Result>
class BackgroundAction;

class BackgroundActionBase {
    template<typename Result>
    friend class BackgroundAction;

private:
    BackgroundActionBase() = default;

    static void enqueue_work(Function<void()>);
    static Thread& background_thread();
};

template<typename Result>
class BackgroundAction final : public Core::Object
    , private BackgroundActionBase {
    C_OBJECT(BackgroundAction);

public:
    void cancel()
    {
        m_cancelled = true;
    }

    bool is_cancelled() const
    {
        return m_cancelled;
    }

    virtual ~BackgroundAction() = default;

private:
    BackgroundAction(Function<Result(BackgroundAction&)> action, Function<ErrorOr<void>(Result)> on_complete, Optional<Function<void(Error)>> on_error = {})
        : Core::Object(&background_thread())
        , m_action(move(action))
        , m_on_complete(move(on_complete))
    {
        if (on_error.has_value())
            m_on_error = on_error.release_value();

        enqueue_work([this, origin_event_loop = &Core::EventLoop::current()] {
            m_result = m_action(*this);
            if (m_on_complete) {
                origin_event_loop->deferred_invoke([this] {
                    auto maybe_error = m_on_complete(m_result.release_value());
                    if (maybe_error.is_error())
                        m_on_error(maybe_error.release_error());
                    remove_from_parent();
                });
                origin_event_loop->wake();
            } else {
                this->remove_from_parent();
            }
        });
    }

    bool m_cancelled { false };
    Function<Result(BackgroundAction&)> m_action;
    Function<ErrorOr<void>(Result)> m_on_complete;
    Function<void(Error)> m_on_error = [](Error error) {
        dbgln("Error occurred while running a BackgroundAction: {}", error);
    };
    Optional<Result> m_result;
};

}
