/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

class ScriptFunction final : public Function {
public:
    static ScriptFunction* create(GlobalObject&, const FlyString& name, const Statement& body, Vector<FlyString> parameters, LexicalEnvironment* parent_environment);

    ScriptFunction(const FlyString& name, const Statement& body, Vector<FlyString> parameters, LexicalEnvironment* parent_environment, Object& prototype);
    virtual ~ScriptFunction();

    const Statement& body() const { return m_body; }
    const Vector<FlyString>& parameters() const { return m_parameters; };

    virtual Value call(Interpreter&) override;
    virtual Value construct(Interpreter&) override;

    virtual const FlyString& name() const override { return m_name; };

private:
    virtual bool is_script_function() const final { return true; }
    virtual const char* class_name() const override { return "ScriptFunction"; }
    virtual LexicalEnvironment* create_environment() override;
    virtual void visit_children(Visitor&) override;

    static Value length_getter(Interpreter&);
    static void length_setter(Interpreter&, Value);

    FlyString m_name;
    NonnullRefPtr<Statement> m_body;
    const Vector<FlyString> m_parameters;
    LexicalEnvironment* m_parent_environment { nullptr };
};

}
