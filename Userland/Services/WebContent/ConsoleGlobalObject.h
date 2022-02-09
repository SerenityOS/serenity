/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace Web::Bindings {
class WindowObject;
}

namespace WebContent {

class ConsoleGlobalObject final : public JS::GlobalObject {
    JS_OBJECT(ConsoleGlobalObject, JS::GlobalObject);

public:
    ConsoleGlobalObject(Web::Bindings::WindowObject&);
    virtual ~ConsoleGlobalObject() override;

    virtual JS::ThrowCompletionOr<Object*> internal_get_prototype_of() const override;
    virtual JS::ThrowCompletionOr<bool> internal_set_prototype_of(Object* prototype) override;
    virtual JS::ThrowCompletionOr<bool> internal_is_extensible() const override;
    virtual JS::ThrowCompletionOr<bool> internal_prevent_extensions() override;
    virtual JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> internal_get_own_property(JS::PropertyKey const& name) const override;
    virtual JS::ThrowCompletionOr<bool> internal_define_own_property(JS::PropertyKey const& name, JS::PropertyDescriptor const& descriptor) override;
    virtual JS::ThrowCompletionOr<bool> internal_has_property(JS::PropertyKey const& name) const override;
    virtual JS::ThrowCompletionOr<JS::Value> internal_get(JS::PropertyKey const&, JS::Value) const override;
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value value, JS::Value receiver) override;
    virtual JS::ThrowCompletionOr<bool> internal_delete(JS::PropertyKey const& name) override;
    virtual JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> internal_own_property_keys() const override;

    virtual void initialize_global_object() override;

private:
    virtual void visit_edges(Visitor&) override;

    // Because $0 is not a nice C++ function name
    JS_DECLARE_NATIVE_FUNCTION(inspected_node_getter);

    Web::Bindings::WindowObject* m_window_object;
};

}
