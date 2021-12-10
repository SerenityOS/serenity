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

    static NonnullRefPtr<AbortController> create(Document& document)
    {
        return adopt_ref(*new AbortController(document));
    }

    static NonnullRefPtr<AbortController> create_with_global_object(Bindings::WindowObject& window_object)
    {
        return AbortController::create(window_object.impl().associated_document());
    }

    virtual ~AbortController() override;

    // https://dom.spec.whatwg.org/#dom-abortcontroller-signal
    NonnullRefPtr<AbortSignal> signal() const { return m_signal; }

    void abort(JS::Value reason);

private:
    AbortController(Document& document);

    // https://dom.spec.whatwg.org/#abortcontroller-signal
    NonnullRefPtr<AbortSignal> m_signal;
};

}
