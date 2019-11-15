#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <Kernel/KeyCode.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GClipboard.h>
#include <LibGUI/GFontDatabase.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWindow.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

//#define DEBUG_GTEXTEDITOR

GTextEditor::GTextEditor(GWidget* parent)
    : GTextEditor(Type::MultiLine, parent)
{
}

GTextEditor::GTextEditor(Type type, GWidget* parent)
    : GScrollableWidget(parent)
    , m_type(type)
{
    set_document(GTextDocument::create());
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);
    set_scrollbars_enabled(is_multi_line());
    set_font(GFontDatabase::the().get_by_name("Csilla Thin"));
    // FIXME: Recompute vertical scrollbar step size on font change.
    vertical_scrollbar().set_step(line_height());
    m_cursor = { 0, 0 };
    create_actions();

    // TODO: Instead of a repating timer, this we should call a delayed 2 sec timer when the user types.
    m_undo_timer = CTimer::construct(
        2000, [&] {
            update_undo_timer();
        },
        this);
}

GTextEditor::~GTextEditor()
{
    if (m_document)
        m_document->unregister_client(*this);
}

void GTextEditor::create_actions()
{
    m_undo_action = GCommonActions::make_undo_action([&](auto&) { undo(); }, this);
    m_redo_action = GCommonActions::make_redo_action([&](auto&) { redo(); }, this);
    m_undo_action->set_enabled(false);
    m_redo_action->set_enabled(false);
    m_cut_action = GCommonActions::make_cut_action([&](auto&) { cut(); }, this);
    m_copy_action = GCommonActions::make_copy_action([&](auto&) { copy(); }, this);
    m_paste_action = GCommonActions::make_paste_action([&](auto&) { paste(); }, this);
    m_delete_action = GCommonActions::make_delete_action([&](auto&) { do_delete(); }, this);
}

void GTextEditor::set_text(const StringView& text)
{
    if (is_single_line() && text.length() == line(0).length() && !memcmp(text.characters_without_null_termination(), line(0).characters(), text.length()))
        return;

    m_selection.clear();

    document().set_text(text);

    update_content_size();
    recompute_all_visual_lines();
    if (is_single_line())
        set_cursor(0, line(0).length());
    else
        set_cursor(0, 0);
    did_update_selection();
    update();
}

void GTextEditor::update_content_size()
{
    int content_width = 0;
    int content_height = 0;
    for (auto& line : m_line_visual_data) {
        content_width = max(line.visual_rect.width(), content_width);
        content_height += line.visual_rect.height();
    }
    content_width += m_horizontal_content_padding * 2;
    if (is_right_text_alignment(m_text_alignment))
        content_width = max(frame_inner_rect().width(), content_width);

    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ ruler_width(), 0 });
}

GTextPosition GTextEditor::text_position_at(const Point& a_position) const
{
    auto position = a_position;
    position.move_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    position.move_by(-(m_horizontal_content_padding + ruler_width()), 0);
    position.move_by(-frame_thickness(), -frame_thickness());

    int line_index = -1;

    if (is_line_wrapping_enabled()) {
        for (int i = 0; i < lines().size(); ++i) {
            auto& rect = m_line_visual_data[i].visual_rect;
            if (position.y() >= rect.top() && position.y() <= rect.bottom()) {
                line_index = i;
                break;
            }
            if (position.y() > rect.bottom())
                line_index = lines().size() - 1;
        }
    } else {
        line_index = position.y() / line_height();
    }

    line_index = max(0, min(line_index, line_count() - 1));

    int column_index;
    switch (m_text_alignment) {
    case TextAlignment::CenterLeft:
        column_index = (position.x() + glyph_width() / 2) / glyph_width();
        if (is_line_wrapping_enabled()) {
            for_each_visual_line(line_index, [&](const Rect& rect, const StringView&, int start_of_line) {
                if (rect.contains_vertically(position.y())) {
                    column_index += start_of_line;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
        }
        break;
    case TextAlignment::CenterRight:
        // FIXME: Support right-aligned line wrapping, I guess.
        ASSERT(!is_line_wrapping_enabled());
        column_index = (position.x() - content_x_for_position({ line_index, 0 }) + glyph_width() / 2) / glyph_width();
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    column_index = max(0, min(column_index, lines()[line_index].length()));
    return { line_index, column_index };
}

void GTextEditor::doubleclick_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;

    // NOTE: This ensures that spans are updated before we look at them.
    flush_pending_change_notification_if_needed();

    m_triple_click_timer.start();
    m_in_drag_select = false;

    auto start = text_position_at(event.position());
    auto end = start;
    auto& line = lines()[start.line()];

    if (!document().has_spans()) {
        while (start.column() > 0) {
            if (isspace(line.characters()[start.column() - 1]))
                break;
            start.set_column(start.column() - 1);
        }

        while (end.column() < line.length()) {
            if (isspace(line.characters()[end.column()]))
                break;
            end.set_column(end.column() + 1);
        }
    } else {
        for (auto& span : document().spans()) {
            if (!span.range.contains(start))
                continue;
            start = span.range.start();
            end = span.range.end();
            end.set_column(end.column() + 1);
            break;
        }
    }

    m_selection.set(start, end);
    set_cursor(end);
    update();
    did_update_selection();
}

void GTextEditor::mousedown_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left) {
        return;
    }

    if (m_triple_click_timer.is_valid() && m_triple_click_timer.elapsed() < 250) {
        m_triple_click_timer = CElapsedTimer();

        GTextPosition start;
        GTextPosition end;

        if (is_multi_line()) {
            // select *current* line
            start = GTextPosition(m_cursor.line(), 0);
            end = GTextPosition(m_cursor.line(), lines()[m_cursor.line()].length());
        } else {
            // select *whole* line
            start = GTextPosition(0, 0);
            end = GTextPosition(line_count() - 1, lines()[line_count() - 1].length());
        }

        m_selection.set(start, end);
        set_cursor(end);
        return;
    }

    if (event.modifiers() & Mod_Shift) {
        if (!has_selection())
            m_selection.set(m_cursor, {});
    } else {
        m_selection.clear();
    }

    m_in_drag_select = true;

    set_cursor(text_position_at(event.position()));

    if (!(event.modifiers() & Mod_Shift)) {
        if (!has_selection())
            m_selection.set(m_cursor, {});
    }

    if (m_selection.start().is_valid() && m_selection.start() != m_cursor)
        m_selection.set_end(m_cursor);

    // FIXME: Only update the relevant rects.
    update();
    did_update_selection();
}

void GTextEditor::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        if (m_in_drag_select) {
            m_in_drag_select = false;
        }
        return;
    }
}

