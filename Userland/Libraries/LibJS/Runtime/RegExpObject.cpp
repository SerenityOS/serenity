/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

static Flags options_from(GlobalObject& global_object, const String& flags)
{
    auto& vm = global_object.vm();
    bool d = false, g = false, i = false, m = false, s = false, u = false, y = false;
    Flags options {
        // JS regexps are all 'global' by default as per our definition, but the "global" flag enables "stateful".
        // FIXME: Enable 'BrowserExtended' only if in a browser context.
        .effective_flags = { (regex::ECMAScriptFlags)regex::AllFlags::Global | (regex::ECMAScriptFlags)regex::AllFlags::SkipTrimEmptyMatches | regex::ECMAScriptFlags::BrowserExtended },
        .declared_flags = {},
    };

    for (auto ch : flags) {
        switch (ch) {
        case 'd':
            if (d)
                vm.throw_exception<SyntaxError>(global_object, ErrorType::RegExpObjectRepeatedFlag, ch);
            d = true;
            break;
        case 'g':
            if (g)
                vm.throw_exception<SyntaxError>(global_object, ErrorType::RegExpObjectRepeatedFlag, ch);
            g = true;
            options.effective_flags |= regex::ECMAScriptFlags::Global;
            options.declared_flags |= regex::ECMAScriptFlags::Global;
            break;
        case 'i':
            if (i)
                vm.throw_exception<SyntaxError>(global_object, ErrorType::RegExpObjectRepeatedFlag, ch);
            i = true;
            options.effective_flags |= regex::ECMAScriptFlags::Insensitive;
            options.declared_flags |= regex::ECMAScriptFlags::Insensitive;
            break;
        case 'm':
            if (m)
                vm.throw_exception<SyntaxError>(global_object, ErrorType::RegExpObjectRepeatedFlag, ch);
            m = true;
            options.effective_flags |= regex::ECMAScriptFlags::Multiline;
            options.declared_flags |= regex::ECMAScriptFlags::Multiline;
            break;
        case 's':
            if (s)
                vm.throw_exception<SyntaxError>(global_object, ErrorType::RegExpObjectRepeatedFlag, ch);
            s = true;
            options.effective_flags |= regex::ECMAScriptFlags::SingleLine;
            options.declared_flags |= regex::ECMAScriptFlags::SingleLine;
            break;
        case 'u':
            if (u)
                vm.throw_exception<SyntaxError>(global_object, ErrorType::RegExpObjectRepeatedFlag, ch);
            u = true;
            options.effective_flags |= regex::ECMAScriptFlags::Unicode;
            options.declared_flags |= regex::ECMAScriptFlags::Unicode;
            break;
        case 'y':
            if (y)
                vm.throw_exception<SyntaxError>(global_object, ErrorType::RegExpObjectRepeatedFlag, ch);
            y = true;
            // Now for the more interesting flag, 'sticky' actually unsets 'global', part of which is the default.
            options.effective_flags.reset_flag(regex::ECMAScriptFlags::Global);
            // "What's the difference between sticky and global, then", that's simple.
            // all the other flags imply 'global', and the "global" flag implies 'stateful';
            // however, the "sticky" flag does *not* imply 'global', only 'stateful'.
            options.effective_flags |= (regex::ECMAScriptFlags)regex::AllFlags::Internal_Stateful;
            options.effective_flags |= regex::ECMAScriptFlags::Sticky;
            options.declared_flags |= regex::ECMAScriptFlags::Sticky;
            break;
        default:
            vm.throw_exception<SyntaxError>(global_object, ErrorType::RegExpObjectBadFlag, ch);
            return options;
        }
    }

    return options;
}

RegExpObject* RegExpObject::create(GlobalObject& global_object, String original_pattern, String parsed_pattern, String flags)
{
    return global_object.heap().allocate<RegExpObject>(global_object, move(original_pattern), move(parsed_pattern), move(flags), *global_object.regexp_prototype());
}

RegExpObject::RegExpObject(String original_pattern, String parsed_pattern, String flags, Object& prototype)
    : Object(prototype)
    , m_original_pattern(move(original_pattern))
    , m_parsed_pattern(move(parsed_pattern))
    , m_flags(move(flags))
    , m_active_flags(options_from(global_object(), m_flags))
    , m_regex(m_parsed_pattern, m_active_flags.effective_flags)
{
    if (m_regex.parser_result.error != regex::Error::NoError) {
        vm().throw_exception<SyntaxError>(global_object(), ErrorType::RegExpCompileError, m_regex.error_string());
    }
}

RegExpObject::~RegExpObject()
{
}

void RegExpObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    define_direct_property(vm.names.lastIndex, {}, Attribute::Writable);
}

// 22.2.3.2.4 RegExpCreate ( P, F ), https://tc39.es/ecma262/#sec-regexpcreate
RegExpObject* regexp_create(GlobalObject& global_object, Value pattern, Value flags)
{
    auto& vm = global_object.vm();

    String f;
    if (flags.is_undefined()) {
        f = String::empty();
    } else {
        f = flags.to_string(global_object);
        if (vm.exception())
            return {};
    }

    String original_pattern;
    String parsed_pattern;

    if (pattern.is_undefined()) {
        original_pattern = String::empty();
        parsed_pattern = String::empty();
    } else {
        auto utf16_pattern = pattern.to_utf16_string(global_object);
        if (vm.exception())
            return {};

        Utf16View utf16_pattern_view { utf16_pattern };
        bool unicode = f.find('u').has_value();
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

        original_pattern = utf16_pattern_view.to_utf8(Utf16View::AllowInvalidCodeUnits::Yes);
        parsed_pattern = builder.build();
    }

    auto* object = RegExpObject::create(global_object, move(original_pattern), move(parsed_pattern), move(f));
    object->set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes);
    if (vm.exception())
        return {};
    return object;
}

}
