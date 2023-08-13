/*
 * Copyright (c) 2021-2023, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/dom.html#domstringmap
class DOMStringMap final : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(DOMStringMap, Bindings::LegacyPlatformObject);

public:
    [[nodiscard]] static JS::NonnullGCPtr<DOMStringMap> create(DOM::Element&);

    virtual ~DOMStringMap() override;

    DeprecatedString determine_value_of_named_property(DeprecatedString const&) const;

    virtual WebIDL::ExceptionOr<void> set_value_of_new_named_property(DeprecatedString const&, JS::Value) override;
    virtual WebIDL::ExceptionOr<void> set_value_of_existing_named_property(DeprecatedString const&, JS::Value) override;

    virtual WebIDL::ExceptionOr<DidDeletionFail> delete_value(DeprecatedString const&) override;

private:
    explicit DOMStringMap(DOM::Element&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // ^LegacyPlatformObject
    virtual WebIDL::ExceptionOr<JS::Value> named_item_value(DeprecatedFlyString const&) const override;
    virtual Vector<DeprecatedString> supported_property_names() const override;

    virtual bool supports_indexed_properties() const override { return false; }
    virtual bool supports_named_properties() const override { return true; }
    virtual bool has_indexed_property_setter() const override { return false; }
    virtual bool has_named_property_setter() const override { return true; }
    virtual bool has_named_property_deleter() const override { return true; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const override { return true; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const override { return false; }
    virtual bool has_global_interface_extended_attribute() const override { return false; }
    virtual bool indexed_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_deleter_has_identifier() const override { return false; }

    struct NameValuePair {
        DeprecatedString name;
        DeprecatedString value;
    };

    Vector<NameValuePair> get_name_value_pairs() const;

    // https://html.spec.whatwg.org/multipage/dom.html#concept-domstringmap-element
    JS::NonnullGCPtr<DOM::Element> m_associated_element;
};

}
