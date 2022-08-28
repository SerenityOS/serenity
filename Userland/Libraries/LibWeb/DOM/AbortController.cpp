/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/AbortController.h>
#include <LibWeb/DOM/AbortSignal.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#dom-abortcontroller-abortcontroller
AbortController::AbortController(HTML::Window& window)
    : m_signal(JS::make_handle(*AbortSignal::create_with_global_object(window)))
{
}

// https://dom.spec.whatwg.org/#dom-abortcontroller-abort
void AbortController::abort(JS::Value reason)
{
    // The abort(reason) method steps are to signal abort on this’s signal with reason if it is given.
    m_signal->signal_abort(reason);
}

}
