/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class WindowProxy final : public JS::Object {
    JS_OBJECT(WindowProxy, JS::Object);

public:
    virtual ~WindowProxy() override = default;

    virtual JS::ThrowCompletionOr<JS::Object*> internal_get_prototype_of() const override;
    virtual JS::ThrowCompletionOr<bool> internal_set_prototype_of(Object* prototype) override;
    virtual JS::ThrowCompletionOr<bool> internal_is_extensible() const override;
    virtual JS::ThrowCompletionOr<bool> internal_prevent_extensions() override;
    virtual JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> internal_get_own_property(JS::PropertyKey const&) const override;
    virtual JS::ThrowCompletionOr<bool> internal_define_own_property(JS::PropertyKey const&, JS::PropertyDescriptor const&) override;
    virtual JS::ThrowCompletionOr<JS::Value> internal_get(JS::PropertyKey const&, JS::Value receiver) const override;
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value value, JS::Value receiver) override;
    virtual JS::ThrowCompletionOr<bool> internal_delete(JS::PropertyKey const&) override;
    virtual JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> internal_own_property_keys() const override;

    HTML::Window& window() { return *m_window; }
    HTML::Window const& window() const { return *m_window; }

    // NOTE: Someone will have to replace the wrapped window object as well:
    // "When the browsing context is navigated, the Window object wrapped by the browsing context's associated WindowProxy object is changed."
    // I haven't found where that actually happens yet. Make sure to use a Badge<T> guarded setter.

private:
    WindowProxy(JS::Realm&, HTML::Window&);

    virtual void visit_edges(JS::Cell::Visitor&) override;

    // [[Window]], https://html.spec.whatwg.org/multipage/window-object.html#concept-windowproxy-window
    JS::GCPtr<HTML::Window> m_window;
};

}
