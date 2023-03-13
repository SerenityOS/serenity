/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/StyleSheetListPrototype.h>
#include <LibWeb/CSS/StyleSheetList.h>
#include <LibWeb/DOM/Document.h>

namespace Web::CSS {

void StyleSheetList::add_sheet(CSSStyleSheet& sheet)
{
    sheet.set_style_sheet_list({}, this);
    m_sheets.append(sheet);

    sort_sheets();

    if (sheet.rules().length() == 0) {
        // NOTE: If the added sheet has no rules, we don't have to invalidate anything.
        return;
    }

    m_document->style_computer().invalidate_rule_cache();
    m_document->style_computer().load_fonts_from_sheet(sheet);
    m_document->invalidate_style();
}

void StyleSheetList::remove_sheet(CSSStyleSheet& sheet)
{
    sheet.set_style_sheet_list({}, nullptr);
    m_sheets.remove_first_matching([&](auto& entry) { return entry.ptr() == &sheet; });

    if (sheet.rules().length() == 0) {
        // NOTE: If the removed sheet had no rules, we don't have to invalidate anything.
        return;
    }

    sort_sheets();

    m_document->style_computer().invalidate_rule_cache();
    m_document->invalidate_style();
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<StyleSheetList>> StyleSheetList::create(DOM::Document& document)
{
    auto& realm = document.realm();
    return MUST_OR_THROW_OOM(realm.heap().allocate<StyleSheetList>(realm, document));
}

StyleSheetList::StyleSheetList(DOM::Document& document)
    : Bindings::LegacyPlatformObject(document.realm())
    , m_document(document)
{
}

JS::ThrowCompletionOr<void> StyleSheetList::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::StyleSheetListPrototype>(realm, "StyleSheetList"));

    return {};
}

void StyleSheetList::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_document);
    for (auto sheet : m_sheets)
        visitor.visit(sheet.ptr());
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

WebIDL::ExceptionOr<JS::Value> StyleSheetList::item_value(size_t index) const
{
    if (index >= m_sheets.size())
        return JS::js_undefined();

    return m_sheets[index].ptr();
}

void StyleSheetList::sort_sheets()
{
    quick_sort(m_sheets, [](JS::NonnullGCPtr<StyleSheet> a, JS::NonnullGCPtr<StyleSheet> b) {
        return a->owner_node()->is_before(*b->owner_node());
    });
}

}
