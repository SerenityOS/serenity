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

ThrowCompletionOr<NonnullGCPtr<RegExpObject>> regexp_create(VM&, Value pattern, Value flags);
ThrowCompletionOr<NonnullGCPtr<RegExpObject>> regexp_alloc(VM&, FunctionObject& new_target);

Result<regex::RegexOptions<ECMAScriptFlags>, String> regex_flags_from_string(StringView flags);
struct ParseRegexPatternError {
    String error;
};
ErrorOr<String, ParseRegexPatternError> parse_regex_pattern(StringView pattern, bool unicode, bool unicode_sets);
ThrowCompletionOr<String> parse_regex_pattern(VM& vm, StringView pattern, bool unicode, bool unicode_sets);

class RegExpObject : public Object {
    JS_OBJECT(RegExpObject, Object);

public:
    // JS regexps are all 'global' by default as per our definition, but the "global" flag enables "stateful".
    // FIXME: Enable 'BrowserExtended' only if in a browser context.
    static constexpr regex::RegexOptions<ECMAScriptFlags> default_flags {
        (regex::ECMAScriptFlags)regex::AllFlags::SingleMatch
        | (regex::ECMAScriptFlags)regex::AllFlags::Global
        | (regex::ECMAScriptFlags)regex::AllFlags::SkipTrimEmptyMatches
        | regex::ECMAScriptFlags::BrowserExtended
    };

    static RegExpObject* create(Realm&);
    static RegExpObject* create(Realm&, Regex<ECMA262> regex, String pattern, String flags);

    ThrowCompletionOr<NonnullGCPtr<RegExpObject>> regexp_initialize(VM&, Value pattern, Value flags);
    String escape_regexp_pattern() const;

    virtual void initialize(Realm&) override;
    virtual ~RegExpObject() override = default;

    String const& pattern() const { return m_pattern; }
    String const& flags() const { return m_flags; }
    Regex<ECMA262> const& regex() { return *m_regex; }
    Regex<ECMA262> const& regex() const { return *m_regex; }

private:
    RegExpObject(Object& prototype);
    RegExpObject(Regex<ECMA262> regex, String pattern, String flags, Object& prototype);

    String m_pattern;
    String m_flags;
    Optional<Regex<ECMA262>> m_regex;
};

}
