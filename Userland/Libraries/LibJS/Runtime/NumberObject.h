/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class NumberObject : public Object {
    JS_OBJECT(NumberObject, Object);

public:
    static NumberObject* create(GlobalObject&, double);

    NumberObject(double, Object& prototype);
    virtual ~NumberObject() override;

    double number() const { return m_value; }

private:
    double m_value { 0 };
};

}
