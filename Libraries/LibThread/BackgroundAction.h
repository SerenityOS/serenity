/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/Queue.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>

namespace LibThread {

template<typename Result>
class BackgroundAction;

class BackgroundActionBase {
    template<typename Result>
    friend class BackgroundAction;

private:
    BackgroundActionBase() {}

    static Lockable<Queue<Function<void()>>>& all_actions();
    static Thread& background_thread();
};

template<typename Result>
class BackgroundAction final : public Core::Object
    , private BackgroundActionBase {
    C_OBJECT(BackgroundAction);

public:
    static NonnullRefPtr<BackgroundAction<Result>> create(
        Function<Result()> action,
        Function<void(Result)> on_complete = nullptr)
    {
        return adopt(*new BackgroundAction(move(action), move(on_complete)));
    }

    virtual ~BackgroundAction() {}

private:
    BackgroundAction(Function<Result()> action, Function<void(Result)> on_complete)
        : Core::Object(&background_thread())
        , m_action(move(action))
        , m_on_complete(move(on_complete))
    {
        LOCKER(all_actions().lock());

        this->ref();
        all_actions().resource().enqueue([this] {
            m_result = m_action();
            if (m_on_complete) {
                Core::EventLoop::current().post_event(*this, make<Core::DeferredInvocationEvent>([this](auto&) {
                    m_on_complete(m_result.release_value());
                    this->unref();
                }));
                Core::EventLoop::wake();
            } else
                this->unref();
        });
    }

    Function<Result()> m_action;
    Function<void(Result)> m_on_complete;
    Optional<Result> m_result;
};

}
