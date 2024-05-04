/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "HexEditor.h"
#include "ValueInspectorModel.h"
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/DynamicWidgetContainer.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

namespace HexEditor {
class HexEditorWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(HexEditorWidget)
public:
    virtual ~HexEditorWidget() override = default;
    void open_file(ByteString const& filename, NonnullOwnPtr<Core::File>);
    void open_annotations_file(StringView filename);
    ErrorOr<void> initialize_menubar(GUI::Window&);
    bool request_close();

    static ErrorOr<NonnullRefPtr<HexEditorWidget>> create();

protected:
    static ErrorOr<NonnullRefPtr<HexEditorWidget>> try_create();

private:
    ErrorOr<void> setup();
    HexEditorWidget() = default;
    void set_path(StringView);
    void update_title();
    void update_side_panel_visibility();
    void set_annotations_visible(bool visible);
    void initialize_annotations_model();
    void set_search_results_visible(bool visible);
    void set_value_inspector_visible(bool visible);
    void update_inspector_values(size_t position);
    void set_inspector_little_endian(bool little_endian, bool force = false);

    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    RefPtr<HexEditor> m_editor;
    ByteString m_path;
    ByteString m_name;
    ByteString m_extension;
    ByteString m_annotations_path;

    int m_goto_history { 0 };
    String m_search_text;
    ByteBuffer m_search_buffer;
    int last_found_index() const { return m_last_found_index == -1 ? 0 : m_last_found_index; }
    int m_last_found_index { -1 };

    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_open_annotations_action;
    RefPtr<GUI::Action> m_save_annotations_action;
    RefPtr<GUI::Action> m_save_annotations_as_action;

    RefPtr<GUI::Action> m_undo_action;
    RefPtr<GUI::Action> m_redo_action;

    RefPtr<GUI::Action> m_find_action;
    RefPtr<GUI::Action> m_goto_offset_action;
    RefPtr<GUI::Action> m_layout_toolbar_action;
    RefPtr<GUI::Action> m_layout_annotations_action;
    RefPtr<GUI::Action> m_layout_search_results_action;
    RefPtr<GUI::Action> m_layout_value_inspector_action;

    RefPtr<GUI::Action> m_copy_hex_action;
    RefPtr<GUI::Action> m_copy_text_action;
    RefPtr<GUI::Action> m_copy_as_c_code_action;
    RefPtr<GUI::Action> m_fill_selection_action;

    RefPtr<GUI::Action> m_show_offsets_column_action;
    GUI::ActionGroup m_offset_format_actions;

    GUI::ActionGroup m_bytes_per_row_actions;

    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::Toolbar> m_toolbar;
    RefPtr<GUI::ToolbarContainer> m_toolbar_container;
    RefPtr<GUI::TableView> m_search_results;
    RefPtr<GUI::Widget> m_search_results_container;
    RefPtr<GUI::DynamicWidgetContainer> m_side_panel_container;

    RefPtr<GUI::Widget> m_value_inspector_container;
    RefPtr<GUI::TableView> m_value_inspector;
    RefPtr<GUI::ComboBox> m_value_inspector_endianness;
    Vector<ByteString> m_endianness_options;

    RefPtr<GUI::Widget> m_annotations_container;
    RefPtr<GUI::TableView> m_annotations;
    RefPtr<GUI::SortingProxyModel> m_annotations_sorting_model;
    RefPtr<GUI::Menu> m_annotations_context_menu;
    RefPtr<GUI::Action> m_edit_annotation_action;
    RefPtr<GUI::Action> m_delete_annotation_action;

    bool m_value_inspector_little_endian { true };
    bool m_selecting_from_inspector { false };
};

}
