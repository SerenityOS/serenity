/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    RefPtr<ListView> m_variant_list_view;
    RefPtr<ListView> m_size_list_view;
    RefPtr<SpinBox> m_size_spin_box;
    RefPtr<Label> m_sample_text_label;

    Vector<String> m_families;
    Vector<String> m_variants;
    Vector<int> m_sizes;

    Optional<String> m_family;
    Optional<String> m_variant;
    Optional<int> m_size;
};

}
