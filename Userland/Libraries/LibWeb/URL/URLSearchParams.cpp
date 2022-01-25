/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibWeb/URL/URL.h>
#include <LibWeb/URL/URLSearchParams.h>

namespace Web::URL {

String url_encode(const Vector<QueryParam>& pairs, AK::URL::PercentEncodeSet percent_encode_set)
{
    StringBuilder builder;
    for (size_t i = 0; i < pairs.size(); ++i) {
        builder.append(AK::URL::percent_encode(pairs[i].name, percent_encode_set));
        builder.append('=');
        builder.append(AK::URL::percent_encode(pairs[i].value, percent_encode_set));
        if (i != pairs.size() - 1)
            builder.append('&');
    }
    return builder.to_string();
}

Vector<QueryParam> url_decode(StringView input)
{
    // 1. Let sequences be the result of splitting input on 0x26 (&).
    auto sequences = input.split_view('&');

    // 2. Let output be an initially empty list of name-value tuples where both name and value hold a string.
    Vector<QueryParam> output;

    // 3. For each byte sequence bytes in sequences:
    for (auto bytes : sequences) {
        // 1. If bytes is the empty byte sequence, then continue.
        if (bytes.is_empty())
            continue;

        StringView name;
        StringView value;

        // 2. If bytes contains a 0x3D (=), then let name be the bytes from the start of bytes up to but excluding its first 0x3D (=), and let value be the bytes, if any, after the first 0x3D (=) up to the end of bytes. If 0x3D (=) is the first byte, then name will be the empty byte sequence. If it is the last, then value will be the empty byte sequence.
        if (auto index = bytes.find('='); index.has_value()) {
            name = bytes.substring_view(0, *index);
            value = bytes.substring_view(*index + 1);
        }
        // 3. Otherwise, let name have the value of bytes and let value be the empty byte sequence.
        else {
            name = bytes;
            value = ""sv;
        }

        // 4. Replace any 0x2B (+) in name and value with 0x20 (SP).
        auto space_decoded_name = name.replace("+"sv, " "sv, true);

        // 5. Let nameString and valueString be the result of running UTF-8 decode without BOM on the percent-decoding of name and value, respectively.
        auto name_string = AK::URL::percent_decode(space_decoded_name);
        auto value_string = AK::URL::percent_decode(value);

        output.empend(move(name_string), move(value_string));
    }

    return output;
}

DOM::ExceptionOr<NonnullRefPtr<URLSearchParams>> URLSearchParams::create_with_global_object(Bindings::WindowObject&, Variant<Vector<Vector<String>>, String> const& init)
{
    // 1. If init is a string and starts with U+003F (?), then remove the first code point from init.
    // NOTE: We do this when we know that it's a string on step 3 of initialization.

    // 2. Initialize this with init.

    // URLSearchParams init from this point forward

    // 1. If init is a sequence, then for each pair in init:
    if (init.has<Vector<Vector<String>>>()) {
        auto const& init_sequence = init.get<Vector<Vector<String>>>();

        Vector<QueryParam> list;
        list.ensure_capacity(init_sequence.size());

        for (auto const& pair : init_sequence) {
            // a. If pair does not contain exactly two items, then throw a TypeError.
            if (pair.size() != 2)
                return DOM::SimpleException { DOM::SimpleExceptionType::TypeError, String::formatted("Expected only 2 items in pair, got {}", pair.size()) };

            // b. Append a new name-value pair whose name is pair’s first item, and value is pair’s second item, to query’s list.
            list.append(QueryParam { .name = pair[0], .value = pair[1] });
        }

        return URLSearchParams::create(move(list));
    }

    // TODO
    // 2. Otherwise, if init is a record, then for each name → value of init, append a new name-value pair whose name is name and value is value, to query’s list.

    // 3. Otherwise:
    // a. Assert: init is a string.
    // NOTE: `get` performs `VERIFY(has<T>())`
    auto const& init_string = init.get<String>();

    // See NOTE at the start of this function.
    StringView stripped_init = init_string.substring_view(init_string.starts_with('?'));

    // b. Set query’s list to the result of parsing init.
    return URLSearchParams::create(url_decode(stripped_init));
}

void URLSearchParams::append(String const& name, String const& value)
{
    // 1. Append a new name-value pair whose name is name and value is value, to list.
    m_list.empend(name, value);
    // 2. Update this.
    update();
}

void URLSearchParams::update()
{
    // 1. If query’s URL object is null, then return.
    if (m_url.is_null())
        return;
    // 2. Let serializedQuery be the serialization of query’s list.
    auto serialized_query = to_string();
    // 3. If serializedQuery is the empty string, then set serializedQuery to null.
    if (serialized_query.is_empty())
        serialized_query = {};
    // 4. Set query’s URL object’s URL’s query to serializedQuery.
    m_url->set_query({}, move(serialized_query));
}

void URLSearchParams::delete_(String const& name)
{
    // 1. Remove all name-value pairs whose name is name from list.
    m_list.remove_all_matching([&name](auto& entry) {
        return entry.name == name;
    });
    // 2. Update this.
    update();
}

String URLSearchParams::get(String const& name)
{
    // return the value of the first name-value pair whose name is name in this’s list, if there is such a pair, and null otherwise.
    auto result = m_list.find_if([&name](auto& entry) {
        return entry.name == name;
    });
    if (result.is_end())
        return {};
    return result->value;
}

// https://url.spec.whatwg.org/#dom-urlsearchparams-getall
Vector<String> URLSearchParams::get_all(String const& name)
{
    // return the values of all name-value pairs whose name is name, in this’s list, in list order, and the empty sequence otherwise.
    Vector<String> values;
    for (auto& entry : m_list) {
        if (entry.name == name)
            values.append(entry.value);
    }
    return values;
}

bool URLSearchParams::has(String const& name)
{
    // return true if there is a name-value pair whose name is name in this’s list, and false otherwise.
    return !m_list.find_if([&name](auto& entry) {
                      return entry.name == name;
                  })
                .is_end();
}

void URLSearchParams::set(const String& name, const String& value)
{
    // 1. If this’s list contains any name-value pairs whose name is name, then set the value of the first such name-value pair to value and remove the others.
    auto existing = m_list.find_if([&name](auto& entry) {
        return entry.name == name;
    });
    if (!existing.is_end()) {
        existing->value = value;
        m_list.remove_all_matching([&name, &existing](auto& entry) {
            return &entry != &*existing && entry.name == name;
        });
    }
    // 2. Otherwise, append a new name-value pair whose name is name and value is value, to this’s list.
    else {
        m_list.empend(name, value);
    }
    // 3. Update this.
    update();
}

void URLSearchParams::sort()
{
    // 1. Sort all name-value pairs, if any, by their names. Sorting must be done by comparison of code units. The relative order between name-value pairs with equal names must be preserved.
    quick_sort(m_list.begin(), m_list.end(), [](auto& a, auto& b) {
        Utf8View a_code_points { a.name };
        Utf8View b_code_points { b.name };

        if (a_code_points.starts_with(b_code_points))
            return false;
        if (b_code_points.starts_with(a_code_points))
            return true;

        for (auto k = a_code_points.begin(), l = b_code_points.begin();
             k != a_code_points.end() && l != b_code_points.end();
             ++k, ++l) {
            if (*k != *l) {
                if (*k < *l) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        VERIFY_NOT_REACHED();
    });
    // 2. Update this.
    update();
}

String URLSearchParams::to_string()
{
    // return the serialization of this’s list.
    return url_encode(m_list, AK::URL::PercentEncodeSet::ApplicationXWWWFormUrlencoded);
}

JS::ThrowCompletionOr<void> URLSearchParams::for_each(ForEachCallback callback)
{
    for (auto i = 0u; i < m_list.size(); ++i) {
        auto& query_param = m_list[i]; // We are explicitly iterating over the indices here as the callback might delete items from the list
        TRY(callback(query_param.name, query_param.value));
    }

    return {};
}

}
