/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Spreadsheet.h"
#include "SpreadsheetModel.h"
#include <LibGUI/AbstractTableView.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Widget.h>
#include <string.h>

namespace Spreadsheet {

class CellEditor final : public GUI::TextEditor {
    C_OBJECT(CellEditor);

public:
    virtual ~CellEditor() { }

    Function<void(GUI::KeyEvent&)> on_cursor_key_pressed;

private:
    CellEditor()
        : TextEditor(TextEditor::Type::SingleLine)
    {
    }

    static bool is_navigation(const GUI::KeyEvent& event)
    {
        if (event.modifiers() == KeyModifier::Mod_Shift && event.key() == KeyCode::Key_Tab)
            return true;

        if (event.modifiers())
            return false;

        switch (event.key()) {
        case KeyCode::Key_Tab:
        case KeyCode::Key_Left:
        case KeyCode::Key_Right:
        case KeyCode::Key_Up:
        case KeyCode::Key_Down:
        case KeyCode::Key_Return:
            return true;
        default:
            return false;
        }
    }

    virtual void keydown_event(GUI::KeyEvent& event) override
    {
        if (is_navigation(event))
            on_cursor_key_pressed(event);
        else
            TextEditor::keydown_event(event);
    }
};

class InfinitelyScrollableTableView : public GUI::TableView {
    C_OBJECT(InfinitelyScrollableTableView)
public:
    Function<void()> on_reaching_vertical_end;
    Function<void()> on_reaching_horizontal_end;

private:
    InfinitelyScrollableTableView()
        : m_horizontal_scroll_end_timer(Core::Timer::construct())
        , m_vertical_scroll_end_timer(Core::Timer::construct())
    {
    }
    virtual void did_scroll() override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;

    bool m_should_intercept_drag { false };
    bool m_has_committed_to_dragging { false };
    bool m_is_dragging_for_copy { false };
    GUI::ModelIndex m_starting_selection_index;
    RefPtr<Core::Timer> m_horizontal_scroll_end_timer;
    RefPtr<Core::Timer> m_vertical_scroll_end_timer;
};

class SpreadsheetView final : public GUI::Widget {
    C_OBJECT(SpreadsheetView);

public:
    ~SpreadsheetView();

    Sheet* sheet_if_available() { return m_sheet; }

    const GUI::ModelIndex* cursor() const
    {
        return &m_table_view->cursor_index();
    }

    Function<void(Vector<Position>&&)> on_selection_changed;
    Function<void()> on_selection_dropped;

    void move_cursor(GUI::AbstractView::CursorMovement);

private:
    virtual void hide_event(GUI::HideEvent&) override;
    virtual void show_event(GUI::ShowEvent&) override;

    void update_with_model();

    SpreadsheetView(Sheet&);

    class EditingDelegate final : public GUI::StringModelEditingDelegate {
    public:
        EditingDelegate(const Sheet& sheet)
            : m_sheet(sheet)
        {
        }
        virtual void set_value(GUI::Variant const&, GUI::ModelEditingDelegate::SelectionBehavior) override;

        virtual RefPtr<Widget> create_widget() override
        {
            auto textbox = CellEditor::construct();
            textbox->on_escape_pressed = [this] {
                rollback();
            };
            textbox->on_cursor_key_pressed = [this](auto& event) {
                commit();
                on_cursor_key_pressed(event);
            };
            textbox->on_focusout = [this] {
                on_cell_focusout(index(), value());
            };
            return textbox;
        }

        Function<void(GUI::KeyEvent&)> on_cursor_key_pressed;
        Function<void(const GUI::ModelIndex&, const GUI::Variant&)> on_cell_focusout;

    private:
        bool m_has_set_initial_value { false };
        const Sheet& m_sheet;
    };

    class TableCellPainter final : public GUI::TableCellPaintingDelegate {
    public:
        TableCellPainter(const GUI::TableView& view)
            : m_table_view(view)
        {
        }
        void paint(GUI::Painter&, const Gfx::IntRect&, const Gfx::Palette&, const GUI::ModelIndex&) override;

    private:
        const GUI::TableView& m_table_view;
    };

    NonnullRefPtr<Sheet> m_sheet;
    NonnullRefPtr<SheetModel> m_sheet_model;
    RefPtr<InfinitelyScrollableTableView> m_table_view;
    RefPtr<GUI::Menu> m_cell_range_context_menu;
};

}
