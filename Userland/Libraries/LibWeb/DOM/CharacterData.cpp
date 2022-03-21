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

void CharacterData::set_data(String data)
{
    if (m_data == data)
        return;
    m_data = move(data);
    if (parent())
        parent()->children_changed();
    set_needs_style_update(true);
    document().set_needs_layout();
}

// https://dom.spec.whatwg.org/#concept-cd-substring
ExceptionOr<String> CharacterData::substring_data(size_t offset, size_t count) const
{
    // 1. Let length be node’s length.
    auto length = this->length();

    // 2. If offset is greater than length, then throw an "IndexSizeError" DOMException.
    if (offset > length)
        return DOM::IndexSizeError::create("Substring offset out of range.");

    // 3. If offset plus count is greater than length, return a string whose value is the code units from the offsetth code unit
    //    to the end of node’s data, and then return.
    if (offset + count > length)
        return m_data.substring(offset);

    // 4. Return a string whose value is the code units from the offsetth code unit to the offset+countth code unit in node’s data.
    return m_data.substring(offset, count);
}

}
