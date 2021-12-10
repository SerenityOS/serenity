/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/PassManager.h>
#include <LibJS/Console.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DateTimeFormat.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/Intl/ListFormat.h>
#include <LibJS/Runtime/Intl/Locale.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/Map.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/ProxyObject.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/Set.h>
#include <LibJS/Runtime/ShadowRealm.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

RefPtr<JS::VM> vm;
Vector<String> repl_statements;

class ReplObject final : public JS::GlobalObject {
    JS_OBJECT(ReplObject, JS::GlobalObject);

public:
    ReplObject() = default;
    virtual void initialize_global_object() override;
    virtual ~ReplObject() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(exit_interpreter);
    JS_DECLARE_NATIVE_FUNCTION(repl_help);
    JS_DECLARE_NATIVE_FUNCTION(load_file);
    JS_DECLARE_NATIVE_FUNCTION(save_to_file);
    JS_DECLARE_NATIVE_FUNCTION(load_json);
};

class ScriptObject final : public JS::GlobalObject {
    JS_OBJECT(ScriptObject, JS::GlobalObject);

public:
    ScriptObject() = default;
    virtual void initialize_global_object() override;
    virtual ~ScriptObject() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(load_file);
    JS_DECLARE_NATIVE_FUNCTION(load_json);
};

static bool s_dump_ast = false;
static bool s_run_bytecode = false;
static bool s_opt_bytecode = false;
static bool s_as_module = false;
static bool s_print_last_result = false;
static bool s_strip_ansi = false;
static bool s_disable_source_location_hints = false;
static RefPtr<Line::Editor> s_editor;
static String s_history_path = String::formatted("{}/.js-history", Core::StandardPaths::home_directory());
static int s_repl_line_level = 0;
static bool s_fail_repl = false;

static String prompt_for_level(int level)
{
    static StringBuilder prompt_builder;
    prompt_builder.clear();
    prompt_builder.append("> ");

    for (auto i = 0; i < level; ++i)
        prompt_builder.append("    ");

    return prompt_builder.build();
}

static String read_next_piece()
{
    StringBuilder piece;

    auto line_level_delta_for_next_line { 0 };

    do {
        auto line_result = s_editor->get_line(prompt_for_level(s_repl_line_level));

        line_level_delta_for_next_line = 0;

        if (line_result.is_error()) {
            s_fail_repl = true;
            return "";
        }

        auto& line = line_result.value();
        s_editor->add_to_history(line);

        piece.append(line);
        piece.append('\n');
        auto lexer = JS::Lexer(line);

        enum {
            NotInLabelOrObjectKey,
            InLabelOrObjectKeyIdentifier,
            InLabelOrObjectKey
        } label_state { NotInLabelOrObjectKey };

        for (JS::Token token = lexer.next(); token.type() != JS::TokenType::Eof; token = lexer.next()) {
            switch (token.type()) {
            case JS::TokenType::BracketOpen:
            case JS::TokenType::CurlyOpen:
            case JS::TokenType::ParenOpen:
                label_state = NotInLabelOrObjectKey;
                s_repl_line_level++;
                break;
            case JS::TokenType::BracketClose:
            case JS::TokenType::CurlyClose:
            case JS::TokenType::ParenClose:
                label_state = NotInLabelOrObjectKey;
                s_repl_line_level--;
                break;

            case JS::TokenType::Identifier:
            case JS::TokenType::StringLiteral:
                if (label_state == NotInLabelOrObjectKey)
                    label_state = InLabelOrObjectKeyIdentifier;
                else
                    label_state = NotInLabelOrObjectKey;
                break;
            case JS::TokenType::Colon:
                if (label_state == InLabelOrObjectKeyIdentifier)
                    label_state = InLabelOrObjectKey;
                else
                    label_state = NotInLabelOrObjectKey;
                break;
            default:
                break;
            }
        }

        if (label_state == InLabelOrObjectKey) {
            // If there's a label or object literal key at the end of this line,
            // prompt for more lines but do not change the line level.
            line_level_delta_for_next_line += 1;
        }
    } while (s_repl_line_level + line_level_delta_for_next_line > 0);

    return piece.to_string();
}

static String strip_ansi(StringView format_string)
{
    if (format_string.is_empty())
        return String::empty();

    StringBuilder builder;
    size_t i;
    for (i = 0; i < format_string.length() - 1; ++i) {
        if (format_string[i] == '\033' && format_string[i + 1] == '[') {
            while (i < format_string.length() && format_string[i] != 'm')
                ++i;
        } else {
            builder.append(format_string[i]);
        }
    }
    if (i < format_string.length())
        builder.append(format_string[i]);
    return builder.to_string();
}

template<typename... Parameters>
static void js_out(CheckedFormatString<Parameters...>&& fmtstr, const Parameters&... parameters)
{
    if (!s_strip_ansi)
        return out(move(fmtstr), parameters...);
    auto stripped_fmtstr = strip_ansi(fmtstr.view());
    out(stripped_fmtstr, parameters...);
}

template<typename... Parameters>
static void js_outln(CheckedFormatString<Parameters...>&& fmtstr, const Parameters&... parameters)
{
    if (!s_strip_ansi)
        return outln(move(fmtstr), parameters...);
    auto stripped_fmtstr = strip_ansi(fmtstr.view());
    outln(stripped_fmtstr, parameters...);
}

inline void js_outln() { outln(); }

static void print_value(JS::Value value, HashTable<JS::Object*>& seen_objects);

static void print_type(FlyString const& name)
{
    js_out("[\033[36;1m{}\033[0m]", name);
}

static void print_separator(bool& first)
{
    js_out(first ? " " : ", ");
    first = false;
}

static void print_array(JS::Array& array, HashTable<JS::Object*>& seen_objects)
{
    js_out("[");
    bool first = true;
    for (auto it = array.indexed_properties().begin(false); it != array.indexed_properties().end(); ++it) {
        print_separator(first);
        auto value_or_error = array.get(it.index());
        // The V8 repl doesn't throw an exception here, and instead just
        // prints 'undefined'. We may choose to replicate that behavior in
        // the future, but for now lets just catch the error
        if (value_or_error.is_error())
            return;
        auto value = value_or_error.release_value();
        print_value(value, seen_objects);
    }
    if (!first)
        js_out(" ");
    js_out("]");
}

