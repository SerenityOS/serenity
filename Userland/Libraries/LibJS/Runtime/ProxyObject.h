/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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

    const Object& target() const { return m_target; }
    const Object& handler() const { return m_handler; }

    virtual Object* prototype() override;
    virtual const Object* prototype() const override;
    virtual bool set_prototype(Object* object) override;
    virtual bool is_extensible() const override;
    virtual bool prevent_extensions() override;
    virtual Optional<PropertyDescriptor> get_own_property_descriptor(const PropertyName&) const override;
    virtual bool define_property(const StringOrSymbol& property_name, const Object& descriptor, bool throw_exceptions = true) override;
    virtual bool has_property(const PropertyName& name) const override;
    virtual Value get(const PropertyName& name, Value receiver, AllowSideEffects = AllowSideEffects::Yes) const override;
    virtual bool put(const PropertyName& name, Value value, Value receiver) override;
    virtual bool delete_property(PropertyName const& name, bool force_throw_exception = false) override;

    bool is_revoked() const { return m_is_revoked; }
    void revoke() { m_is_revoked = true; }

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
