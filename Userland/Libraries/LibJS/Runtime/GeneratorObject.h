/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/OrdinaryFunctionObject.h>

namespace JS {

class GeneratorObject final : public Object {
    JS_OBJECT(GeneratorObject, Object);

public:
    static GeneratorObject* create(GlobalObject&, Value, OrdinaryFunctionObject*, Environment*, Bytecode::RegisterWindow);
    GeneratorObject(GlobalObject&, Object& prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~GeneratorObject() override;
    void visit_edges(Cell::Visitor&) override;

    Value next_impl(VM&, GlobalObject&, Optional<Value> value_to_throw);
    void set_done() { m_done = true; }

private:
    Environment* m_environment { nullptr };
    OrdinaryFunctionObject* m_generating_function { nullptr };
    Value m_previous_value;
    Bytecode::RegisterWindow m_frame;
    bool m_done { false };
};

}
