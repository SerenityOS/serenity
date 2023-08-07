/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/LegacyPlatformObject.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/system-state.html#pluginarray
class PluginArray : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(PluginArray, Bindings::LegacyPlatformObject);

public:
    virtual ~PluginArray() override;

    void refresh() const;
    size_t length() const;
    JS::GCPtr<Plugin> item(u32 index) const;
    JS::GCPtr<Plugin> named_item(String const& name) const;

private:
    PluginArray(JS::Realm&);

    virtual void initialize(JS::Realm&) override;

    // ^Bindings::LegacyPlatformObject
    virtual Vector<DeprecatedString> supported_property_names() const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;
    virtual WebIDL::ExceptionOr<JS::Value> named_item_value(DeprecatedFlyString const& name) const override;
    virtual bool is_supported_property_index(u32) const override;

    virtual bool supports_indexed_properties() const override { return true; }
    virtual bool supports_named_properties() const override { return true; }
    virtual bool has_indexed_property_setter() const override { return false; }
    virtual bool has_named_property_setter() const override { return false; }
    virtual bool has_named_property_deleter() const override { return false; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const override { return false; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const override { return true; }
    virtual bool has_global_interface_extended_attribute() const override { return false; }
    virtual bool indexed_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_deleter_has_identifier() const override { return false; }
};

}
