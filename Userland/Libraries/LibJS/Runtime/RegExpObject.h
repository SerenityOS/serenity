/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Result.h>
#include <LibJS/AST.h>
#include <LibJS/Runtime/Object.h>
#include <LibRegex/Regex.h>

namespace JS {

ThrowCompletionOr<RegExpObject*> regexp_create(GlobalObject&, Value pattern, Value flags);

Result<regex::RegexOptions<ECMAScriptFlags>, String> regex_flags_from_string(StringView flags);
String parse_regex_pattern(StringView pattern, bool unicode);

class RegExpObject : public Object {
    JS_OBJECT(RegExpObject, Object);

public:
    // JS regexps are all 'global' by default as per our definition, but the "global" flag enables "stateful".
    // FIXME: Enable 'BrowserExtended' only if in a browser context.
    static constexpr regex::RegexOptions<ECMAScriptFlags> default_flags { (regex::ECMAScriptFlags)regex::AllFlags::Global | (regex::ECMAScriptFlags)regex::AllFlags::SkipTrimEmptyMatches | regex::ECMAScriptFlags::BrowserExtended };

    static RegExpObject* create(GlobalObject&);
    static RegExpObject* create(GlobalObject&, Regex<ECMA262> regex, String pattern, String flags);

    RegExpObject(Object& prototype);
    RegExpObject(Regex<ECMA262> regex, String pattern, String flags, Object& prototype);

    ThrowCompletionOr<RegExpObject*> regexp_initialize(GlobalObject&, Value pattern, Value flags);
    String escape_regexp_pattern() const;

    virtual void initialize(GlobalObject&) override;
    virtual ~RegExpObject() override;

    const String& pattern() const { return m_pattern; }
    const String& flags() const { return m_flags; }
    const Regex<ECMA262>& regex() { return *m_regex; }
    const Regex<ECMA262>& regex() const { return *m_regex; }

private:
    String m_pattern;
    String m_flags;
    Optional<Regex<ECMA262>> m_regex;
};

}
