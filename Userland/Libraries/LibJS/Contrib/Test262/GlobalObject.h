/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Contrib/Test262/$262Object.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS::Test262 {

class GlobalObject final : public JS::GlobalObject {
    JS_OBJECT(GlobalObject, JS::GlobalObject);

public:
    GlobalObject() = default;
    virtual void initialize_global_object() override;
    virtual ~GlobalObject() override = default;

    $262Object* $262() const { return m_$262; }

private:
    virtual void visit_edges(Visitor&) override;

    $262Object* m_$262 { nullptr };

    JS_DECLARE_NATIVE_FUNCTION(print);
};

}