void GTextEditor::mousemove_event(GMouseEvent& event)
{
    if (m_in_drag_select) {
        set_cursor(text_position_at(event.position()));
        m_selection.set_end(m_cursor);
        did_update_selection();
        update();
        return;
    }
}

int GTextEditor::ruler_width() const
{
    if (!m_ruler_visible)
        return 0;
    // FIXME: Resize based on needed space.
    return 5 * font().glyph_width('x') + 4;
}

Rect GTextEditor::ruler_content_rect(int line_index) const
{
    if (!m_ruler_visible)
        return {};
    return {
        0 - ruler_width() + horizontal_scrollbar().value(),
        line_content_rect(line_index).y(),
        ruler_width(),
        line_content_rect(line_index).height()
    };
}

Rect GTextEditor::ruler_rect_in_inner_coordinates() const
{
    return { 0, 0, ruler_width(), height() - height_occupied_by_horizontal_scrollbar() };
}

Rect GTextEditor::visible_text_rect_in_inner_coordinates() const
{
    return {
        m_horizontal_content_padding + (m_ruler_visible ? (ruler_rect_in_inner_coordinates().right() + 1) : 0),
        0,
        frame_inner_rect().width() - (m_horizontal_content_padding * 2) - width_occupied_by_vertical_scrollbar() - ruler_width(),
        frame_inner_rect().height() - height_occupied_by_horizontal_scrollbar()
    };
}

void GTextEditor::paint_event(GPaintEvent& event)
{
    // NOTE: This ensures that spans are updated before we look at them.
    flush_pending_change_notification_if_needed();

    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::White);

    painter.translate(frame_thickness(), frame_thickness());

    auto ruler_rect = ruler_rect_in_inner_coordinates();

    if (m_ruler_visible) {
        painter.fill_rect(ruler_rect, Color::WarmGray);
        painter.draw_line(ruler_rect.top_right(), ruler_rect.bottom_right(), Color::DarkGray);
    }

    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    if (m_ruler_visible)
        painter.translate(ruler_width(), 0);

    int first_visible_line = text_position_at(event.rect().top_left()).line();
    int last_visible_line = text_position_at(event.rect().bottom_right()).line();

    auto selection = normalized_selection();
    bool has_selection = selection.is_valid();

    if (m_ruler_visible) {
        for (int i = first_visible_line; i <= last_visible_line; ++i) {
            bool is_current_line = i == m_cursor.line();
            auto ruler_line_rect = ruler_content_rect(i);
            painter.draw_text(
                ruler_line_rect.shrunken(2, 0).translated(0, m_line_spacing / 2),
                String::number(i + 1),
                is_current_line ? Font::default_bold_font() : font(),
                TextAlignment::TopRight,
                is_current_line ? Color::DarkGray : Color::MidGray);
        }
    }

    Rect text_clip_rect {
        (m_ruler_visible ? (ruler_rect_in_inner_coordinates().right() + frame_thickness() + 1) : frame_thickness()),
        frame_thickness(),
        width() - width_occupied_by_vertical_scrollbar() - ruler_width(),
        height() - height_occupied_by_horizontal_scrollbar()
    };
    painter.add_clip_rect(text_clip_rect);

    for (int line_index = first_visible_line; line_index <= last_visible_line; ++line_index) {
        auto& line = lines()[line_index];

        bool physical_line_has_selection = has_selection && line_index >= selection.start().line() && line_index <= selection.end().line();
        int first_visual_line_with_selection = -1;
        int last_visual_line_with_selection = -1;
        if (physical_line_has_selection) {
            if (selection.start().line() < line_index)
                first_visual_line_with_selection = 0;
            else
                first_visual_line_with_selection = visual_line_containing(line_index, selection.start().column());

            if (selection.end().line() > line_index)
                last_visual_line_with_selection = m_line_visual_data[line_index].visual_line_breaks.size();
            else
                last_visual_line_with_selection = visual_line_containing(line_index, selection.end().column());
        }

        int selection_start_column_within_line = selection.start().line() == line_index ? selection.start().column() : 0;
        int selection_end_column_within_line = selection.end().line() == line_index ? selection.end().column() : line.length();

        int visual_line_index = 0;
        for_each_visual_line(line_index, [&](const Rect& visual_line_rect, const StringView& visual_line_text, int start_of_visual_line) {
            if (is_multi_line() && line_index == m_cursor.line())
                painter.fill_rect(visual_line_rect, Color(230, 230, 230));
#ifdef DEBUG_GTEXTEDITOR
            painter.draw_rect(visual_line_rect, Color::Cyan);
#endif
            if (!document().has_spans()) {
                // Fast-path for plain text
                painter.draw_text(visual_line_rect, visual_line_text, m_text_alignment, Color::Black);
            } else {
                int advance = font().glyph_width(' ') + font().glyph_spacing();
                Rect character_rect = { visual_line_rect.location(), { font().glyph_width(' '), line_height() } };
                for (int i = 0; i < visual_line_text.length(); ++i) {
                    const Font* font = &this->font();
                    Color color;
                    GTextPosition physical_position(line_index, start_of_visual_line + i);
                    // FIXME: This is *horribly* inefficient.
                    for (auto& span : document().spans()) {
                        if (!span.range.contains(physical_position))
                            continue;
                        color = span.color;
                        if (span.font)
                            font = span.font;
                        break;
                    }
                    painter.draw_text(character_rect, visual_line_text.substring_view(i, 1), *font, m_text_alignment, color);
                    character_rect.move_by(advance, 0);
                }
            }
            bool physical_line_has_selection = has_selection && line_index >= selection.start().line() && line_index <= selection.end().line();
            if (physical_line_has_selection) {

                bool current_visual_line_has_selection = (line_index != selection.start().line() && line_index != selection.end().line())
                    || (visual_line_index >= first_visual_line_with_selection && visual_line_index <= last_visual_line_with_selection);
                if (current_visual_line_has_selection) {
                    bool selection_begins_on_current_visual_line = visual_line_index == first_visual_line_with_selection;
                    bool selection_ends_on_current_visual_line = visual_line_index == last_visual_line_with_selection;

                    int selection_left = selection_begins_on_current_visual_line
                        ? content_x_for_position({ line_index, selection_start_column_within_line })
                        : m_horizontal_content_padding;

                    int selection_right = selection_ends_on_current_visual_line
                        ? content_x_for_position({ line_index, selection_end_column_within_line })
                        : visual_line_rect.right() + 1;

                    Rect selection_rect {
                        selection_left,
                        visual_line_rect.y(),
                        selection_right - selection_left,
                        visual_line_rect.height()
                    };

                    painter.fill_rect(selection_rect, Color::from_rgb(0x955233));

                    int start_of_selection_within_visual_line = max(0, selection_start_column_within_line - start_of_visual_line);
                    int end_of_selection_within_visual_line = selection_end_column_within_line - start_of_visual_line;

                    StringView visual_selected_text {
                        visual_line_text.characters_without_null_termination() + start_of_selection_within_visual_line,
                        end_of_selection_within_visual_line - start_of_selection_within_visual_line
                    };

                    painter.draw_text(selection_rect, visual_selected_text, TextAlignment::CenterLeft, Color::White);
                }
            }
            ++visual_line_index;
            return IterationDecision::Continue;
        });
    }

    if (is_focused() && m_cursor_state)
        painter.fill_rect(cursor_content_rect(), Color::Red);
}

