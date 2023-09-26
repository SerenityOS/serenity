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
    static JS::NonnullGCPtr<Timer> create(JS::Object&, i32 milliseconds, Function<void()> callback, i32 id);
    virtual ~Timer() override;

    void start();
    void stop();

private:
    Timer(JS::Object& window, i32 milliseconds, Function<void()> callback, i32 id);

    virtual void visit_edges(Cell::Visitor&) override;

    RefPtr<Platform::Timer> m_timer;
    JS::NonnullGCPtr<JS::Object> m_window_or_worker_global_scope;
    Function<void()> m_callback;
    i32 m_id { 0 };
};

}
