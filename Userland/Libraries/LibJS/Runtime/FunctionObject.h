/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class FunctionObject : public Object {
    JS_OBJECT(Function, Object);

public:
    enum class ConstructorKind {
        Base,
        Derived,
    };

    virtual ~FunctionObject();
    virtual void initialize(GlobalObject&) override { }

    virtual Value call() = 0;
    virtual Value construct(FunctionObject& new_target) = 0;
    virtual const FlyString& name() const = 0;
    virtual FunctionEnvironment* create_environment(FunctionObject&) = 0;

    BoundFunction* bind(Value bound_this_value, Vector<Value> arguments);

    Value bound_this() const { return m_bound_this; }

    const Vector<Value>& bound_arguments() const { return m_bound_arguments; }

    Object* home_object() const { return m_home_object; }
    void set_home_object(Object* home_object) { m_home_object = home_object; }

    ConstructorKind constructor_kind() const { return m_constructor_kind; };
    void set_constructor_kind(ConstructorKind constructor_kind) { m_constructor_kind = constructor_kind; }

    virtual bool is_strict_mode() const { return false; }

    // [[Environment]]
    // The Environment Record that the function was closed over.
    // Used as the outer environment when evaluating the code of the function.
    virtual Environment* environment() { return nullptr; }

    // [[Realm]]
    virtual Realm* realm() const { return nullptr; }

    // This is for IsSimpleParameterList (static semantics)
    bool has_simple_parameter_list() const { return m_has_simple_parameter_list; }

    // [[Fields]]
    struct InstanceField {
        StringOrSymbol name;
        FunctionObject* initializer { nullptr };

        void define_field(VM& vm, Object& receiver) const;
    };

    Vector<InstanceField> const& fields() const { return m_fields; }
    void add_field(StringOrSymbol property_key, FunctionObject* initializer);

protected:
    virtual void visit_edges(Visitor&) override;

    explicit FunctionObject(Object& prototype);
    FunctionObject(Value bound_this, Vector<Value> bound_arguments, Object& prototype);

    void set_has_simple_parameter_list(bool b) { m_has_simple_parameter_list = b; }

private:
    virtual bool is_function() const override { return true; }
    Value m_bound_this;
    Vector<Value> m_bound_arguments;
    Object* m_home_object { nullptr };
    ConstructorKind m_constructor_kind = ConstructorKind::Base;
    bool m_has_simple_parameter_list { false };
    Vector<InstanceField> m_fields;
};

}
