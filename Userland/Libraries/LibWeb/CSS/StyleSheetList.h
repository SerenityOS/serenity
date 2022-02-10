/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class StyleSheetList
    : public RefCounted<StyleSheetList>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::StyleSheetListWrapper;

    static NonnullRefPtr<StyleSheetList> create(DOM::Document& document)
    {
        return adopt_ref(*new StyleSheetList(document));
    }

    void add_sheet(NonnullRefPtr<CSSStyleSheet>);
    void remove_sheet(CSSStyleSheet&);

    NonnullRefPtrVector<CSSStyleSheet> const& sheets() const { return m_sheets; }
    NonnullRefPtrVector<CSSStyleSheet>& sheets() { return m_sheets; }

    RefPtr<CSSStyleSheet> item(size_t index) const
    {
        if (index >= m_sheets.size())
            return {};
        return m_sheets[index];
    }

    size_t length() const { return m_sheets.size(); }

    bool is_supported_property_index(u32) const;

    int generation() const { return m_generation; }
    void bump_generation() { ++m_generation; }

private:
    explicit StyleSheetList(DOM::Document&);

    DOM::Document& m_document;
    NonnullRefPtrVector<CSSStyleSheet> m_sheets;

    int m_generation { 0 };
};

}

namespace Web::Bindings {

StyleSheetListWrapper* wrap(JS::GlobalObject&, CSS::StyleSheetList&);

}
