/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/CSS/CSSStyleSheet.h>

namespace Web::CSS {

class StyleSheetList : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(StyleSheetList, Bindings::LegacyPlatformObject);

public:
    static StyleSheetList* create(DOM::Document& document);

    void add_sheet(CSSStyleSheet&);
    void remove_sheet(CSSStyleSheet&);

    Vector<JS::NonnullGCPtr<CSSStyleSheet>> const& sheets() const { return m_sheets; }
    Vector<JS::NonnullGCPtr<CSSStyleSheet>>& sheets() { return m_sheets; }

    CSSStyleSheet* item(size_t index) const
    {
        if (index >= m_sheets.size())
            return {};
        return const_cast<CSSStyleSheet*>(m_sheets[index].ptr());
    }

    size_t length() const { return m_sheets.size(); }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual JS::Value item_value(size_t index) const override;

    DOM::Document& document() { return m_document; }
    DOM::Document const& document() const { return m_document; }

private:
    explicit StyleSheetList(DOM::Document&);

    virtual void visit_edges(Cell::Visitor&) override;

    void sort_sheets();

    DOM::Document& m_document;
    Vector<JS::NonnullGCPtr<CSSStyleSheet>> m_sheets;
};

}

WRAPPER_HACK(StyleSheetList, Web::CSS)
