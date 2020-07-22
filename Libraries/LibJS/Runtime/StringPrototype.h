/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    JS_DECLARE_NATIVE_FUNCTION(char_at);
    JS_DECLARE_NATIVE_FUNCTION(char_code_at);
    JS_DECLARE_NATIVE_FUNCTION(repeat);
    JS_DECLARE_NATIVE_FUNCTION(starts_with);
    JS_DECLARE_NATIVE_FUNCTION(index_of);
    JS_DECLARE_NATIVE_FUNCTION(to_lowercase);
    JS_DECLARE_NATIVE_FUNCTION(to_uppercase);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(pad_start);
    JS_DECLARE_NATIVE_FUNCTION(pad_end);
    JS_DECLARE_NATIVE_FUNCTION(substring);

    JS_DECLARE_NATIVE_GETTER(length_getter);

    JS_DECLARE_NATIVE_FUNCTION(trim);
    JS_DECLARE_NATIVE_FUNCTION(trim_start);
    JS_DECLARE_NATIVE_FUNCTION(trim_end);
    JS_DECLARE_NATIVE_FUNCTION(concat);
    JS_DECLARE_NATIVE_FUNCTION(includes);
    JS_DECLARE_NATIVE_FUNCTION(slice);
    JS_DECLARE_NATIVE_FUNCTION(last_index_of);

    JS_DECLARE_NATIVE_FUNCTION(symbol_iterator);
};

}
