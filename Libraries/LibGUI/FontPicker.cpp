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

#include <LibGUI/Button.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/FontPickerDialogGML.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Widget.h>
#include <LibGfx/FontDatabase.h>

namespace GUI {

FontPicker::FontPicker(Window* parent_window, const Gfx::Font* current_font, bool fixed_width_only)
    : Dialog(parent_window)
    , m_fixed_width_only(fixed_width_only)
{
    set_title("Font picker");
    resize(540, 300);
    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-font-editor.png"));

    auto& widget = set_main_widget<GUI::Widget>();
    if (!widget.load_from_gml(font_picker_dialog_gml))
        ASSERT_NOT_REACHED();

    m_family_list_view = static_cast<ListView&>(*widget.find_descendant_by_name("family_list_view"));
    m_weight_list_view = static_cast<ListView&>(*widget.find_descendant_by_name("weight_list_view"));
    m_size_list_view = static_cast<ListView&>(*widget.find_descendant_by_name("size_list_view"));
    m_sample_text_label = static_cast<Label&>(*widget.find_descendant_by_name("sample_text_label"));

    HashTable<String> families;
    Gfx::FontDatabase::the().for_each_font([&](auto& font) {
        if (m_fixed_width_only && !font.is_fixed_width())
            return;
        families.set(font.family());
    });

    for (auto& family : families)
        m_families.append(family);

    m_family_list_view->set_model(ItemListModel<String>::create(m_families));

    m_family_list_view->on_selection = [this](auto& index) {
        m_family = index.data().to_string();
        HashTable<int> weights;
        Gfx::FontDatabase::the().for_each_font([&](auto& font) {
            if (m_fixed_width_only && !font.is_fixed_width())
                return;
            if (font.family() == m_family.value())
                weights.set(font.weight());
        });
        m_weights.clear();
        Optional<size_t> index_of_old_weight_in_new_list;
        size_t i = 0;
        for (auto& weight : weights) {
            m_weights.append(weight);
            if (m_weight.has_value() && weight == m_weight.value())
                index_of_old_weight_in_new_list = i;
            ++i;
        }

        m_weight_list_view->set_model(ItemListModel<int>::create(m_weights));
        m_weight_list_view->set_cursor(m_weight_list_view->model()->index(index_of_old_weight_in_new_list.value_or(0)), GUI::AbstractView::SelectionUpdate::Set);
        update_sample_label();
    };

    m_weight_list_view->on_selection = [this](auto& index) {
        m_weight = index.data().to_i32();
        m_sizes.clear();
        Optional<size_t> index_of_old_size_in_new_list;
        Gfx::FontDatabase::the().for_each_font([&](auto& font) {
            if (m_fixed_width_only && !font.is_fixed_width())
                return;
            if (font.family() == m_family.value() && font.weight() == m_weight.value()) {
                if (m_size.has_value() && m_size.value() == font.presentation_size())
                    index_of_old_size_in_new_list = m_sizes.size();
                m_sizes.append(font.presentation_size());
            }
        });

        m_size_list_view->set_model(ItemListModel<int>::create(m_sizes));
        m_size_list_view->set_cursor(m_size_list_view->model()->index(index_of_old_size_in_new_list.value_or(0)), GUI::AbstractView::SelectionUpdate::Set);
        update_sample_label();
    };

    m_size_list_view->on_selection = [this](auto& index) {
        m_size = index.data().to_i32();
        update_sample_label();
    };

    auto& ok_button = static_cast<Button&>(*widget.find_descendant_by_name("ok_button"));
    ok_button.on_click = [this](auto) {
        done(ExecOK);
    };

    auto& cancel_button = static_cast<Button&>(*widget.find_descendant_by_name("cancel_button"));
    cancel_button.on_click = [this](auto) {
        done(ExecCancel);
    };

    set_font(current_font);
}

FontPicker::~FontPicker()
{
}

void FontPicker::set_font(const Gfx::Font* font)
{
    if (m_font == font)
        return;
    m_font = font;
    m_sample_text_label->set_font(m_font);

    if (!m_font) {
        m_weights.clear();
        m_sizes.clear();
        m_weight_list_view->set_model(nullptr);
        m_size_list_view->set_model(nullptr);
        return;
    }

    size_t family_index = 0;
    for (size_t i = 0; i < m_families.size(); ++i) {
        if (m_families[i] == m_font->family()) {
            family_index = i;
            break;
        }
    }

    m_family_list_view->set_cursor(m_family_list_view->model()->index(family_index), GUI::AbstractView::SelectionUpdate::Set);

    size_t weight_index = 0;
    for (size_t i = 0; i < m_weights.size(); ++i) {
        if (m_weights[i] == m_font->weight()) {
            weight_index = i;
            break;
        }
    }

    m_weight_list_view->set_cursor(m_weight_list_view->model()->index(weight_index), GUI::AbstractView::SelectionUpdate::Set);

    size_t size_index = 0;
    for (size_t i = 0; i < m_sizes.size(); ++i) {
        if (m_sizes[i] == m_font->presentation_size()) {
            size_index = i;
            break;
        }
    }

    m_size_list_view->set_cursor(m_size_list_view->model()->index(size_index), GUI::AbstractView::SelectionUpdate::Set);
}

void FontPicker::update_sample_label()
{
    if (m_family.has_value() && m_size.has_value() && m_weight.has_value())
        set_font(Gfx::FontDatabase::the().get(m_family.value(), m_size.value(), m_weight.value()));
}

}
