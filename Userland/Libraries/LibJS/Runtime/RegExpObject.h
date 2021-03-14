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

#include <LibJS/AST.h>
#include <LibJS/Runtime/Object.h>
#include <LibRegex/Regex.h>

struct Flags {
    regex::RegexOptions<ECMAScriptFlags> effective_flags;
    regex::RegexOptions<ECMAScriptFlags> declared_flags;
};

namespace JS {

RegExpObject* regexp_create(GlobalObject&, Value pattern, Value flags);

class RegExpObject : public Object {
    JS_OBJECT(RegExpObject, Object);

public:
    static RegExpObject* create(GlobalObject&, String pattern, String flags);

    RegExpObject(String pattern, String flags, Object& prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~RegExpObject() override;

    const String& pattern() const { return m_pattern; }
    const String& flags() const { return m_flags; }
    const regex::RegexOptions<ECMAScriptFlags>& declared_options() { return m_active_flags.declared_flags; }
    const Regex<ECMA262>& regex() { return m_regex; }
    const Regex<ECMA262>& regex() const { return m_regex; }

private:
    JS_DECLARE_NATIVE_GETTER(last_index);
    JS_DECLARE_NATIVE_SETTER(set_last_index);

    String m_pattern;
    String m_flags;
    Flags m_active_flags;
    Regex<ECMA262> m_regex;
};

}
