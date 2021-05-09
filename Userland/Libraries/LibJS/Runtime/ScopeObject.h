/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

class ScopeObject : public Object {
    JS_OBJECT(ScopeObject, Object);

public:
    virtual Optional<Variable> get_from_scope(const FlyString&) const = 0;
    virtual void put_to_scope(const FlyString&, Variable) = 0;
    virtual bool has_this_binding() const = 0;
    virtual Value get_this_binding(GlobalObject&) const = 0;

    ScopeObject* parent() { return m_parent; }
    const ScopeObject* parent() const { return m_parent; }

protected:
    explicit ScopeObject(ScopeObject* parent);
    explicit ScopeObject(GlobalObjectTag);

    virtual void visit_edges(Visitor&) override;

private:
    ScopeObject* m_parent { nullptr };
};

}
