/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Token.h>

namespace JS {

Result<regex::RegexOptions<ECMAScriptFlags>, String> regex_flags_from_string(StringView flags)
{
    bool d = false, g = false, i = false, m = false, s = false, u = false, y = false;
    auto options = RegExpObject::default_flags;

    for (auto ch : flags) {
        switch (ch) {
        case 'd':
            if (d)
                return String::formatted(ErrorType::RegExpObjectRepeatedFlag.message(), ch);
            d = true;
            break;
        case 'g':
            if (g)
                return String::formatted(ErrorType::RegExpObjectRepeatedFlag.message(), ch);
            g = true;
            options |= regex::ECMAScriptFlags::Global;
            break;
        case 'i':
            if (i)
                return String::formatted(ErrorType::RegExpObjectRepeatedFlag.message(), ch);
            i = true;
            options |= regex::ECMAScriptFlags::Insensitive;
            break;
        case 'm':
            if (m)
                return String::formatted(ErrorType::RegExpObjectRepeatedFlag.message(), ch);
            m = true;
            options |= regex::ECMAScriptFlags::Multiline;
            break;
        case 's':
            if (s)
                return String::formatted(ErrorType::RegExpObjectRepeatedFlag.message(), ch);
            s = true;
            options |= regex::ECMAScriptFlags::SingleLine;
            break;
        case 'u':
            if (u)
                return String::formatted(ErrorType::RegExpObjectRepeatedFlag.message(), ch);
            u = true;
            options |= regex::ECMAScriptFlags::Unicode;
            break;
        case 'y':
            if (y)
                return String::formatted(ErrorType::RegExpObjectRepeatedFlag.message(), ch);
            y = true;
            // Now for the more interesting flag, 'sticky' actually unsets 'global', part of which is the default.
            options.reset_flag(regex::ECMAScriptFlags::Global);
            // "What's the difference between sticky and global, then", that's simple.
            // all the other flags imply 'global', and the "global" flag implies 'stateful';
            // however, the "sticky" flag does *not* imply 'global', only 'stateful'.
            options |= (regex::ECMAScriptFlags)regex::AllFlags::Internal_Stateful;
            options |= regex::ECMAScriptFlags::Sticky;
            break;
        default:
            return String::formatted(ErrorType::RegExpObjectBadFlag.message(), ch);
        }
    }

    return options;
}

String parse_regex_pattern(StringView pattern, bool unicode)
{
    auto utf16_pattern = AK::utf8_to_utf16(pattern);
    Utf16View utf16_pattern_view { utf16_pattern };
    StringBuilder builder;

    // If the Unicode flag is set, append each code point to the pattern. Otherwise, append each
    // code unit. But unlike the spec, multi-byte code units must be escaped for LibRegex to parse.
    for (size_t i = 0; i < utf16_pattern_view.length_in_code_units();) {
        if (unicode) {
            auto code_point = code_point_at(utf16_pattern_view, i);
            builder.append_code_point(code_point.code_point);
            i += code_point.code_unit_count;
            continue;
        }

        u16 code_unit = utf16_pattern_view.code_unit_at(i);
        ++i;

        if (code_unit > 0x7f)
            builder.appendff("\\u{:04x}", code_unit);
        else
            builder.append_code_point(code_unit);
    }

    return builder.build();
}

RegExpObject* RegExpObject::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<RegExpObject>(global_object, *global_object.regexp_prototype());
}

RegExpObject* RegExpObject::create(GlobalObject& global_object, Regex<ECMA262> regex, String pattern, String flags)
{
    return global_object.heap().allocate<RegExpObject>(global_object, move(regex), move(pattern), move(flags), *global_object.regexp_prototype());
}

RegExpObject::RegExpObject(Object& prototype)
    : Object(prototype)
{
}

RegExpObject::RegExpObject(Regex<ECMA262> regex, String pattern, String flags, Object& prototype)
    : Object(prototype)
    , m_pattern(move(pattern))
    , m_flags(move(flags))
    , m_regex(move(regex))
{
    VERIFY(m_regex->parser_result.error == regex::Error::NoError);
}

RegExpObject::~RegExpObject()
{
}

void RegExpObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    define_direct_property(vm.names.lastIndex, Value(0), Attribute::Writable);
}

// 22.2.3.2.2 RegExpInitialize ( obj, pattern, flags ), https://tc39.es/ecma262/#sec-regexpinitialize
ThrowCompletionOr<RegExpObject*> RegExpObject::regexp_initialize(GlobalObject& global_object, Value pattern, Value flags)
{
    auto& vm = global_object.vm();

    String f;
    if (flags.is_undefined()) {
        f = String::empty();
    } else {
        f = TRY(flags.to_string(global_object));
    }

    String original_pattern;
    String parsed_pattern;

    if (pattern.is_undefined()) {
        original_pattern = String::empty();
        parsed_pattern = String::empty();
    } else {
        original_pattern = TRY(pattern.to_string(global_object));
        bool unicode = f.find('u').has_value();
        parsed_pattern = parse_regex_pattern(original_pattern, unicode);
    }

    auto parsed_flags_or_error = regex_flags_from_string(f);
    if (parsed_flags_or_error.is_error())
        return vm.throw_completion<SyntaxError>(global_object, parsed_flags_or_error.release_error());

    Regex<ECMA262> regex(move(parsed_pattern), parsed_flags_or_error.release_value());
    if (regex.parser_result.error != regex::Error::NoError)
        return vm.throw_completion<SyntaxError>(global_object, ErrorType::RegExpCompileError, regex.error_string());

    m_pattern = move(original_pattern);
    m_flags = move(f);
    m_regex = move(regex);

    TRY(set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));

    return this;
}

// 22.2.3.2.5 EscapeRegExpPattern ( P, F ), https://tc39.es/ecma262/#sec-escaperegexppattern
String RegExpObject::escape_regexp_pattern() const
{
    if (m_pattern.is_empty())
        return "(?:)";
    // FIXME: Check u flag and escape accordingly
    return m_pattern.replace("\n", "\\n", true).replace("\r", "\\r", true).replace(LINE_SEPARATOR_STRING, "\\u2028", true).replace(PARAGRAPH_SEPARATOR_STRING, "\\u2029", true).replace("/", "\\/", true);
}

// 22.2.3.2.4 RegExpCreate ( P, F ), https://tc39.es/ecma262/#sec-regexpcreate
ThrowCompletionOr<RegExpObject*> regexp_create(GlobalObject& global_object, Value pattern, Value flags)
{
    auto* regexp_object = RegExpObject::create(global_object);
    return TRY(regexp_object->regexp_initialize(global_object, pattern, flags));
}

}
