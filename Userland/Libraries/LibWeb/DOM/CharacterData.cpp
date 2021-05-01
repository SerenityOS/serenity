/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/CharacterData.h>
#include <LibWeb/DOM/Document.h>

namespace Web::DOM {

CharacterData::CharacterData(Document& document, NodeType type, const String& data)
    : Node(document, type)
    , m_data(data)
{
}

CharacterData::~CharacterData()
{
}

void CharacterData::set_data(String data)
{
    if (m_data == data)
        return;
    m_data = move(data);
    // FIXME: This is definitely too aggressive.
    document().schedule_forced_layout();
}

}
