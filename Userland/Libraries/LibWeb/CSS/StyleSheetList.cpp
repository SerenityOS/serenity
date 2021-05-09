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

}
