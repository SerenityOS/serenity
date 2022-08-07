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
    , public Weakable<StyleSheetList>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::StyleSheetListWrapper;

    static NonnullRefPtr<StyleSheetList> create(DOM::Document& document)
    {
        return adopt_ref(*new StyleSheetList(document));
    }

    void add_sheet(CSSStyleSheet&);
    void remove_sheet(CSSStyleSheet&);

    Vector<JS::Handle<CSSStyleSheet>> const& sheets() const { return m_sheets; }
    Vector<JS::Handle<CSSStyleSheet>>& sheets() { return m_sheets; }

    CSSStyleSheet* item(size_t index) const
    {
        if (index >= m_sheets.size())
            return {};
        return const_cast<CSSStyleSheet*>(m_sheets[index].cell());
    }

    size_t length() const { return m_sheets.size(); }

    bool is_supported_property_index(u32) const;

    DOM::Document& document() { return m_document; }
    DOM::Document const& document() const { return m_document; }

private:
    explicit StyleSheetList(DOM::Document&);

    DOM::Document& m_document;
    Vector<JS::Handle<CSSStyleSheet>> m_sheets;
};

}

namespace Web::Bindings {

StyleSheetListWrapper* wrap(JS::Realm&, CSS::StyleSheetList&);

}
