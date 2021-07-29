/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainDateTimePrototype final : public Object {
    JS_OBJECT(PlainDateTimePrototype, Object);

public:
    explicit PlainDateTimePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PlainDateTimePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(calendar_getter);
    JS_DECLARE_NATIVE_FUNCTION(year_getter);
    JS_DECLARE_NATIVE_FUNCTION(month_getter);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
    JS_DECLARE_NATIVE_FUNCTION(to_plain_date);
    JS_DECLARE_NATIVE_FUNCTION(get_iso_fields);
};

}
