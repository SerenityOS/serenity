/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/WeakPtr.h>
#include <LibCore/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class Timer final : public JS::Cell {
    JS_CELL(Timer, JS::Cell);

public:
    static JS::NonnullGCPtr<Timer> create(Window&, i32 milliseconds, Function<void()> callback, i32 id);
    virtual ~Timer() override;

    void start();

private:
    Timer(Window& window, i32 milliseconds, Function<void()> callback, i32 id);

    virtual void visit_edges(Cell::Visitor&) override;

    RefPtr<Core::Timer> m_timer;
    JS::NonnullGCPtr<Window> m_window;
    Function<void()> m_callback;
    i32 m_id { 0 };
};

}
