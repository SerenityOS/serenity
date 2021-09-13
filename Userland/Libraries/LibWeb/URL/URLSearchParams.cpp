/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/Utf8View.h>
#include <LibWeb/URL/URLSearchParams.h>

namespace Web::URL {

DOM::ExceptionOr<NonnullRefPtr<URLSearchParams>> URLSearchParams::create_with_global_object(Bindings::WindowObject&, String const& init)
{
    // 1. If init is a string and starts with U+003F (?), then remove the first code point from init.
    StringView stripped_init = init.substring_view(init.starts_with('?'));
    // 2. Initialize this with init.

    // URLSearchParams init from this point forward

    // TODO
    // 1. If init is a sequence, then for each pair in init:
    // a. If pair does not contain exactly two items, then throw a TypeError.
    // b. Append a new name-value pair whose name is pair’s first item, and value is pair’s second item, to query’s list.

    // TODO
    // 2. Otherwise, if init is a record, then for each name → value of init, append a new name-value pair whose name is name and value is value, to query’s list.

    // 3. Otherwise:
    // a. Assert: init is a string.
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
    // TODO
    // 1. If query’s URL object is null, then return.
    // 2. Let serializedQuery be the serialization of query’s list.
    // 3. If serializedQuery is the empty string, then set serializedQuery to null.
    // 4. Set query’s URL object’s URL’s query to serializedQuery.
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

}
