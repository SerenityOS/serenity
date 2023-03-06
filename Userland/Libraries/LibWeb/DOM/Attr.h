/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/WeakPtr.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/QualifiedName.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#attr
class Attr final : public Node {
    WEB_PLATFORM_OBJECT(Attr, Node);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Attr>> create(Document&, DeprecatedFlyString local_name, DeprecatedString value, Element const* = nullptr);
    JS::NonnullGCPtr<Attr> clone(Document&);

    virtual ~Attr() override = default;

    virtual DeprecatedFlyString node_name() const override { return name(); }

    DeprecatedFlyString const& namespace_uri() const { return m_qualified_name.namespace_(); }
    DeprecatedFlyString const& prefix() const { return m_qualified_name.prefix(); }
    DeprecatedFlyString const& local_name() const { return m_qualified_name.local_name(); }
    DeprecatedString const& name() const { return m_qualified_name.as_string(); }

    DeprecatedString const& value() const { return m_value; }
    void set_value(DeprecatedString value);

    Element const* owner_element() const;
    void set_owner_element(Element const* owner_element);

    // Always returns true: https://dom.spec.whatwg.org/#dom-attr-specified
    constexpr bool specified() const { return true; }

    void handle_attribute_changes(Element const&, DeprecatedString const& old_value, DeprecatedString const& new_value);

private:
    Attr(Document&, QualifiedName, DeprecatedString value, Element const*);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    QualifiedName m_qualified_name;
    DeprecatedString m_value;
    JS::GCPtr<Element const> m_owner_element;
};

template<>
inline bool Node::fast_is<Attr>() const { return is_attribute(); }

}
