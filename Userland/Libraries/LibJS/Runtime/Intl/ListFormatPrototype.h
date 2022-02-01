/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/ListFormat.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class ListFormatPrototype final : public PrototypeObject<ListFormatPrototype, ListFormat> {
    JS_PROTOTYPE_OBJECT(ListFormatPrototype, ListFormat, Intl.ListFormat);

public:
    explicit ListFormatPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ListFormatPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(format);
    JS_DECLARE_NATIVE_FUNCTION(format_to_parts);
    JS_DECLARE_NATIVE_FUNCTION(resolved_options);
};

}
