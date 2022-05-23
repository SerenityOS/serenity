/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/AsyncGenerator.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS {

class AsyncGeneratorPrototype final : public PrototypeObject<AsyncGeneratorPrototype, AsyncGenerator> {
    JS_PROTOTYPE_OBJECT(AsyncGeneratorPrototype, AsyncGenerator, AsyncGenerator)

public:
    explicit AsyncGeneratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~AsyncGeneratorPrototype() override = default;
};

}
