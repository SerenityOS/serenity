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
    virtual Optional<Variable> get_from_scope(const FlyString&) const = 0;
    virtual void put_to_scope(const FlyString&, Variable) = 0;
    virtual bool delete_from_scope(FlyString const&) = 0;
    virtual bool has_this_binding() const = 0;
    virtual Value get_this_binding(GlobalObject&) const = 0;

    EnvironmentRecord* parent() { return m_parent; }
    const EnvironmentRecord* parent() const { return m_parent; }

protected:
    explicit EnvironmentRecord(EnvironmentRecord* parent);
    explicit EnvironmentRecord(GlobalObjectTag);

    virtual void visit_edges(Visitor&) override;

private:
    EnvironmentRecord* m_parent { nullptr };
};

}
