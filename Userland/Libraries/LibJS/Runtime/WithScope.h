/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/ScopeObject.h>

namespace JS {

class WithScope : public ScopeObject {
    JS_OBJECT(WithScope, ScopeObject);

public:
    WithScope(Object&, ScopeObject* parent_scope);

    virtual Optional<Variable> get_from_scope(const FlyString&) const override;
    virtual void put_to_scope(const FlyString&, Variable) override;
    virtual bool has_this_binding() const override;
    virtual Value get_this_binding(GlobalObject&) const override;

private:
    virtual void visit_edges(Visitor&) override;

    Object& m_object;
};

}
