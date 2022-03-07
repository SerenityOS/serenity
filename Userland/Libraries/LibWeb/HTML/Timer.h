/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <LibCore/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class Timer final : public RefCounted<Timer> {
public:
    static NonnullRefPtr<Timer> create(Window& window, i32 milliseconds, Function<void()> callback, i32 id);
    ~Timer();

    void start();

private:
    Timer(Window& window, i32 milliseconds, Function<void()> callback, i32 id);

    RefPtr<Core::Timer> m_timer;
    Window& m_window;
    i32 m_id { 0 };
};

}
