/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

class Storage : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(Storage, Bindings::LegacyPlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Storage>> create(JS::Realm&);
    ~Storage();

    size_t length() const;
    DeprecatedString key(size_t index);
    DeprecatedString get_item(DeprecatedString const& key) const;
    WebIDL::ExceptionOr<void> set_item(DeprecatedString const& key, DeprecatedString const& value);
    void remove_item(DeprecatedString const& key);
    void clear();

    auto const& map() const { return m_map; }

    void dump() const;

private:
    explicit Storage(JS::Realm&);

    virtual void initialize(JS::Realm&) override;

    // ^LegacyPlatformObject
    virtual WebIDL::ExceptionOr<JS::Value> named_item_value(DeprecatedFlyString const&) const override;
    virtual WebIDL::ExceptionOr<DidDeletionFail> delete_value(DeprecatedString const&) override;
    virtual Vector<DeprecatedString> supported_property_names() const override;
    virtual WebIDL::ExceptionOr<void> set_value_of_named_property(DeprecatedString const& key, JS::Value value) override;

    virtual bool supports_indexed_properties() const override { return false; }
    virtual bool supports_named_properties() const override { return true; }
    virtual bool has_indexed_property_setter() const override { return false; }
    virtual bool has_named_property_setter() const override { return true; }
    virtual bool has_named_property_deleter() const override { return true; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const override { return true; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const override { return false; }
    virtual bool has_global_interface_extended_attribute() const override { return false; }
    virtual bool indexed_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_setter_has_identifier() const override { return true; }
    virtual bool named_property_deleter_has_identifier() const override { return true; }

    void reorder();
    void broadcast(DeprecatedString const& key, DeprecatedString const& old_value, DeprecatedString const& new_value);

    OrderedHashMap<DeprecatedString, DeprecatedString> m_map;
};

}