void GTextEditor::toggle_selection_if_needed_for_event(const GKeyEvent& event)
{
    if (event.shift() && !m_selection.is_valid()) {
        m_selection.set(m_cursor, {});
        did_update_selection();
        update();
        return;
    }
    if (!event.shift() && m_selection.is_valid()) {
        m_selection.clear();
        did_update_selection();
        update();
        return;
    }
}

void GTextEditor::select_all()
{
    GTextPosition start_of_document { 0, 0 };
    GTextPosition end_of_document { line_count() - 1, lines()[line_count() - 1].length() };
    m_selection.set(start_of_document, end_of_document);
    did_update_selection();
    set_cursor(end_of_document);
    update();
}

void GTextEditor::undo()
{
    if (!can_undo())
        return;

    auto& undo_container = m_undo_stack[m_undo_stack_index];
    auto& undo_vector = undo_container.m_undo_vector;

    //If we try to undo a empty vector, delete it and skip over.
    if (undo_vector.is_empty()) {
        m_undo_stack.remove(m_undo_stack_index);
        undo();
        return;
    }

    for (int i = 0; i < undo_vector.size(); i++) {
        auto& undo_command = undo_vector[i];
        undo_command.undo();
    }

    m_undo_stack_index++;
    did_change();
}

void GTextEditor::redo()
{
    if (!can_redo())
        return;

    auto& undo_container = m_undo_stack[m_undo_stack_index - 1];
    auto& redo_vector = undo_container.m_undo_vector;

    for (int i = redo_vector.size() - 1; i >= 0; i--) {
        auto& undo_command = redo_vector[i];
        undo_command.redo();
    }

    m_undo_stack_index--;
    did_change();
}

void GTextEditor::get_selection_line_boundaries(int& first_line, int& last_line)
{
    auto selection = normalized_selection();
    if (!selection.is_valid()) {
        first_line = m_cursor.line();
        last_line = m_cursor.line();
        return;
    }
    first_line = selection.start().line();
    last_line = selection.end().line();
    if (first_line != last_line && selection.end().column() == 0)
        last_line -= 1;
}

void GTextEditor::move_selected_lines_up()
{
    int first_line;
    int last_line;
    get_selection_line_boundaries(first_line, last_line);

    if (first_line == 0)
        return;

    auto& lines = document().lines();
    lines.insert(last_line, lines.take(first_line - 1));
    m_cursor = { first_line - 1, 0 };

    if (has_selection()) {
        m_selection.set_start({ first_line - 1, 0 });
        m_selection.set_end({ last_line - 1, line(last_line - 1).length() });
    }

    did_change();
    update();
}

void GTextEditor::move_selected_lines_down()
{
    int first_line;
    int last_line;
    get_selection_line_boundaries(first_line, last_line);

    auto& lines = document().lines();
    if (last_line >= (lines.size() - 1))
        return;

    lines.insert(first_line, lines.take(last_line + 1));
    m_cursor = { first_line + 1, 0 };

    if (has_selection()) {
        m_selection.set_start({ first_line + 1, 0 });
        m_selection.set_end({ last_line + 1, line(last_line + 1).length() });
    }

    did_change();
    update();
}

void GTextEditor::sort_selected_lines()
{
    if (is_readonly())
        return;

    if (!has_selection())
        return;
        
    int first_line;
    int last_line;
    get_selection_line_boundaries(first_line, last_line);

    auto& lines = document().lines();

    auto start = lines.begin() + first_line;
    auto end = lines.begin() + last_line + 1;

    quick_sort(start, end, [](auto& a, auto& b) {
        return strcmp(a.characters(), b.characters()) < 0;
    });

    did_change();
    update();
}

