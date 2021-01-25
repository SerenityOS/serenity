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

#include <LibGUI/Dialog.h>
#include <LibGfx/Font.h>
#include <LibGfx/Forward.h>

namespace GUI {

class FontPicker final : public GUI::Dialog {
    C_OBJECT(FontPicker);

public:
    virtual ~FontPicker() override;

    RefPtr<Gfx::Font> font() const { return m_font; }
    void set_font(const Gfx::Font*);

private:
    FontPicker(Window* parent_window = nullptr, const Gfx::Font* current_font = nullptr, bool fixed_width_only = false);

    void update_font();

    const bool m_fixed_width_only;

    RefPtr<Gfx::Font> m_font;

    RefPtr<ListView> m_family_list_view;
    RefPtr<ListView> m_weight_list_view;
    RefPtr<ListView> m_size_list_view;
    RefPtr<Label> m_sample_text_label;

    Vector<String> m_families;
    Vector<int> m_weights;
    Vector<int> m_sizes;

    Optional<String> m_family;
    Optional<int> m_weight;
    Optional<int> m_size;
};

}
