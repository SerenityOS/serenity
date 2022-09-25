/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/AbortController.h>
#include <LibWeb/DOM/AbortSignal.h>

namespace Web::DOM {

JS::NonnullGCPtr<AbortController> AbortController::construct_impl(JS::Realm& realm)
{
    auto signal = AbortSignal::construct_impl(realm);
    return *realm.heap().allocate<AbortController>(realm, realm, move(signal));
}

// https://dom.spec.whatwg.org/#dom-abortcontroller-abortcontroller
AbortController::AbortController(JS::Realm& realm, JS::NonnullGCPtr<AbortSignal> signal)
    : PlatformObject(realm)
    , m_signal(move(signal))
{
    set_prototype(&Bindings::cached_web_prototype(realm, "AbortController"));
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
