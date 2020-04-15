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

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

struct Variable {
    Value value;
    DeclarationKind declaration_kind;
};

class LexicalEnvironment final : public Cell {
public:
    LexicalEnvironment();
    LexicalEnvironment(HashMap<FlyString, Variable> variables, LexicalEnvironment* parent);
    virtual ~LexicalEnvironment() override;

    LexicalEnvironment* parent() { return m_parent; }

    Optional<Variable> get(const FlyString&) const;
    void set(const FlyString&, Variable);

    void clear();

    const HashMap<FlyString, Variable>& variables() const { return m_variables; }

private:
    virtual const char* class_name() const override { return "LexicalEnvironment"; }
    virtual void visit_children(Visitor&) override;

    LexicalEnvironment* m_parent { nullptr };
    HashMap<FlyString, Variable> m_variables;
};

}
