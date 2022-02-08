/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class Storage
    : public RefCounted<Storage>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::StorageWrapper;

    static NonnullRefPtr<Storage> create();
    ~Storage();

    size_t length() const;
    String key(size_t index);
    String get_item(String const& key) const;
    DOM::ExceptionOr<void> set_item(String const& key, String const& value);
    void remove_item(String const& key);
    void clear();

    Vector<String> supported_property_names() const;

    void dump() const;

private:
    Storage();

    void reorder();
    void broadcast(String const& key, String const& old_value, String const& new_value);

    OrderedHashMap<String, String> m_map;
};

}

namespace Web::Bindings {

StorageWrapper* wrap(JS::GlobalObject&, HTML::Storage&);

}
