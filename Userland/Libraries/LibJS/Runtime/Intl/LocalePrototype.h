/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/Locale.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class LocalePrototype final : public PrototypeObject<LocalePrototype, Locale> {
    JS_PROTOTYPE_OBJECT(LocalePrototype, Locale, Intl.Locale);

public:
    explicit LocalePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~LocalePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(maximize);
    JS_DECLARE_NATIVE_FUNCTION(minimize);
    JS_DECLARE_NATIVE_FUNCTION(to_string);

    JS_DECLARE_NATIVE_FUNCTION(base_name);
    JS_DECLARE_NATIVE_FUNCTION(calendar);
    JS_DECLARE_NATIVE_FUNCTION(case_first);
    JS_DECLARE_NATIVE_FUNCTION(collation);
    JS_DECLARE_NATIVE_FUNCTION(hour_cycle);
    JS_DECLARE_NATIVE_FUNCTION(numbering_system);
    JS_DECLARE_NATIVE_FUNCTION(numeric);
    JS_DECLARE_NATIVE_FUNCTION(language);
    JS_DECLARE_NATIVE_FUNCTION(script);
    JS_DECLARE_NATIVE_FUNCTION(region);
};

}
