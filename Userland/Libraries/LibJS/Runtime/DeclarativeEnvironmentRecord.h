/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <LibJS/Runtime/EnvironmentRecord.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class DeclarativeEnvironmentRecord final : public EnvironmentRecord {
    JS_OBJECT(DeclarativeEnvironmentRecord, EnvironmentRecord);

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

    DeclarativeEnvironmentRecord();
    DeclarativeEnvironmentRecord(EnvironmentRecordType);
    DeclarativeEnvironmentRecord(HashMap<FlyString, Variable> variables, EnvironmentRecord* parent_scope);
    DeclarativeEnvironmentRecord(HashMap<FlyString, Variable> variables, EnvironmentRecord* parent_scope, EnvironmentRecordType);
    virtual ~DeclarativeEnvironmentRecord() override;

    // ^EnvironmentRecord
    virtual Optional<Variable> get_from_environment_record(FlyString const&) const override;
    virtual void put_into_environment_record(FlyString const&, Variable) override;
    virtual bool delete_from_environment_record(FlyString const&) override;
    virtual bool has_this_binding() const override;
    virtual Value get_this_binding(GlobalObject&) const override;

    HashMap<FlyString, Variable> const& variables() const { return m_variables; }

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
    virtual bool is_declarative_environment_record() const override { return true; }
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

template<>
inline bool Object::fast_is<DeclarativeEnvironmentRecord>() const { return is_declarative_environment_record(); }

}
