/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/EnvironmentRecord.h>

namespace JS {

class ObjectEnvironmentRecord : public EnvironmentRecord {
    JS_ENVIRONMENT_RECORD(ObjectEnvironmentRecord, EnvironmentRecord);

public:
    enum class IsWithEnvironment {
        No,
        Yes,
    };
    ObjectEnvironmentRecord(Object&, IsWithEnvironment, EnvironmentRecord* parent_scope);

    virtual Optional<Variable> get_from_environment_record(FlyString const&) const override;
    virtual void put_into_environment_record(FlyString const&, Variable) override;
    virtual bool delete_from_environment_record(FlyString const&) override;

    virtual bool has_binding(FlyString const& name) const override;
    virtual void create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted) override;
    virtual void create_immutable_binding(GlobalObject&, FlyString const& name, bool strict) override;
    virtual void initialize_binding(GlobalObject&, FlyString const& name, Value) override;
    virtual void set_mutable_binding(GlobalObject&, FlyString const& name, Value, bool strict) override;
    virtual Value get_binding_value(GlobalObject&, FlyString const& name, bool strict) override;
    virtual bool delete_binding(GlobalObject&, FlyString const& name) override;

    // 9.1.1.2.10 WithBaseObject ( ), https://tc39.es/ecma262/#sec-object-environment-records-withbaseobject
    virtual Object* with_base_object() const override
    {
        if (is_with_environment())
            return &m_object;
        return nullptr;
    }

    bool is_with_environment() const { return m_with_environment; }

    Object& object() { return m_object; }

private:
    virtual void visit_edges(Visitor&) override;

    Object& m_object;
    bool m_with_environment { false };
};

}
