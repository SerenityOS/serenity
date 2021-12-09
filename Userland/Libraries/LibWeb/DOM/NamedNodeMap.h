/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCountForwarder.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/WeakPtr.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#interface-namednodemap
class NamedNodeMap final
    : public RefCountForwarder<Element>
    , public Bindings::Wrappable {

public:
    using WrapperType = Bindings::NamedNodeMapWrapper;

    static NonnullRefPtr<NamedNodeMap> create(Element& associated_element);
    ~NamedNodeMap() = default;

    bool is_supported_property_index(u32 index) const;
    Vector<String> supported_property_names() const;

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
    explicit NamedNodeMap(Element& associated_element);

    Element& associated_element() { return ref_count_target(); }
    Element const& associated_element() const { return ref_count_target(); }

    NonnullRefPtrVector<Attribute> m_attributes;
};

}

namespace Web::Bindings {

NamedNodeMapWrapper* wrap(JS::GlobalObject&, DOM::NamedNodeMap&);

}
