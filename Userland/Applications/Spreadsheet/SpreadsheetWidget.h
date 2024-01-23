/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SpreadsheetView.h"
#include "Workbook.h"
#include <LibGUI/Clipboard.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Widget.h>

namespace Spreadsheet {

class SpreadsheetWidget final
    : public GUI::Widget
    , public GUI::Clipboard::ClipboardClient {
    C_OBJECT(SpreadsheetWidget);

public:
    virtual ~SpreadsheetWidget() override = default;

    void save(ByteString const& filename, Core::File&);
    void load_file(ByteString const& filename, Core::File&);
    void import_sheets(ByteString const& filename, Core::File&);
    bool request_close();
    void add_sheet();
    void add_sheet(NonnullRefPtr<Sheet>&&);

    ByteString const& current_filename() const { return m_workbook->current_filename(); }
    SpreadsheetView* current_view() { return static_cast<SpreadsheetView*>(m_tab_widget->active_widget()); }
    Sheet* current_worksheet_if_available() { return current_view() ? current_view()->sheet_if_available() : nullptr; }
    void update_window_title();

    Workbook& workbook() { return *m_workbook; }
    Workbook const& workbook() const { return *m_workbook; }

    const GUI::ModelIndex* current_selection_cursor()
    {
        if (!current_view())
            return nullptr;

        return current_view()->cursor();
    }

    ErrorOr<void> initialize_menubar(GUI::Window&);

    void undo();
    void redo();
    void change_cell_static_color_format(Spreadsheet::FormatType);
    auto& undo_stack() { return m_undo_stack; }

private:
    // ^GUI::Widget
    virtual void resize_event(GUI::ResizeEvent&) override;

    // ^GUI::Clipboard::ClipboardClient
    virtual void clipboard_content_did_change(ByteString const& mime_type) override;

    explicit SpreadsheetWidget(GUI::Window& window, Vector<NonnullRefPtr<Sheet>>&& sheets = {}, bool should_add_sheet_if_empty = true);

    void setup_tabs(Vector<NonnullRefPtr<Sheet>> new_sheets);

    void try_generate_tip_for_input_expression(StringView source, size_t offset);

    RefPtr<GUI::Label> m_current_cell_label;
    RefPtr<GUI::TextEditor> m_cell_value_editor;
    RefPtr<GUI::Window> m_inline_documentation_window;
    RefPtr<GUI::Label> m_inline_documentation_label;
    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<GUI::Menu> m_tab_context_menu;
    RefPtr<SpreadsheetView> m_tab_context_menu_sheet_view;
    bool m_should_change_selected_cells { false };
    GUI::UndoStack m_undo_stack;

    OwnPtr<Workbook> m_workbook;

    void clipboard_action(bool is_cut);
    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_quit_action;

    RefPtr<GUI::Action> m_import_action;

    RefPtr<GUI::Action> m_cut_action;
    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_paste_action;
    RefPtr<GUI::Action> m_insert_emoji_action;
    RefPtr<GUI::Action> m_undo_action;
    RefPtr<GUI::Action> m_redo_action;
    RefPtr<GUI::Action> m_change_background_color_action;
    RefPtr<GUI::Action> m_change_foreground_color_action;

    RefPtr<GUI::Action> m_search_action;
    RefPtr<GUI::Action> m_functions_help_action;
    RefPtr<GUI::Action> m_about_action;

    RefPtr<GUI::Action> m_rename_action;
};

}
