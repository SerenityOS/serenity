/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibWeb/HTML/Storage.h>

namespace Web::HTML {

NonnullRefPtr<Storage> Storage::create()
{
    return adopt_ref(*new Storage);
}

Storage::Storage()
{
}

Storage::~Storage()
{
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-storage-length
size_t Storage::length() const
{
    // The length getter steps are to return this's map's size.
    return m_map.size();
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-storage-key
String Storage::key(size_t index)
{
    // 1. If index is greater than or equal to this's map's size, then return null.
    if (index >= m_map.size())
        return {};

    // 2. Let keys be the result of running get the keys on this's map.
    auto keys = m_map.keys();

    // 3. Return keys[index].
    return keys[index];
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-storage-getitem
String Storage::get_item(String const& key) const
{
    // 1. If this's map[key] does not exist, then return null.
    auto it = m_map.find(key);
    if (it == m_map.end())
        return {};

    // 2. Return this's map[key].
    return it->value;
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-storage-setitem
DOM::ExceptionOr<void> Storage::set_item(String const& key, String const& value)
{
    // 1. Let oldValue be null.
    String old_value;

    // 2. Let reorder be true.
    bool reorder = true;

    // 3. If this's map[key] exists:
    if (auto it = m_map.find(key); it != m_map.end()) {
        // 1. Set oldValue to this's map[key].
        old_value = it->value;

        // 2. If oldValue is value, then return.
        if (old_value == value)
            return {};

        // 3. Set reorder to false.
        reorder = false;
    }

    // FIXME: 4. If value cannot be stored, then throw a "QuotaExceededError" DOMException exception.

    // 5. Set this's map[key] to value.
    m_map.set(key, value);

    // 6. If reorder is true, then reorder this.
    if (reorder)
        this->reorder();

    // 7. Broadcast this with key, oldValue, and value.
    broadcast(key, old_value, value);

    return {};
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-storage-removeitem
void Storage::remove_item(String const& key)
{
    // 1. If this's map[key] does not exist, then return null.
    // FIXME: Return null?
    auto it = m_map.find(key);
    if (it == m_map.end())
        return;

    // 2. Set oldValue to this's map[key].
    auto old_value = it->value;

    // 3. Remove this's map[key].
    m_map.remove(it);

    // 4. Reorder this.
    reorder();

    // 5. Broadcast this with key, oldValue, and null.
    broadcast(key, old_value, {});
}

// https://html.spec.whatwg.org/multipage/webstorage.html#dom-storage-clear
void Storage::clear()
{
    // 1. Clear this's map.
    m_map.clear();

    // 2. Broadcast this with null, null, and null.
    broadcast({}, {}, {});
}

// https://html.spec.whatwg.org/multipage/webstorage.html#concept-storage-reorder
void Storage::reorder()
{
    // To reorder a Storage object storage, reorder storage's map's entries in an implementation-defined manner.
    // NOTE: This basically means that we're not required to maintain any particular iteration order.
}

// https://html.spec.whatwg.org/multipage/webstorage.html#concept-storage-broadcast
void Storage::broadcast(String const& key, String const& old_value, String const& new_value)
{
    (void)key;
    (void)old_value;
    (void)new_value;
    // FIXME: Implement.
}

Vector<String> Storage::supported_property_names() const
{
    // The supported property names on a Storage object storage are the result of running get the keys on storage's map.
    return m_map.keys();
}

void Storage::dump() const
{
    dbgln("Storage ({} key(s))", m_map.size());
    size_t i = 0;
    for (auto const& it : m_map) {
        dbgln("[{}] \"{}\": \"{}\"", i, it.key, it.value);
        ++i;
    }
}

}
