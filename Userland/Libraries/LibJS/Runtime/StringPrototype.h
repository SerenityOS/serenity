/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/StringObject.h>

namespace JS {

class StringPrototype final : public StringObject {
    JS_OBJECT(StringPrototype, StringObject);

public:
    explicit StringPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~StringPrototype() override;

private:
    JS_DECLARE_NATIVE_GETTER(length_getter);

    JS_DECLARE_NATIVE_FUNCTION(char_at);
    JS_DECLARE_NATIVE_FUNCTION(char_code_at);
    JS_DECLARE_NATIVE_FUNCTION(repeat);
    JS_DECLARE_NATIVE_FUNCTION(starts_with);
    JS_DECLARE_NATIVE_FUNCTION(ends_with);
    JS_DECLARE_NATIVE_FUNCTION(index_of);
    JS_DECLARE_NATIVE_FUNCTION(to_lowercase);
    JS_DECLARE_NATIVE_FUNCTION(to_uppercase);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(pad_start);
    JS_DECLARE_NATIVE_FUNCTION(pad_end);
    JS_DECLARE_NATIVE_FUNCTION(substring);
    JS_DECLARE_NATIVE_FUNCTION(substr);
    JS_DECLARE_NATIVE_FUNCTION(trim);
    JS_DECLARE_NATIVE_FUNCTION(trim_start);
    JS_DECLARE_NATIVE_FUNCTION(trim_end);
    JS_DECLARE_NATIVE_FUNCTION(concat);
    JS_DECLARE_NATIVE_FUNCTION(includes);
    JS_DECLARE_NATIVE_FUNCTION(slice);
    JS_DECLARE_NATIVE_FUNCTION(split);
    JS_DECLARE_NATIVE_FUNCTION(last_index_of);
    JS_DECLARE_NATIVE_FUNCTION(at);
    JS_DECLARE_NATIVE_FUNCTION(match);
    JS_DECLARE_NATIVE_FUNCTION(replace);
    JS_DECLARE_NATIVE_FUNCTION(anchor);
    JS_DECLARE_NATIVE_FUNCTION(big);
    JS_DECLARE_NATIVE_FUNCTION(blink);
    JS_DECLARE_NATIVE_FUNCTION(bold);
    JS_DECLARE_NATIVE_FUNCTION(fixed);
    JS_DECLARE_NATIVE_FUNCTION(fontcolor);
    JS_DECLARE_NATIVE_FUNCTION(fontsize);
    JS_DECLARE_NATIVE_FUNCTION(italics);
    JS_DECLARE_NATIVE_FUNCTION(link);
    JS_DECLARE_NATIVE_FUNCTION(small);
    JS_DECLARE_NATIVE_FUNCTION(strike);
    JS_DECLARE_NATIVE_FUNCTION(sub);
    JS_DECLARE_NATIVE_FUNCTION(sup);

    JS_DECLARE_NATIVE_FUNCTION(symbol_iterator);
};

}
