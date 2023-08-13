/*
 * Copyright (c) 2022, DerpyCrabs <derpycrabs@gmail.com>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/Geometry/DOMRect.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry-1/#DOMRectList
class DOMRectList final : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(DOMRectList, Bindings::LegacyPlatformObject);

public:
    [[nodiscard]] static JS::NonnullGCPtr<DOMRectList> create(JS::Realm&, Vector<JS::Handle<DOMRect>>);

    virtual ~DOMRectList() override;

    u32 length() const;
    DOMRect const* item(u32 index) const;

    virtual bool is_supported_property_index(u32) const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;

private:
    DOMRectList(JS::Realm&, Vector<JS::NonnullGCPtr<DOMRect>>);

    virtual void initialize(JS::Realm&) override;

    // ^Bindings::LegacyPlatformObject
    virtual bool supports_indexed_properties() const override { return true; }
    virtual bool supports_named_properties() const override { return false; }
    virtual bool has_indexed_property_setter() const override { return false; }
    virtual bool has_named_property_setter() const override { return false; }
    virtual bool has_named_property_deleter() const override { return false; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const override { return false; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const override { return false; }
    virtual bool has_global_interface_extended_attribute() const override { return false; }
    virtual bool indexed_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_deleter_has_identifier() const override { return false; }

    Vector<JS::NonnullGCPtr<DOMRect>> m_rects;
};

}
