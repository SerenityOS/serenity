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

#include "FontEditor.h"
#include "GlyphEditorWidget.h"
#include "GlyphMapWidget.h"
#include <AK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <stdlib.h>

FontEditorWidget::FontEditorWidget(const String& path, RefPtr<Gfx::Font>&& edited_font)
    : m_edited_font(move(edited_font))
    , m_path(path)
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    // Top
    auto& main_container = add<GUI::Widget>();
    main_container.set_layout<GUI::HorizontalBoxLayout>();
    main_container.layout()->set_margins({ 4, 4, 4, 4 });
    main_container.set_background_role(Gfx::ColorRole::SyntaxKeyword);
    main_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

    // Top-Left Glyph Editor and info
    auto& editor_container = main_container.add<GUI::Widget>();
    editor_container.set_layout<GUI::VerticalBoxLayout>();
    editor_container.layout()->set_margins({ 4, 4, 4, 4 });
    editor_container.set_background_role(Gfx::ColorRole::SyntaxKeyword);
    editor_container.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);

    m_glyph_editor_widget = editor_container.add<GlyphEditorWidget>(*m_edited_font);
    m_glyph_editor_widget->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_glyph_editor_widget->set_preferred_size(m_glyph_editor_widget->preferred_width(), m_glyph_editor_widget->preferred_height());

    editor_container.set_preferred_size(m_glyph_editor_widget->preferred_width(), 0);

    auto& glyph_width_label = editor_container.add<GUI::Label>();
    glyph_width_label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    glyph_width_label.set_preferred_size(0, 22);
    glyph_width_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    glyph_width_label.set_text("Glyph width:");

    auto& glyph_width_spinbox = editor_container.add<GUI::SpinBox>();
    glyph_width_spinbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    glyph_width_spinbox.set_preferred_size(0, 22);
    glyph_width_spinbox.set_min(0);
    glyph_width_spinbox.set_max(32);
    glyph_width_spinbox.set_value(0);
    glyph_width_spinbox.set_enabled(!m_edited_font->is_fixed_width());

    auto& info_label = editor_container.add<GUI::Label>();
    info_label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    info_label.set_preferred_size(0, 22);
    info_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    info_label.set_text("info_label");

    /// Top-Right glyph map and font meta data

    auto& map_and_test_container = main_container.add<GUI::Widget>();
    map_and_test_container.set_layout<GUI::VerticalBoxLayout>();
    map_and_test_container.layout()->set_margins({ 4, 4, 4, 4 });
    map_and_test_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

    m_glyph_map_widget = map_and_test_container.add<GlyphMapWidget>(*m_edited_font);
    m_glyph_map_widget->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_glyph_map_widget->set_preferred_size(m_glyph_map_widget->preferred_width(), m_glyph_map_widget->preferred_height());

    auto& font_mtest_group_box = map_and_test_container.add<GUI::GroupBox>();
    font_mtest_group_box.set_layout<GUI::VerticalBoxLayout>();
    font_mtest_group_box.layout()->set_margins({ 5, 15, 5, 5 });
    font_mtest_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    font_mtest_group_box.set_preferred_size(0, 2 * m_edited_font->glyph_height() + 50);
    font_mtest_group_box.set_title("Test");

    auto& demo_label_1 = font_mtest_group_box.add<GUI::Label>();
    demo_label_1.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    demo_label_1.set_font(m_edited_font);
    demo_label_1.set_text("quick fox jumps nightly above wizard.");

    auto& demo_label_2 = font_mtest_group_box.add<GUI::Label>();
    demo_label_2.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    demo_label_2.set_font(m_edited_font);
    demo_label_2.set_text("QUICK FOX JUMPS NIGHTLY ABOVE WIZARD!");

    auto& font_metadata_group_box = map_and_test_container.add<GUI::GroupBox>();
    font_metadata_group_box.set_layout<GUI::VerticalBoxLayout>();
    font_metadata_group_box.layout()->set_margins({ 5, 15, 5, 5 });
    font_metadata_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    font_metadata_group_box.set_preferred_size(0, 195);
    font_metadata_group_box.set_title("Font metadata");

    //// Name Row
    auto& namecontainer = font_metadata_group_box.add<GUI::Widget>();
    namecontainer.set_layout<GUI::HorizontalBoxLayout>();
    namecontainer.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    namecontainer.set_preferred_size(0, 22);

    auto& name_label = namecontainer.add<GUI::Label>();
    name_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    name_label.set_preferred_size(100, 0);
    name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    name_label.set_text("Name:");

    auto& name_textbox = namecontainer.add<GUI::TextBox>();
    name_textbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    name_textbox.set_text(m_edited_font->name());
    name_textbox.on_change = [&] {
        m_edited_font->set_name(name_textbox.text());
    };

    //// Glyph spacing Row
    auto& glyph_spacing_container = font_metadata_group_box.add<GUI::Widget>();
    glyph_spacing_container.set_layout<GUI::HorizontalBoxLayout>();
    glyph_spacing_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    glyph_spacing_container.set_preferred_size(0, 22);

    auto& glyph_spacing = glyph_spacing_container.add<GUI::Label>();
    glyph_spacing.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    glyph_spacing.set_preferred_size(100, 0);
    glyph_spacing.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    glyph_spacing.set_text("Glyph spacing:");

    auto& spacing_spinbox = glyph_spacing_container.add<GUI::SpinBox>();
    spacing_spinbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    spacing_spinbox.set_min(0);
    spacing_spinbox.set_max(255);
    spacing_spinbox.set_value(m_edited_font->glyph_spacing());

    //// Glyph Height Row
    auto& glyph_height_container = font_metadata_group_box.add<GUI::Widget>();
    glyph_height_container.set_layout<GUI::HorizontalBoxLayout>();
    glyph_height_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    glyph_height_container.set_preferred_size(0, 22);

    auto& glyph_height = glyph_height_container.add<GUI::Label>();
    glyph_height.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    glyph_height.set_preferred_size(100, 0);
    glyph_height.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    glyph_height.set_text("Glyph height:");

    auto& glyph_height_spinbox = glyph_height_container.add<GUI::SpinBox>();
    glyph_height_spinbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    glyph_height_spinbox.set_min(0);
    glyph_height_spinbox.set_max(255);
    glyph_height_spinbox.set_value(m_edited_font->glyph_height());
    glyph_height_spinbox.set_enabled(false);

    //// Glyph width Row
    auto& glyph_weight_container = font_metadata_group_box.add<GUI::Widget>();
    glyph_weight_container.set_layout<GUI::HorizontalBoxLayout>();
    glyph_weight_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    glyph_weight_container.set_preferred_size(0, 22);

    auto& glyph_header_width_label = glyph_weight_container.add<GUI::Label>();
    glyph_header_width_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    glyph_header_width_label.set_preferred_size(100, 0);
    glyph_header_width_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    glyph_header_width_label.set_text("Glyph width:");

    auto& glyph_header_width_spinbox = glyph_weight_container.add<GUI::SpinBox>();
    glyph_header_width_spinbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    glyph_header_width_spinbox.set_min(0);
    glyph_header_width_spinbox.set_max(255);
    glyph_header_width_spinbox.set_value(m_edited_font->glyph_fixed_width());
    glyph_header_width_spinbox.set_enabled(false);

    //// Baseline Row
    auto& baseline_container = font_metadata_group_box.add<GUI::Widget>();
    baseline_container.set_layout<GUI::HorizontalBoxLayout>();
    baseline_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    baseline_container.set_preferred_size(0, 22);

    auto& baseline_label = baseline_container.add<GUI::Label>();
    baseline_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    baseline_label.set_preferred_size(100, 0);
    baseline_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    baseline_label.set_text("Baseline:");

    auto& baseline_spinbox = baseline_container.add<GUI::SpinBox>();
    baseline_spinbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    baseline_spinbox.set_preferred_size(100, 0);
    baseline_spinbox.set_min(0);
    baseline_spinbox.set_max(m_edited_font->glyph_height() - 1);
    baseline_spinbox.set_value(m_edited_font->baseline());

    //// Mean line Row
    auto& mean_line_container = font_metadata_group_box.add<GUI::Widget>();
    mean_line_container.set_layout<GUI::HorizontalBoxLayout>();
    mean_line_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    mean_line_container.set_preferred_size(0, 22);

    auto& mean_line_label = mean_line_container.add<GUI::Label>();
    mean_line_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    mean_line_label.set_preferred_size(100, 0);
    mean_line_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    mean_line_label.set_text("Mean Line:");

    auto& mean_line_spinbox = mean_line_container.add<GUI::SpinBox>();
    mean_line_spinbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    mean_line_spinbox.set_preferred_size(100, 0);
    mean_line_spinbox.set_min(0);
    mean_line_spinbox.set_max(m_edited_font->glyph_height() - 1);
    mean_line_spinbox.set_value(m_edited_font->mean_line());

    //// Fixed checkbox Row
    auto& fixed_width_checkbox = font_metadata_group_box.add<GUI::CheckBox>();
    fixed_width_checkbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    fixed_width_checkbox.set_preferred_size(0, 22);
    fixed_width_checkbox.set_text("Fixed width");
    fixed_width_checkbox.set_checked(m_edited_font->is_fixed_width());

    // Bottom
    auto& bottom_container = add<GUI::Widget>();
    bottom_container.set_layout<GUI::HorizontalBoxLayout>();
    bottom_container.layout()->set_margins({ 8, 0, 8, 8 });
    bottom_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    bottom_container.set_preferred_size(0, 32);

    bottom_container.layout()->add_spacer();

    auto& save_button = bottom_container.add<GUI::Button>();
    save_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    save_button.set_preferred_size(80, 0);
    save_button.set_text("Save");
    save_button.on_click = [this](auto) { save_as(m_path); };

    auto& quit_button = bottom_container.add<GUI::Button>();
    quit_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    quit_button.set_preferred_size(80, 0);
    quit_button.set_text("Quit");
    quit_button.on_click = [](auto) {
        exit(0);
    };

    // Event hanglers
    auto update_demo = [&] {
        demo_label_1.update();
        demo_label_2.update();
    };

    auto calculate_prefed_sizes = [&] {
        int right_site_width = m_edited_font->width("QUICK FOX JUMPS NIGHTLY ABOVE WIZARD!") + 20;
        right_site_width = max(right_site_width, m_glyph_map_widget->preferred_width());

        m_preferred_width = m_glyph_editor_widget->width() + right_site_width + 20;
        m_preferred_height = m_glyph_map_widget->relative_rect().height() + 2 * m_edited_font->glyph_height() + 300;
    };

    m_glyph_editor_widget->on_glyph_altered = [this, update_demo](u8 glyph) {
        m_glyph_map_widget->update_glyph(glyph);
        update_demo();
    };

    m_glyph_map_widget->on_glyph_selected = [&](size_t glyph) {
        m_glyph_editor_widget->set_glyph(glyph);
        glyph_width_spinbox.set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
        StringBuilder builder;
        builder.appendf("0x%b (", glyph);
        if (glyph < 128) {
            builder.append(glyph);
        } else {
            builder.append(128 | 64 | (glyph / 64));
            builder.append(128 | (glyph % 64));
        }
        builder.append(')');
        info_label.set_text(builder.to_string());
    };

    fixed_width_checkbox.on_checked = [&, update_demo](bool checked) {
        m_edited_font->set_fixed_width(checked);
        glyph_width_spinbox.set_enabled(!m_edited_font->is_fixed_width());
        glyph_width_spinbox.set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
        m_glyph_editor_widget->update();
        update_demo();
    };

    glyph_width_spinbox.on_change = [this, update_demo](int value) {
        m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), value);
        m_glyph_editor_widget->update();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
        update_demo();
    };

    spacing_spinbox.on_change = [this, update_demo](int value) {
        m_edited_font->set_glyph_spacing(value);
        update_demo();
    };

    baseline_spinbox.on_change = [this, update_demo](int value) {
        m_edited_font->set_baseline(value);
        m_glyph_editor_widget->update();
        update_demo();
    };

    mean_line_spinbox.on_change = [this, update_demo](int value) {
        m_edited_font->set_mean_line(value);
        m_glyph_editor_widget->update();
        update_demo();
    };

    // init widget
    calculate_prefed_sizes();
    m_glyph_map_widget->set_selected_glyph('A');
}

FontEditorWidget::~FontEditorWidget()
{
}

bool FontEditorWidget::save_as(const String& path)
{
    auto ret_val = m_edited_font->write_to_file(path);
    if (!ret_val) {
        GUI::MessageBox::show(window(), "The font file could not be saved.", "Save failed", GUI::MessageBox::Type::Error);
        return false;
    }
    m_path = path;
    return true;
}
