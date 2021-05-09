/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Function.h>

namespace JS {

class ProxyObject final : public Function {
    JS_OBJECT(ProxyObject, Function);

public:
    static ProxyObject* create(GlobalObject&, Object& target, Object& handler);

    ProxyObject(Object& target, Object& handler, Object& prototype);
    virtual ~ProxyObject() override;

    virtual Value call() override;
    virtual Value construct(Function& new_target) override;
    virtual const FlyString& name() const override;
    virtual LexicalEnvironment* create_environment() override;

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
    virtual Value get(const PropertyName& name, Value receiver, bool without_side_effects = false) const override;
    virtual bool put(const PropertyName& name, Value value, Value receiver) override;
    virtual bool delete_property(const PropertyName& name) override;

    void revoke() { m_is_revoked = true; }

private:
    virtual void visit_edges(Visitor&) override;

    virtual bool is_function() const override { return m_target.is_function(); }
    virtual bool is_array() const override { return m_target.is_array(); };

    Object& m_target;
    Object& m_handler;
    bool m_is_revoked { false };
};

}
