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
#include "UI_FontEditorBottom.h"
#include <AK/StringBuilder.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GTextBox.h>
#include <stdlib.h>

FontEditorWidget::FontEditorWidget(const String& path, RefPtr<Gfx::Font>&& edited_font, GUI::Widget* parent)
    : GUI::Widget(parent)
    , m_edited_font(move(edited_font))
{
    set_fill_with_background_color(true);

    if (path.is_empty())
        m_path = "/tmp/saved.font";
    else
        m_path = path;

    m_glyph_map_widget = GlyphMapWidget::construct(*m_edited_font, this);
    m_glyph_map_widget->move_to({ 90, 5 });

    m_glyph_editor_widget = GlyphEditorWidget::construct(*m_edited_font, this);
    m_glyph_editor_widget->move_to({ 5, 5 });

    m_ui = make<UI_FontEditorBottom>();
    add_child(*m_ui->main_widget);
    m_ui->main_widget->set_relative_rect(5, 110, 380, 240);

    m_ui->name_textbox->set_text(m_edited_font->name());
    m_ui->name_textbox->on_change = [this] {
        m_edited_font->set_name(m_ui->name_textbox->text());
    };

    m_ui->fixed_width_checkbox->set_text("Fixed width");
    m_ui->fixed_width_checkbox->set_checked(m_edited_font->is_fixed_width());

    m_ui->spacing_spinbox->set_value(m_edited_font->glyph_spacing());

    m_ui->path_textbox->set_text(m_path);
    m_ui->path_textbox->on_change = [this] {
        m_path = m_ui->path_textbox->text();
    };

    m_ui->save_button->set_text("Save");
    m_ui->save_button->on_click = [this](GUI::Button&) {
        dbgprintf("write to file: '%s'\n", m_path.characters());
        m_edited_font->write_to_file(m_path);
    };

    m_ui->quit_button->set_text("Quit");
    m_ui->quit_button->on_click = [](auto&) {
        exit(0);
    };

    m_ui->info_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_ui->demo_label_1->set_font(m_edited_font);
    m_ui->demo_label_1->set_text("quick fox jumps nightly above wizard.");

    m_ui->demo_label_2->set_font(m_edited_font);
    m_ui->demo_label_2->set_text("QUICK FOX JUMPS NIGHTLY ABOVE WIZARD!");

    auto update_demo = [this] {
        m_ui->demo_label_1->update();
        m_ui->demo_label_2->update();
    };

    m_glyph_editor_widget->on_glyph_altered = [this, update_demo](u8 glyph) {
        m_glyph_map_widget->update_glyph(glyph);
        update_demo();
    };

    m_glyph_map_widget->on_glyph_selected = [this](u8 glyph) {
        m_glyph_editor_widget->set_glyph(glyph);
        m_ui->width_spinbox->set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
        StringBuilder builder;
        builder.appendf("0x%b (", glyph);
        if (glyph < 128) {
            builder.append(glyph);
        } else {
            builder.append(128 | 64 | (glyph / 64));
            builder.append(128 | (glyph % 64));
        }
        builder.append(')');
        m_ui->info_label->set_text(builder.to_string());
    };

    m_ui->fixed_width_checkbox->on_checked = [this, update_demo](bool checked) {
        m_edited_font->set_fixed_width(checked);
        m_ui->width_spinbox->set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
        m_glyph_editor_widget->update();
        update_demo();
    };

    m_ui->width_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), value);
        m_glyph_editor_widget->update();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
        update_demo();
    };

    m_ui->spacing_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_glyph_spacing(value);
        update_demo();
    };

    m_glyph_map_widget->set_selected_glyph('A');
}

FontEditorWidget::~FontEditorWidget()
{
}
