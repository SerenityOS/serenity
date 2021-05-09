/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
