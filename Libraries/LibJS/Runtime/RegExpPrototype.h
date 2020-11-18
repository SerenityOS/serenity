/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <LibJS/Runtime/RegExpObject.h>

namespace JS {

class RegExpPrototype final : public RegExpObject {
    JS_OBJECT(RegExpPrototype, RegExpObject);

public:
    explicit RegExpPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~RegExpPrototype() override;

private:
    static RegexResult do_match(const Regex<ECMA262>&, const StringView&);

    JS_DECLARE_NATIVE_GETTER(dot_all);
    JS_DECLARE_NATIVE_GETTER(flags);
    JS_DECLARE_NATIVE_GETTER(global);
    JS_DECLARE_NATIVE_GETTER(ignore_case);
    JS_DECLARE_NATIVE_GETTER(multiline);
    JS_DECLARE_NATIVE_GETTER(source);
    JS_DECLARE_NATIVE_GETTER(sticky);
    JS_DECLARE_NATIVE_GETTER(unicode);

    JS_DECLARE_NATIVE_FUNCTION(test);
    JS_DECLARE_NATIVE_FUNCTION(to_string);
};

}
