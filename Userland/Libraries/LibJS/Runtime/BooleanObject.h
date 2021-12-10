/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {
class BooleanObject : public Object {
    JS_OBJECT(BooleanObject, Object);

public:
    static BooleanObject* create(GlobalObject&, bool);

    BooleanObject(bool, Object& prototype);
    virtual ~BooleanObject() override;

    bool boolean() const { return m_value; }

private:
    bool m_value { false };
};
}
