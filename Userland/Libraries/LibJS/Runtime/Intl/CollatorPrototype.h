/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/Collator.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class CollatorPrototype final : public PrototypeObject<CollatorPrototype, Collator> {
    JS_PROTOTYPE_OBJECT(CollatorPrototype, Collator, Collator);

public:
    explicit CollatorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~CollatorPrototype() override = default;
};

}
