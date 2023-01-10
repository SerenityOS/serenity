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
    static JS::NonnullGCPtr<Storage> create(JS::Realm&);
    ~Storage();

    size_t length() const;
    DeprecatedString key(size_t index);
    DeprecatedString get_item(DeprecatedString const& key) const;
    WebIDL::ExceptionOr<void> set_item(DeprecatedString const& key, DeprecatedString const& value);
    void remove_item(DeprecatedString const& key);
    void clear();

    Vector<DeprecatedString> supported_property_names() const;

    auto const& map() const { return m_map; }

    void dump() const;

private:
    explicit Storage(JS::Realm&);

    virtual void initialize(JS::Realm&) override;

    void reorder();
    void broadcast(DeprecatedString const& key, DeprecatedString const& old_value, DeprecatedString const& new_value);

    OrderedHashMap<DeprecatedString, DeprecatedString> m_map;
};

}
