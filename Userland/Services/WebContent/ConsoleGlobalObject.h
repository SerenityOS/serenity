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
    virtual Optional<JS::PropertyDescriptor> internal_get_own_property(JS::PropertyName const& name) const override;
    virtual bool internal_define_own_property(JS::PropertyName const& name, JS::PropertyDescriptor const& descriptor) override;
    virtual bool internal_has_property(JS::PropertyName const& name) const override;
    virtual JS::Value internal_get(JS::PropertyName const&, JS::Value) const override;
    virtual bool internal_set(JS::PropertyName const&, JS::Value value, JS::Value receiver) override;
    virtual bool internal_delete(JS::PropertyName const& name) override;
    virtual JS::MarkedValueList internal_own_property_keys() const override;

    virtual void initialize_global_object() override;

private:
    virtual void visit_edges(Visitor&) override;

    // Because $0 is not a nice C++ function name
    JS_DECLARE_NATIVE_GETTER(inspected_node_getter);

    Web::Bindings::WindowObject* m_window_object;
};

}
