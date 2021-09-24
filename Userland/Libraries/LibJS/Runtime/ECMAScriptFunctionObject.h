/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Runtime/FunctionObject.h>

namespace JS {

// 10.2 ECMAScript Function Objects, https://tc39.es/ecma262/#sec-ecmascript-function-objects
class ECMAScriptFunctionObject final : public FunctionObject {
    JS_OBJECT(ECMAScriptFunctionObject, FunctionObject);

public:
    static ECMAScriptFunctionObject* create(GlobalObject&, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, Environment* parent_scope, FunctionKind, bool is_strict, bool is_arrow_function = false);

    ECMAScriptFunctionObject(GlobalObject&, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, Environment* parent_scope, Object& prototype, FunctionKind, bool is_strict, bool is_arrow_function = false);
    virtual void initialize(GlobalObject&) override;
    virtual ~ECMAScriptFunctionObject();

    const Statement& body() const { return m_body; }
    const Vector<FunctionNode::Parameter>& parameters() const { return m_parameters; };

    virtual Value call() override;
    virtual Value construct(FunctionObject& new_target) override;

    virtual const FlyString& name() const override { return m_name; };
    void set_name(const FlyString& name);

    void set_is_class_constructor() { m_is_class_constructor = true; };

    auto& bytecode_executable() const { return m_bytecode_executable; }

    virtual Environment* environment() override { return m_environment; }
    virtual Realm* realm() const override { return m_realm; }

protected:
    virtual bool is_strict_mode() const final { return m_is_strict; }

private:
    virtual bool is_ecmascript_function_object() const override { return true; }
    virtual FunctionEnvironment* create_environment(FunctionObject&) override;
    virtual void visit_edges(Visitor&) override;

    Value execute_function_body();

    FlyString m_name;
    NonnullRefPtr<Statement> m_body;
    const Vector<FunctionNode::Parameter> m_parameters;
    Optional<Bytecode::Executable> m_bytecode_executable;
    Environment* m_environment { nullptr };
    Realm* m_realm { nullptr };
    i32 m_function_length { 0 };
    FunctionKind m_kind { FunctionKind::Regular };
    bool m_is_strict { false };
    bool m_is_arrow_function { false };
    bool m_is_class_constructor { false };
};

}
