/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/MutationRecord.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NodeList.h>

namespace Web::DOM {

class MutationRecordImpl final : public MutationRecord {
public:
    MutationRecordImpl(FlyString const& type, Node& target, NodeList& added_nodes, NodeList& removed_nodes, Node* previous_sibling, Node* next_sibling, String const& attribute_name, String const& attribute_namespace, String const& old_value)
        : m_type(type)
        , m_target(JS::make_handle(target))
        , m_added_nodes(added_nodes)
        , m_removed_nodes(removed_nodes)
        , m_previous_sibling(JS::make_handle(previous_sibling))
        , m_next_sibling(JS::make_handle(next_sibling))
        , m_attribute_name(attribute_name)
        , m_attribute_namespace(attribute_namespace)
        , m_old_value(old_value)
    {
    }

    virtual ~MutationRecordImpl() override = default;

    virtual FlyString const& type() const override { return m_type; }
    virtual Node const* target() const override { return m_target.ptr(); }
    virtual NodeList const* added_nodes() const override { return m_added_nodes; }
    virtual NodeList const* removed_nodes() const override { return m_removed_nodes; }
    virtual Node const* previous_sibling() const override { return m_previous_sibling.ptr(); }
    virtual Node const* next_sibling() const override { return m_next_sibling.ptr(); }
    virtual String const& attribute_name() const override { return m_attribute_name; }
    virtual String const& attribute_namespace() const override { return m_attribute_namespace; }
    virtual String const& old_value() const override { return m_old_value; }

private:
    FlyString m_type;
    JS::Handle<Node> m_target;
    NonnullRefPtr<NodeList> m_added_nodes;
    NonnullRefPtr<NodeList> m_removed_nodes;
    JS::Handle<Node> m_previous_sibling;
    JS::Handle<Node> m_next_sibling;
    String m_attribute_name;
    String m_attribute_namespace;
    String m_old_value;
};

NonnullRefPtr<MutationRecord> MutationRecord::create(FlyString const& type, Node& target, NodeList& added_nodes, NodeList& removed_nodes, Node* previous_sibling, Node* next_sibling, String const& attribute_name, String const& attribute_namespace, String const& old_value)
{
    return adopt_ref(*new MutationRecordImpl(type, target, added_nodes, removed_nodes, previous_sibling, next_sibling, attribute_name, attribute_namespace, old_value));
}

}
