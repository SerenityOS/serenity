/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/StyleSheetList.h>

namespace Web::CSS {

void StyleSheetList::add_sheet(NonnullRefPtr<CSSStyleSheet> sheet)
{
    m_sheets.append(move(sheet));
}

StyleSheetList::StyleSheetList(DOM::Document& document)
    : m_document(document)
{
}

// https://drafts.csswg.org/cssom/#ref-for-dfn-supported-property-indices%E2%91%A1
bool StyleSheetList::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of CSS style sheets represented by the collection.
    // If there are no such CSS style sheets, then there are no supported property indices.
    if (m_sheets.is_empty())
        return false;

    return index < m_sheets.size();
}

}
