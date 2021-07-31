/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class TimeZonePrototype final : public Object {
    JS_OBJECT(TimeZonePrototype, Object);

public:
    explicit TimeZonePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~TimeZonePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(id_getter);
    JS_DECLARE_NATIVE_FUNCTION(get_offset_nanoseconds_for);
    JS_DECLARE_NATIVE_FUNCTION(get_offset_string_for);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(to_json);
};

}
