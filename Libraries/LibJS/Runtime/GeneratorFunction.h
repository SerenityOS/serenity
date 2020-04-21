/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <LibJS/Runtime/ScriptFunction.h>

namespace JS {

class GeneratorFunction final : public ScriptFunction {
public:
    static GeneratorFunction* create(GlobalObject&, const FlyString& name, const Statement& body, Vector<FlyString> parameters, LexicalEnvironment* parent_environment, LexicalEnvironment* own_environment = nullptr);

    GeneratorFunction(const FlyString& name, const Statement& body, Vector<FlyString> parameters, LexicalEnvironment* parent_environment, Object& prototype, LexicalEnvironment* own_environment);
    virtual ~GeneratorFunction();

    virtual Value call(Interpreter&) override;
    virtual Value construct(Interpreter&) override;

private:
    virtual bool is_generator_function() const final { return true; }
    virtual const char* class_name() const override { return "GeneratorFunction"; }
    virtual LexicalEnvironment* create_environment() override;
    virtual void visit_children(Visitor&) override;

    static Value iterator_method(Interpreter&);
    static Value next(Interpreter&);

    bool m_done { false };
    LexicalEnvironment* m_own_environment { nullptr };
};

}
