/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#interface-namednodemap
class NamedNodeMap : public Bindings::LegacyPlatformObject {
    JS_OBJECT(NamedNodeMap, Bindings::LegacyPlatformObject);

public:
    static NamedNodeMap* create(Element&);
    explicit NamedNodeMap(Element&);
    ~NamedNodeMap() = default;

    NamedNodeMap& impl() { return *this; }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual Vector<String> supported_property_names() const override;
    virtual JS::Value item_value(size_t index) const override;
    virtual JS::Value named_item_value(FlyString const& name) const override;

    size_t length() const { return m_attributes.size(); }
    bool is_empty() const { return m_attributes.is_empty(); }

    // Methods defined by the spec for JavaScript:
    Attribute const* item(u32 index) const;
    Attribute const* get_named_item(StringView qualified_name) const;
    ExceptionOr<Attribute const*> set_named_item(Attribute& attribute);
    ExceptionOr<Attribute const*> remove_named_item(StringView qualified_name);

    // Methods defined by the spec for internal use:
    Attribute* get_attribute(StringView qualified_name, size_t* item_index = nullptr);
    Attribute const* get_attribute(StringView qualified_name, size_t* item_index = nullptr) const;
    ExceptionOr<Attribute const*> set_attribute(Attribute& attribute);
    void replace_attribute(Attribute& old_attribute, Attribute& new_attribute, size_t old_attribute_index);
    void append_attribute(Attribute& attribute);
    Attribute const* remove_attribute(StringView qualified_name);

private:
    Element& associated_element() { return m_element; }
    Element const& associated_element() const { return m_element; }

    void remove_attribute_at_index(size_t attribute_index);

    DOM::Element& m_element;
    NonnullRefPtrVector<Attribute> m_attributes;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::DOM::NamedNodeMap& object) { return &object; }
using NamedNodeMapWrapper = Web::DOM::NamedNodeMap;
}
