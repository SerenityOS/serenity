/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Maxwell Trussell <maxtrussell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Error.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/NumericLimits.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

class SingleValueKey : public RefCounted<SingleValueKey> {
public:
    SingleValueKey(StringView key)
        : m_key(key)
    {
    }

    static NonnullRefPtr<SingleValueKey> parse(StringView);
    ErrorOr<JsonValue> select(JsonValue const& value);

private:
    StringView m_key;
};

class MultiValueKey : public RefCounted<MultiValueKey> {
public:
    static ErrorOr<NonnullRefPtr<MultiValueKey>> parse(StringView const&);
    virtual ~MultiValueKey() = default;
    virtual Vector<JsonValue> select(JsonValue const& value) = 0;
};

Vector<StringView> split_dotted_key(StringView);
bool is_single_value_key(StringView);
int normalize_index(int i, size_t length);
static ErrorOr<JsonValue> query(JsonValue const& value, Vector<StringView>& key_parts, size_t key_index = 0);
static void print(JsonValue const& value, int spaces_per_indent, int indent = 0, bool use_color = true);

static void print_indent(int indent, int spaces_per_indent)
{
    for (int i = 0; i < indent * spaces_per_indent; ++i)
        out(" ");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView path;
    StringView dotted_key;
    int spaces_in_indent = 4;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Pretty-print a JSON file with syntax-coloring and indentation.");
    args_parser.add_option(dotted_key, "Dotted query key", "query", 'q', "foo.*.bar");
    args_parser.add_option(spaces_in_indent, "Indent size", "indent-size", 'i', "spaces_in_indent");
    args_parser.add_positional_argument(path, "Path to JSON file", "path", Core::ArgsParser::Required::No);
    VERIFY(spaces_in_indent >= 0);
    args_parser.parse(arguments);

    RefPtr<Core::File> file;
    if (path == nullptr)
        file = Core::File::standard_input();
    else
        file = TRY(Core::File::open(path, Core::OpenMode::ReadOnly));

    TRY(Core::System::pledge("stdio"));

    auto file_contents = file->read_all();
    auto json = TRY(JsonValue::from_string(file_contents));
    if (!dotted_key.is_empty()) {
        auto key_parts = split_dotted_key(dotted_key);
        json = TRY(query(json, key_parts));
    }

    print(json, spaces_in_indent, 0, isatty(STDOUT_FILENO));
    outln();

    return 0;
}

void print(JsonValue const& value, int spaces_per_indent, int indent, bool use_color)
{
    if (value.is_object()) {
        size_t printed_members = 0;
        auto& object = value.as_object();
        outln("{{");
        object.for_each_member([&](auto& member_name, auto& member_value) {
            ++printed_members;
            print_indent(indent + 1, spaces_per_indent);
            if (use_color)
                out("\"\033[33;1m{}\033[0m\": ", member_name);
            else
                out("\"{}\": ", member_name);
            print(member_value, spaces_per_indent, indent + 1, use_color);
            if (printed_members < static_cast<size_t>(object.size()))
                out(",");
            outln();
        });
        print_indent(indent, spaces_per_indent);
        out("}}");
        return;
    }
    if (value.is_array()) {
        size_t printed_entries = 0;
        auto array = value.as_array();
        outln("[");
        array.for_each([&](auto& entry_value) {
            ++printed_entries;
            print_indent(indent + 1, spaces_per_indent);
            print(entry_value, spaces_per_indent, indent + 1, use_color);
            if (printed_entries < static_cast<size_t>(array.size()))
                out(",");
            outln();
        });
        print_indent(indent, spaces_per_indent);
        out("]");
        return;
    }
    if (use_color) {
        if (value.is_string())
            out("\033[31;1m");
        else if (value.is_number())
            out("\033[35;1m");
        else if (value.is_bool())
            out("\033[32;1m");
        else if (value.is_null())
            out("\033[34;1m");
    }
    if (value.is_string())
        out("\"");
    out("{}", value.to_string());
    if (value.is_string())
        out("\"");
    if (use_color)
        out("\033[0m");
}

class WildcardKey : public MultiValueKey {
public:
    static NonnullRefPtr<WildcardKey> parse(StringView const&)
    {
        return make_ref_counted<WildcardKey>();
    }

    Vector<JsonValue> select(JsonValue const& value) override
    {
        Vector<JsonValue> matches;
        if (value.is_object()) {
            value.as_object().for_each_member([&](auto const&, auto& member_value) {
                matches.append(member_value);
            });
        } else if (value.is_array()) {
            matches = value.as_array().values();
        }
        return matches;
    }
};

class SliceKey : public MultiValueKey {
public:
    SliceKey(int start, int end, int step)
        : m_start(start)
        , m_end(end)
        , m_step(step)
    {
    }

