/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>

namespace Core {
template<typename Result>
class Promise : public Object {
    C_OBJECT(Promise);

public:
    Function<void(Result&)> on_resolved;

    void resolve(Result&& result)
    {
        m_pending = move(result);
        if (on_resolved)
            on_resolved(m_pending.value());
    }

    bool is_resolved()
    {
        return m_pending.has_value();
    };

    Result await()
    {
        while (!is_resolved()) {
            Core::EventLoop::current().pump();
        }
        return m_pending.release_value();
    }

    // Converts a Promise<A> to a Promise<B> using a function func: A -> B
    template<typename T>
    RefPtr<Promise<T>> map(T func(Result&))
    {
        RefPtr<Promise<T>> new_promise = Promise<T>::construct();
        on_resolved = [new_promise, func](Result& result) mutable {
            auto t = func(result);
            new_promise->resolve(move(t));
        };
        return new_promise;
    }

private:
    Promise() = default;

    Optional<Result> m_pending;
};
}
