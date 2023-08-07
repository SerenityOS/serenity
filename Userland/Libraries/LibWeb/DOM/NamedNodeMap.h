/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Alexander Narsudinov <a.narsudinov@gmail.com>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/StringView.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#interface-namednodemap
class NamedNodeMap : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(NamedNodeMap, Bindings::LegacyPlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<NamedNodeMap>> create(Element&);
    ~NamedNodeMap() = default;

    virtual bool is_supported_property_index(u32 index) const override;
    virtual Vector<DeprecatedString> supported_property_names() const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;
    virtual WebIDL::ExceptionOr<JS::Value> named_item_value(DeprecatedFlyString const& name) const override;

    size_t length() const { return m_attributes.size(); }
    bool is_empty() const { return m_attributes.is_empty(); }

    // Methods defined by the spec for JavaScript:
    Attr const* item(u32 index) const;
    Attr const* get_named_item(StringView qualified_name) const;
    Attr const* get_named_item_ns(StringView namespace_, StringView local_name) const;
    WebIDL::ExceptionOr<JS::GCPtr<Attr>> set_named_item(Attr& attribute);
    WebIDL::ExceptionOr<JS::GCPtr<Attr>> set_named_item_ns(Attr& attribute);
    WebIDL::ExceptionOr<Attr const*> remove_named_item(StringView qualified_name);
    WebIDL::ExceptionOr<Attr const*> remove_named_item_ns(StringView namespace_, StringView local_name);

    // Methods defined by the spec for internal use:
    Attr* get_attribute(StringView qualified_name, size_t* item_index = nullptr);
    Attr* get_attribute_ns(StringView namespace_, StringView local_name, size_t* item_index = nullptr);
    Attr const* get_attribute(StringView qualified_name, size_t* item_index = nullptr) const;
    Attr const* get_attribute_ns(StringView namespace_, StringView local_name, size_t* item_index = nullptr) const;
    WebIDL::ExceptionOr<JS::GCPtr<Attr>> set_attribute(Attr& attribute);
    void replace_attribute(Attr& old_attribute, Attr& new_attribute, size_t old_attribute_index);
    void append_attribute(Attr& attribute);
    Attr const* remove_attribute(StringView qualified_name);
    Attr const* remove_attribute_ns(StringView namespace_, StringView local_name);

private:
    explicit NamedNodeMap(Element&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // ^Bindings::LegacyPlatformObject
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

    Element& associated_element() { return *m_element; }
    Element const& associated_element() const { return *m_element; }

    void remove_attribute_at_index(size_t attribute_index);

    JS::NonnullGCPtr<DOM::Element> m_element;
    Vector<JS::NonnullGCPtr<Attr>> m_attributes;
};

}
