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

struct Binding {
    Value value;
    bool strict;
    bool mutable_ { false };
    bool can_be_deleted { false };
    bool initialized { false };
};

class DeclarativeEnvironmentRecord : public EnvironmentRecord {
    JS_ENVIRONMENT_RECORD(DeclarativeEnvironmentRecord, EnvironmentRecord);

public:
    DeclarativeEnvironmentRecord();
    explicit DeclarativeEnvironmentRecord(EnvironmentRecord* parent_scope);
    DeclarativeEnvironmentRecord(HashMap<FlyString, Variable> variables, EnvironmentRecord* parent_scope);
    virtual ~DeclarativeEnvironmentRecord() override;

    // ^EnvironmentRecord
    virtual Optional<Variable> get_from_environment_record(FlyString const&) const override;
    virtual void put_into_environment_record(FlyString const&, Variable) override;
    virtual bool delete_from_environment_record(FlyString const&) override;

    HashMap<FlyString, Variable> const& variables() const { return m_variables; }

    virtual bool has_binding(FlyString const& name) const override;
    virtual void create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted) override;
    virtual void create_immutable_binding(GlobalObject&, FlyString const& name, bool strict) override;
    virtual void initialize_binding(GlobalObject&, FlyString const& name, Value) override;
    virtual void set_mutable_binding(GlobalObject&, FlyString const& name, Value, bool strict) override;
    virtual Value get_binding_value(GlobalObject&, FlyString const& name, bool strict) override;
    virtual bool delete_binding(GlobalObject&, FlyString const& name) override;

protected:
    virtual void visit_edges(Visitor&) override;

private:
    virtual bool is_declarative_environment_record() const override { return true; }

    HashMap<FlyString, Variable> m_variables;
    HashMap<FlyString, Binding> m_bindings;
};

template<>
inline bool EnvironmentRecord::fast_is<DeclarativeEnvironmentRecord>() const { return is_declarative_environment_record(); }

}
