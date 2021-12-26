/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/FunctionObject.h>

namespace JS {

class ProxyObject final : public FunctionObject {
    JS_OBJECT(ProxyObject, FunctionObject);

public:
    static ProxyObject* create(GlobalObject&, Object& target, Object& handler);

    ProxyObject(Object& target, Object& handler, Object& prototype);
    virtual ~ProxyObject() override;

    virtual Value call() override;
    virtual Value construct(FunctionObject& new_target) override;
    virtual const FlyString& name() const override;
    virtual FunctionEnvironment* create_environment(FunctionObject&) override;
    virtual bool has_constructor() const override { return true; }

    const Object& target() const { return m_target; }
    const Object& handler() const { return m_handler; }

    bool is_revoked() const { return m_is_revoked; }
    void revoke() { m_is_revoked = true; }

    // 10.5 Proxy Object Internal Methods and Internal Slots, https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots

    virtual ThrowCompletionOr<Object*> internal_get_prototype_of() const override;
    virtual ThrowCompletionOr<bool> internal_set_prototype_of(Object* prototype) override;
    virtual bool internal_is_extensible() const override;
    virtual bool internal_prevent_extensions() override;
    virtual Optional<PropertyDescriptor> internal_get_own_property(PropertyName const&) const override;
    virtual bool internal_define_own_property(PropertyName const&, PropertyDescriptor const&) override;
    virtual bool internal_has_property(PropertyName const&) const override;
    virtual Value internal_get(PropertyName const&, Value receiver) const override;
    virtual bool internal_set(PropertyName const&, Value value, Value receiver) override;
    virtual bool internal_delete(PropertyName const&) override;
    virtual MarkedValueList internal_own_property_keys() const override;

private:
    virtual void visit_edges(Visitor&) override;

    virtual bool is_function() const override { return m_target.is_function(); }
    virtual bool is_proxy_object() const final { return true; }

    Object& m_target;
    Object& m_handler;
    bool m_is_revoked { false };
};

template<>
inline bool Object::fast_is<ProxyObject>() const { return is_proxy_object(); }

}
