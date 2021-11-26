/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class DisplayNamesPrototype final : public PrototypeObject<DisplayNamesPrototype, DisplayNames> {
    JS_PROTOTYPE_OBJECT(DisplayNamesPrototype, DisplayNames, Intl.DisplayNames);

public:
    explicit DisplayNamesPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~DisplayNamesPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(of);
    JS_DECLARE_NATIVE_FUNCTION(resolved_options);
};

}
