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

class OrdinaryFunctionObject final : public FunctionObject {
    JS_OBJECT(OrdinaryFunctionObject, FunctionObject);

public:
    static OrdinaryFunctionObject* create(GlobalObject&, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, EnvironmentRecord* parent_scope, FunctionKind, bool is_strict, bool is_arrow_function = false);

    OrdinaryFunctionObject(GlobalObject&, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, EnvironmentRecord* parent_scope, Object& prototype, FunctionKind, bool is_strict, bool is_arrow_function = false);
    virtual void initialize(GlobalObject&) override;
    virtual ~OrdinaryFunctionObject();

    const Statement& body() const { return m_body; }
    const Vector<FunctionNode::Parameter>& parameters() const { return m_parameters; };

    virtual Value call() override;
    virtual Value construct(FunctionObject& new_target) override;

    virtual const FlyString& name() const override { return m_name; };
    void set_name(const FlyString& name) { m_name = name; };

    void set_is_class_constructor() { m_is_class_constructor = true; };

    auto& bytecode_executable() const { return m_bytecode_executable; }

    virtual EnvironmentRecord* environment() override { return m_environment; }

protected:
    virtual bool is_strict_mode() const final { return m_is_strict; }

private:
    virtual FunctionEnvironmentRecord* create_environment_record(FunctionObject&) override;
    virtual void visit_edges(Visitor&) override;

    Value execute_function_body();

    JS_DECLARE_NATIVE_GETTER(length_getter);
    JS_DECLARE_NATIVE_GETTER(name_getter);

    FlyString m_name;
    NonnullRefPtr<Statement> m_body;
    const Vector<FunctionNode::Parameter> m_parameters;
    Optional<Bytecode::Executable> m_bytecode_executable;
    EnvironmentRecord* m_environment { nullptr };
    i32 m_function_length { 0 };
    FunctionKind m_kind { FunctionKind::Regular };
    bool m_is_strict { false };
    bool m_is_arrow_function { false };
    bool m_is_class_constructor { false };
};

}
