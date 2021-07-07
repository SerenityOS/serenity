/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    String m_pattern;
    String m_flags;
    Flags m_active_flags;
    Regex<ECMA262> m_regex;
};

}
