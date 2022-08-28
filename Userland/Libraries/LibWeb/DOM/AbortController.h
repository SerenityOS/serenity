/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Window.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#abortcontroller
class AbortController final
    : public RefCounted<AbortController>
    , public Weakable<AbortController>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::AbortControllerWrapper;

    static NonnullRefPtr<AbortController> create_with_global_object(HTML::Window& window)
    {
        return adopt_ref(*new AbortController(window));
    }

    virtual ~AbortController() override = default;

    // https://dom.spec.whatwg.org/#dom-abortcontroller-signal
    JS::NonnullGCPtr<AbortSignal> signal() const { return *m_signal; }

    void abort(JS::Value reason);

private:
    explicit AbortController(HTML::Window&);

    // https://dom.spec.whatwg.org/#abortcontroller-signal
    JS::Handle<AbortSignal> m_signal;
};

}
