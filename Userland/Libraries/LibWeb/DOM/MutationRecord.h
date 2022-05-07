/*
 * Copyright (c) 2022, Łukasz Maciejewski <lukasz.m.maciejewski@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/String.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::DOM {

enum class MutationRecordType {
    INVALID = 0,
    CHARACTER_DATA,
    ATTRIBUTE_DATA,
    TREE,
};

class MutationRecord
    : public RefCounted<MutationRecord>
    , public Bindings::Wrappable {

    MutationRecordType m_type;
    NonnullRefPtr<Node> m_target;
    NonnullRefPtr<NodeList> m_added_nodes;
    NonnullRefPtr<NodeList> m_removed_nodes;
    String m_old_value;

public:
    using WrapperType = Bindings::MutationRecordWrapper;

    MutationRecord(MutationRecordType type, Node& target, String data);

    virtual ~MutationRecord() = default;

    String type();

    Node* target() { return m_target; }
    NodeList* added_nodes() { return m_added_nodes; }
    NodeList* removed_nodes() { return m_removed_nodes; }
    Node* previous_sibling() { return nullptr; }
    Node* next_sibling() { return nullptr; }
    String attribute_name() { return ""; }
    String attribute_namespace() { return ""; }
    String old_value() { return m_old_value; }
};

}
