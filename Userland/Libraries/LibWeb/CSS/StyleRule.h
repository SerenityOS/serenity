/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleDeclaration.h>

namespace Web::CSS {

class StyleRule : public RefCounted<StyleRule> {
    AK_MAKE_NONCOPYABLE(StyleRule);
    AK_MAKE_NONMOVABLE(StyleRule);

public:
    static NonnullRefPtr<StyleRule> create(Vector<Selector>&& selectors, NonnullRefPtr<StyleDeclaration>&& declaration)
    {
        return adopt(*new StyleRule(move(selectors), move(declaration)));
    }

    ~StyleRule();

    const Vector<Selector>& selectors() const { return m_selectors; }
    const StyleDeclaration& declaration() const { return m_declaration; }

private:
    StyleRule(Vector<Selector>&&, NonnullRefPtr<StyleDeclaration>&&);

    Vector<Selector> m_selectors;
    NonnullRefPtr<StyleDeclaration> m_declaration;
};

}
