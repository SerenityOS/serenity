/*
 * Copyright (c) 2022, Łukasz Maciejewski <lukasz.m.maciejewski@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/MutationRecord.h>

#include <AK/NonnullRefPtr.h>
#include <LibWeb/DOM/StaticNodeList.h>

namespace Web::DOM {

MutationRecord::MutationRecord(MutationRecordType type, Node& target, String data)
    : m_type { type }
    , m_target { adopt_ref(target) }
    , m_added_nodes { StaticNodeList::create(NonnullRefPtrVector<Node> {}) }
    , m_removed_nodes { StaticNodeList::create(NonnullRefPtrVector<Node> {}) }
    , m_old_value { move(data) }
{
}

String MutationRecord::type()
{
    switch (m_type) {
    case MutationRecordType::CHARACTER_DATA:
        return "characterData";
    case MutationRecordType::ATTRIBUTE_DATA:
        return "attributeData";
    case MutationRecordType::TREE:
        return "tree";
    case MutationRecordType::INVALID:
        return "INVALID";
    }

    return "INVALID";
}

}
