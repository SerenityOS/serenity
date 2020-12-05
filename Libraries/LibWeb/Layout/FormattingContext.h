/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/Forward.h>

namespace Web::Layout {

class FormattingContext {
public:
    virtual void run(LayoutMode) = 0;

    Box& context_box() { return *m_context_box; }
    const Box& context_box() const { return *m_context_box; }

    FormattingContext* parent() { return m_parent; }
    const FormattingContext* parent() const { return m_parent; }

    virtual bool is_block_formatting_context() const { return false; }

    static bool creates_block_formatting_context(const Box&);

protected:
    FormattingContext(Box&, FormattingContext* parent = nullptr);
    virtual ~FormattingContext();

    void set_context_box(Box& box) { m_context_box = &box; }

    void layout_inside(Box&, LayoutMode);

    struct ShrinkToFitResult {
        float preferred_width { 0 };
        float preferred_minimum_width { 0 };
    };

    ShrinkToFitResult calculate_shrink_to_fit_widths(Box&);

    FormattingContext* m_parent { nullptr };
    Box* m_context_box { nullptr };
};

}
