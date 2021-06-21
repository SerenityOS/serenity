/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

struct Variable {
    Value value;
    DeclarationKind declaration_kind;
};

class EnvironmentRecord : public Object {
    JS_OBJECT(EnvironmentRecord, Object);

public:
    virtual Optional<Variable> get_from_environment_record(FlyString const&) const = 0;
    virtual void put_into_environment_record(FlyString const&, Variable) = 0;
    virtual bool delete_from_environment_record(FlyString const&) = 0;

    virtual bool has_this_binding() const = 0;
    virtual Value get_this_binding(GlobalObject&) const = 0;

    // [[OuterEnv]]
    EnvironmentRecord* outer_environment() { return m_outer_environment; }
    EnvironmentRecord const* outer_environment() const { return m_outer_environment; }

protected:
    explicit EnvironmentRecord(EnvironmentRecord* parent);
    explicit EnvironmentRecord(GlobalObjectTag);

    virtual void visit_edges(Visitor&) override;

private:
    EnvironmentRecord* m_outer_environment { nullptr };
};

}
