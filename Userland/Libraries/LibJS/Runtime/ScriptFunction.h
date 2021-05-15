/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/AST.h>
#include <LibJS/Runtime/Function.h>

namespace JS {

class ScriptFunction final : public Function {
    JS_OBJECT(ScriptFunction, Function);

public:
    static ScriptFunction* create(GlobalObject&, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, ScopeObject* parent_scope, bool is_strict, bool is_arrow_function = false);

    ScriptFunction(GlobalObject&, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, ScopeObject* parent_scope, Object& prototype, bool is_strict, bool is_arrow_function = false);
    virtual void initialize(GlobalObject&) override;
    virtual ~ScriptFunction();

    const Statement& body() const { return m_body; }
    const Vector<FunctionNode::Parameter>& parameters() const { return m_parameters; };

    virtual Value call() override;
    virtual Value construct(Function& new_target) override;

    virtual const FlyString& name() const override { return m_name; };
    void set_name(const FlyString& name) { m_name = name; };

    void set_is_class_constructor() { m_is_class_constructor = true; };

protected:
    virtual bool is_strict_mode() const final { return m_is_strict; }

private:
    virtual LexicalEnvironment* create_environment() override;
    virtual void visit_edges(Visitor&) override;

    Value execute_function_body();

    JS_DECLARE_NATIVE_GETTER(length_getter);
    JS_DECLARE_NATIVE_GETTER(name_getter);

    FlyString m_name;
    NonnullRefPtr<Statement> m_body;
    const Vector<FunctionNode::Parameter> m_parameters;
    ScopeObject* m_parent_scope { nullptr };
    i32 m_function_length { 0 };
    bool m_is_strict { false };
    bool m_is_arrow_function { false };
    bool m_is_class_constructor { false };
};

}
