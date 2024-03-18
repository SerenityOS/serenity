/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpreadsheetView.h"
#include "CellTypeDialog.h"
#include <AK/ScopeGuard.h>
#include <LibCore/MimeData.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/TableView.h>
#include <LibGfx/Palette.h>
#include <LibURL/URL.h>

namespace Spreadsheet {

void SpreadsheetView::EditingDelegate::set_value(GUI::Variant const& value, GUI::ModelEditingDelegate::SelectionBehavior selection_behavior)
{
    if (!value.is_valid()) {
        StringModelEditingDelegate::set_value("", selection_behavior);
        commit();
        return;
    }

    if (m_has_set_initial_value)
        return StringModelEditingDelegate::set_value(value, selection_behavior);

    m_has_set_initial_value = true;
    auto const option = m_sheet.at({ (size_t)index().column(), (size_t)index().row() });
    if (option)
        return StringModelEditingDelegate::set_value(option->source(), selection_behavior);

    StringModelEditingDelegate::set_value("", selection_behavior);
}

void InfinitelyScrollableTableView::did_scroll()
{
    TableView::did_scroll();
    auto& vscrollbar = vertical_scrollbar();
    auto& hscrollbar = horizontal_scrollbar();
    if (!m_vertical_scroll_end_timer->is_active()) {
        if (vscrollbar.is_visible() && vscrollbar.value() == vscrollbar.max()) {
            m_vertical_scroll_end_timer->on_timeout = [&] {
                if (on_reaching_vertical_end)
                    on_reaching_vertical_end();
                m_vertical_scroll_end_timer->stop();
            };
            m_vertical_scroll_end_timer->start(50);
        }
    }
    if (!m_horizontal_scroll_end_timer->is_active()) {
        if (hscrollbar.is_visible() && hscrollbar.value() == hscrollbar.max()) {
            m_horizontal_scroll_end_timer->on_timeout = [&] {
                if (on_reaching_horizontal_end)
                    on_reaching_horizontal_end();
                m_horizontal_scroll_end_timer->stop();
            };
            m_horizontal_scroll_end_timer->start(50);
        }
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

        if (!is_dragging()) {
            auto tooltip = model->data(index, static_cast<GUI::ModelRole>(SheetModel::Role::Tooltip));
            if (tooltip.is_string()) {
                set_tooltip(MUST(String::from_byte_string(tooltip.as_string())));
                show_or_hide_tooltip();
            } else {
                set_tooltip({});
            }
        }

        m_is_hovering_cut_zone = false;
        m_is_hovering_extend_zone = false;
        if (selection().size() > 0 && !m_is_dragging_for_select) {
            // Get top-left and bottom-right most cells of selection
            auto bottom_right_most_index = selection().first();
            auto top_left_most_index = selection().first();
            selection().for_each_index([&](auto& index) {
                if (index.row() > bottom_right_most_index.row())
                    bottom_right_most_index = index;
                else if (index.column() > bottom_right_most_index.column())
                    bottom_right_most_index = index;
                if (index.row() < top_left_most_index.row())
                    top_left_most_index = index;
                else if (index.column() < top_left_most_index.column())
                    top_left_most_index = index;
            });

            auto top_left_rect = content_rect_minus_scrollbars(top_left_most_index);
            auto bottom_right_rect = content_rect_minus_scrollbars(bottom_right_most_index);
            auto distance_tl = top_left_rect.center() - event.position();
            auto distance_br = bottom_right_rect.center() - event.position();
            auto is_over_top_line = false;
            auto is_over_bottom_line = false;
            auto is_over_left_line = false;
            auto is_over_right_line = false;

            // If cursor is within the bounds of the selection
            auto select_padding = 2;
            if ((distance_br.y() >= -(bottom_right_rect.height() / 2 + select_padding)) && (distance_tl.y() <= (top_left_rect.height() / 2 + select_padding)) && (distance_br.x() >= -(bottom_right_rect.width() / 2 + select_padding)) && (distance_tl.x() <= (top_left_rect.width() / 2 + select_padding))) {
                if (distance_tl.y() >= (top_left_rect.height() / 2 - select_padding))
                    is_over_top_line = true;
                else if (distance_br.y() <= -(bottom_right_rect.height() / 2 - select_padding))
                    is_over_bottom_line = true;

                if (distance_tl.x() >= (top_left_rect.width() / 2 - select_padding))
                    is_over_left_line = true;
                else if (distance_br.x() <= -(bottom_right_rect.width() / 2 - select_padding))
                    is_over_right_line = true;
            }

            if (is_over_bottom_line && is_over_right_line) {
                m_target_cell = bottom_right_most_index;
                m_is_hovering_extend_zone = true;
            } else if (is_over_top_line || is_over_bottom_line || is_over_left_line || is_over_right_line) {
                m_target_cell = top_left_most_index;
                m_is_hovering_cut_zone = true;
            }
        }

        if (m_is_hovering_cut_zone || m_is_dragging_for_cut)
            set_override_cursor(Gfx::StandardCursor::Drag);
        else if (m_is_hovering_extend_zone || m_is_dragging_for_extend)
            set_override_cursor(Gfx::StandardCursor::Crosshair);
        else
            set_override_cursor(Gfx::StandardCursor::Arrow);

        auto holding_left_button = !!(event.buttons() & GUI::MouseButton::Primary);
        if (m_is_dragging_for_cut) {
            m_is_dragging_for_select = false;
            if (holding_left_button) {
                m_has_committed_to_cutting = true;
            }
        } else if (!m_is_dragging_for_select) {
            if (!holding_left_button) {
                m_starting_selection_index = index;
            } else {
                m_is_dragging_for_select = true;
                m_might_drag = false;
            }
        }

        if (!m_has_committed_to_extending) {
            if (m_is_dragging_for_extend && holding_left_button) {
                m_has_committed_to_extending = true;
                m_starting_selection_index = m_target_cell;
            }
        }

        if (holding_left_button && m_is_dragging_for_select && !m_has_committed_to_cutting) {
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

            // Since the extend function has similarities to the select, then do
            // a check within the selection process to see if extending.
            if (m_has_committed_to_extending) {
                if (index.row() == m_target_cell.row() || index.column() == m_target_cell.column())
                    selection().add_all(new_selection);
                else
                    // Prevent the target cell from being unselected in the cases
                    // when extending in a direction that is not in the same column or
                    // row as the same. (Extension can only be done linearly, not diagonally)
                    selection().add(m_target_cell);
            } else {
                selection().add_all(new_selection);
            }
        }
    }

    TableView::mousemove_event(event);
}

void InfinitelyScrollableTableView::mousedown_event(GUI::MouseEvent& event)
{
    // Override the mouse event so that the cell that is 'clicked' is not
    // the one right beneath the cursor but instead the one that is referred to
    // when m_is_hovering_cut_zone as it can be the case that the user is targeting
    // a cell yet be outside of its bounding box due to the select_padding.
    if (m_is_hovering_cut_zone || m_is_hovering_extend_zone) {
        if (m_is_hovering_cut_zone)
            m_is_dragging_for_cut = true;
        else if (m_is_hovering_extend_zone)
            m_is_dragging_for_extend = true;
        auto rect = content_rect_minus_scrollbars(m_target_cell);
        GUI::MouseEvent adjusted_event = { (GUI::Event::Type)event.type(), rect.center(), event.buttons(), event.button(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y() };
        AbstractTableView::mousedown_event(adjusted_event);
    } else {
        AbstractTableView::mousedown_event(event);
        if (event.button() == GUI::Primary) {
            auto index = index_at_event_position(event.position());
            AbstractTableView::set_cursor(index, SelectionUpdate::Set);
        }
    }
}

void InfinitelyScrollableTableView::mouseup_event(GUI::MouseEvent& event)
{
    // If done extending
    if (m_has_committed_to_extending) {
        Vector<Position> from;
        Position position { (size_t)m_target_cell.column(), (size_t)m_target_cell.row() };
        from.append(position);
        Vector<CellChange> cell_changes;
        selection().for_each_index([&](auto& index) {
            if (index == m_starting_selection_index)
                return;
            auto& sheet = static_cast<SheetModel&>(*this->model()).sheet();
            Vector<Position> to;
            Position position { (size_t)index.column(), (size_t)index.row() };
            to.append(position);
            auto cell_change = sheet.copy_cells(from, to);
            cell_changes.extend(cell_change);
        });
        if (static_cast<SheetModel&>(*this->model()).on_cells_data_change)
            static_cast<SheetModel&>(*this->model()).on_cells_data_change(cell_changes);
        update();
    }

    m_is_dragging_for_select = false;
    m_is_dragging_for_cut = false;
    m_is_dragging_for_extend = false;
    m_has_committed_to_cutting = false;
    m_has_committed_to_extending = false;
    if (m_is_hovering_cut_zone || m_is_hovering_extend_zone) {
        auto rect = content_rect_minus_scrollbars(m_target_cell);
        GUI::MouseEvent adjusted_event = { (GUI::Event::Type)event.type(), rect.center(), event.buttons(), event.button(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y() };
        TableView::mouseup_event(adjusted_event);
    } else {
        TableView::mouseup_event(event);
    }
}

void InfinitelyScrollableTableView::drop_event(GUI::DropEvent& event)
{
    m_is_dragging_for_cut = false;
    set_override_cursor(Gfx::StandardCursor::Arrow);
    if (!index_at_event_position(event.position()).is_valid())
        return;

    TableView::drop_event(event);
    auto drop_index = index_at_event_position(event.position());
    if (selection().size() > 0) {
        // Get top left index position of previous selection
        auto top_left_most_index = selection().first();
        selection().for_each_index([&](auto& index) {
            if (index.row() < top_left_most_index.row())
                top_left_most_index = index;
            else if (index.column() < top_left_most_index.column())
                top_left_most_index = index;
        });

        // Compare with drag location
        auto x_diff = drop_index.column() - top_left_most_index.column();
        auto y_diff = drop_index.row() - top_left_most_index.row();

        // Set new selection
        Vector<GUI::ModelIndex> new_selection;
        for (auto& index : selection().indices()) {
            auto new_index = model()->index(index.row() + y_diff, index.column() + x_diff);
            new_selection.append(move(new_index));
        }
        selection().clear();
        AbstractTableView::set_cursor(drop_index, SelectionUpdate::Set);
        selection().add_all(new_selection);
    }
}

void SpreadsheetView::update_with_model()
{
    m_sheet_model->update();
    m_table_view->update();
}

SpreadsheetView::SpreadsheetView(Sheet& sheet)
    : m_sheet(sheet)
    , m_sheet_model(SheetModel::create(*m_sheet))
{
    set_layout<GUI::VerticalBoxLayout>(2);
    m_table_view = add<InfinitelyScrollableTableView>();
    m_table_view->set_grid_style(GUI::TableView::GridStyle::Both);
    m_table_view->set_selection_behavior(GUI::AbstractView::SelectionBehavior::SelectItems);
    m_table_view->set_edit_triggers(GUI::AbstractView::EditTrigger::EditKeyPressed | GUI::AbstractView::AnyKeyPressed | GUI::AbstractView::DoubleClicked);
    m_table_view->set_tab_key_navigation_enabled(true);
    m_table_view->row_header().set_visible(true);
    m_table_view->set_model(m_sheet_model);
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
            m_table_view->set_default_column_width(last_column_index, 50);
            m_table_view->set_column_header_alignment(last_column_index, Gfx::TextAlignment::Center);
            m_table_view->set_column_painting_delegate(last_column_index, make<TableCellPainter>(*m_table_view));
        }
        update_with_model();
    };

    set_focus_proxy(m_table_view);

    // FIXME: This is dumb.
    for (size_t i = 0; i < m_sheet->column_count(); ++i) {
        m_table_view->set_column_painting_delegate(i, make<TableCellPainter>(*m_table_view));
        m_table_view->set_column_width(i, 50);
        m_table_view->set_default_column_width(i, 50);
        m_table_view->set_column_header_alignment(i, Gfx::TextAlignment::Center);
    }

    m_table_view->set_alternating_row_colors(false);
    m_table_view->set_highlight_selected_rows(false);
    m_table_view->set_editable(true);
    m_table_view->aid_create_editing_delegate = [this](auto&) {
        auto delegate = make<EditingDelegate>(*m_sheet);
        delegate->on_cursor_key_pressed = [this](auto& event) {
            m_table_view->stop_editing();
            m_table_view->dispatch_event(event);
        };
        delegate->on_cell_focusout = [this](auto& index, auto& value) {
            m_table_view->model()->set_data(index, value);
        };
        return delegate;
    };

    m_table_view->on_selection_change = [&] {
        m_sheet->selected_cells().clear();
        for (auto& index : m_table_view->selection().indices()) {
            Position position { (size_t)index.column(), (size_t)index.row() };
            m_sheet->selected_cells().set(position);
        }

        if (m_table_view->selection().is_empty() && on_selection_dropped)
            return on_selection_dropped();

        Vector<Position> selected_positions;
        selected_positions.ensure_capacity(m_table_view->selection().size());
        for (auto& selection : m_table_view->selection().indices())
            selected_positions.empend((size_t)selection.column(), (size_t)selection.row());

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
    m_cell_range_context_menu->add_action(GUI::Action::create("Format...", [this](auto&) {
        Vector<Position> positions;
        for (auto& index : m_table_view->selection().indices()) {
            Position position { (size_t)index.column(), (size_t)index.row() };
            positions.append(move(position));
        }

        if (positions.is_empty()) {
            auto& index = m_table_view->cursor_index();
            Position position { (size_t)index.column(), (size_t)index.row() };
            positions.append(move(position));
        }

        auto dialog = CellTypeDialog::construct(positions, *m_sheet, window());
        if (dialog->exec() == GUI::Dialog::ExecResult::OK) {
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

        if (event.mime_data().has_format("text/x-spreadsheet-data"sv)) {
            auto const& data = event.mime_data().data("text/x-spreadsheet-data"sv);
            StringView urls { data.data(), data.size() };
            Vector<Position> source_positions, target_positions;

            for (auto& line : urls.lines(StringView::ConsiderCarriageReturn::No)) {
                auto position = m_sheet->position_from_url(line);
                if (position.has_value())
                    source_positions.append(position.release_value());
            }

            // Drop always has a single target.
            Position target { (size_t)index.column(), (size_t)index.row() };
            target_positions.append(move(target));

            if (source_positions.is_empty())
                return;

            auto first_position = source_positions.take_first();
            auto cell_changes = m_sheet->copy_cells(move(source_positions), move(target_positions), first_position, Spreadsheet::Sheet::CopyOperation::Cut);
            if (model()->on_cells_data_change)
                model()->on_cells_data_change(cell_changes);

            return;
        }

        if (event.mime_data().has_text()) {
            auto& target_cell = m_sheet->ensure({ (size_t)index.column(), (size_t)index.row() });
            target_cell.set_data(event.text());
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
        for (auto& selection : m_table_view->selection().indices())
            selected_positions.empend((size_t)selection.column(), (size_t)selection.row());

        on_selection_changed(move(selected_positions));
    }
}

void SpreadsheetView::move_cursor(GUI::AbstractView::CursorMovement direction)
{
    m_table_view->move_cursor(direction, GUI::AbstractView::SelectionUpdate::Set);
}

void SpreadsheetView::TableCellPainter::paint(GUI::Painter& painter, Gfx::IntRect const& rect, Gfx::Palette const& palette, const GUI::ModelIndex& index)
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
    painter.draw_text(rect, data.to_byte_string(), m_table_view.font_for_index(index), text_alignment, text_color, Gfx::TextElision::Right);
}

}
