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

#include "SpreadsheetView.h"
#include "CellTypeDialog.h"
#include "SpreadsheetModel.h"
#include <AK/ScopeGuard.h>
#include <AK/URL.h>
#include <LibCore/MimeData.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/TableView.h>
#include <LibGfx/Palette.h>

namespace Spreadsheet {

SpreadsheetView::~SpreadsheetView()
{
}

void SpreadsheetView::EditingDelegate::set_value(const GUI::Variant& value)
{
    if (value.as_string().is_null()) {
        StringModelEditingDelegate::set_value("");
        commit();
        return;
    }

    if (m_has_set_initial_value)
        return StringModelEditingDelegate::set_value(value);

    m_has_set_initial_value = true;
    const auto option = m_sheet.at({ m_sheet.column(index().column()), (size_t)index().row() });
    if (option)
        return StringModelEditingDelegate::set_value(option->source());

    StringModelEditingDelegate::set_value("");
}

void InfinitelyScrollableTableView::did_scroll()
{
    TableView::did_scroll();
    auto& vscrollbar = vertical_scrollbar();
    auto& hscrollbar = horizontal_scrollbar();
    if (vscrollbar.is_visible() && vscrollbar.value() == vscrollbar.max()) {
        if (on_reaching_vertical_end)
            on_reaching_vertical_end();
    }
    if (hscrollbar.is_visible() && hscrollbar.value() == hscrollbar.max()) {
        if (on_reaching_horizontal_end)
            on_reaching_horizontal_end();
    }
}

void InfinitelyScrollableTableView::mousemove_event(GUI::MouseEvent& event)
{
    if (auto model = this->model()) {
        auto index = index_at_event_position(event.position());
        if (!index.is_valid())
            return TableView::mousemove_event(event);

        auto& sheet = static_cast<SheetModel&>(*model).sheet();
        sheet.disable_updates();
        ScopeGuard sheet_update_enabler { [&] { sheet.enable_updates(); } };

        auto holding_left_button = !!(event.buttons() & GUI::MouseButton::Left);
        auto rect = content_rect(index);
        auto distance = rect.center().absolute_relative_distance_to(event.position());
        if (distance.x() >= rect.width() / 2 - 5 && distance.y() >= rect.height() / 2 - 5) {
            set_override_cursor(Gfx::StandardCursor::Crosshair);
            m_should_intercept_drag = false;
            if (holding_left_button) {
                m_has_committed_to_dragging = true;
                // Force a drag to happen by moving the mousedown position to the center of the cell.
                m_left_mousedown_position = rect.center();
            }
        } else if (!m_should_intercept_drag) {
            set_override_cursor(Gfx::StandardCursor::Arrow);
            if (!holding_left_button) {
                m_starting_selection_index = index;
            } else {
                m_should_intercept_drag = true;
                m_might_drag = false;
            }
        }

        if (holding_left_button && m_should_intercept_drag && !m_has_committed_to_dragging) {
            if (!m_starting_selection_index.is_valid())
                m_starting_selection_index = index;

            Vector<GUI::ModelIndex> new_selection;
            for (auto i = min(m_starting_selection_index.row(), index.row()), imax = max(m_starting_selection_index.row(), index.row()); i <= imax; ++i) {
                for (auto j = min(m_starting_selection_index.column(), index.column()), jmax = max(m_starting_selection_index.column(), index.column()); j <= jmax; ++j) {
                    auto index = model->index(i, j);
                    if (index.is_valid())
                        new_selection.append(move(index));
                }
            }

            if (!event.ctrl())
                selection().clear();
            selection().add_all(new_selection);
        }
    }

    TableView::mousemove_event(event);
}

void InfinitelyScrollableTableView::mouseup_event(GUI::MouseEvent& event)
{
    m_should_intercept_drag = false;
    m_has_committed_to_dragging = false;
    TableView::mouseup_event(event);
}

void SpreadsheetView::update_with_model()
{
    m_table_view->model()->update();
    m_table_view->update();
}

SpreadsheetView::SpreadsheetView(Sheet& sheet)
    : m_sheet(sheet)
{
    set_layout<GUI::VerticalBoxLayout>().set_margins({ 2, 2, 2, 2 });
    m_table_view = add<InfinitelyScrollableTableView>();
    m_table_view->set_grid_style(GUI::TableView::GridStyle::Both);
    m_table_view->set_selection_behavior(GUI::AbstractView::SelectionBehavior::SelectItems);
    m_table_view->set_edit_triggers(GUI::AbstractView::EditTrigger::EditKeyPressed | GUI::AbstractView::AnyKeyPressed | GUI::AbstractView::DoubleClicked);
    m_table_view->set_tab_key_navigation_enabled(true);
    m_table_view->row_header().set_visible(true);
    m_table_view->set_model(SheetModel::create(*m_sheet));
    m_table_view->on_reaching_vertical_end = [&]() {
        for (size_t i = 0; i < 100; ++i) {
            auto index = m_sheet->add_row();
            m_table_view->set_column_painting_delegate(index, make<TableCellPainter>(*m_table_view));
        };
        update_with_model();
    };
    m_table_view->on_reaching_horizontal_end = [&]() {
        for (size_t i = 0; i < 10; ++i) {
            m_sheet->add_column();
            auto last_column_index = m_sheet->column_count() - 1;
            m_table_view->set_column_width(last_column_index, 50);
            m_table_view->set_column_header_alignment(last_column_index, Gfx::TextAlignment::Center);
        }
        update_with_model();
    };

    set_focus_proxy(m_table_view);

    // FIXME: This is dumb.
    for (size_t i = 0; i < m_sheet->column_count(); ++i) {
        m_table_view->set_column_painting_delegate(i, make<TableCellPainter>(*m_table_view));
        m_table_view->set_column_width(i, 50);
        m_table_view->set_column_header_alignment(i, Gfx::TextAlignment::Center);
    }

    m_table_view->set_alternating_row_colors(false);
    m_table_view->set_highlight_selected_rows(false);
    m_table_view->set_editable(true);
    m_table_view->aid_create_editing_delegate = [this](auto&) {
        auto delegate = make<EditingDelegate>(*m_sheet);
        delegate->on_cursor_key_pressed = [this](auto& event) {
            m_table_view->stop_editing();
            m_table_view->event(event);
        };
        return delegate;
    };

    m_table_view->on_selection_change = [&] {
        m_sheet->selected_cells().clear();
        for (auto& index : m_table_view->selection().indexes()) {
            Position position { m_sheet->column(index.column()), (size_t)index.row() };
            m_sheet->selected_cells().set(position);
        }

        if (m_table_view->selection().is_empty() && on_selection_dropped)
            return on_selection_dropped();

        Vector<Position> selected_positions;
        selected_positions.ensure_capacity(m_table_view->selection().size());
        for (auto& selection : m_table_view->selection().indexes())
            selected_positions.empend(m_sheet->column(selection.column()), (size_t)selection.row());

        if (on_selection_changed) {
            on_selection_changed(move(selected_positions));
            update_with_model();
        };
    };

    m_table_view->on_activation = [this](auto&) {
        m_table_view->move_cursor(GUI::AbstractView::CursorMovement::Down, GUI::AbstractView::SelectionUpdate::Set);
    };

    m_table_view->on_context_menu_request = [&](const GUI::ModelIndex&, const GUI::ContextMenuEvent& event) {
        // NOTE: We ignore the specific cell for now.
        m_cell_range_context_menu->popup(event.screen_position());
    };

    m_cell_range_context_menu = GUI::Menu::construct();
    m_cell_range_context_menu->add_action(GUI::Action::create("Type and Formatting...", [this](auto&) {
        Vector<Position> positions;
        for (auto& index : m_table_view->selection().indexes()) {
            Position position { m_sheet->column(index.column()), (size_t)index.row() };
            positions.append(move(position));
        }

        if (positions.is_empty()) {
            auto& index = m_table_view->cursor_index();
            Position position { m_sheet->column(index.column()), (size_t)index.row() };
            positions.append(move(position));
        }

        auto dialog = CellTypeDialog::construct(positions, *m_sheet, window());
        if (dialog->exec() == GUI::Dialog::ExecOK) {
            for (auto& position : positions) {
                auto& cell = m_sheet->ensure(position);
                cell.set_type(dialog->type());
                cell.set_type_metadata(dialog->metadata());
                cell.set_conditional_formats(dialog->conditional_formats());
            }

            m_table_view->update();
        }
    }));

    m_table_view->on_drop = [&](const GUI::ModelIndex& index, const GUI::DropEvent& event) {
        if (!index.is_valid())
            return;

        ScopeGuard update_after_drop { [this] { update(); } };

        if (event.mime_data().has_format("text/x-spreadsheet-data")) {
            auto data = event.mime_data().data("text/x-spreadsheet-data");
            StringView urls { data.data(), data.size() };
            Vector<Position> source_positions, target_positions;

            for (auto& line : urls.lines(false)) {
                auto position = m_sheet->position_from_url(line);
                if (position.has_value())
                    source_positions.append(position.release_value());
            }

            // Drop always has a single target.
            Position target { m_sheet->column(index.column()), (size_t)index.row() };
            target_positions.append(move(target));

            if (source_positions.is_empty())
                return;

            auto first_position = source_positions.take_first();
            m_sheet->copy_cells(move(source_positions), move(target_positions), first_position);

            return;
        }

        if (event.mime_data().has_text()) {
            auto* target_cell = m_sheet->at({ m_sheet->column(index.column()), (size_t)index.row() });
            ASSERT(target_cell);

            target_cell->set_data(event.text());
            return;
        }
    };
}

void SpreadsheetView::hide_event(GUI::HideEvent&)
{
    if (on_selection_dropped)
        on_selection_dropped();
}

void SpreadsheetView::show_event(GUI::ShowEvent&)
{
    if (on_selection_changed && !m_table_view->selection().is_empty()) {
        Vector<Position> selected_positions;
        selected_positions.ensure_capacity(m_table_view->selection().size());
        for (auto& selection : m_table_view->selection().indexes())
            selected_positions.empend(m_sheet->column(selection.column()), (size_t)selection.row());

        on_selection_changed(move(selected_positions));
    }
}

void SpreadsheetView::TableCellPainter::paint(GUI::Painter& painter, const Gfx::IntRect& rect, const Gfx::Palette& palette, const GUI::ModelIndex& index)
{
    // Draw a border.
    // Undo the horizontal padding done by the table view...
    auto cell_rect = rect.inflated(m_table_view.horizontal_padding() * 2, 0);

    if (auto bg = index.data(GUI::ModelRole::BackgroundColor); bg.is_color())
        painter.fill_rect(cell_rect, bg.as_color());

    if (m_table_view.selection().contains(index)) {
        Color fill_color = palette.selection();
        fill_color.set_alpha(80);
        painter.fill_rect(cell_rect, fill_color);
    }

    auto text_color = index.data(GUI::ModelRole::ForegroundColor).to_color(palette.color(m_table_view.foreground_role()));
    auto data = index.data();
    auto text_alignment = index.data(GUI::ModelRole::TextAlignment).to_text_alignment(Gfx::TextAlignment::CenterRight);
    painter.draw_text(rect, data.to_string(), m_table_view.font_for_index(index), text_alignment, text_color, Gfx::TextElision::Right);
}

}
