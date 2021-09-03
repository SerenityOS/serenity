/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class LocalePrototype final : public Object {
    JS_OBJECT(LocalePrototype, Object);

public:
    explicit LocalePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~LocalePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(maximize);
    JS_DECLARE_NATIVE_FUNCTION(minimize);
    JS_DECLARE_NATIVE_FUNCTION(to_string);

    JS_DECLARE_NATIVE_GETTER(base_name);
    JS_DECLARE_NATIVE_GETTER(calendar);
    JS_DECLARE_NATIVE_GETTER(case_first);
    JS_DECLARE_NATIVE_GETTER(collation);
    JS_DECLARE_NATIVE_GETTER(hour_cycle);
    JS_DECLARE_NATIVE_GETTER(numbering_system);
    JS_DECLARE_NATIVE_GETTER(numeric);
    JS_DECLARE_NATIVE_GETTER(language);
    JS_DECLARE_NATIVE_GETTER(script);
    JS_DECLARE_NATIVE_GETTER(region);
};

}
