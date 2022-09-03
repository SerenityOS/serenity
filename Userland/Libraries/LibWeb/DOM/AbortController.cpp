/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/AbortController.h>
#include <LibWeb/DOM/AbortSignal.h>

namespace Web::DOM {

JS::NonnullGCPtr<AbortController> AbortController::create_with_global_object(HTML::Window& window)
{
    auto signal = AbortSignal::create_with_global_object(window);
    return *window.heap().allocate<AbortController>(window.realm(), window, move(signal));
}

// https://dom.spec.whatwg.org/#dom-abortcontroller-abortcontroller
AbortController::AbortController(HTML::Window& window, JS::NonnullGCPtr<AbortSignal> signal)
    : PlatformObject(window.realm())
    , m_signal(move(signal))
{
    set_prototype(&window.cached_web_prototype("AbortController"));
}

AbortController::~AbortController() = default;

void AbortController::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_signal.ptr());
}

// https://dom.spec.whatwg.org/#dom-abortcontroller-abort
void AbortController::abort(JS::Value reason)
{
    // The abort(reason) method steps are to signal abort on this’s signal with reason if it is given.
    m_signal->signal_abort(reason);
}

}