void GTextEditor::keydown_event(GKeyEvent& event)
{
    if (is_single_line() && event.key() == KeyCode::Key_Tab)
        return GWidget::keydown_event(event);

    if (is_single_line() && event.key() == KeyCode::Key_Return) {
        if (on_return_pressed)
            on_return_pressed();
        return;
    }

    if (event.key() == KeyCode::Key_Escape) {
        if (on_escape_pressed)
            on_escape_pressed();
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        if (m_cursor.line() > 0) {
            if (event.ctrl() && event.shift()) {
                move_selected_lines_up();
                return;
            }
            int new_line = m_cursor.line() - 1;
            int new_column = min(m_cursor.column(), lines()[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        if (m_cursor.line() < (lines().size() - 1)) {
            if (event.ctrl() && event.shift()) {
                move_selected_lines_down();
                return;
            }
            int new_line = m_cursor.line() + 1;
            int new_column = min(m_cursor.column(), lines()[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        if (m_cursor.line() > 0) {
            int new_line = max(0, m_cursor.line() - visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), lines()[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        if (m_cursor.line() < (lines().size() - 1)) {
            int new_line = min(line_count() - 1, m_cursor.line() + visible_content_rect().height() / line_height());
            int new_column = min(m_cursor.column(), lines()[new_line].length());
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_Left) {
        if (event.ctrl() && document().has_spans()) {
            // FIXME: Do something nice when the document has no spans.
            auto span = document().first_non_skippable_span_before(m_cursor);
            GTextPosition new_cursor = !span.has_value()
                ? GTextPosition(0, 0)
                : span.value().range.start();
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_cursor);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
            return;
        }
        if (m_cursor.column() > 0) {
            int new_column = m_cursor.column() - 1;
            toggle_selection_if_needed_for_event(event);
            set_cursor(m_cursor.line(), new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        } else if (m_cursor.line() > 0) {
            int new_line = m_cursor.line() - 1;
            int new_column = lines()[new_line].length();
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_line, new_column);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
        }
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
        if (event.ctrl() && document().has_spans()) {
            // FIXME: Do something nice when the document has no spans.
            auto span = document().first_non_skippable_span_after(m_cursor);
            GTextPosition new_cursor = !span.has_value()
                ? document().spans().last().range.end()
                : span.value().range.start();
            toggle_selection_if_needed_for_event(event);
            set_cursor(new_cursor);
            if (event.shift() && m_selection.start().is_valid()) {
                m_selection.set_end(m_cursor);
                did_update_selection();
            }
            return;
        }
        int new_line = m_cursor.line();
        int new_column = m_cursor.column();
        if (m_cursor.column() < current_line().length()) {
            new_line = m_cursor.line();
            new_column = m_cursor.column() + 1;
        } else if (m_cursor.line() != line_count() - 1) {
            new_line = m_cursor.line() + 1;
            new_column = 0;
        }
        toggle_selection_if_needed_for_event(event);
        set_cursor(new_line, new_column);
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_Home) {
        int first_nonspace_column = current_line().first_non_whitespace_column();
        toggle_selection_if_needed_for_event(event);
        if (m_cursor.column() == first_nonspace_column)
            set_cursor(m_cursor.line(), 0);
        else
            set_cursor(m_cursor.line(), first_nonspace_column);
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_End) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(m_cursor.line(), current_line().length());
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_Home) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(0, 0);
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        toggle_selection_if_needed_for_event(event);
        set_cursor(line_count() - 1, lines()[line_count() - 1].length());
        if (event.shift() && m_selection.start().is_valid()) {
            m_selection.set_end(m_cursor);
            did_update_selection();
        }
        return;
    }
    if (event.modifiers() == Mod_Ctrl && event.key() == KeyCode::Key_A) {
        select_all();
        return;
    }
    if (event.alt() && event.shift() && event.key() == KeyCode::Key_S) {
        sort_selected_lines();
        return;
    }
    if (event.key() == KeyCode::Key_Backspace) {
        if (is_readonly())
            return;
        if (has_selection()) {
            delete_selection();
            did_update_selection();
            return;
        }
        if (m_cursor.column() > 0) {
            int erase_count = 1;
            if (current_line().first_non_whitespace_column() >= m_cursor.column()) {
                int new_column;
                if (m_cursor.column() % m_soft_tab_width == 0)
                    new_column = m_cursor.column() - m_soft_tab_width;
                else
                    new_column = (m_cursor.column() / m_soft_tab_width) * m_soft_tab_width;
                erase_count = m_cursor.column() - new_column;
            }

            // Backspace within line
            for (int i = 0; i < erase_count; ++i) {
                int row = m_cursor.line();
                int column = m_cursor.column() - 1 - i;
                add_to_undo_stack(make<RemoveCharacterCommand>(*this, document().line(row).characters()[column], GTextPosition(row, column)));
                current_line().remove(document(), m_cursor.column() - 1 - i);
            }
            update_content_size();
            set_cursor(m_cursor.line(), m_cursor.column() - erase_count);
            did_change();
            return;
        }
        if (m_cursor.column() == 0 && m_cursor.line() != 0) {
            // Backspace at column 0; merge with previous line
            auto& previous_line = lines()[m_cursor.line() - 1];
            int previous_length = previous_line.length();

            int row = m_cursor.line();
            int column = previous_length;
            add_to_undo_stack(make<RemoveLineCommand>(*this, String(lines()[m_cursor.line()].view()), GTextPosition(row, column), true));

            previous_line.append(document(), current_line().characters(), current_line().length());
            document().remove_line(m_cursor.line());
            update_content_size();
            update();
            set_cursor(m_cursor.line() - 1, previous_length);
            did_change();
            return;
        }
        return;
    }

    if (event.modifiers() == Mod_Shift && event.key() == KeyCode::Key_Delete) {
        if (is_readonly())
            return;
        delete_current_line();
        return;
    }

    if (event.key() == KeyCode::Key_Delete) {
        if (is_readonly())
            return;
        do_delete();
        return;
    }

    if (!is_readonly() && !event.ctrl() && !event.alt() && !event.text().is_empty())
        insert_at_cursor_or_replace_selection(event.text());
}

void GTextEditor::delete_current_line()
{
    if (has_selection())
        return delete_selection();

    document().remove_line(m_cursor.line());
    if (lines().is_empty())
        document().append_line(make<GTextDocumentLine>(document()));
    m_cursor.set_column(0);

    update_content_size();
    update();
}

void GTextEditor::do_delete()
{
    if (is_readonly())
        return;

    if (has_selection())
        return delete_selection();

    if (m_cursor.column() < current_line().length()) {
        // Delete within line
        current_line().remove(document(), m_cursor.column());
        did_change();
        update_cursor();
        return;
    }
    if (m_cursor.column() == current_line().length() && m_cursor.line() != line_count() - 1) {
        // Delete at end of line; merge with next line
        auto& next_line = lines()[m_cursor.line() + 1];
        int previous_length = current_line().length();
        current_line().append(document(), next_line.characters(), next_line.length());
        document().remove_line(m_cursor.line() + 1);
        update();
        did_change();
        set_cursor(m_cursor.line(), previous_length);
        return;
    }
}

void GTextEditor::insert_at_cursor(const StringView& text)
{
    // FIXME: This should obviously not be implemented this way.
    for (int i = 0; i < text.length(); ++i) {
        insert_at_cursor(text[i]);
    }
}

void GTextEditor::insert_at_cursor(char ch)
{
    bool at_head = m_cursor.column() == 0;
    bool at_tail = m_cursor.column() == current_line().length();
    if (ch == '\n') {
        if (at_tail || at_head) {
            String new_line_contents;
            if (m_automatic_indentation_enabled && at_tail) {
                int leading_spaces = 0;
                auto& old_line = lines()[m_cursor.line()];
                for (int i = 0; i < old_line.length(); ++i) {
                    if (old_line.characters()[i] == ' ')
                        ++leading_spaces;
                    else
                        break;
                }
                if (leading_spaces)
                    new_line_contents = String::repeated(' ', leading_spaces);
            }

            int row = m_cursor.line();
            int column = m_cursor.column() + 1;
            Vector<char> line_content;
            for (int i = m_cursor.column(); i < document().lines()[row].length(); i++)
                line_content.append(document().lines()[row].characters()[i]);
            add_to_undo_stack(make<CreateLineCommand>(*this, line_content, GTextPosition(row, column)));

            document().insert_line(m_cursor.line() + (at_tail ? 1 : 0), make<GTextDocumentLine>(document(), new_line_contents));
            update();
            did_change();
            set_cursor(m_cursor.line() + 1, lines()[m_cursor.line() + 1].length());
            return;
        }
        auto new_line = make<GTextDocumentLine>(document());
        new_line->append(document(), current_line().characters() + m_cursor.column(), current_line().length() - m_cursor.column());

        int row = m_cursor.line();
        int column = m_cursor.column() + 1;
        Vector<char> line_content;
        for (int i = 0; i < new_line->length(); i++)
            line_content.append(new_line->characters()[i]);
        add_to_undo_stack(make<CreateLineCommand>(*this, line_content, GTextPosition(row, column)));

        current_line().truncate(document(), m_cursor.column());
        document().insert_line(m_cursor.line() + 1, move(new_line));
        update();
        did_change();
        set_cursor(m_cursor.line() + 1, 0);
        return;
    }
    if (ch == '\t') {
        int next_soft_tab_stop = ((m_cursor.column() + m_soft_tab_width) / m_soft_tab_width) * m_soft_tab_width;
        int spaces_to_insert = next_soft_tab_stop - m_cursor.column();
        for (int i = 0; i < spaces_to_insert; ++i) {
            current_line().insert(document(), m_cursor.column(), ' ');
        }
        did_change();
        set_cursor(m_cursor.line(), next_soft_tab_stop);
        return;
    }
    current_line().insert(document(), m_cursor.column(), ch);
    did_change();
    set_cursor(m_cursor.line(), m_cursor.column() + 1);

    add_to_undo_stack(make<InsertCharacterCommand>(*this, ch, m_cursor));
}

int GTextEditor::content_x_for_position(const GTextPosition& position) const
{
    auto& line = lines()[position.line()];
    int x_offset = -1;
    switch (m_text_alignment) {
    case TextAlignment::CenterLeft:
        for_each_visual_line(position.line(), [&](const Rect&, const StringView& view, int start_of_visual_line) {
            if (position.column() >= start_of_visual_line && ((position.column() - start_of_visual_line) <= view.length())) {
                x_offset = (position.column() - start_of_visual_line) * glyph_width();
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return m_horizontal_content_padding + x_offset;
    case TextAlignment::CenterRight:
        // FIXME
        ASSERT(!is_line_wrapping_enabled());
        return content_width() - m_horizontal_content_padding - (line.length() * glyph_width()) + (position.column() * glyph_width());
    default:
        ASSERT_NOT_REACHED();
    }
}

Rect GTextEditor::content_rect_for_position(const GTextPosition& position) const
{
    if (!position.is_valid())
        return {};
    ASSERT(!lines().is_empty());
    ASSERT(position.column() <= (current_line().length() + 1));

    int x = content_x_for_position(position);

    if (is_single_line()) {
        Rect rect { x, 0, 1, font().glyph_height() + 2 };
        rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return rect;
    }

    Rect rect;
    for_each_visual_line(position.line(), [&](const Rect& visual_line_rect, const StringView& view, int start_of_visual_line) {
        if (position.column() >= start_of_visual_line && ((position.column() - start_of_visual_line) <= view.length())) {
            // NOTE: We have to subtract the horizontal padding here since it's part of the visual line rect
            //       *and* included in what we get from content_x_for_position().
            rect = {
                visual_line_rect.x() + x - (m_horizontal_content_padding),
                visual_line_rect.y(),
                1,
                line_height()
            };
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return rect;
}

Rect GTextEditor::cursor_content_rect() const
{
    return content_rect_for_position(m_cursor);
}

Rect GTextEditor::line_widget_rect(int line_index) const
{
    auto rect = line_content_rect(line_index);
    rect.set_x(frame_thickness());
    rect.set_width(frame_inner_rect().width());
    rect.move_by(0, -(vertical_scrollbar().value()));
    rect.move_by(0, frame_thickness());
    rect.intersect(frame_inner_rect());
    return rect;
}

void GTextEditor::scroll_position_into_view(const GTextPosition& position)
{
    auto rect = content_rect_for_position(position);
    if (position.column() == 0)
        rect.set_x(content_x_for_position({ position.line(), 0 }) - 2);
    else if (position.column() == lines()[position.line()].length())
        rect.set_x(content_x_for_position({ position.line(), lines()[position.line()].length() }) + 2);
    scroll_into_view(rect, true, true);
}

void GTextEditor::scroll_cursor_into_view()
{
    scroll_position_into_view(m_cursor);
}

Rect GTextEditor::line_content_rect(int line_index) const
{
    auto& line = lines()[line_index];
    if (is_single_line()) {
        Rect line_rect = { content_x_for_position({ line_index, 0 }), 0, line.length() * glyph_width(), font().glyph_height() + 2 };
        line_rect.center_vertically_within({ {}, frame_inner_rect().size() });
        return line_rect;
    }
    if (is_line_wrapping_enabled())
        return m_line_visual_data[line_index].visual_rect;
    return {
        content_x_for_position({ line_index, 0 }),
        line_index * line_height(),
        line.length() * glyph_width(),
        line_height()
    };
}

void GTextEditor::update_cursor()
{
    update(line_widget_rect(m_cursor.line()));
}

void GTextEditor::update_undo_timer()
{
    if (m_undo_stack.is_empty())
        return;

    auto& undo_vector = m_undo_stack[0].m_undo_vector;

    if (undo_vector.size() == m_last_updated_undo_vector_size && !undo_vector.is_empty()) {
        auto undo_commands_container = make<UndoCommandsContainer>();
        m_undo_stack.prepend(move(undo_commands_container));
        // Note: Remove dbg() if we're 100% sure there are no bugs left.
        dbg() << "Undo stack increased to " << m_undo_stack.size();

        // Shift the index to the left since we're adding an empty container.
        if (m_undo_stack_index > 0)
            m_undo_stack_index++;
    }

    m_last_updated_undo_vector_size = undo_vector.size();
}

void GTextEditor::set_cursor(int line, int column)
{
    set_cursor({ line, column });
}

void GTextEditor::set_cursor(const GTextPosition& a_position)
{
    ASSERT(!lines().is_empty());

    GTextPosition position = a_position;

    if (position.line() >= lines().size())
        position.set_line(lines().size() - 1);

    if (position.column() > lines()[position.line()].length())
        position.set_column(lines()[position.line()].length());

    if (m_cursor != position) {
        // NOTE: If the old cursor is no longer valid, repaint everything just in case.
        auto old_cursor_line_rect = m_cursor.line() < lines().size()
            ? line_widget_rect(m_cursor.line())
            : rect();
        m_cursor = position;
        m_cursor_state = true;
        scroll_cursor_into_view();
        update(old_cursor_line_rect);
        update_cursor();
    }
    if (on_cursor_change)
        on_cursor_change();
}

void GTextEditor::focusin_event(CEvent&)
{
    update_cursor();
    start_timer(500);
}

void GTextEditor::focusout_event(CEvent&)
{
    stop_timer();
}

void GTextEditor::timer_event(CTimerEvent&)
{
    m_cursor_state = !m_cursor_state;
    if (is_focused())
        update_cursor();
}

bool GTextEditor::write_to_file(const StringView& path)
{
    int fd = open_with_path_length(path.characters_without_null_termination(), path.length(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
    if (fd < 0) {
        perror("open");
        return false;
    }

    // Compute the final file size and ftruncate() to make writing fast.
    // FIXME: Remove this once the kernel is smart enough to do this instead.
    off_t file_size = 0;
    for (int i = 0; i < lines().size(); ++i)
        file_size += lines()[i].length();
    file_size += lines().size() - 1;

    int rc = ftruncate(fd, file_size);
    if (rc < 0) {
        perror("ftruncate");
        return false;
    }

    for (int i = 0; i < lines().size(); ++i) {
        auto& line = lines()[i];
        if (line.length()) {
            ssize_t nwritten = write(fd, line.characters(), line.length());
            if (nwritten < 0) {
                perror("write");
                close(fd);
                return false;
            }
        }
        if (i != lines().size() - 1) {
            char ch = '\n';
            ssize_t nwritten = write(fd, &ch, 1);
            if (nwritten != 1) {
                perror("write");
                close(fd);
                return false;
            }
        }
    }

    close(fd);
    return true;
}

String GTextEditor::text() const
{
    StringBuilder builder;
    for (int i = 0; i < line_count(); ++i) {
        auto& line = lines()[i];
        builder.append(line.characters(), line.length());
        if (i != line_count() - 1)
            builder.append('\n');
    }
    return builder.to_string();
}

void GTextEditor::clear()
{
    document().remove_all_lines();
    document().append_line(make<GTextDocumentLine>(document()));
    m_selection.clear();
    did_update_selection();
    set_cursor(0, 0);
    update();
}

String GTextEditor::selected_text() const
{
    if (!has_selection())
        return {};

    return document().text_in_range(m_selection);
}

void GTextEditor::delete_selection()
{
    if (!has_selection())
        return;

    auto selection = normalized_selection();

    // First delete all the lines in between the first and last one.
    for (int i = selection.start().line() + 1; i < selection.end().line();) {
        int row = i;
        int column = lines()[i].length();
        add_to_undo_stack(make<RemoveLineCommand>(*this, String(lines()[i].view()), GTextPosition(row, column), false));

        document().remove_line(i);
        selection.end().set_line(selection.end().line() - 1);
    }

    if (selection.start().line() == selection.end().line()) {
        // Delete within same line.
        auto& line = lines()[selection.start().line()];
        bool whole_line_is_selected = selection.start().column() == 0 && selection.end().column() == line.length();

        for (int i = selection.end().column() - 1; i >= selection.start().column(); i--) {
            int row = selection.start().line();
            int column = i;
            add_to_undo_stack(make<RemoveCharacterCommand>(*this, document().line(row).characters()[column], GTextPosition(row, column)));
        }

        if (whole_line_is_selected) {
            line.clear(document());
        } else {
            auto before_selection = String(line.characters(), line.length()).substring(0, selection.start().column());
            auto after_selection = String(line.characters(), line.length()).substring(selection.end().column(), line.length() - selection.end().column());
            StringBuilder builder(before_selection.length() + after_selection.length());
            builder.append(before_selection);
            builder.append(after_selection);
            line.set_text(document(), builder.to_string());
        }
    } else {
        // Delete across a newline, merging lines.
        ASSERT(selection.start().line() == selection.end().line() - 1);
        auto& first_line = lines()[selection.start().line()];
        auto& second_line = lines()[selection.end().line()];
        auto before_selection = String(first_line.characters(), first_line.length()).substring(0, selection.start().column());
        auto after_selection = String(second_line.characters(), second_line.length()).substring(selection.end().column(), second_line.length() - selection.end().column());
        StringBuilder builder(before_selection.length() + after_selection.length());
        builder.append(before_selection);
        builder.append(after_selection);

        for (int i = first_line.length() - 1; i > selection.start().column() - 1; i--) {
            int row = selection.start().line();
            int column = i;
            add_to_undo_stack(make<RemoveCharacterCommand>(*this, document().line(row).characters()[column], GTextPosition(row, column)));
        }

        add_to_undo_stack(make<RemoveLineCommand>(*this, String(second_line.view()), selection.end(), false));

        first_line.set_text(document(), builder.to_string());
        document().remove_line(selection.end().line());

        for (int i = (first_line.length()) - after_selection.length(); i < first_line.length(); i++)
            add_to_undo_stack(make<InsertCharacterCommand>(*this, first_line.characters()[i], GTextPosition(selection.start().line(), i + 1)));
    }

    if (lines().is_empty()) {
        document().append_line(make<GTextDocumentLine>(document()));
    }

    m_selection.clear();
    did_update_selection();
    did_change();
    set_cursor(selection.start());
    update();
}

void GTextEditor::insert_at_cursor_or_replace_selection(const StringView& text)
{
    ASSERT(!is_readonly());
    if (has_selection())
        delete_selection();
    insert_at_cursor(text);
}

void GTextEditor::cut()
{
    if (is_readonly())
        return;
    auto selected_text = this->selected_text();
    printf("Cut: \"%s\"\n", selected_text.characters());
    GClipboard::the().set_data(selected_text);
    delete_selection();
}

void GTextEditor::copy()
{
    auto selected_text = this->selected_text();
    printf("Copy: \"%s\"\n", selected_text.characters());
    GClipboard::the().set_data(selected_text);
}

void GTextEditor::paste()
{
    if (is_readonly())
        return;
    auto paste_text = GClipboard::the().data();
    printf("Paste: \"%s\"\n", paste_text.characters());
    insert_at_cursor_or_replace_selection(paste_text);
}

void GTextEditor::enter_event(CEvent&)
{
    ASSERT(window());
    window()->set_override_cursor(GStandardCursor::IBeam);
}

void GTextEditor::leave_event(CEvent&)
{
    ASSERT(window());
    window()->set_override_cursor(GStandardCursor::None);
}

void GTextEditor::did_change()
{
    ASSERT(!is_readonly());
    update_content_size();
    recompute_all_visual_lines();
    m_undo_action->set_enabled(can_undo());
    m_redo_action->set_enabled(can_redo());
    if (!m_has_pending_change_notification) {
        m_has_pending_change_notification = true;
        deferred_invoke([this](auto&) {
            if (!m_has_pending_change_notification)
                return;
            if (on_change)
                on_change();
            m_has_pending_change_notification = false;
        });
    }
}

void GTextEditor::set_readonly(bool readonly)
{
    if (m_readonly == readonly)
        return;
    m_readonly = readonly;
    m_cut_action->set_enabled(!is_readonly() && has_selection());
    m_delete_action->set_enabled(!is_readonly());
    m_paste_action->set_enabled(!is_readonly());
}

void GTextEditor::did_update_selection()
{
    m_cut_action->set_enabled(!is_readonly() && has_selection());
    m_copy_action->set_enabled(has_selection());
    if (on_selection_change)
        on_selection_change();
    if (is_line_wrapping_enabled()) {
        // FIXME: Try to repaint less.
        update();
    }
}

void GTextEditor::context_menu_event(GContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = make<GMenu>();
        m_context_menu->add_action(undo_action());
        m_context_menu->add_action(redo_action());
        m_context_menu->add_separator();
        m_context_menu->add_action(cut_action());
        m_context_menu->add_action(copy_action());
        m_context_menu->add_action(paste_action());
        m_context_menu->add_action(delete_action());
        if (!m_custom_context_menu_actions.is_empty()) {
            m_context_menu->add_separator();
            for (auto& action : m_custom_context_menu_actions) {
                m_context_menu->add_action(action);
            }
        }
    }
    m_context_menu->popup(event.screen_position());
}

void GTextEditor::set_text_alignment(TextAlignment alignment)
{
    if (m_text_alignment == alignment)
        return;
    m_text_alignment = alignment;
    update();
}

void GTextEditor::resize_event(GResizeEvent& event)
{
    GScrollableWidget::resize_event(event);
    update_content_size();
    recompute_all_visual_lines();
}

void GTextEditor::set_selection(const GTextRange& selection)
{
    if (m_selection == selection)
        return;
    m_selection = selection;
    set_cursor(m_selection.end());
    scroll_position_into_view(normalized_selection().start());
    update();
}

void GTextEditor::recompute_all_visual_lines()
{
    int y_offset = 0;
    for (int line_index = 0; line_index < line_count(); ++line_index) {
        recompute_visual_lines(line_index);
        m_line_visual_data[line_index].visual_rect.set_y(y_offset);
        y_offset += m_line_visual_data[line_index].visual_rect.height();
    }

    update_content_size();
}

void GTextEditor::ensure_cursor_is_valid()
{
    if (cursor().column() > lines()[cursor().line()].length())
        set_cursor(cursor().line(), cursor().column() - (lines()[cursor().line()].length() - cursor().column()));
}

void GTextEditor::add_to_undo_stack(NonnullOwnPtr<UndoCommand> undo_command)
{
    if (m_undo_stack.is_empty()) {
        auto undo_commands_container = make<UndoCommandsContainer>();
        m_undo_stack.prepend(move(undo_commands_container));
    }

    // Clear the elements of the stack before the m_undo_stack_index (Excluding our new element)
    for (int i = 1; i < m_undo_stack_index; i++)
        m_undo_stack.remove(1);

    if (m_undo_stack_index > 0 && !m_undo_stack.is_empty())
        m_undo_stack[0].m_undo_vector.clear();

    m_undo_stack_index = 0;

    m_undo_stack[0].m_undo_vector.prepend(move(undo_command));
}

int GTextEditor::visual_line_containing(int line_index, int column) const
{
    int visual_line_index = 0;
    for_each_visual_line(line_index, [&](const Rect&, const StringView& view, int start_of_visual_line) {
        if (column >= start_of_visual_line && ((column - start_of_visual_line) < view.length()))
            return IterationDecision::Break;
        ++visual_line_index;
        return IterationDecision::Continue;
    });
    return visual_line_index;
}

void GTextEditor::recompute_visual_lines(int line_index)
{
    auto& line = document().line(line_index);
    auto& visual_data = m_line_visual_data[line_index];

    visual_data.visual_line_breaks.clear_with_capacity();

    int available_width = visible_text_rect_in_inner_coordinates().width();

    if (is_line_wrapping_enabled()) {
        int line_width_so_far = 0;

        for (int i = 0; i < line.length(); ++i) {
            auto ch = line.characters()[i];
            auto glyph_width = font().glyph_width(ch);
            if ((line_width_so_far + glyph_width) > available_width) {
                visual_data.visual_line_breaks.append(i);
                line_width_so_far = glyph_width;
                continue;
            }
            line_width_so_far += glyph_width;
        }
    }

    visual_data.visual_line_breaks.append(line.length());

    if (is_line_wrapping_enabled())
        visual_data.visual_rect = { m_horizontal_content_padding, 0, available_width, visual_data.visual_line_breaks.size() * line_height() };
    else
        visual_data.visual_rect = { m_horizontal_content_padding, 0, font().width(line.view()), line_height() };
}

template<typename Callback>
void GTextEditor::for_each_visual_line(int line_index, Callback callback) const
{
    auto editor_visible_text_rect = visible_text_rect_in_inner_coordinates();
    int start_of_line = 0;
    int visual_line_index = 0;

    auto& line = document().line(line_index);
    auto& visual_data = m_line_visual_data[line_index];

    for (auto visual_line_break : visual_data.visual_line_breaks) {
        auto visual_line_view = StringView(line.characters() + start_of_line, visual_line_break - start_of_line);
        Rect visual_line_rect {
            visual_data.visual_rect.x(),
            visual_data.visual_rect.y() + (visual_line_index * line_height()),
            font().width(visual_line_view),
            line_height()
        };
        if (is_right_text_alignment(text_alignment()))
            visual_line_rect.set_right_without_resize(editor_visible_text_rect.right());
        if (!is_multi_line())
            visual_line_rect.center_vertically_within(editor_visible_text_rect);
        if (callback(visual_line_rect, visual_line_view, start_of_line) == IterationDecision::Break)
            break;
        start_of_line = visual_line_break;
        ++visual_line_index;
    }
}

void GTextEditor::set_line_wrapping_enabled(bool enabled)
{
    if (m_line_wrapping_enabled == enabled)
        return;

    m_line_wrapping_enabled = enabled;
    horizontal_scrollbar().set_visible(!m_line_wrapping_enabled);
    update_content_size();
    recompute_all_visual_lines();
    update();
}

void GTextEditor::add_custom_context_menu_action(GAction& action)
{
    m_custom_context_menu_actions.append(action);
}

void GTextEditor::did_change_font()
{
    vertical_scrollbar().set_step(line_height());
    GWidget::did_change_font();
}

void GTextEditor::document_did_append_line()
{
    m_line_visual_data.append(make<LineVisualData>());
    recompute_all_visual_lines();
    update();
}

void GTextEditor::document_did_remove_line(int line_index)
{
    m_line_visual_data.remove(line_index);
    recompute_all_visual_lines();
    update();
}

void GTextEditor::document_did_remove_all_lines()
{
    m_line_visual_data.clear();
    recompute_all_visual_lines();
    update();
}

void GTextEditor::document_did_insert_line(int line_index)
{
    m_line_visual_data.insert(line_index, make<LineVisualData>());
    recompute_all_visual_lines();
    update();
}

void GTextEditor::document_did_change()
{
    recompute_all_visual_lines();
    update();
}

void GTextEditor::set_document(GTextDocument& document)
{
    if (m_document.ptr() == &document)
        return;
    if (m_document)
        m_document->unregister_client(*this);
    m_document = document;
    m_line_visual_data.clear();
    for (int i = 0; i < m_document->line_count(); ++i) {
        m_line_visual_data.append(make<LineVisualData>());
    }
    m_cursor = { 0, 0 };
    recompute_all_visual_lines();
    update();
    m_document->register_client(*this);
}

GTextEditor::UndoCommand::UndoCommand(GTextEditor& text_editor)
    : m_text_editor(text_editor)
{
}

GTextEditor::UndoCommand::~UndoCommand()
{
}

void GTextEditor::UndoCommand::undo() {}
void GTextEditor::UndoCommand::redo() {}

GTextEditor::InsertCharacterCommand::InsertCharacterCommand(GTextEditor& text_editor, char ch, GTextPosition text_position)
    : UndoCommand(text_editor)
    , m_character(ch)
    , m_text_position(text_position)
{
}

GTextEditor::RemoveCharacterCommand::RemoveCharacterCommand(GTextEditor& text_editor, char ch, GTextPosition text_position)
    : UndoCommand(text_editor)
    , m_character(ch)
    , m_text_position(text_position)
{
}

GTextEditor::RemoveLineCommand::RemoveLineCommand(GTextEditor& text_editor, String line_content, GTextPosition text_position, bool has_merged_content)
    : UndoCommand(text_editor)
    , m_line_content(line_content)
    , m_text_position(text_position)
    , m_has_merged_content(has_merged_content)
{
}

GTextEditor::CreateLineCommand::CreateLineCommand(GTextEditor& text_editor, Vector<char> line_content, GTextPosition text_position)
    : UndoCommand(text_editor)
    , m_line_content(line_content)
    , m_text_position(text_position)
{
}

void GTextEditor::InsertCharacterCommand::undo()
{
    m_text_editor.lines()[m_text_position.line()].remove(m_text_editor.document(), (m_text_position.column() - 1));
    m_text_editor.ensure_cursor_is_valid();
}

void GTextEditor::InsertCharacterCommand::redo()
{
    m_text_editor.lines()[m_text_position.line()].insert(m_text_editor.document(), m_text_position.column() - 1, m_character);
}

void GTextEditor::RemoveCharacterCommand::undo()
{
    m_text_editor.lines()[m_text_position.line()].insert(m_text_editor.document(), m_text_position.column(), m_character);
}

void GTextEditor::RemoveCharacterCommand::redo()
{
    m_text_editor.lines()[m_text_position.line()].remove(m_text_editor.document(), (m_text_position.column()));
    m_text_editor.ensure_cursor_is_valid();
}

void GTextEditor::RemoveLineCommand::undo()
{
    // Insert back the line
    m_text_editor.document().insert_line(m_text_position.line(), make<GTextDocumentLine>(m_text_editor.document(), m_line_content));

    // Remove the merged line contents
    if (m_has_merged_content) {
        for (int i = m_line_content.length() - 1; i >= 0; i--)
            m_text_editor.document().lines()[m_text_position.line() - 1].remove(m_text_editor.document(), (m_text_position.column()) + i);
    }
}

void GTextEditor::RemoveLineCommand::redo()
{
    // Remove the created line
    m_text_editor.document().remove_line(m_text_position.line());

    // Add back the line contents
    if (m_has_merged_content) {
        for (int i = 0; i < m_line_content.length(); i++)
            m_text_editor.document().lines()[m_text_position.line() - 1].insert(m_text_editor.document(), (m_text_position.column()) + i, m_line_content[i]);
    }
}

void GTextEditor::CreateLineCommand::undo()
{
    // Insert back the created line portion
    for (int i = 0; i < m_line_content.size(); i++)
        m_text_editor.document().lines()[m_text_position.line()].insert(m_text_editor.document(), (m_text_position.column() - 1) + i, m_line_content[i]);

    // Move the cursor up a row back before the split.
    m_text_editor.set_cursor(m_text_position.line(), m_text_editor.document().lines()[m_text_position.line()].length());

    // Remove the created line
    m_text_editor.document().remove_line(m_text_position.line() + 1);
}

void GTextEditor::CreateLineCommand::redo()
{
    // Remove the characters that we're inserted back
    for (int i = m_line_content.size() - 1; i >= 0; i--)
        m_text_editor.document().lines()[m_text_position.line()].remove(m_text_editor.document(), (m_text_position.column()) + i);

    m_text_editor.ensure_cursor_is_valid();

    // Then we want to add BACK the created line
    m_text_editor.document().insert_line(m_text_position.line() + 1, make<GTextDocumentLine>(m_text_editor.document(), ""));

    for (int i = 0; i < m_line_content.size(); i++)
        m_text_editor.document().lines()[m_text_position.line() + 1].insert(m_text_editor.document(), i, m_line_content[i]);
}

void GTextEditor::flush_pending_change_notification_if_needed()
{
    if (!m_has_pending_change_notification)
        return;
    if (on_change)
        on_change();
    m_has_pending_change_notification = false;
}
