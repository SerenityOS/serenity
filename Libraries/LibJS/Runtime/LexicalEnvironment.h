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
#include <LibJS/Runtime/ScopeObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class LexicalEnvironment final : public ScopeObject {
    JS_OBJECT(LexicalEnvironment, ScopeObject);

public:
    enum class ThisBindingStatus {
        Lexical,
        Initialized,
        Uninitialized,
    };

    enum class EnvironmentRecordType {
        Declarative,
        Function,
        Object,
        Module,
    };

    LexicalEnvironment();
    LexicalEnvironment(EnvironmentRecordType);
    LexicalEnvironment(HashMap<FlyString, Variable> variables, ScopeObject* parent_scope);
    LexicalEnvironment(HashMap<FlyString, Variable> variables, ScopeObject* parent_scope, EnvironmentRecordType);
    virtual ~LexicalEnvironment() override;

    // ^ScopeObject
    virtual Optional<Variable> get_from_scope(const FlyString&) const override;
    virtual void put_to_scope(const FlyString&, Variable) override;
    virtual bool has_this_binding() const override;
    virtual Value get_this_binding(GlobalObject&) const override;

    void clear();

    const HashMap<FlyString, Variable>& variables() const { return m_variables; }

    void set_home_object(Value object) { m_home_object = object; }
    bool has_super_binding() const;
    Value get_super_base();

    ThisBindingStatus this_binding_status() const { return m_this_binding_status; }
    void bind_this_value(GlobalObject&, Value this_value);

    // Not a standard operation.
    void replace_this_binding(Value this_value) { m_this_value = this_value; }

    Value new_target() const { return m_new_target; };
    void set_new_target(Value new_target) { m_new_target = new_target; }

    Function* current_function() const { return m_current_function; }
    void set_current_function(Function& function) { m_current_function = &function; }

    EnvironmentRecordType type() const { return m_environment_record_type; }

private:
    virtual void visit_edges(Visitor&) override;

    EnvironmentRecordType m_environment_record_type : 8 { EnvironmentRecordType::Declarative };
    ThisBindingStatus m_this_binding_status : 8 { ThisBindingStatus::Uninitialized };
    HashMap<FlyString, Variable> m_variables;
    Value m_home_object;
    Value m_this_value;
    Value m_new_target;
    // Corresponds to [[FunctionObject]]
    Function* m_current_function { nullptr };
};

}
