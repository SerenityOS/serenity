/*
 * Copyright (c) 2020, the SerenityOS developers.
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

    const GUI::ModelIndex* current_selection_cursor() const
    {
        if (!m_selected_view)
            return nullptr;

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
