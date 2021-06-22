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

class DeclarativeEnvironmentRecord : public EnvironmentRecord {
    JS_OBJECT(DeclarativeEnvironmentRecord, EnvironmentRecord);

public:
    enum class EnvironmentRecordType {
        Declarative,
        Function,
        Object,
        Module,
    };

    DeclarativeEnvironmentRecord();
    DeclarativeEnvironmentRecord(EnvironmentRecordType);
    explicit DeclarativeEnvironmentRecord(EnvironmentRecord* parent_scope);
    DeclarativeEnvironmentRecord(HashMap<FlyString, Variable> variables, EnvironmentRecord* parent_scope);
    DeclarativeEnvironmentRecord(HashMap<FlyString, Variable> variables, EnvironmentRecord* parent_scope, EnvironmentRecordType);
    virtual ~DeclarativeEnvironmentRecord() override;

    // ^EnvironmentRecord
    virtual Optional<Variable> get_from_environment_record(FlyString const&) const override;
    virtual void put_into_environment_record(FlyString const&, Variable) override;
    virtual bool delete_from_environment_record(FlyString const&) override;

    HashMap<FlyString, Variable> const& variables() const { return m_variables; }

    EnvironmentRecordType type() const { return m_environment_record_type; }

protected:
    virtual void visit_edges(Visitor&) override;

private:
    virtual bool is_declarative_environment_record() const override { return true; }

    EnvironmentRecordType m_environment_record_type : 8 { EnvironmentRecordType::Declarative };
    HashMap<FlyString, Variable> m_variables;
};

template<>
inline bool Object::fast_is<DeclarativeEnvironmentRecord>() const { return is_declarative_environment_record(); }

}
