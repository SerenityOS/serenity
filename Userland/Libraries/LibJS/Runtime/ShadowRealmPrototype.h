/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PrototypeObject.h>
#include <LibJS/Runtime/ShadowRealm.h>

namespace JS {

class ShadowRealmPrototype final : public PrototypeObject<ShadowRealmPrototype, ShadowRealm> {
    JS_PROTOTYPE_OBJECT(ShadowRealmPrototype, ShadowRealm, ShadowRealm);

public:
    explicit ShadowRealmPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ShadowRealmPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(evaluate);
    JS_DECLARE_NATIVE_FUNCTION(import_value);
};

}
