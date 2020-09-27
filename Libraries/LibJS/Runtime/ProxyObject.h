/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    virtual Value get(const PropertyName& name, Value receiver) const override;
    virtual bool put(const PropertyName& name, Value value, Value receiver) override;
    virtual Value delete_property(const PropertyName& name) override;

    void revoke() { m_is_revoked = true; }

private:
    virtual void visit_children(Visitor&) override;
    virtual bool is_proxy_object() const override { return true; }

    virtual bool is_function() const override { return m_target.is_function(); }
    virtual bool is_array() const override { return m_target.is_array(); };

    Object& m_target;
    Object& m_handler;
    bool m_is_revoked { false };
};

}
