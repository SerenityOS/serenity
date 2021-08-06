/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SpreadsheetView.h"
#include "Workbook.h"
#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/Widget.h>

namespace Spreadsheet {

class SpreadsheetWidget final : public GUI::Widget {
    C_OBJECT(SpreadsheetWidget);

public:
    ~SpreadsheetWidget();

    void save(const StringView& filename);
    void load(const StringView& filename);
    bool request_close();
    void add_sheet();
    void add_sheet(NonnullRefPtr<Sheet>&&);

    const String& current_filename() const { return m_workbook->current_filename(); }
    Sheet* current_worksheet_if_available() { return m_selected_view ? m_selected_view->sheet_if_available() : nullptr; }
    void set_filename(const String& filename);

    Workbook& workbook() { return *m_workbook; }
    const Workbook& workbook() const { return *m_workbook; }

    GUI::ModelIndex current_selection_cursor() const
    {
        if (!m_selected_view)
            return {};

        return m_selected_view->cursor();
    }

private:
    virtual void resize_event(GUI::ResizeEvent&) override;

    explicit SpreadsheetWidget(NonnullRefPtrVector<Sheet>&& sheets = {}, bool should_add_sheet_if_empty = true);

    void setup_tabs(NonnullRefPtrVector<Sheet> new_sheets);

    void try_generate_tip_for_input_expression(StringView source, size_t offset);

    SpreadsheetView* m_selected_view { nullptr };
    RefPtr<GUI::Label> m_current_cell_label;
    RefPtr<GUI::TextEditor> m_cell_value_editor;
    RefPtr<GUI::Window> m_inline_documentation_window;
    RefPtr<GUI::Label> m_inline_documentation_label;
    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<GUI::Menu> m_tab_context_menu;
    RefPtr<SpreadsheetView> m_tab_context_menu_sheet_view;
    bool m_should_change_selected_cells { false };

    OwnPtr<Workbook> m_workbook;
};

}
