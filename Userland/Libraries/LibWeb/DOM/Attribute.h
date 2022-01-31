/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/WeakPtr.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/QualifiedName.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#attr
class Attribute final : public Node {
public:
    using WrapperType = Bindings::AttributeWrapper;

    static NonnullRefPtr<Attribute> create(Document&, FlyString local_name, String value, Element const* = nullptr);

    virtual ~Attribute() override = default;

    virtual FlyString node_name() const override { return name(); }

    FlyString const& namespace_uri() const { return m_qualified_name.namespace_(); }
    FlyString const& prefix() const { return m_qualified_name.prefix(); }
    FlyString const& local_name() const { return m_qualified_name.local_name(); }
    String const& name() const { return m_qualified_name.as_string(); }

    String const& value() const { return m_value; }
    void set_value(String value) { m_value = move(value); }

    Element const* owner_element() const;
    void set_owner_element(Element const* owner_element);

    // Always returns true: https://dom.spec.whatwg.org/#dom-attr-specified
    constexpr bool specified() const { return true; }

private:
    Attribute(Document&, FlyString local_name, String value, Element const*);

    QualifiedName m_qualified_name;
    String m_value;
    WeakPtr<Element> m_owner_element;
};

template<>
inline bool Node::fast_is<Attribute>() const { return is_attribute(); }

}
