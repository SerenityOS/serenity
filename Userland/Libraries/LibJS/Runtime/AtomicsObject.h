/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class AtomicsObject : public Object {
    JS_OBJECT(AtomicsObject, Object);

public:
    explicit AtomicsObject(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~AtomicsObject() override = default;
};

}
