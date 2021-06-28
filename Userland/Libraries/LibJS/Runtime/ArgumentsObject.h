/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class ArgumentsObject final : public Object {
    JS_OBJECT(ArgumentsObject, Object);

public:
    explicit ArgumentsObject(GlobalObject&);

    virtual void initialize(GlobalObject&) override;
    virtual ~ArgumentsObject() override;
};

}
