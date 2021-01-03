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

#include <AK/QuickSort.h>
#include <LibGUI/Button.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/FontPickerDialogGML.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Widget.h>
#include <LibGfx/FontDatabase.h>

namespace GUI {

struct FontWeightNameMapping {
    constexpr FontWeightNameMapping(int w, const char* n)
        : weight(w)
        , name(n)
    {
    }
    int weight { 0 };
    StringView name;
};

static constexpr FontWeightNameMapping font_weight_names[] = {
    { 100, "Thin" },
    { 200, "Extra Light" },
    { 300, "Light" },
    { 400, "Regular" },
    { 500, "Medium" },
    { 600, "Semi Bold" },
    { 700, "Bold" },
    { 800, "Extra Bold" },
    { 900, "Black" },
    { 950, "Extra Black" },
};

static constexpr StringView weight_to_name(int weight)
{
    for (auto& it : font_weight_names) {
        if (it.weight == weight)
            return it.name;
    }
    return {};
}

class FontWeightListModel : public ItemListModel<int> {
public:
    FontWeightListModel(const Vector<int>& weights)
        : ItemListModel(weights)
    {
    }

    virtual Variant data(const ModelIndex& index, ModelRole role) const override
    {
        if (role == ModelRole::Custom)
            return m_data.at(index.row());
        if (role == ModelRole::Display)
            return String(weight_to_name(m_data.at(index.row())));
        return ItemListModel::data(index, role);
    }
};

FontPicker::FontPicker(Window* parent_window, const Gfx::Font* current_font, bool fixed_width_only)
    : Dialog(parent_window)
    , m_fixed_width_only(fixed_width_only)
{
    set_title("Font picker");
    resize(430, 280);
    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-font-editor.png"));

    auto& widget = set_main_widget<GUI::Widget>();
    if (!widget.load_from_gml(font_picker_dialog_gml))
        ASSERT_NOT_REACHED();

    m_family_list_view = *widget.find_descendant_of_type_named<ListView>("family_list_view");
    m_family_list_view->set_model(ItemListModel<String>::create(m_families));
    m_family_list_view->horizontal_scrollbar().set_visible(false);

    m_weight_list_view = *widget.find_descendant_of_type_named<ListView>("weight_list_view");
    m_weight_list_view->set_model(adopt(*new FontWeightListModel(m_weights)));
    m_weight_list_view->horizontal_scrollbar().set_visible(false);

    m_size_list_view = *widget.find_descendant_of_type_named<ListView>("size_list_view");
    m_size_list_view->set_model(ItemListModel<int>::create(m_sizes));
    m_size_list_view->horizontal_scrollbar().set_visible(false);

    m_sample_text_label = *widget.find_descendant_of_type_named<Label>("sample_text_label");

    m_families.clear();
    Gfx::FontDatabase::the().for_each_typeface([&](auto& typeface) {
        if (m_fixed_width_only && !typeface.is_fixed_width())
            return;
        if (!m_families.contains_slow(typeface.family()))
            m_families.append(typeface.family());
    });
    quick_sort(m_families);

    m_family_list_view->on_selection = [this](auto& index) {
        m_family = index.data().to_string();
        m_weights.clear();
        Gfx::FontDatabase::the().for_each_typeface([&](auto& typeface) {
            if (m_fixed_width_only && !typeface.is_fixed_width())
                return;
            if (typeface.family() == m_family.value() && !m_weights.contains_slow(typeface.weight())) {
                m_weights.append(typeface.weight());
            }
        });
        quick_sort(m_weights);
        Optional<size_t> index_of_old_weight_in_new_list;
        if (m_weight.has_value())
            index_of_old_weight_in_new_list = m_weights.find_first_index(m_weight.value());

        m_weight_list_view->model()->update();
        m_weight_list_view->set_cursor(m_weight_list_view->model()->index(index_of_old_weight_in_new_list.value_or(0)), GUI::AbstractView::SelectionUpdate::Set);
        update_font();
    };

    m_weight_list_view->on_selection = [this](auto& index) {
        m_weight = index.data(ModelRole::Custom).to_i32();
        m_sizes.clear();
        dbgln("Selected weight: {}", m_weight.value());
        Gfx::FontDatabase::the().for_each_typeface([&](auto& typeface) {
            if (m_fixed_width_only && !typeface.is_fixed_width())
                return;
            if (typeface.family() == m_family.value() && (int)typeface.weight() == m_weight.value()) {
                if (typeface.is_fixed_size()) {
                    typeface.for_each_fixed_size_font([&](auto& font) {
                        m_sizes.append(font.presentation_size());
                    });
                } else {
                    m_sizes.append(8);
                    m_sizes.append(10);
                    m_sizes.append(12);
                    m_sizes.append(14);
                    m_sizes.append(16);
                    m_sizes.append(18);
                    m_sizes.append(20);
                    m_sizes.append(22);
                    m_sizes.append(24);
                    m_sizes.append(36);
                }
            }
        });
        quick_sort(m_sizes);
        Optional<size_t> index_of_old_size_in_new_list;
        if (m_size.has_value()) {
            index_of_old_size_in_new_list = m_sizes.find_first_index(m_size.value());
        }

        m_size_list_view->model()->update();
        m_size_list_view->set_cursor(m_size_list_view->model()->index(index_of_old_size_in_new_list.value_or(0)), GUI::AbstractView::SelectionUpdate::Set);
        update_font();
    };

    m_size_list_view->on_selection = [this](auto& index) {
        m_size = index.data().to_i32();
        update_font();
    };

    auto& ok_button = *widget.find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [this](auto) {
        done(ExecOK);
    };

    auto& cancel_button = *widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
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
        m_family = {};
        m_weight = {};
        m_size = {};
        m_weights.clear();
        m_sizes.clear();
        m_weight_list_view->model()->update();
        m_size_list_view->model()->update();
        return;
    }

    m_family = font->family();
    m_weight = font->weight();
    m_size = font->presentation_size();

    auto family_index = m_families.find_first_index(m_font->family());
    if (family_index.has_value())
        m_family_list_view->set_cursor(m_family_list_view->model()->index(family_index.value()), GUI::AbstractView::SelectionUpdate::Set);

    auto weight_index = m_weights.find_first_index(m_font->weight());
    if (weight_index.has_value()) {
        m_weight_list_view->set_cursor(m_weight_list_view->model()->index(weight_index.value()), GUI::AbstractView::SelectionUpdate::Set);
    }

    auto size_index = m_sizes.find_first_index(m_font->presentation_size());
    if (size_index.has_value())
        m_size_list_view->set_cursor(m_size_list_view->model()->index(size_index.value()), GUI::AbstractView::SelectionUpdate::Set);
}

void FontPicker::update_font()
{
    if (m_family.has_value() && m_size.has_value() && m_weight.has_value()) {
        m_font = Gfx::FontDatabase::the().get(m_family.value(), m_size.value(), m_weight.value());
        m_sample_text_label->set_font(m_font);
    }
}
}