static void print_object(JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    js_out("{{");
    bool first = true;
    for (auto& entry : object.indexed_properties()) {
        print_separator(first);
        js_out("\"\033[33;1m{}\033[0m\": ", entry.index());
        auto value_or_error = object.get(entry.index());
        // The V8 repl doesn't throw an exception here, and instead just
        // prints 'undefined'. We may choose to replicate that behavior in
        // the future, but for now lets just catch the error
        if (value_or_error.is_error())
            return;
        auto value = value_or_error.release_value();
        print_value(value, seen_objects);
    }
    for (auto& it : object.shape().property_table_ordered()) {
        print_separator(first);
        if (it.key.is_string()) {
            js_out("\"\033[33;1m{}\033[0m\": ", it.key.to_display_string());
        } else {
            js_out("[\033[33;1m{}\033[0m]: ", it.key.to_display_string());
        }
        print_value(object.get_direct(it.value.offset), seen_objects);
    }
    if (!first)
        js_out(" ");
    js_out("}}");
}

static void print_function(JS::Object const& object, HashTable<JS::Object*>&)
{
    print_type(object.class_name());
    if (is<JS::ECMAScriptFunctionObject>(object))
        js_out(" {}", static_cast<JS::ECMAScriptFunctionObject const&>(object).name());
    else if (is<JS::NativeFunction>(object))
        js_out(" {}", static_cast<JS::NativeFunction const&>(object).name());
}

static void print_date(JS::Object const& object, HashTable<JS::Object*>&)
{
    print_type("Date");
    js_out(" \033[34;1m{}\033[0m", static_cast<JS::Date const&>(object).string());
}

static void print_error(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto name = object.get_without_side_effects(vm->names.name).value_or(JS::js_undefined());
    auto message = object.get_without_side_effects(vm->names.message).value_or(JS::js_undefined());
    if (name.is_accessor() || message.is_accessor()) {
        print_value(&object, seen_objects);
    } else {
        auto name_string = name.to_string_without_side_effects();
        auto message_string = message.to_string_without_side_effects();
        print_type(name_string);
        if (!message_string.is_empty())
            js_out(" \033[31;1m{}\033[0m", message_string);
    }
}

static void print_regexp_object(JS::Object const& object, HashTable<JS::Object*>&)
{
    auto& regexp_object = static_cast<JS::RegExpObject const&>(object);
    print_type("RegExp");
    js_out(" \033[34;1m/{}/{}\033[0m", regexp_object.escape_regexp_pattern(), regexp_object.flags());
}

static void print_proxy_object(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& proxy_object = static_cast<JS::ProxyObject const&>(object);
    print_type("Proxy");
    js_out("\n  target: ");
    print_value(&proxy_object.target(), seen_objects);
    js_out("\n  handler: ");
    print_value(&proxy_object.handler(), seen_objects);
}

static void print_map(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& map = static_cast<JS::Map const&>(object);
    auto& entries = map.entries();
    print_type("Map");
    js_out(" {{");
    bool first = true;
    for (auto& entry : entries) {
        print_separator(first);
        print_value(entry.key, seen_objects);
        js_out(" => ");
        print_value(entry.value, seen_objects);
    }
    if (!first)
        js_out(" ");
    js_out("}}");
}

static void print_set(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& set = static_cast<JS::Set const&>(object);
    auto& values = set.values();
    print_type("Set");
    js_out(" {{");
    bool first = true;
    for (auto& value : values) {
        print_separator(first);
        print_value(value, seen_objects);
    }
    if (!first)
        js_out(" ");
    js_out("}}");
}

