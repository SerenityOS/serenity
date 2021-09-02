/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/AbortController.h>
#include <LibWeb/DOM/AbortSignal.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#dom-abortcontroller-abortcontroller
AbortController::AbortController(Document& document)
    : m_signal(AbortSignal::create(document))
{
}

AbortController::~AbortController()
{
}

// https://dom.spec.whatwg.org/#dom-abortcontroller-abort
void AbortController::abort()
{
    m_signal->signal_abort();
}

}
