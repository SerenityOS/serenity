/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#abortcontroller
class AbortController final
    : public RefCounted<AbortController>
    , public Weakable<AbortController>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::AbortControllerWrapper;

    static NonnullRefPtr<AbortController> create()
    {
        return adopt_ref(*new AbortController());
    }

    static NonnullRefPtr<AbortController> create_with_global_object(Bindings::WindowObject&)
    {
        return AbortController::create();
    }

    virtual ~AbortController() override;

    // https://dom.spec.whatwg.org/#dom-abortcontroller-signal
    NonnullRefPtr<AbortSignal> signal() const { return m_signal; }

    void abort(JS::Value reason);

private:
    AbortController();

    // https://dom.spec.whatwg.org/#abortcontroller-signal
    NonnullRefPtr<AbortSignal> m_signal;
};

}
