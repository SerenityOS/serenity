/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/EnvironmentRecord.h>

namespace JS {

class GlobalEnvironmentRecord final : public EnvironmentRecord {
    JS_OBJECT(GlobalEnvironmentRecord, EnvironmentRecord);

public:
    explicit GlobalEnvironmentRecord(GlobalObject&);

    virtual Optional<Variable> get_from_environment_record(FlyString const&) const override;
    virtual void put_into_environment_record(FlyString const&, Variable) override;
    virtual bool delete_from_environment_record(FlyString const&) override;
    virtual bool has_this_binding() const final { return true; }
    virtual Value get_this_binding(GlobalObject&) const final;

    Value global_this_value() const;

    // [[ObjectRecord]]
    ObjectEnvironmentRecord& object_record() { return *m_object_record; }

    // [[DeclarativeReco rd]]
    DeclarativeEnvironmentRecord& declarative_record() { return *m_declarative_record; }

private:
    virtual bool is_global_environment_record() const override { return true; }
    virtual void visit_edges(Visitor&) override;

    GlobalObject& m_global_object;

    ObjectEnvironmentRecord* m_object_record { nullptr };
    DeclarativeEnvironmentRecord* m_declarative_record { nullptr };
};

template<>
inline bool Object::fast_is<GlobalEnvironmentRecord>() const { return is_global_environment_record(); }

}
