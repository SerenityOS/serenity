/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
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
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMTokenList>> create(Element& associated_element, DeprecatedFlyString associated_attribute);
    ~DOMTokenList() = default;

    void associated_attribute_changed(StringView value);

    virtual bool is_supported_property_index(u32 index) const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;

    size_t length() const { return m_token_set.size(); }
    DeprecatedString const& item(size_t index) const;
    bool contains(StringView token);
    WebIDL::ExceptionOr<void> add(Vector<DeprecatedString> const& tokens);
    WebIDL::ExceptionOr<void> remove(Vector<DeprecatedString> const& tokens);
    WebIDL::ExceptionOr<bool> toggle(DeprecatedString const& token, Optional<bool> force);
    WebIDL::ExceptionOr<bool> replace(DeprecatedString const& token, DeprecatedString const& new_token);
    WebIDL::ExceptionOr<bool> supports(StringView token);
    DeprecatedString value() const;
    void set_value(DeprecatedString value);

private:
    DOMTokenList(Element& associated_element, DeprecatedFlyString associated_attribute);

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
    DeprecatedFlyString m_associated_attribute;
    Vector<DeprecatedString> m_token_set;
};

}
