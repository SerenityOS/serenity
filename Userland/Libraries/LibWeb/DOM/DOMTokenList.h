/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#domtokenlist
class DOMTokenList final : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(DOMTokenList, Bindings::LegacyPlatformObject);

public:
    [[nodiscard]] static JS::NonnullGCPtr<DOMTokenList> create(Element& associated_element, FlyString associated_attribute);
    ~DOMTokenList() = default;

    void associated_attribute_changed(StringView value);

    virtual bool is_supported_property_index(u32 index) const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;

    size_t length() const { return m_token_set.size(); }
    Optional<String> item(size_t index) const;
    bool contains(String const& token);
    WebIDL::ExceptionOr<void> add(Vector<String> const& tokens);
    WebIDL::ExceptionOr<void> remove(Vector<String> const& tokens);
    WebIDL::ExceptionOr<bool> toggle(String const& token, Optional<bool> force);
    WebIDL::ExceptionOr<bool> replace(String const& token, String const& new_token);
    WebIDL::ExceptionOr<bool> supports(StringView token);
    String value() const;
    void set_value(String const& value);

private:
    DOMTokenList(Element& associated_element, FlyString associated_attribute);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

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

    WebIDL::ExceptionOr<void> validate_token(StringView token) const;
    void run_update_steps();

    JS::NonnullGCPtr<Element> m_associated_element;
    FlyString m_associated_attribute;
    Vector<String> m_token_set;
};

}
