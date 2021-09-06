/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class InstantPrototype final : public Object {
    JS_OBJECT(InstantPrototype, Object);

public:
    explicit InstantPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~InstantPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(epoch_seconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(epoch_milliseconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(epoch_microseconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(epoch_nanoseconds_getter);
    JS_DECLARE_NATIVE_FUNCTION(add);
    JS_DECLARE_NATIVE_FUNCTION(subtract);
    JS_DECLARE_NATIVE_FUNCTION(until);
    JS_DECLARE_NATIVE_FUNCTION(round);
    JS_DECLARE_NATIVE_FUNCTION(equals);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(to_locale_string);
    JS_DECLARE_NATIVE_FUNCTION(to_json);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
};

}
