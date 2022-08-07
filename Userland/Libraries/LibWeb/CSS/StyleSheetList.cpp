/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/StyleSheetListPrototype.h>
#include <LibWeb/CSS/StyleSheetList.h>
#include <LibWeb/DOM/Document.h>

namespace Web::CSS {

void StyleSheetList::add_sheet(CSSStyleSheet& sheet)
{
    sheet.set_style_sheet_list({}, this);
    m_sheets.append(sheet);

    m_document.style_computer().invalidate_rule_cache();
    m_document.style_computer().load_fonts_from_sheet(sheet);
    m_document.invalidate_style();
}

void StyleSheetList::remove_sheet(CSSStyleSheet& sheet)
{
    sheet.set_style_sheet_list({}, nullptr);
    m_sheets.remove_first_matching([&](auto& entry) { return &entry == &sheet; });

    m_document.style_computer().invalidate_rule_cache();
    m_document.invalidate_style();
}

StyleSheetList* StyleSheetList::create(DOM::Document& document)
{
    auto& realm = document.preferred_window_object().realm();
    return realm.heap().allocate<StyleSheetList>(realm, document);
}

StyleSheetList::StyleSheetList(DOM::Document& document)
    : Bindings::LegacyPlatformObject(document.preferred_window_object().ensure_web_prototype<Bindings::StyleSheetListPrototype>("StyleSheetList"))
    , m_document(document)
{
}

void StyleSheetList::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_document);
    for (auto& sheet : m_sheets)
        visitor.visit(&sheet);
}

// https://www.w3.org/TR/cssom/#ref-for-dfn-supported-property-indices%E2%91%A1
bool StyleSheetList::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of CSS style sheets represented by the collection.
    // If there are no such CSS style sheets, then there are no supported property indices.
    if (m_sheets.is_empty())
        return false;

    return index < m_sheets.size();
}

JS::Value StyleSheetList::item_value(size_t index) const
{
    if (index >= m_sheets.size())
        return JS::js_undefined();

    return &m_sheets[index];
}

}
