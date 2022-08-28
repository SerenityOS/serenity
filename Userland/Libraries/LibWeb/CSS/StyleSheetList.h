/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class StyleSheetList : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(StyleSheetList, Bindings::LegacyPlatformObject);

public:
    static StyleSheetList* create(DOM::Document& document);

    void add_sheet(CSSStyleSheet&);
    void remove_sheet(CSSStyleSheet&);

    Vector<CSSStyleSheet&> const& sheets() const { return m_sheets; }
    Vector<CSSStyleSheet&>& sheets() { return m_sheets; }

    CSSStyleSheet* item(size_t index) const
    {
        if (index >= m_sheets.size())
            return {};
        return &const_cast<CSSStyleSheet&>(m_sheets[index]);
    }

    size_t length() const { return m_sheets.size(); }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual JS::Value item_value(size_t index) const override;

    DOM::Document& document() { return m_document; }
    DOM::Document const& document() const { return m_document; }

private:
    explicit StyleSheetList(DOM::Document&);

    virtual void visit_edges(Cell::Visitor&) override;

    DOM::Document& m_document;
    Vector<CSSStyleSheet&> m_sheets;
};

}

WRAPPER_HACK(StyleSheetList, Web::CSS)
