/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibGUI/Button.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/FontPickerDialogWidget.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/FontDatabase.h>

namespace GUI {

FontPicker::FontPicker(Window* parent_window, Gfx::Font const* current_font, bool fixed_width_only)
    : Dialog(parent_window)
    , m_fixed_width_only(fixed_width_only)
{
    set_title("Font Picker");
    resize(430, 280);
    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-font-editor.png"sv).release_value_but_fixme_should_propagate_errors());

    auto widget = FontPickerDialogWidget::try_create().release_value_but_fixme_should_propagate_errors();
    set_main_widget(widget);

    m_family_list_view = *widget->find_descendant_of_type_named<ListView>("family_list_view");
    m_family_list_view->set_model(ItemListModel<FlyString>::create(m_families));
    m_family_list_view->horizontal_scrollbar().set_visible(false);

    m_variant_list_view = *widget->find_descendant_of_type_named<ListView>("variant_list_view");
    m_variant_list_view->set_model(ItemListModel<FlyString>::create(m_variants));
    m_variant_list_view->horizontal_scrollbar().set_visible(false);

    m_size_spin_box = *widget->find_descendant_of_type_named<SpinBox>("size_spin_box");
    m_size_spin_box->set_range(1, 255);

    m_size_list_view = *widget->find_descendant_of_type_named<ListView>("size_list_view");
    m_size_list_view->set_model(ItemListModel<int>::create(m_sizes));
    m_size_list_view->horizontal_scrollbar().set_visible(false);

    m_sample_text_label = *widget->find_descendant_of_type_named<Label>("sample_text_label");

    m_families.clear();
    Gfx::FontDatabase::the().for_each_typeface([&](auto& typeface) {
        if (m_fixed_width_only && !typeface.is_fixed_width())
            return;
        if (!m_families.contains_slow(typeface.family()))
            m_families.append(typeface.family());
    });
    quick_sort(m_families);

    m_family_list_view->on_selection_change = [this] {
        auto const& index = m_family_list_view->selection().first();
        m_family = MUST(String::from_byte_string(index.data().to_byte_string()));
        m_variants.clear();
        Gfx::FontDatabase::the().for_each_typeface([&](auto& typeface) {
            if (m_fixed_width_only && !typeface.is_fixed_width())
                return;
            if (typeface.family() == m_family.value() && !m_variants.contains_slow(typeface.variant()))
                m_variants.append(typeface.variant());
        });
        quick_sort(m_variants);
        Optional<size_t> index_of_old_variant_in_new_list;
        if (m_variant.has_value())
            index_of_old_variant_in_new_list = m_variants.find_first_index(m_variant.value());

        m_variant_list_view->model()->invalidate();
        m_variant_list_view->set_cursor(m_variant_list_view->model()->index(index_of_old_variant_in_new_list.value_or(0)), GUI::AbstractView::SelectionUpdate::Set);
        update_font();
    };

    m_variant_list_view->on_selection_change = [this] {
        auto const& index = m_variant_list_view->selection().first();
        bool font_is_fixed_size = false;
        m_variant = MUST(String::from_byte_string(index.data().to_byte_string()));
        m_sizes.clear();
        Gfx::FontDatabase::the().for_each_typeface([&](auto& typeface) {
            if (m_fixed_width_only && !typeface.is_fixed_width())
                return;
            if (typeface.family() == m_family.value() && typeface.variant() == m_variant.value()) {
                font_is_fixed_size = typeface.is_fixed_size();
                if (font_is_fixed_size) {
                    m_size_spin_box->set_visible(false);

                    typeface.for_each_fixed_size_font([&](auto& font) {
                        m_sizes.append(font.presentation_size());
                    });
                } else {
                    m_size_spin_box->set_visible(true);

                    m_sizes.append(8);
                    m_sizes.append(9);
                    m_sizes.append(10);
                    m_sizes.append(11);
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
        m_size_list_view->model()->invalidate();
        m_size_list_view->set_selection_mode(GUI::AbstractView::SelectionMode::SingleSelection);

        if (m_size.has_value()) {
            Optional<size_t> index_of_old_size_in_new_list = m_sizes.find_first_index(m_size.value());
            if (index_of_old_size_in_new_list.has_value()) {
                m_size_list_view->set_cursor(m_size_list_view->model()->index(index_of_old_size_in_new_list.value()), GUI::AbstractView::SelectionUpdate::Set);
            } else {
                if (font_is_fixed_size) {
                    m_size_list_view->set_cursor(m_size_list_view->model()->index(0), GUI::AbstractView::SelectionUpdate::Set);
                } else {
                    m_size_list_view->set_selection_mode(GUI::AbstractView::SelectionMode::NoSelection);
                    m_size_spin_box->set_value(m_size.value());
                }
            }
        } else {
            m_size_list_view->set_cursor(m_size_list_view->model()->index(0), GUI::AbstractView::SelectionUpdate::Set);
        }
        update_font();
    };

    m_size_list_view->on_selection_change = [this] {
        auto const& index = m_size_list_view->selection().first();
        auto size = index.data().to_i32();
        Optional<size_t> index_of_new_size_in_list = m_sizes.find_first_index(size);
        if (index_of_new_size_in_list.has_value()) {
            m_size_list_view->set_selection_mode(GUI::AbstractView::SelectionMode::SingleSelection);
            m_size = size;
            m_size_spin_box->set_value(m_size.value());
        }
        update_font();
    };

    m_size_spin_box->on_change = [this](int value) {
        m_size = value;

        Optional<size_t> index_of_new_size_in_list = m_sizes.find_first_index(m_size.value());

        if (index_of_new_size_in_list.has_value()) {
            m_size_list_view->set_selection_mode(GUI::AbstractView::SelectionMode::SingleSelection);
            m_size_list_view->set_cursor(m_size_list_view->model()->index(index_of_new_size_in_list.value()), GUI::AbstractView::SelectionUpdate::Set);
        } else {
            m_size_list_view->set_selection_mode(GUI::AbstractView::SelectionMode::NoSelection);
        }

        update_font();
    };

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [this](auto) {
        done(ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    set_font(current_font);
}

void FontPicker::set_font(Gfx::Font const* font)
{
    if (m_font == font)
        return;
    m_font = font;
    m_sample_text_label->set_font(m_font);

    if (!m_font) {
        m_family = {};
        m_variant = {};
        m_size = {};
        m_variants.clear();
        m_sizes.clear();
        m_variant_list_view->model()->invalidate();
        m_size_list_view->model()->invalidate();
        return;
    }

    m_family = font->family();
    m_variant = font->variant();
    m_size = font->presentation_size();

    auto family_index = m_families.find_first_index(m_font->family());
    if (family_index.has_value())
        m_family_list_view->set_cursor(m_family_list_view->model()->index(family_index.value()), GUI::AbstractView::SelectionUpdate::Set);

    auto variant_index = m_variants.find_first_index(m_font->variant());
    if (variant_index.has_value())
        m_variant_list_view->set_cursor(m_variant_list_view->model()->index(variant_index.value()), GUI::AbstractView::SelectionUpdate::Set);

    auto size_index = m_sizes.find_first_index(m_font->presentation_size());
    if (size_index.has_value())
        m_size_list_view->set_cursor(m_size_list_view->model()->index(size_index.value()), GUI::AbstractView::SelectionUpdate::Set);
}

void FontPicker::update_font()
{
    if (m_family.has_value() && m_size.has_value() && m_variant.has_value()) {
        m_font = Gfx::FontDatabase::the().get(m_family.value(), m_variant.value(), m_size.value());
        m_sample_text_label->set_font(m_font);
    }
}
}