static void print_promise(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& promise = static_cast<JS::Promise const&>(object);
    print_type("Promise");
    switch (promise.state()) {
    case JS::Promise::State::Pending:
        js_out("\n  state: ");
        js_out("\033[36;1mPending\033[0m");
        break;
    case JS::Promise::State::Fulfilled:
        js_out("\n  state: ");
        js_out("\033[32;1mFulfilled\033[0m");
        js_out("\n  result: ");
        print_value(promise.result(), seen_objects);
        break;
    case JS::Promise::State::Rejected:
        js_out("\n  state: ");
        js_out("\033[31;1mRejected\033[0m");
        js_out("\n  result: ");
        print_value(promise.result(), seen_objects);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

static void print_array_buffer(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& array_buffer = static_cast<JS::ArrayBuffer const&>(object);
    auto& buffer = array_buffer.buffer();
    auto byte_length = array_buffer.byte_length();
    print_type("ArrayBuffer");
    js_out("\n  byteLength: ");
    print_value(JS::Value((double)byte_length), seen_objects);
    if (!byte_length)
        return;
    js_outln();
    for (size_t i = 0; i < byte_length; ++i) {
        js_out("{:02x}", buffer[i]);
        if (i + 1 < byte_length) {
            if ((i + 1) % 32 == 0)
                js_outln();
            else if ((i + 1) % 16 == 0)
                js_out("  ");
            else
                js_out(" ");
        }
    }
}

static void print_shadow_realm(JS::Object const&, HashTable<JS::Object*>&)
{
    // Not much we can show here that would be useful. Realm pointer address?!
    print_type("ShadowRealm");
}

template<typename T>
static void print_number(T number) requires IsArithmetic<T>
{
    js_out("\033[35;1m");
    js_out("{}", number);
    js_out("\033[0m");
}

static void print_typed_array(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& typed_array_base = static_cast<JS::TypedArrayBase const&>(object);
    auto& array_buffer = *typed_array_base.viewed_array_buffer();
    auto length = typed_array_base.array_length();
    print_type(object.class_name());
    js_out("\n  length: ");
    print_value(JS::Value(length), seen_objects);
    js_out("\n  byteLength: ");
    print_value(JS::Value(typed_array_base.byte_length()), seen_objects);
    js_out("\n  buffer: ");
    print_type("ArrayBuffer");
    if (array_buffer.is_detached())
        js_out(" (detached)");
    js_out(" @ {:p}", &array_buffer);
    if (!length || array_buffer.is_detached())
        return;
    js_outln();
    // FIXME: This kinda sucks.
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    if (is<JS::ClassName>(object)) {                                                     \
        js_out("[ ");                                                                    \
        auto& typed_array = static_cast<JS::ClassName const&>(typed_array_base);         \
        auto data = typed_array.data();                                                  \
        for (size_t i = 0; i < length; ++i) {                                            \
            if (i > 0)                                                                   \
                js_out(", ");                                                            \
            print_number(data[i]);                                                       \
        }                                                                                \
        js_out(" ]");                                                                    \
        return;                                                                          \
    }
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE
    VERIFY_NOT_REACHED();
}

static void print_data_view(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& data_view = static_cast<JS::DataView const&>(object);
    print_type("DataView");
    js_out("\n  byteLength: ");
    print_value(JS::Value(data_view.byte_length()), seen_objects);
    js_out("\n  byteOffset: ");
    print_value(JS::Value(data_view.byte_offset()), seen_objects);
    js_out("\n  buffer: ");
    print_type("ArrayBuffer");
    js_out(" @ {:p}", data_view.viewed_array_buffer());
}

static void print_temporal_calendar(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& calendar = static_cast<JS::Temporal::Calendar const&>(object);
    print_type("Temporal.Calendar");
    js_out(" ");
    print_value(JS::js_string(object.vm(), calendar.identifier()), seen_objects);
}

static void print_temporal_duration(JS::Object const& object, HashTable<JS::Object*>&)
{
    auto& duration = static_cast<JS::Temporal::Duration const&>(object);
    print_type("Temporal.Duration");
    js_out(" \033[34;1m{} y, {} M, {} w, {} d, {} h, {} m, {} s, {} ms, {} us, {} ns\033[0m", duration.years(), duration.months(), duration.weeks(), duration.days(), duration.hours(), duration.minutes(), duration.seconds(), duration.milliseconds(), duration.microseconds(), duration.nanoseconds());
}

static void print_temporal_instant(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& instant = static_cast<JS::Temporal::Instant const&>(object);
    print_type("Temporal.Instant");
    js_out(" ");
    // FIXME: Print human readable date and time, like in print_date() - ideally handling arbitrarily large values since we get a bigint.
    print_value(&instant.nanoseconds(), seen_objects);
}

static void print_temporal_plain_date(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& plain_date = static_cast<JS::Temporal::PlainDate const&>(object);
    print_type("Temporal.PlainDate");
    js_out(" \033[34;1m{:04}-{:02}-{:02}\033[0m", plain_date.iso_year(), plain_date.iso_month(), plain_date.iso_day());
    js_out("\n  calendar: ");
    print_value(&plain_date.calendar(), seen_objects);
}

static void print_temporal_plain_date_time(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& plain_date_time = static_cast<JS::Temporal::PlainDateTime const&>(object);
    print_type("Temporal.PlainDateTime");
    js_out(" \033[34;1m{:04}-{:02}-{:02} {:02}:{:02}:{:02}.{:03}{:03}{:03}\033[0m", plain_date_time.iso_year(), plain_date_time.iso_month(), plain_date_time.iso_day(), plain_date_time.iso_hour(), plain_date_time.iso_minute(), plain_date_time.iso_second(), plain_date_time.iso_millisecond(), plain_date_time.iso_microsecond(), plain_date_time.iso_nanosecond());
    js_out("\n  calendar: ");
    print_value(&plain_date_time.calendar(), seen_objects);
}

static void print_temporal_plain_month_day(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& plain_month_day = static_cast<JS::Temporal::PlainMonthDay const&>(object);
    print_type("Temporal.PlainMonthDay");
    // Also has an [[ISOYear]] internal slot, but showing that here seems rather unexpected.
    js_out(" \033[34;1m{:02}-{:02}\033[0m", plain_month_day.iso_month(), plain_month_day.iso_day());
    js_out("\n  calendar: ");
    print_value(&plain_month_day.calendar(), seen_objects);
}

static void print_temporal_plain_time(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& plain_time = static_cast<JS::Temporal::PlainTime const&>(object);
    print_type("Temporal.PlainTime");
    js_out(" \033[34;1m{:02}:{:02}:{:02}.{:03}{:03}{:03}\033[0m", plain_time.iso_hour(), plain_time.iso_minute(), plain_time.iso_second(), plain_time.iso_millisecond(), plain_time.iso_microsecond(), plain_time.iso_nanosecond());
    js_out("\n  calendar: ");
    print_value(&plain_time.calendar(), seen_objects);
}

static void print_temporal_plain_year_month(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& plain_year_month = static_cast<JS::Temporal::PlainYearMonth const&>(object);
    print_type("Temporal.PlainYearMonth");
    // Also has an [[ISODay]] internal slot, but showing that here seems rather unexpected.
    js_out(" \033[34;1m{:04}-{:02}\033[0m", plain_year_month.iso_year(), plain_year_month.iso_month());
    js_out("\n  calendar: ");
    print_value(&plain_year_month.calendar(), seen_objects);
}

static void print_temporal_time_zone(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& time_zone = static_cast<JS::Temporal::TimeZone const&>(object);
    print_type("Temporal.TimeZone");
    js_out(" ");
    print_value(JS::js_string(object.vm(), time_zone.identifier()), seen_objects);
    if (time_zone.offset_nanoseconds().has_value()) {
        js_out("\n  offset (ns): ");
        print_value(JS::Value(*time_zone.offset_nanoseconds()), seen_objects);
    }
}

static void print_temporal_zoned_date_time(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& zoned_date_time = static_cast<JS::Temporal::ZonedDateTime const&>(object);
    print_type("Temporal.ZonedDateTime");
    js_out("\n  epochNanoseconds: ");
    print_value(&zoned_date_time.nanoseconds(), seen_objects);
    js_out("\n  timeZone: ");
    print_value(&zoned_date_time.time_zone(), seen_objects);
    js_out("\n  calendar: ");
    print_value(&zoned_date_time.calendar(), seen_objects);
}

static void print_intl_display_names(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& display_names = static_cast<JS::Intl::DisplayNames const&>(object);
    print_type("Intl.DisplayNames");
    js_out("\n  locale: ");
    print_value(js_string(object.vm(), display_names.locale()), seen_objects);
    js_out("\n  type: ");
    print_value(js_string(object.vm(), display_names.type_string()), seen_objects);
    js_out("\n  style: ");
    print_value(js_string(object.vm(), display_names.style_string()), seen_objects);
    js_out("\n  fallback: ");
    print_value(js_string(object.vm(), display_names.fallback_string()), seen_objects);
}

static void print_intl_locale(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& locale = static_cast<JS::Intl::Locale const&>(object);
    print_type("Intl.Locale");
    js_out("\n  locale: ");
    print_value(js_string(object.vm(), locale.locale()), seen_objects);
    if (locale.has_calendar()) {
        js_out("\n  calendar: ");
        print_value(js_string(object.vm(), locale.calendar()), seen_objects);
    }
    if (locale.has_case_first()) {
        js_out("\n  caseFirst: ");
        print_value(js_string(object.vm(), locale.case_first()), seen_objects);
    }
    if (locale.has_collation()) {
        js_out("\n  collation: ");
        print_value(js_string(object.vm(), locale.collation()), seen_objects);
    }
    if (locale.has_hour_cycle()) {
        js_out("\n  hourCycle: ");
        print_value(js_string(object.vm(), locale.hour_cycle()), seen_objects);
    }
    if (locale.has_numbering_system()) {
        js_out("\n  numberingSystem: ");
        print_value(js_string(object.vm(), locale.numbering_system()), seen_objects);
    }
    js_out("\n  numeric: ");
    print_value(JS::Value(locale.numeric()), seen_objects);
}

static void print_intl_list_format(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& list_format = static_cast<JS::Intl::ListFormat const&>(object);
    print_type("Intl.ListFormat");
    js_out("\n  locale: ");
    print_value(js_string(object.vm(), list_format.locale()), seen_objects);
    js_out("\n  type: ");
    print_value(js_string(object.vm(), list_format.type_string()), seen_objects);
    js_out("\n  style: ");
    print_value(js_string(object.vm(), list_format.style_string()), seen_objects);
}

static void print_intl_number_format(JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    auto& number_format = static_cast<JS::Intl::NumberFormat const&>(object);
    print_type("Intl.NumberFormat");
    js_out("\n  locale: ");
    print_value(js_string(object.vm(), number_format.locale()), seen_objects);
    js_out("\n  dataLocale: ");
    print_value(js_string(object.vm(), number_format.data_locale()), seen_objects);
    js_out("\n  numberingSystem: ");
    print_value(js_string(object.vm(), number_format.numbering_system()), seen_objects);
    js_out("\n  style: ");
    print_value(js_string(object.vm(), number_format.style_string()), seen_objects);
    if (number_format.has_currency()) {
        js_out("\n  currency: ");
        print_value(js_string(object.vm(), number_format.currency()), seen_objects);
    }
    if (number_format.has_currency_display()) {
        js_out("\n  currencyDisplay: ");
        print_value(js_string(object.vm(), number_format.currency_display_string()), seen_objects);
    }
    if (number_format.has_currency_sign()) {
        js_out("\n  currencySign: ");
        print_value(js_string(object.vm(), number_format.currency_sign_string()), seen_objects);
    }
    if (number_format.has_unit()) {
        js_out("\n  unit: ");
        print_value(js_string(object.vm(), number_format.unit()), seen_objects);
    }
    if (number_format.has_unit_display()) {
        js_out("\n  unitDisplay: ");
        print_value(js_string(object.vm(), number_format.unit_display_string()), seen_objects);
    }
    js_out("\n  minimumIntegerDigits: ");
    print_value(JS::Value(number_format.min_integer_digits()), seen_objects);
    if (number_format.has_min_fraction_digits()) {
        js_out("\n  minimumFractionDigits: ");
        print_value(JS::Value(number_format.min_fraction_digits()), seen_objects);
    }
    if (number_format.has_max_fraction_digits()) {
        js_out("\n  maximumFractionDigits: ");
        print_value(JS::Value(number_format.max_fraction_digits()), seen_objects);
    }
    if (number_format.has_min_significant_digits()) {
        js_out("\n  minimumSignificantDigits: ");
        print_value(JS::Value(number_format.min_significant_digits()), seen_objects);
    }
    if (number_format.has_max_significant_digits()) {
        js_out("\n  maximumSignificantDigits: ");
        print_value(JS::Value(number_format.max_significant_digits()), seen_objects);
    }
    js_out("\n  useGrouping: ");
    print_value(JS::Value(number_format.use_grouping()), seen_objects);
    js_out("\n  roundingType: ");
    print_value(js_string(object.vm(), number_format.rounding_type_string()), seen_objects);
    js_out("\n  notation: ");
    print_value(js_string(object.vm(), number_format.notation_string()), seen_objects);
    if (number_format.has_compact_display()) {
        js_out("\n  compactDisplay: ");
        print_value(js_string(object.vm(), number_format.compact_display_string()), seen_objects);
    }
    js_out("\n  signDisplay: ");
    print_value(js_string(object.vm(), number_format.sign_display_string()), seen_objects);
}

static void print_intl_date_time_format(JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    auto& date_time_format = static_cast<JS::Intl::DateTimeFormat&>(object);
    print_type("Intl.DateTimeFormat");
    js_out("\n  locale: ");
    print_value(js_string(object.vm(), date_time_format.locale()), seen_objects);
    js_out("\n  pattern: ");
    print_value(js_string(object.vm(), date_time_format.pattern()), seen_objects);
    js_out("\n  calendar: ");
    print_value(js_string(object.vm(), date_time_format.calendar()), seen_objects);
    js_out("\n  numberingSystem: ");
    print_value(js_string(object.vm(), date_time_format.numbering_system()), seen_objects);
    if (date_time_format.has_hour_cycle()) {
        js_out("\n  hourCycle: ");
        print_value(js_string(object.vm(), date_time_format.hour_cycle_string()), seen_objects);
    }
    js_out("\n  timeZone: ");
    print_value(js_string(object.vm(), date_time_format.time_zone()), seen_objects);
    if (date_time_format.has_date_style()) {
        js_out("\n  dateStyle: ");
        print_value(js_string(object.vm(), date_time_format.date_style_string()), seen_objects);
    }
    if (date_time_format.has_time_style()) {
        js_out("\n  timeStyle: ");
        print_value(js_string(object.vm(), date_time_format.time_style_string()), seen_objects);
    }

    JS::Intl::for_each_calendar_field(date_time_format.global_object(), date_time_format, [&](auto& option, auto const& property, auto const&) -> JS::ThrowCompletionOr<void> {
        using ValueType = typename RemoveReference<decltype(option)>::ValueType;

        if (!option.has_value())
            return {};

        js_out("\n  {}: ", property);

        if constexpr (IsIntegral<ValueType>) {
            print_value(JS::Value(*option), seen_objects);
        } else {
            auto name = Unicode::calendar_pattern_style_to_string(*option);
            print_value(js_string(object.vm(), name), seen_objects);
        }

        return {};
    });
}

static void print_primitive_wrapper_object(FlyString const& name, JS::Object const& object, HashTable<JS::Object*>& seen_objects)
{
    // BooleanObject, NumberObject, StringObject
    print_type(name);
    js_out(" ");
    print_value(&object, seen_objects);
}

static void print_value(JS::Value value, HashTable<JS::Object*>& seen_objects)
{
    if (value.is_empty()) {
        js_out("\033[34;1m<empty>\033[0m");
        return;
    }

    if (value.is_object()) {
        if (seen_objects.contains(&value.as_object())) {
            // FIXME: Maybe we should only do this for circular references,
            //        not for all reoccurring objects.
            js_out("<already printed Object {}>", &value.as_object());
            return;
        }
        seen_objects.set(&value.as_object());
    }

    if (value.is_object()) {
        auto& object = value.as_object();
        if (is<JS::Array>(object))
            return print_array(static_cast<JS::Array&>(object), seen_objects);
        if (object.is_function())
            return print_function(object, seen_objects);
        if (is<JS::Date>(object))
            return print_date(object, seen_objects);
        if (is<JS::Error>(object))
            return print_error(object, seen_objects);

        auto prototype_or_error = object.internal_get_prototype_of();
        if (prototype_or_error.has_value() && prototype_or_error.value() == object.global_object().error_prototype())
            return print_error(object, seen_objects);
        vm->clear_exception();
        vm->stop_unwind();

        if (is<JS::RegExpObject>(object))
            return print_regexp_object(object, seen_objects);
        if (is<JS::Map>(object))
            return print_map(object, seen_objects);
        if (is<JS::Set>(object))
            return print_set(object, seen_objects);
        if (is<JS::DataView>(object))
            return print_data_view(object, seen_objects);
        if (is<JS::ProxyObject>(object))
            return print_proxy_object(object, seen_objects);
        if (is<JS::Promise>(object))
            return print_promise(object, seen_objects);
        if (is<JS::ArrayBuffer>(object))
            return print_array_buffer(object, seen_objects);
        if (is<JS::ShadowRealm>(object))
            return print_shadow_realm(object, seen_objects);
        if (object.is_typed_array())
            return print_typed_array(object, seen_objects);
        if (is<JS::StringObject>(object))
            return print_primitive_wrapper_object("String", object, seen_objects);
        if (is<JS::NumberObject>(object))
            return print_primitive_wrapper_object("Number", object, seen_objects);
        if (is<JS::BooleanObject>(object))
            return print_primitive_wrapper_object("Boolean", object, seen_objects);
        if (is<JS::Temporal::Calendar>(object))
            return print_temporal_calendar(object, seen_objects);
        if (is<JS::Temporal::Duration>(object))
            return print_temporal_duration(object, seen_objects);
        if (is<JS::Temporal::Instant>(object))
            return print_temporal_instant(object, seen_objects);
        if (is<JS::Temporal::PlainDate>(object))
            return print_temporal_plain_date(object, seen_objects);
        if (is<JS::Temporal::PlainDateTime>(object))
            return print_temporal_plain_date_time(object, seen_objects);
        if (is<JS::Temporal::PlainMonthDay>(object))
            return print_temporal_plain_month_day(object, seen_objects);
        if (is<JS::Temporal::PlainTime>(object))
            return print_temporal_plain_time(object, seen_objects);
        if (is<JS::Temporal::PlainYearMonth>(object))
            return print_temporal_plain_year_month(object, seen_objects);
        if (is<JS::Temporal::TimeZone>(object))
            return print_temporal_time_zone(object, seen_objects);
        if (is<JS::Temporal::ZonedDateTime>(object))
            return print_temporal_zoned_date_time(object, seen_objects);
        if (is<JS::Intl::DisplayNames>(object))
            return print_intl_display_names(object, seen_objects);
        if (is<JS::Intl::Locale>(object))
            return print_intl_locale(object, seen_objects);
        if (is<JS::Intl::ListFormat>(object))
            return print_intl_list_format(object, seen_objects);
        if (is<JS::Intl::NumberFormat>(object))
            return print_intl_number_format(object, seen_objects);
        if (is<JS::Intl::DateTimeFormat>(object))
            return print_intl_date_time_format(object, seen_objects);
        return print_object(object, seen_objects);
    }

    if (value.is_string())
        js_out("\033[32;1m");
    else if (value.is_number() || value.is_bigint())
        js_out("\033[35;1m");
    else if (value.is_boolean())
        js_out("\033[33;1m");
    else if (value.is_null())
        js_out("\033[33;1m");
    else if (value.is_undefined())
        js_out("\033[34;1m");
    if (value.is_string())
        js_out("\"");
    else if (value.is_negative_zero())
        js_out("-");
    js_out("{}", value.to_string_without_side_effects());
    if (value.is_string())
        js_out("\"");
    js_out("\033[0m");
}

static void print(JS::Value value)
{
    HashTable<JS::Object*> seen_objects;
    print_value(value, seen_objects);
    js_outln();
}

static bool write_to_file(String const& path)
{
    int fd = open(path.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
    for (size_t i = 0; i < repl_statements.size(); i++) {
        auto line = repl_statements[i];
        if (line.length() && i != repl_statements.size() - 1) {
            ssize_t nwritten = write(fd, line.characters(), line.length());
            if (nwritten < 0) {
                close(fd);
                return false;
            }
        }
        if (i != repl_statements.size() - 1) {
            char ch = '\n';
            ssize_t nwritten = write(fd, &ch, 1);
            if (nwritten != 1) {
                perror("write");
                close(fd);
                return false;
            }
        }
    }
    close(fd);
    return true;
}

static bool parse_and_run(JS::Interpreter& interpreter, StringView source, StringView source_name)
{
    auto program_type = s_as_module ? JS::Program::Type::Module : JS::Program::Type::Script;
    auto parser = JS::Parser(JS::Lexer(source), program_type);
    auto program = parser.parse_program();

    if (s_dump_ast)
        program->dump(0);

    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        if (!s_disable_source_location_hints) {
            auto hint = error.source_location_hint(source);
            if (!hint.is_empty())
                js_outln("{}", hint);
        }
        vm->throw_exception<JS::SyntaxError>(interpreter.global_object(), error.to_string());
    } else {
        if (JS::Bytecode::g_dump_bytecode || s_run_bytecode) {
            auto executable = JS::Bytecode::Generator::generate(*program);
            executable.name = source_name;
            if (s_opt_bytecode) {
                auto& passes = JS::Bytecode::Interpreter::optimization_pipeline();
                passes.perform(executable);
                dbgln("Optimisation passes took {}us", passes.elapsed());
            }

            if (JS::Bytecode::g_dump_bytecode)
                executable.dump();

            if (s_run_bytecode) {
                JS::Bytecode::Interpreter bytecode_interpreter(interpreter.global_object(), interpreter.realm());
                auto result = bytecode_interpreter.run(executable);
                // Since all the error handling code uses vm.exception() we just rethrow any exception we got here.
                if (result.is_error())
                    vm->throw_exception(interpreter.global_object(), result.throw_completion().value());
            } else {
                return true;
            }
        } else {
            interpreter.run(interpreter.global_object(), *program);
        }
    }

    auto handle_exception = [&] {
        auto* exception = vm->exception();
        vm->clear_exception();
        js_out("Uncaught exception: ");
        print(exception->value());
        auto& traceback = exception->traceback();
        if (traceback.size() > 1) {
            unsigned repetitions = 0;
            for (size_t i = 0; i < traceback.size(); ++i) {
                auto& traceback_frame = traceback[i];
                if (i + 1 < traceback.size()) {
                    auto& next_traceback_frame = traceback[i + 1];
                    if (next_traceback_frame.function_name == traceback_frame.function_name) {
                        repetitions++;
                        continue;
                    }
                }
                if (repetitions > 4) {
                    // If more than 5 (1 + >4) consecutive function calls with the same name, print
                    // the name only once and show the number of repetitions instead. This prevents
                    // printing ridiculously large call stacks of recursive functions.
                    js_outln(" -> {}", traceback_frame.function_name);
                    js_outln(" {} more calls", repetitions);
                } else {
                    for (size_t j = 0; j < repetitions + 1; ++j)
                        js_outln(" -> {}", traceback_frame.function_name);
                }
                repetitions = 0;
            }
        }
    };

    if (vm->exception()) {
        handle_exception();
        return false;
    }
    if (s_print_last_result)
        print(vm->last_value());
    if (vm->exception()) {
        handle_exception();
        return false;
    }
    return true;
}

static JS::ThrowCompletionOr<JS::Value> load_file_impl(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto filename = TRY(vm.argument(0).to_string(global_object));
    auto file = Core::File::construct(filename);
    if (!file->open(Core::OpenMode::ReadOnly))
        return vm.throw_completion<JS::Error>(global_object, String::formatted("Failed to open '{}': {}", filename, file->error_string()));
    auto file_contents = file->read_all();
    auto source = StringView { file_contents };
    auto parser = JS::Parser(JS::Lexer(source));
    auto program = parser.parse_program();
    if (parser.has_errors()) {
        auto& error = parser.errors()[0];
        return vm.throw_completion<JS::SyntaxError>(global_object, error.to_string());
    }
    // FIXME: Use eval()-like semantics and execute in current scope?
    vm.interpreter().run(global_object, *program);
    return JS::js_undefined();
}

static JS::ThrowCompletionOr<JS::Value> load_json_impl(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto filename = TRY(vm.argument(0).to_string(global_object));
    auto file = Core::File::construct(filename);
    if (!file->open(Core::OpenMode::ReadOnly))
        return vm.throw_completion<JS::Error>(global_object, String::formatted("Failed to open '{}': {}", filename, file->error_string()));
    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    if (json.is_error())
        return vm.throw_completion<JS::SyntaxError>(global_object, JS::ErrorType::JsonMalformed);
    return JS::JSONObject::parse_json_value(global_object, json.value());
}

void ReplObject::initialize_global_object()
{
    Base::initialize_global_object();
    define_direct_property("global", this, JS::Attribute::Enumerable);
    u8 attr = JS::Attribute::Configurable | JS::Attribute::Writable | JS::Attribute::Enumerable;
    define_native_function("exit", exit_interpreter, 0, attr);
    define_native_function("help", repl_help, 0, attr);
    define_native_function("load", load_file, 1, attr);
    define_native_function("save", save_to_file, 1, attr);
    define_native_function("loadJSON", load_json, 1, attr);
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::save_to_file)
{
    if (!vm.argument_count())
        return JS::Value(false);
    String save_path = vm.argument(0).to_string_without_side_effects();
    StringView path = StringView(save_path.characters());
    if (write_to_file(path)) {
        return JS::Value(true);
    }
    return JS::Value(false);
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::exit_interpreter)
{
    if (!vm.argument_count())
        exit(0);
    exit(TRY(vm.argument(0).to_number(global_object)).as_double());
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::repl_help)
{
    js_outln("REPL commands:");
    js_outln("    exit(code): exit the REPL with specified code. Defaults to 0.");
    js_outln("    help(): display this menu");
    js_outln("    load(file): load given JS file into running session. For example: load(\"foo.js\")");
    js_outln("    save(file): write REPL input history to the given file. For example: save(\"foo.txt\")");
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::load_file)
{
    return load_file_impl(vm, global_object);
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::load_json)
{
    return load_json_impl(vm, global_object);
}

void ScriptObject::initialize_global_object()
{
    Base::initialize_global_object();
    define_direct_property("global", this, JS::Attribute::Enumerable);
    u8 attr = JS::Attribute::Configurable | JS::Attribute::Writable | JS::Attribute::Enumerable;
    define_native_function("load", load_file, 1, attr);
    define_native_function("loadJSON", load_json, 1, attr);
}

JS_DEFINE_NATIVE_FUNCTION(ScriptObject::load_file)
{
    return load_file_impl(vm, global_object);
}

JS_DEFINE_NATIVE_FUNCTION(ScriptObject::load_json)
{
    return load_json_impl(vm, global_object);
}

static void repl(JS::Interpreter& interpreter)
{
    while (!s_fail_repl) {
        String piece = read_next_piece();
        if (piece.is_empty())
            continue;
        repl_statements.append(piece);
        parse_and_run(interpreter, piece, "REPL");
    }
}

static Function<void()> interrupt_interpreter;
static void sigint_handler()
{
    interrupt_interpreter();
}

class ReplConsoleClient final : public JS::ConsoleClient {
public:
    ReplConsoleClient(JS::Console& console)
        : ConsoleClient(console)
    {
    }

    virtual JS::Value log() override
    {
        js_outln("{}", vm().join_arguments());
        return JS::js_undefined();
    }

    virtual JS::Value info() override
    {
        js_outln("(i) {}", vm().join_arguments());
        return JS::js_undefined();
    }

    virtual JS::Value debug() override
    {
        js_outln("\033[36;1m{}\033[0m", vm().join_arguments());
        return JS::js_undefined();
    }

    virtual JS::Value warn() override
    {
        js_outln("\033[33;1m{}\033[0m", vm().join_arguments());
        return JS::js_undefined();
    }

    virtual JS::Value error() override
    {
        js_outln("\033[31;1m{}\033[0m", vm().join_arguments());
        return JS::js_undefined();
    }

    virtual JS::Value clear() override
    {
        js_out("\033[3J\033[H\033[2J");
        fflush(stdout);
        return JS::js_undefined();
    }

    virtual JS::Value trace() override
    {
        js_outln("{}", vm().join_arguments());
        auto trace = get_trace();
        for (auto& function_name : trace) {
            if (function_name.is_empty())
                function_name = "<anonymous>";
            js_outln(" -> {}", function_name);
        }
        return JS::js_undefined();
    }

    virtual JS::Value count() override
    {
        auto label = vm().argument_count() ? vm().argument(0).to_string_without_side_effects() : "default";
        auto counter_value = m_console.counter_increment(label);
        js_outln("{}: {}", label, counter_value);
        return JS::js_undefined();
    }

    virtual JS::Value count_reset() override
    {
        auto label = vm().argument_count() ? vm().argument(0).to_string_without_side_effects() : "default";
        if (m_console.counter_reset(label))
            js_outln("{}: 0", label);
        else
            js_outln("\033[33;1m\"{}\" doesn't have a count\033[0m", label);
        return JS::js_undefined();
    }

    virtual JS::Value assert_() override
    {
        auto& vm = this->vm();
        if (!vm.argument(0).to_boolean()) {
            if (vm.argument_count() > 1) {
                js_out("\033[31;1mAssertion failed:\033[0m");
                js_outln(" {}", vm.join_arguments(1));
            } else {
                js_outln("\033[31;1mAssertion failed\033[0m");
            }
        }
        return JS::js_undefined();
    }
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
#ifdef __serenity__
    TRY(Core::System::pledge("stdio rpath wpath cpath tty sigaction"));
#endif

    bool gc_on_every_allocation = false;
    bool disable_syntax_highlight = false;
    Vector<StringView> script_paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("This is a JavaScript interpreter.");
    args_parser.add_option(s_dump_ast, "Dump the AST", "dump-ast", 'A');
    args_parser.add_option(JS::Bytecode::g_dump_bytecode, "Dump the bytecode", "dump-bytecode", 'd');
    args_parser.add_option(s_run_bytecode, "Run the bytecode", "run-bytecode", 'b');
    args_parser.add_option(s_opt_bytecode, "Optimize the bytecode", "optimize-bytecode", 'p');
    args_parser.add_option(s_as_module, "Treat as module", "as-module", 'm');
    args_parser.add_option(s_print_last_result, "Print last result", "print-last-result", 'l');
    args_parser.add_option(s_strip_ansi, "Disable ANSI colors", "disable-ansi-colors", 'c');
    args_parser.add_option(s_disable_source_location_hints, "Disable source location hints", "disable-source-location-hints", 'h');
    args_parser.add_option(gc_on_every_allocation, "GC on every allocation", "gc-on-every-allocation", 'g');
#ifdef JS_TRACK_ZOMBIE_CELLS
    bool zombify_dead_cells = false;
    args_parser.add_option(zombify_dead_cells, "Zombify dead cells (to catch missing GC marks)", "zombify-dead-cells", 'z');
#endif
    args_parser.add_option(disable_syntax_highlight, "Disable live syntax highlighting", "no-syntax-highlight", 's');
    args_parser.add_positional_argument(script_paths, "Path to script files", "scripts", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    bool syntax_highlight = !disable_syntax_highlight;

    vm = JS::VM::create();
    // NOTE: These will print out both warnings when using something like Promise.reject().catch(...) -
    // which is, as far as I can tell, correct - a promise is created, rejected without handler, and a
    // handler then attached to it. The Node.js REPL doesn't warn in this case, so it's something we
    // might want to revisit at a later point and disable warnings for promises created this way.
    vm->on_promise_unhandled_rejection = [](auto& promise) {
        // FIXME: Optionally make print_value() to print to stderr
        js_out("WARNING: A promise was rejected without any handlers");
        js_out(" (result: ");
        HashTable<JS::Object*> seen_objects;
        print_value(promise.result(), seen_objects);
        js_outln(")");
    };
    vm->on_promise_rejection_handled = [](auto& promise) {
        // FIXME: Optionally make print_value() to print to stderr
        js_out("WARNING: A handler was added to an already rejected promise");
        js_out(" (result: ");
        HashTable<JS::Object*> seen_objects;
        print_value(promise.result(), seen_objects);
        js_outln(")");
    };
    OwnPtr<JS::Interpreter> interpreter;

    interrupt_interpreter = [&] {
        auto error = JS::Error::create(interpreter->global_object(), "Received SIGINT");
        vm->throw_exception(interpreter->global_object(), error);
    };

    if (script_paths.is_empty()) {
        s_print_last_result = true;
        interpreter = JS::Interpreter::create<ReplObject>(*vm);
        ReplConsoleClient console_client(interpreter->global_object().console());
        interpreter->global_object().console().set_client(console_client);
        interpreter->heap().set_should_collect_on_every_allocation(gc_on_every_allocation);
#ifdef JS_TRACK_ZOMBIE_CELLS
        interpreter->heap().set_zombify_dead_cells(zombify_dead_cells);
#endif
        interpreter->vm().set_underscore_is_last_value(true);

        auto& global_environment = interpreter->realm().global_environment();

        s_editor = Line::Editor::construct();
        s_editor->load_history(s_history_path);

        signal(SIGINT, [](int) {
            if (!s_editor->is_editing())
                sigint_handler();
            s_editor->save_history(s_history_path);
        });

        s_editor->on_display_refresh = [syntax_highlight](Line::Editor& editor) {
            auto stylize = [&](Line::Span span, Line::Style styles) {
                if (syntax_highlight)
                    editor.stylize(span, styles);
            };
            editor.strip_styles();

            size_t open_indents = s_repl_line_level;

            auto line = editor.line();
            JS::Lexer lexer(line);
            bool indenters_starting_line = true;
            for (JS::Token token = lexer.next(); token.type() != JS::TokenType::Eof; token = lexer.next()) {
                auto length = Utf8View { token.value() }.length();
                auto start = token.line_column() - 1;
                auto end = start + length;
                if (indenters_starting_line) {
                    if (token.type() != JS::TokenType::ParenClose && token.type() != JS::TokenType::BracketClose && token.type() != JS::TokenType::CurlyClose) {
                        indenters_starting_line = false;
                    } else {
                        --open_indents;
                    }
                }

                switch (token.category()) {
                case JS::TokenCategory::Invalid:
                    stylize({ start, end, Line::Span::CodepointOriented }, { Line::Style::Foreground(Line::Style::XtermColor::Red), Line::Style::Underline });
                    break;
                case JS::TokenCategory::Number:
                    stylize({ start, end, Line::Span::CodepointOriented }, { Line::Style::Foreground(Line::Style::XtermColor::Magenta) });
                    break;
                case JS::TokenCategory::String:
                    stylize({ start, end, Line::Span::CodepointOriented }, { Line::Style::Foreground(Line::Style::XtermColor::Green), Line::Style::Bold });
                    break;
                case JS::TokenCategory::Punctuation:
                    break;
                case JS::TokenCategory::Operator:
                    break;
                case JS::TokenCategory::Keyword:
                    switch (token.type()) {
                    case JS::TokenType::BoolLiteral:
                    case JS::TokenType::NullLiteral:
                        stylize({ start, end, Line::Span::CodepointOriented }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow), Line::Style::Bold });
                        break;
                    default:
                        stylize({ start, end, Line::Span::CodepointOriented }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Bold });
                        break;
                    }
                    break;
                case JS::TokenCategory::ControlKeyword:
                    stylize({ start, end, Line::Span::CodepointOriented }, { Line::Style::Foreground(Line::Style::XtermColor::Cyan), Line::Style::Italic });
                    break;
                case JS::TokenCategory::Identifier:
                    stylize({ start, end, Line::Span::CodepointOriented }, { Line::Style::Foreground(Line::Style::XtermColor::White), Line::Style::Bold });
                    break;
                default:
                    break;
                }
            }

            editor.set_prompt(prompt_for_level(open_indents));
        };

        auto complete = [&interpreter, &global_environment](Line::Editor const& editor) -> Vector<Line::CompletionSuggestion> {
            auto line = editor.line(editor.cursor());

            JS::Lexer lexer { line };
            enum {
                Initial,
                CompleteVariable,
                CompleteNullProperty,
                CompleteProperty,
            } mode { Initial };

            StringView variable_name;
            StringView property_name;

            // we're only going to complete either
            //    - <N>
            //        where N is part of the name of a variable
            //    - <N>.<P>
            //        where N is the complete name of a variable and
            //        P is part of the name of one of its properties
            auto js_token = lexer.next();
            for (; js_token.type() != JS::TokenType::Eof; js_token = lexer.next()) {
                switch (mode) {
                case CompleteVariable:
                    switch (js_token.type()) {
                    case JS::TokenType::Period:
                        // ...<name> <dot>
                        mode = CompleteNullProperty;
                        break;
                    default:
                        // not a dot, reset back to initial
                        mode = Initial;
                        break;
                    }
                    break;
                case CompleteNullProperty:
                    if (js_token.is_identifier_name()) {
                        // ...<name> <dot> <name>
                        mode = CompleteProperty;
                        property_name = js_token.value();
                    } else {
                        mode = Initial;
                    }
                    break;
                case CompleteProperty:
                    // something came after the property access, reset to initial
                case Initial:
                    if (js_token.type() == JS::TokenType::Identifier) {
                        // ...<name>...
                        mode = CompleteVariable;
                        variable_name = js_token.value();
                    } else {
                        mode = Initial;
                    }
                    break;
                }
            }

            bool last_token_has_trivia = js_token.trivia().length() > 0;

            if (mode == CompleteNullProperty) {
                mode = CompleteProperty;
                property_name = "";
                last_token_has_trivia = false; // <name> <dot> [tab] is sensible to complete.
            }

            if (mode == Initial || last_token_has_trivia)
                return {}; // we do not know how to complete this

            Vector<Line::CompletionSuggestion> results;

            Function<void(JS::Shape const&, StringView)> list_all_properties = [&results, &list_all_properties](JS::Shape const& shape, auto property_pattern) {
                for (auto const& descriptor : shape.property_table()) {
                    if (!descriptor.key.is_string())
                        continue;
                    auto key = descriptor.key.as_string();
                    if (key.view().starts_with(property_pattern)) {
                        Line::CompletionSuggestion completion { key, Line::CompletionSuggestion::ForSearch };
                        if (!results.contains_slow(completion)) { // hide duplicates
                            results.append(String(key));
                        }
                    }
                }
                if (auto const* prototype = shape.prototype()) {
                    list_all_properties(prototype->shape(), property_pattern);
                }
            };

            switch (mode) {
            case CompleteProperty: {
                Optional<JS::Value> maybe_value;
                auto maybe_variable = vm->resolve_binding(variable_name, &global_environment);
                if (vm->exception())
                    break;
                maybe_value = TRY_OR_DISCARD(maybe_variable.get_value(interpreter->global_object()));
                VERIFY(!maybe_value->is_empty());

                auto variable = *maybe_value;
                if (!variable.is_object())
                    break;

                auto const* object = MUST(variable.to_object(interpreter->global_object()));
                auto const& shape = object->shape();
                list_all_properties(shape, property_name);
                if (results.size())
                    editor.suggest(property_name.length());
                break;
            }
            case CompleteVariable: {
                auto const& variable = interpreter->global_object();
                list_all_properties(variable.shape(), variable_name);

                for (String& name : global_environment.declarative_record().bindings()) {
                    if (name.starts_with(variable_name))
                        results.empend(name);
                }

                if (results.size())
                    editor.suggest(variable_name.length());
                break;
            }
            default:
                VERIFY_NOT_REACHED();
            }

            return results;
        };
        s_editor->on_tab_complete = move(complete);
        repl(*interpreter);
        s_editor->save_history(s_history_path);
    } else {
        interpreter = JS::Interpreter::create<ScriptObject>(*vm);
        ReplConsoleClient console_client(interpreter->global_object().console());
        interpreter->global_object().console().set_client(console_client);
        interpreter->heap().set_should_collect_on_every_allocation(gc_on_every_allocation);
#ifdef JS_TRACK_ZOMBIE_CELLS
        interpreter->heap().set_zombify_dead_cells(zombify_dead_cells);
#endif

        signal(SIGINT, [](int) {
            sigint_handler();
        });

        StringBuilder builder;
        for (auto& path : script_paths) {
            auto file = TRY(Core::File::open(path, Core::OpenMode::ReadOnly));
            auto file_contents = file->read_all();
            auto source = StringView { file_contents };
            builder.append(source);
        }

        StringBuilder source_name_builder;
        source_name_builder.join(", ", script_paths);

        if (!parse_and_run(*interpreter, builder.string_view(), source_name_builder.string_view()))
            return 1;
    }

    return 0;
}
