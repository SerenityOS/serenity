/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

class Storage : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Storage, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<Storage> create(HTML::Window&);
    ~Storage();

    size_t length() const;
    String key(size_t index);
    String get_item(String const& key) const;
    WebIDL::ExceptionOr<void> set_item(String const& key, String const& value);
    void remove_item(String const& key);
    void clear();

    Vector<String> supported_property_names() const;

    auto const& map() const { return m_map; }

    void dump() const;

private:
    explicit Storage(HTML::Window&);

    void reorder();
    void broadcast(String const& key, String const& old_value, String const& new_value);

    OrderedHashMap<String, String> m_map;
};

}
