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
    static NumberObject* create(Realm&, double);

    virtual ~NumberObject() override = default;

    double number() const { return m_value; }

protected:
    NumberObject(double, Object& prototype);

private:
    double m_value { 0 };
};

}
