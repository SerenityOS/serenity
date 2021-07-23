/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainDatePrototype final : public Object {
    JS_OBJECT(PlainDatePrototype, Object);

public:
    explicit PlainDatePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PlainDatePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(calendar_getter);
    JS_DECLARE_NATIVE_FUNCTION(year_getter);
    JS_DECLARE_NATIVE_FUNCTION(month_getter);
    JS_DECLARE_NATIVE_FUNCTION(month_code_getter);

    JS_DECLARE_NATIVE_FUNCTION(with_calendar);
    JS_DECLARE_NATIVE_FUNCTION(equals);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
};

}