    static ErrorOr<NonnullRefPtr<SliceKey>> parse(StringView const& key)
    {
        auto contents = key.substring_view(1, key.length() - 2);
        auto components = contents.split_view(':', SplitBehavior::KeepEmpty);

        auto start = NumericLimits<int>::min();
        auto end = NumericLimits<int>::max();
        int step = 1;
        if (components.size() > 0 && !components.at(0).is_empty()) {
            auto index = components.at(0).to_int();
            if (!index.has_value())
                return Error::from_string_view("Invalid start index"sv);
            start = index.value();
        }
        if (components.size() > 1 && !components.at(1).is_empty()) {
            auto index = components.at(1).to_int();
            if (!index.has_value())
                return Error::from_string_view("Invalid end index"sv);
            end = index.value();
        }
        if (components.size() > 2 && !components.at(2).is_empty()) {
            auto optional_step = components.at(2).to_int();
            if (!optional_step.has_value())
                return Error::from_string_view("Invalid step"sv);
            step = optional_step.value();

            // Start and end have different default values if the step is negative
            if (step < 0 && start == NumericLimits<int>::min())
                start = NumericLimits<int>::max();
            if (step < 0 && end == NumericLimits<int>::max())
                end = NumericLimits<int>::min();
        }
        return make_ref_counted<SliceKey>(start, end, step);
    }

    Vector<JsonValue> select(JsonValue const& value) override
    {
        Vector<JsonValue> matches;
        auto const& array = value.as_array();
        if (m_step > 0) {
            int start = max(0, normalize_index(m_start, array.size()));
            int end = min(array.size(), normalize_index(m_end, array.size()));
            for (int i = start; i < end; i += m_step)
                matches.append(array.at(i));
        } else {
            int start = min(array.size() - 1, normalize_index(m_start, array.size()));
            int end = max(-1, normalize_index(m_end, array.size()));
            for (int i = start; i > end; i += m_step)
                matches.append(array.at(i));
        }
        return matches;
    }

private:
    int m_start = 0;
    int m_end = 0;
    int m_step = 1;
};

ErrorOr<JsonValue> query(JsonValue const& value, Vector<StringView>& key_parts, size_t key_index)
{
    if (key_index == key_parts.size())
        return value;
    auto key_str = key_parts[key_index++];

    if (is_single_value_key(key_str)) {
        auto key = SingleValueKey::parse(key_str);
        auto result = TRY(key->select(value));
        return query(result, key_parts, key_index);
    } else {
        auto key = TRY(MultiValueKey::parse(key_str));
        Vector<JsonValue> results;
        for (auto const& match : key->select(value)) {
            auto result = TRY(query(match, key_parts, key_index));
            results.append(result);
        }
        return JsonValue(JsonArray(results));
    }
}

// Normalize negative indices, e.g. an index of -1 would wrap to the last element.
int normalize_index(int i, size_t length) { return (i < 0) ? length + i : i; }

bool is_single_value_key(StringView key)
{
    if (key == "*"sv || key == "[*]"sv)
        return false;
    if (key.starts_with('[') && key.ends_with(']')) {
        auto contains_quote = key.contains('"') || key.contains('\'');
        auto is_filter = key[1] == '?';
        return contains_quote && !is_filter;
    }
    return true;
}

NonnullRefPtr<SingleValueKey> SingleValueKey::parse(StringView key)
{
    if (key.starts_with('[') && key.ends_with(']'))
        key = key.substring_view(1, key.length() - 2);

    if ((key.starts_with('\'') && key.ends_with('\''))
        || (key.starts_with('"') && key.ends_with('"')))
        key = key.substring_view(1, key.length() - 2);

    return make_ref_counted<SingleValueKey>(key);
}

ErrorOr<JsonValue> SingleValueKey::select(JsonValue const& value)
{
    JsonValue result {};
    if (value.is_object()) {
        result = value.as_object().get(m_key);
    } else if (value.is_array()) {
        auto const& array = value.as_array();
        auto key_as_index = m_key.to_int();
        if (!key_as_index.has_value())
            return Error::from_string_view("Invalid array index"sv);
        result = array.at(normalize_index(key_as_index.value(), array.size()));
    }
    return result;
}

ErrorOr<NonnullRefPtr<MultiValueKey>> MultiValueKey::parse(StringView const& key)
{
    if (key == "*"sv || key == "[*]"sv)
        return WildcardKey::parse(key);
    if (key.starts_with('[') && key.ends_with(']'))
        return TRY(SliceKey::parse(key));
    VERIFY_NOT_REACHED();
}

Vector<StringView> split_dotted_key(StringView dotted_key)
{
    // FIXME: Allow for '[' or '.' within a json key, e.g. "foo.bar" is a valid json key.
    Vector<StringView> key_parts;
    size_t i = 0;
    for (size_t j = 0; j < dotted_key.length(); j++) {
        if (dotted_key[j] == '.') {
            key_parts.append(dotted_key.substring_view(i, j - i));
            i = j + 1;
        } else if (dotted_key[j] == '[' && j != 0) {
            key_parts.append(dotted_key.substring_view(i, j - i));
            i = j;
        }
    }
    key_parts.append(dotted_key.substring_view(i));
    return key_parts;
}
