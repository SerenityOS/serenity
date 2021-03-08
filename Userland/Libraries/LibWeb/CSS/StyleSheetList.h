/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        return adopt(*new StyleSheetList(document));
    }

    void add_sheet(NonnullRefPtr<CSSStyleSheet>);
    const NonnullRefPtrVector<CSSStyleSheet>& sheets() const { return m_sheets; }

    RefPtr<CSSStyleSheet> item(size_t index) const
    {
        if (index >= m_sheets.size())
            return {};
        return m_sheets[index];
    }

    size_t length() const { return m_sheets.size(); }

private:
    explicit StyleSheetList(DOM::Document&);

    DOM::Document& m_document;
    NonnullRefPtrVector<CSSStyleSheet> m_sheets;
};

}

namespace Web::Bindings {

StyleSheetListWrapper* wrap(JS::GlobalObject&, CSS::StyleSheetList&);

}
