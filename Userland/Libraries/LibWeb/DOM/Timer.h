/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibCore/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/Function.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

class Timer final : public RefCounted<Timer> {
public:
    enum class Type {
        Interval,
        Timeout,
    };

    static NonnullRefPtr<Timer> create_interval(Window&, int milliseconds, JS::Function&);
    static NonnullRefPtr<Timer> create_timeout(Window&, int milliseconds, JS::Function&);

    ~Timer();

    i32 id() const { return m_id; }
    Type type() const { return m_type; }

    JS::Function& callback() { return *m_callback.cell(); }

private:
    Timer(Window&, Type, int ms, JS::Function&);

    Window& m_window;
    RefPtr<Core::Timer> m_timer;
    Type m_type;
    int m_id { 0 };
    JS::Handle<JS::Function> m_callback;
};

}
