/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, the SerenityOS developers.
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
#include <LibCore/EventReceiver.h>
#include <LibCore/Promise.h>
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
class BackgroundAction final : public Core::EventReceiver
    , private BackgroundActionBase {
    C_OBJECT(BackgroundAction);

public:
    // Promise is an implementation detail of BackgroundAction in order to communicate with EventLoop.
    // All of the promise's callbacks and state are either managed by us or by EventLoop.
    using Promise = Core::Promise<NonnullRefPtr<Core::EventReceiver>>;

    virtual ~BackgroundAction() = default;

    Optional<Result> const& result() const { return m_result; }
    Optional<Result>& result() { return m_result; }

    void cancel() { m_canceled = true; }
    // If your action is long-running, you should periodically check the cancel state and possibly return early.
    bool is_canceled() const { return m_canceled; }

private:
    BackgroundAction(Function<ErrorOr<Result>(BackgroundAction&)> action, Function<ErrorOr<void>(Result)> on_complete, Optional<Function<void(Error)>> on_error = {})
        : Core::EventReceiver(&background_thread())
        , m_promise(Promise::try_create().release_value_but_fixme_should_propagate_errors())
        , m_action(move(action))
        , m_on_complete(move(on_complete))
    {
        if (m_on_complete) {
            m_promise->on_resolution = [](NonnullRefPtr<Core::EventReceiver>& object) -> ErrorOr<void> {
                auto self = static_ptr_cast<BackgroundAction<Result>>(object);
                VERIFY(self->m_result.has_value());
                if (auto maybe_error = self->m_on_complete(self->m_result.value()); maybe_error.is_error())
                    self->m_on_error(maybe_error.release_error());

                return {};
            };
            Core::EventLoop::current().add_job(m_promise);
        }

        if (on_error.has_value())
            m_on_error = on_error.release_value();

        enqueue_work([this, origin_event_loop = &Core::EventLoop::current()] {
            auto result = m_action(*this);
            // The event loop cancels the promise when it exits.
            m_canceled |= m_promise->is_rejected();
            auto callback_scheduled = false;
            // All of our work was successful and we weren't cancelled; resolve the event loop's promise.
            if (!m_canceled && !result.is_error()) {
                m_result = result.release_value();
                // If there is no completion callback, we don't rely on the user keeping around the event loop.
                if (m_on_complete) {
                    callback_scheduled = true;
                    origin_event_loop->deferred_invoke([this] {
                        // Our promise's resolution function will never error.
                        (void)m_promise->resolve(*this);
                        remove_from_parent();
                    });
                    origin_event_loop->wake();
                }
            } else {
                // We were either unsuccessful or cancelled (in which case there is no error).
                auto error = Error::from_errno(ECANCELED);
                if (result.is_error())
                    error = result.release_error();

                m_promise->reject(Error::from_errno(ECANCELED));
                if (!m_canceled && m_on_error) {
                    callback_scheduled = true;
                    origin_event_loop->deferred_invoke([this, error = move(error)]() mutable {
                        m_on_error(move(error));
                        remove_from_parent();
                    });
                    origin_event_loop->wake();
                } else if (m_on_error) {
                    m_on_error(move(error));
                }
            }

            if (!callback_scheduled)
                remove_from_parent();
        });
    }

    NonnullRefPtr<Promise> m_promise;
    Function<ErrorOr<Result>(BackgroundAction&)> m_action;
    Function<ErrorOr<void>(Result)> m_on_complete;
    Function<void(Error)> m_on_error = [](Error error) {
        dbgln("Error occurred while running a BackgroundAction: {}", error);
    };
    Optional<Result> m_result;
    bool m_canceled { false };
};

}
