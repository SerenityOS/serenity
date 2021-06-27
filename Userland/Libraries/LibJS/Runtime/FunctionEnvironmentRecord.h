/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/DeclarativeEnvironmentRecord.h>

namespace JS {

class FunctionEnvironmentRecord final : public DeclarativeEnvironmentRecord {
    JS_ENVIRONMENT_RECORD(FunctionEnvironmentRecord, DeclarativeEnvironmentRecord);

public:
    enum class ThisBindingStatus : u8 {
        Lexical,
        Initialized,
        Uninitialized,
    };

    FunctionEnvironmentRecord(EnvironmentRecord* parent_scope, HashMap<FlyString, Variable> variables);
    virtual ~FunctionEnvironmentRecord() override;

    // [[ThisValue]]
    Value this_value() const { return m_this_value; }
    void set_this_value(Value value) { m_this_value = value; }

    // Not a standard operation.
    void replace_this_binding(Value this_value) { m_this_value = this_value; }

    // [[ThisBindingStatus]]
    ThisBindingStatus this_binding_status() const { return m_this_binding_status; }
    void set_this_binding_status(ThisBindingStatus status) { m_this_binding_status = status; }

    // [[FunctionObject]]
    FunctionObject& function_object() { return *m_function_object; }
    FunctionObject const& function_object() const { return *m_function_object; }
    void set_function_object(FunctionObject& function) { m_function_object = &function; }

    // [[NewTarget]]
    Value new_target() const { return m_new_target; }
    void set_new_target(Value new_target) { m_new_target = new_target; }

    // Abstract operations
    Value get_super_base() const;
    bool has_super_binding() const;
    virtual bool has_this_binding() const override;
    virtual Value get_this_binding(GlobalObject&) const override;
    Value bind_this_value(GlobalObject&, Value);

private:
    virtual bool is_function_environment_record() const override { return true; }
    virtual void visit_edges(Visitor&) override;

    Value m_this_value;
    ThisBindingStatus m_this_binding_status { ThisBindingStatus::Uninitialized };
    FunctionObject* m_function_object { nullptr };
    Value m_new_target;
};

template<>
inline bool EnvironmentRecord::fast_is<FunctionEnvironmentRecord>() const { return is_function_environment_record(); }

}
