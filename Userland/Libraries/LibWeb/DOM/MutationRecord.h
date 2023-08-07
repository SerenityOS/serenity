/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#mutationrecord
class MutationRecord : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(MutationRecord, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<MutationRecord>> create(JS::Realm&, FlyString const& type, Node const& target, NodeList& added_nodes, NodeList& removed_nodes, Node* previous_sibling, Node* next_sibling, DeprecatedString const& attribute_name, DeprecatedString const& attribute_namespace, DeprecatedString const& old_value);

    virtual ~MutationRecord() override;

    FlyString const& type() const { return m_type; }
    Node const* target() const { return m_target; }
    NodeList const* added_nodes() const { return m_added_nodes; }
    NodeList const* removed_nodes() const { return m_removed_nodes; }
    Node const* previous_sibling() const { return m_previous_sibling; }
    Node const* next_sibling() const { return m_next_sibling; }
    DeprecatedString const& attribute_name() const { return m_attribute_name; }
    DeprecatedString const& attribute_namespace() const { return m_attribute_namespace; }
    DeprecatedString const& old_value() const { return m_old_value; }

private:
    MutationRecord(JS::Realm& realm, FlyString const& type, Node const& target, NodeList& added_nodes, NodeList& removed_nodes, Node* previous_sibling, Node* next_sibling, DeprecatedString const& attribute_name, DeprecatedString const& attribute_namespace, DeprecatedString const& old_value);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    FlyString m_type;
    JS::GCPtr<Node const> m_target;
    JS::GCPtr<NodeList> m_added_nodes;
    JS::GCPtr<NodeList> m_removed_nodes;
    JS::GCPtr<Node> m_previous_sibling;
    JS::GCPtr<Node> m_next_sibling;
    DeprecatedString m_attribute_name;
    DeprecatedString m_attribute_namespace;
    DeprecatedString m_old_value;
};

}
