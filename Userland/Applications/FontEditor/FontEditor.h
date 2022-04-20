/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "UndoSelection.h"
#include <LibGUI/ActionGroup.h>
#include <LibGUI/FilteringProxyModel.h>
#include <LibGUI/GlyphMapWidget.h>
#include <LibGUI/UndoStack.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/BitmapFont.h>

class GlyphEditorWidget;

class FontEditorWidget final
    : public GUI::Widget {
    C_OBJECT(FontEditorWidget)
public:
    virtual ~FontEditorWidget() override = default;

    bool open_file(String const&);
    bool save_file(String const&);
    bool request_close();
    void update_title();

    String const& path() { return m_path; }
    Gfx::BitmapFont const& edited_font() { return *m_edited_font; }
    void initialize(String const& path, RefPtr<Gfx::BitmapFont>&&);
    void initialize_menubar(GUI::Window&);

    bool is_showing_font_metadata() { return m_font_metadata; }
    void set_show_font_metadata(bool);

    bool is_showing_unicode_blocks() { return m_unicode_blocks; }
    void set_show_unicode_blocks(bool);

    Function<void()> on_initialize;

private:
    FontEditorWidget();

    virtual void drop_event(GUI::DropEvent&) override;

    void undo();
    void redo();
    void did_modify_font();
    void did_resize_glyph_editor();
    void update_statusbar();
    void update_preview();
    void set_scale(i32);
    void set_scale_and_save(i32);

    void copy_selected_glyphs();
    void cut_selected_glyphs();
    void paste_glyphs();
    void delete_selected_glyphs();

    void reset_selection_and_push_undo();

    RefPtr<Gfx::BitmapFont> m_edited_font;

    RefPtr<GUI::GlyphMapWidget> m_glyph_map_widget;
    RefPtr<GlyphEditorWidget> m_glyph_editor_widget;

    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;

    RefPtr<GUI::Action> m_cut_action;
    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_paste_action;
    RefPtr<GUI::Action> m_delete_action;

    RefPtr<GUI::Action> m_copy_text_action;
    RefPtr<GUI::Action> m_select_all_action;

    RefPtr<GUI::Action> m_undo_action;
    RefPtr<GUI::Action> m_redo_action;
    RefPtr<UndoSelection> m_undo_selection;
    OwnPtr<GUI::UndoStack> m_undo_stack;

    RefPtr<GUI::Action> m_go_to_glyph_action;
    RefPtr<GUI::Action> m_previous_glyph_action;
    RefPtr<GUI::Action> m_next_glyph_action;

    RefPtr<GUI::Action> m_open_preview_action;
    RefPtr<GUI::Action> m_show_metadata_action;
    RefPtr<GUI::Action> m_show_unicode_blocks_action;

    GUI::ActionGroup m_glyph_editor_scale_actions;
    RefPtr<GUI::Action> m_scale_five_action;
    RefPtr<GUI::Action> m_scale_ten_action;
    RefPtr<GUI::Action> m_scale_fifteen_action;

    GUI::ActionGroup m_glyph_tool_actions;
    RefPtr<GUI::Action> m_move_glyph_action;
    RefPtr<GUI::Action> m_paint_glyph_action;

    RefPtr<GUI::Action> m_flip_horizontal_action;
    RefPtr<GUI::Action> m_flip_vertical_action;
    RefPtr<GUI::Action> m_rotate_clockwise_action;
    RefPtr<GUI::Action> m_rotate_counterclockwise_action;

    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::Window> m_font_preview_window;
    RefPtr<GUI::Widget> m_left_column_container;
    RefPtr<GUI::Widget> m_glyph_editor_container;
    RefPtr<GUI::Widget> m_unicode_block_container;
    RefPtr<GUI::ComboBox> m_weight_combobox;
    RefPtr<GUI::ComboBox> m_slope_combobox;
    RefPtr<GUI::SpinBox> m_spacing_spinbox;
    RefPtr<GUI::SpinBox> m_baseline_spinbox;
    RefPtr<GUI::SpinBox> m_mean_line_spinbox;
    RefPtr<GUI::SpinBox> m_presentation_spinbox;
    RefPtr<GUI::SpinBox> m_glyph_editor_width_spinbox;
    RefPtr<GUI::CheckBox> m_glyph_editor_present_checkbox;
    RefPtr<GUI::TextBox> m_name_textbox;
    RefPtr<GUI::TextBox> m_family_textbox;
    RefPtr<GUI::TextBox> m_search_textbox;
    RefPtr<GUI::CheckBox> m_fixed_width_checkbox;
    RefPtr<GUI::GroupBox> m_font_metadata_groupbox;
    RefPtr<GUI::ListView> m_unicode_block_listview;
    RefPtr<GUI::Model> m_unicode_block_model;
    RefPtr<GUI::FilteringProxyModel> m_filter_model;
    RefPtr<GUI::Menu> m_context_menu;

    String m_path;
    Vector<String> m_font_weight_list;
    Vector<String> m_font_slope_list;
    Vector<String> m_unicode_block_list;
    bool m_font_metadata { true };
    bool m_unicode_blocks { true };
    Unicode::CodePointRange m_range { 0x0000, 0x10FFFF };
};
