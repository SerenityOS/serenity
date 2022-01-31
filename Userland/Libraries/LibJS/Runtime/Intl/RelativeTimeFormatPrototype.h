/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/RelativeTimeFormat.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class RelativeTimeFormatPrototype final : public PrototypeObject<RelativeTimeFormatPrototype, RelativeTimeFormat> {
    JS_PROTOTYPE_OBJECT(RelativeTimeFormatPrototype, RelativeTimeFormat, Intl.RelativeTimeFormat);

public:
    explicit RelativeTimeFormatPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~RelativeTimeFormatPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(format);
    JS_DECLARE_NATIVE_FUNCTION(format_to_parts);
    JS_DECLARE_NATIVE_FUNCTION(resolved_options);
};

}
