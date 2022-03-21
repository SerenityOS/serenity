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

// https://dom.spec.whatwg.org/#concept-cd-replace
ExceptionOr<void> CharacterData::replace_data(size_t offset, size_t count, String const& data)
{
    // 1. Let length be node’s length.
    auto length = this->length();

    // 2. If offset is greater than length, then throw an "IndexSizeError" DOMException.
    if (offset > length)
        return DOM::IndexSizeError::create("Replacement offset out of range.");

    // 3. If offset plus count is greater than length, then set count to length minus offset.
    if (offset + count > length)
        count = length - offset;

    // FIXME: 4. Queue a mutation record of "characterData" for node with null, null, node’s data, « », « », null, and null.

    // 5. Insert data into node’s data after offset code units.
    // 6. Let delete offset be offset + data’s length.
    // 7. Starting from delete offset code units, remove count code units from node’s data.
    StringBuilder builder;
    builder.append(this->data().substring_view(0, offset));
    builder.append(data.characters(), count);
    builder.append(this->data().substring_view(offset + count));
    set_data(builder.to_string());

    // FIXME: 8. For each live range whose start node is node and start offset is greater than offset but less than or equal to offset plus count, set its start offset to offset.

    // FIXME: 9. For each live range whose end node is node and end offset is greater than offset but less than or equal to offset plus count, set its end offset to offset.

    // FIXME: 10. For each live range whose start node is node and start offset is greater than offset plus count, increase its start offset by data’s length and decrease it by count.

    // FIXME: 11. For each live range whose end node is node and end offset is greater than offset plus count, increase its end offset by data’s length and decrease it by count.

    // 12. If node’s parent is non-null, then run the children changed steps for node’s parent.
    if (parent())
        parent()->children_changed();

    return {};
}

}
