/*
 * Copyright (c) 2021-2023, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/LegacyPlatformObject.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#nodelist
class NodeList : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(NodeList, Bindings::LegacyPlatformObject);

public:
    virtual ~NodeList() override;

    virtual u32 length() const = 0;
    virtual Node const* item(u32 index) const = 0;

    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;
    virtual bool is_supported_property_index(u32) const override;

protected:
    explicit NodeList(JS::Realm&);

    virtual void initialize(JS::Realm&) override;

    // ^Bindings::LegacyPlatformObject
    virtual bool supports_indexed_properties() const final override { return true; }
    virtual bool supports_named_properties() const final override { return false; }
    virtual bool has_indexed_property_setter() const final override { return false; }
    virtual bool has_named_property_setter() const final override { return false; }
    virtual bool has_named_property_deleter() const final override { return false; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const final override { return false; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const final override { return false; }
    virtual bool has_global_interface_extended_attribute() const final override { return false; }
    virtual bool indexed_property_setter_has_identifier() const final override { return false; }
    virtual bool named_property_setter_has_identifier() const final override { return false; }
    virtual bool named_property_deleter_has_identifier() const final override { return false; }
};

}
