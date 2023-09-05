/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibGUI/Dialog.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Forward.h>

namespace GUI {

class FontPicker final : public GUI::Dialog {
    C_OBJECT(FontPicker);

public:
    virtual ~FontPicker() override = default;

    RefPtr<Gfx::Font const> font() const { return m_font; }
    void set_font(Gfx::Font const*);

private:
    FontPicker(Window* parent_window = nullptr, Gfx::Font const* current_font = nullptr, bool fixed_width_only = false);

    void update_font();

    bool const m_fixed_width_only;

    RefPtr<Gfx::Font const> m_font;

    RefPtr<ListView> m_family_list_view;
    RefPtr<ListView> m_variant_list_view;
    RefPtr<ListView> m_size_list_view;
    RefPtr<SpinBox> m_size_spin_box;
    RefPtr<Label> m_sample_text_label;

    Vector<FlyString> m_families;
    Vector<FlyString> m_variants;
    Vector<int> m_sizes;

    Optional<FlyString> m_family;
    Optional<FlyString> m_variant;
    Optional<int> m_size;
};

}
