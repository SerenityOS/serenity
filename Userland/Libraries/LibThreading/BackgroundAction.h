/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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
    BackgroundActionBase() { }

    static void enqueue_work(Function<void()>);
    static Thread& background_thread();
};

template<typename Result>
class BackgroundAction final : public Core::Object
    , private BackgroundActionBase {
    C_OBJECT(BackgroundAction);

public:
    static NonnullRefPtr<BackgroundAction<Result>> create(
        Function<Result(BackgroundAction&)> action,
        Function<void(Result)> on_complete = nullptr)
    {
        return adopt_ref(*new BackgroundAction(move(action), move(on_complete)));
    }

    void cancel()
    {
        m_cancelled = true;
    }

    bool is_cancelled() const
    {
        return m_cancelled;
    }

    virtual ~BackgroundAction() { }

private:
    BackgroundAction(Function<Result(BackgroundAction&)> action, Function<void(Result)> on_complete)
        : Core::Object(&background_thread())
        , m_action(move(action))
        , m_on_complete(move(on_complete))
    {
        enqueue_work([this] {
            m_result = m_action(*this);
            if (m_on_complete) {
                Core::EventLoop::current().post_event(*this, make<Core::DeferredInvocationEvent>([this, result = m_result.release_value()](auto&) {
                    m_on_complete(result);
                    this->remove_from_parent();
                }));
                Core::EventLoop::wake();
            } else {
                this->remove_from_parent();
            }
        });
    }

    bool m_cancelled { false };
    Function<Result(BackgroundAction&)> m_action;
    Function<void(Result)> m_on_complete;
    Optional<Result> m_result;
};

}
