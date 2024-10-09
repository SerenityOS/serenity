/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibGUI/Event.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/VimEditingEngine.h>
#include <string.h>

namespace GUI {

void VimCursor::move()
{
    if (m_forwards)
        move_forwards();
    else
        move_backwards();
}

void VimCursor::move_reverse()
{
    if (m_forwards)
        move_backwards();
    else
        move_forwards();
}

u32 VimCursor::peek()
{
    TextPosition saved_position = m_position;

    move();
    u32 peeked = current_char();

    m_position = saved_position;
    return peeked;
}

u32 VimCursor::peek_reverse()
{
    TextPosition saved_position = m_position;

    move_reverse();
    u32 peeked = current_char();

    m_position = saved_position;
    return peeked;
}

TextDocumentLine& VimCursor::current_line()
{
    return m_editor.line(m_position.line());
}

u32 VimCursor::current_char()
{
    if (on_empty_line()) {
        // Fails all of isspace, ispunct, isalnum so should be good.
        return 0;
    } else {
        return current_line().view().code_points()[m_position.column()];
    }
}

bool VimCursor::on_empty_line()
{
    return current_line().length() == 0;
}

bool VimCursor::will_cross_line_boundary()
{
    if (on_empty_line())
        return true;
    else if (m_forwards && m_position.column() == current_line().length() - 1)
        return true;
    else if (!m_forwards && m_position.column() == 0)
        return true;
    else
        return false;
}

void VimCursor::move_forwards()
{
    if (on_empty_line() || m_position.column() == current_line().length() - 1) {
        if (m_position.line() == m_editor.line_count() - 1) {
            // We have reached the end of the document, so any other
            // forward movements are no-ops.
            m_hit_edge = true;
        } else {
            m_position.set_column(0);
            m_position.set_line(m_position.line() + 1);
            m_crossed_line_boundary = true;
        }
    } else {
        m_position.set_column(m_position.column() + 1);
        m_crossed_line_boundary = false;
    }
}

void VimCursor::move_backwards()
{
    if (m_position.column() == 0) {
        if (m_position.line() == 0) {
            // We have reached the start of the document, so any other
            // backward movements are no-ops.
            m_hit_edge = true;
        } else {
            m_position.set_line(m_position.line() - 1);
            if (!on_empty_line())
                m_position.set_column(current_line().length() - 1);
            else
                m_position.set_column(0);
            m_crossed_line_boundary = true;
        }
    } else {
        m_position.set_column(m_position.column() - 1);
        m_crossed_line_boundary = false;
    }
}

void VimMotion::add_key_code(KeyCode key, [[maybe_unused]] bool ctrl, bool shift, [[maybe_unused]] bool alt)
{
    if (is_complete())
        return;

    if (m_find_mode != FindMode::None) {
        // We need to consume the next character because we are going to find
        // until that character.

        // HACK: there is no good way to obtain whether a character is alphanumeric
        // from the keycode itself.
        char const* keycode_str = key_code_to_string(key);

        if (strlen(keycode_str) == 1 && (isalpha(keycode_str[0]) || isspace(keycode_str[0]))) {
            m_next_character = tolower(keycode_str[0]);
            m_unit = Unit::Find;
        } else {
            m_unit = Unit::Unknown;
        }

        m_is_complete = true;
        m_should_consume_next_character = false;
        return;
    }

    bool should_use_guirky = m_guirky_mode;

    switch (key) {
#define DIGIT(n)                        \
    case KeyCode::Key_##n:              \
        m_amount = (m_amount * 10) + n; \
        break

        // Digits add digits to the amount.
        DIGIT(1);
        DIGIT(2);
        DIGIT(3);
        DIGIT(4);
        DIGIT(5);
        DIGIT(6);
        DIGIT(7);
        DIGIT(8);
        DIGIT(9);

#undef DIGIT

    // Home means to the beginning of the line.
    case KeyCode::Key_Home:
        m_unit = Unit::Character;
        m_amount = START_OF_LINE;
        m_is_complete = true;
        break;

    // If 0 appears while amount is 0, then it means beginning of line.
    // Otherwise, it adds 0 to the amount.
    case KeyCode::Key_0:
        if (m_amount == 0) {
            m_unit = Unit::Character;
            m_amount = START_OF_LINE;
            m_is_complete = true;
        } else {
            m_amount = m_amount * 10;
        }
        break;

    // End or $ means end of line.
    // TODO: d2$ in vim deletes to the end of the line and then the next line.
    case KeyCode::Key_End:
    case KeyCode::Key_Dollar:
        m_unit = Unit::Character;
        m_amount = END_OF_LINE;
        m_is_complete = true;
        break;

    // ^ means the first non-whitespace character for this line.
    // It deletes backwards if you're in front of it, and forwards if you're behind.
    case KeyCode::Key_Circumflex:
        m_unit = Unit::Character;
        m_amount = START_OF_NON_WHITESPACE;
        m_is_complete = true;
        break;

    // j, down or + operates on this line and amount line(s) after.
    case KeyCode::Key_J:
    case KeyCode::Key_Down:
    case KeyCode::Key_Plus:
        m_unit = Unit::Line;

        if (m_amount == 0)
            m_amount = 1;

        m_is_complete = true;
        break;

    // k, up or - operates on this line and amount line(s) before.
    case KeyCode::Key_K:
    case KeyCode::Key_Up:
    case KeyCode::Key_Minus:
        m_unit = Unit::Line;

        if (m_amount == 0)
            m_amount = -1;
        else
            m_amount = -m_amount;

        m_is_complete = true;
        break;

    // BS, h or left operates on this character and amount character(s) before.
    case KeyCode::Key_Backspace:
    case KeyCode::Key_H:
    case KeyCode::Key_Left:
        m_unit = Unit::Character;

        if (m_amount == 0)
            m_amount = -1;
        else
            m_amount = -m_amount;

        m_is_complete = true;
        break;

    // l or right operates on this character and amount character(s) after.
    case KeyCode::Key_L:
    case KeyCode::Key_Right:
        m_unit = Unit::Character;

        if (m_amount > 0)
            m_amount--;

        m_is_complete = true;
        break;

    // w operates on amount word(s) after.
    // W operates on amount WORD(s) after.
    case KeyCode::Key_W:
        if (shift)
            m_unit = Unit::WORD;
        else
            m_unit = Unit::Word;

        if (m_amount == 0)
            m_amount = 1;

        m_is_complete = true;
        break;

    // b operates on amount word(s) before.
    // B operates on amount WORD(s) before.
    case KeyCode::Key_B:
        if (shift)
            m_unit = Unit::WORD;
        else
            m_unit = Unit::Word;

        if (m_amount == 0)
            m_amount = -1;
        else
            m_amount = -m_amount;

        m_is_complete = true;
        break;

    // e operates on amount of word(s) after, till the end of the last word.
    // E operates on amount of WORD(s) after, till the end of the last WORD.
    // ge operates on amount of word(s) before, till the end of the last word.
    // gE operates on amount of WORD(s) before, till the end of the last WORD.
    case KeyCode::Key_E:
        if (shift)
            m_unit = Unit::EndOfWORD;
        else
            m_unit = Unit::EndOfWord;

        if (m_guirky_mode) {
            if (m_amount == 0)
                m_amount = -1;
            else
                m_amount = -m_amount;

            m_guirky_mode = false;
        } else {
            if (m_amount == 0)
                m_amount = 1;
        }

        m_is_complete = true;
        break;

    // g enables guirky (g-prefix commands) mode.
    // gg operates from the start of the document to the cursor.
    // G operates from the cursor to the end of the document.
    case KeyCode::Key_G:
        if (m_guirky_mode) {
            if (shift) {
                // gG is not a valid command in vim.
                m_guirky_mode = false;
                m_unit = Unit::Unknown;
                m_is_complete = true;
            } else {
                m_guirky_mode = false;
                m_unit = Unit::Document;
                m_amount = -1;
                m_is_complete = true;
            }
        } else {
            if (shift) {
                m_unit = Unit::Document;
                m_amount = 1;
                m_is_complete = true;
            } else {
                m_guirky_mode = true;
            }
        }
        break;

    // t operates until the given character.
    case KeyCode::Key_T:
        m_find_mode = FindMode::To;
        m_should_consume_next_character = true;

        if (m_amount == 0)
            m_amount = 1;
        break;

    // f operates through the given character.
    case KeyCode::Key_F:
        m_find_mode = FindMode::Find;
        m_should_consume_next_character = true;

        if (m_amount == 0)
            m_amount = 1;
        break;

    default:
        m_unit = Unit::Unknown;
        m_is_complete = true;
        break;
    }

    if (should_use_guirky && m_guirky_mode) {
        // If we didn't use the g then we cancel the motion.
        m_guirky_mode = false;
        m_unit = Unit::Unknown;
        m_is_complete = true;
    }
}

Optional<TextRange> VimMotion::get_range(VimEditingEngine& engine, bool normalize_for_position)
{
    if (!is_complete() || is_cancelled())
        return {};

    TextEditor& editor = engine.editor();

    auto position = editor.cursor();
    int amount = abs(m_amount);
    bool forwards = m_amount >= 0;
    VimCursor cursor { editor, position, forwards };

    m_start_line = m_end_line = position.line();
    m_start_column = m_end_column = position.column();

    switch (m_unit) {
    case Unit::Unknown:
        VERIFY_NOT_REACHED();
    case Unit::Document: {
        calculate_document_range(editor);
        break;
    }
    case Unit::Line: {
        calculate_line_range(editor, normalize_for_position);
        break;
    }
    case Unit::EndOfWord:
    case Unit::Word:
    case Unit::EndOfWORD:
    case Unit::WORD: {
        calculate_word_range(cursor, amount, normalize_for_position);
        break;
    }
    case Unit::Character: {
        calculate_character_range(cursor, amount, normalize_for_position);
        break;
    }
    case Unit::Find: {
        calculate_find_range(cursor, amount);
        break;
    }
    }

    return { TextRange { { m_start_line, m_start_column }, { m_end_line, m_end_column } } };
}

Optional<TextRange> VimMotion::get_repeat_range(VimEditingEngine& engine, VimMotion::Unit unit, bool normalize_for_position)
{
    TextEditor& editor = engine.editor();

    if (m_amount > 0) {
        m_amount--;
    } else if (m_amount < 0) {
        m_amount++;
    }
    auto position = editor.cursor();
    int amount = abs(m_amount);
    bool forwards = m_amount >= 0;
    VimCursor cursor { editor, position, forwards };

    m_start_line = m_end_line = position.line();
    m_start_column = m_end_column = position.column();

    switch (unit) {
    case Unit::Line: {
        calculate_line_range(editor, normalize_for_position);
        break;
    }
    case Unit::Character: {
        calculate_character_range(cursor, amount, normalize_for_position);
        break;
    }
    default:
        return {};
    }

    return { TextRange { { m_start_line, m_start_column }, { m_end_line, m_end_column } } };
}

void VimMotion::calculate_document_range(TextEditor& editor)
{
    if (m_amount >= 0) {
        m_end_line = editor.line_count() - 1;
        auto& last_line = editor.line(m_end_line);
        m_end_column = last_line.length();
    } else {
        m_start_line = 0;
        m_start_column = 0;
    }
}

void VimMotion::calculate_line_range(TextEditor& editor, bool normalize_for_position)
{
    // Use this line +/- m_amount lines.
    m_start_column = 0;
    m_end_column = 0;

    if (m_amount >= 0) {
        m_end_line = min(m_end_line + !normalize_for_position + m_amount, editor.line_count());

        // We can't delete to "last line + 1", so if we're on the last line,
        // delete until the end.
        if (m_end_line == editor.line_count()) {
            m_end_line--;
            m_end_column = editor.line(m_end_line).length();
        }
    } else {
        // Can't write it as max(start_line + m_amount, 0) because of unsigned
        // shenanigans.
        if (m_start_line <= (unsigned)-m_amount)
            m_start_line = 0;
        else
            m_start_line += m_amount;

        if (m_end_line == editor.line_count() - 1)
            m_end_column = editor.line(m_end_line).length();
        else
            m_end_line++;
    }
}

void VimMotion::calculate_word_range(VimCursor& cursor, int amount, bool normalize_for_position)
{
    enum {
        Whitespace,
        Word,
        Punctuation,
        Unknown
    };
    // Word is defined as a-zA-Z0-9_.
    auto part_of_word = [](u32 ch) { return ch == '_' || isalnum(ch); };
    auto part_of_punctuation = [](u32 ch) { return ch != '_' && ispunct(ch); };
    auto classify = [&](u32 ch) {
        if (isspace(ch))
            return Whitespace;
        else if (part_of_word(ch))
            return Word;
        else if (part_of_punctuation(ch))
            return Punctuation;
        else
            return Unknown;
    };

    // A small explanation for the code below: Because the direction of the
    // movement for this motion determines what the "start" and "end" of a word
    // is, the code below treats the motions like so:
    // - Start of word: w/W/ge/gE
    // - End of word: e/E/b/B

    while (amount > 0) {
        if (cursor.hit_edge())
            break;

        if ((!cursor.forwards() && (m_unit == Unit::Word || m_unit == Unit::WORD))
            || (cursor.forwards() && (m_unit == Unit::EndOfWord || m_unit == Unit::EndOfWORD))) {
            // End-of-word motions peek at the "next" character and if its class
            // is not the same as ours, they move over one character (to end up
            // at the new character class). This is required because we don't
            // want to exit the word with end-of-word motions.

            if (m_unit == Unit::Word || m_unit == Unit::EndOfWord) {
                // Word-style peeking
                int current_class = classify(cursor.current_char());
                int peeked_class = classify(cursor.peek());
                if (current_class != peeked_class) {
                    cursor.move();
                }
            } else {
                // WORD-style peeking, much simpler
                if (isspace(cursor.peek())) {
                    cursor.move();
                }
            }
        } else {
            // Start-of-word motions want to exit the word no matter which part
            // of it we're in.
            if (m_unit == Unit::Word || m_unit == Unit::EndOfWord) {
                // Word-style consumption
                if (part_of_word(cursor.current_char())) {
                    do {
                        cursor.move();
                        if (cursor.hit_edge() || cursor.crossed_line_boundary())
                            break;
                    } while (part_of_word(cursor.current_char()));
                } else if (part_of_punctuation(cursor.current_char())) {
                    do {
                        cursor.move();
                        if (cursor.hit_edge() || cursor.crossed_line_boundary())
                            break;
                    } while (part_of_punctuation(cursor.current_char()));
                } else if (cursor.on_empty_line()) {
                    cursor.move();
                }
            } else {
                // WORD-style consumption
                if (!isspace(cursor.current_char())) {
                    do {
                        cursor.move();
                        if (cursor.hit_edge() || cursor.crossed_line_boundary())
                            break;
                    } while (!isspace(cursor.current_char()));
                } else if (cursor.on_empty_line()) {
                    cursor.move();
                }
            }
        }

        // Now consume any space if it exists.
        if (isspace(cursor.current_char())) {
            do {
                cursor.move();
                if (cursor.hit_edge())
                    break;
            } while (isspace(cursor.current_char()));
        }

        if ((!cursor.forwards() && (m_unit == Unit::Word || m_unit == Unit::WORD))
            || (cursor.forwards() && (m_unit == Unit::EndOfWord || m_unit == Unit::EndOfWORD))) {
            // End-of-word motions consume until the class doesn't match.

            if (m_unit == Unit::Word || m_unit == Unit::EndOfWord) {
                // Word-style consumption
                int current_class = classify(cursor.current_char());
                while (classify(cursor.current_char()) == current_class) {
                    cursor.move();
                    if (cursor.hit_edge() || cursor.crossed_line_boundary())
                        break;
                }
            } else {
                // WORD-style consumption
                while (!isspace(cursor.current_char())) {
                    cursor.move();
                    if (cursor.hit_edge() || cursor.crossed_line_boundary())
                        break;
                }
            }
        }

        amount--;
    }

    // If we need to normalize for position then we do a move_reverse for
    // end-of-word motions, because vim acts on end-of-word ranges through the
    // character your cursor is placed on but acts on start-of-words *until* the
    // character your cursor is placed on.
    if (normalize_for_position) {
        if ((!cursor.forwards() && (m_unit == Unit::Word || m_unit == Unit::WORD))
            || (cursor.forwards() && (m_unit == Unit::EndOfWord || m_unit == Unit::EndOfWORD))) {
            if (!cursor.hit_edge())
                cursor.move_reverse();
        }
    }

    if (cursor.forwards()) {
        m_end_line = cursor.current_position().line();
        m_end_column = cursor.current_position().column() + normalize_for_position;
    } else {
        m_start_line = cursor.current_position().line();
        m_start_column = cursor.current_position().column();
    }
}

void VimMotion::calculate_character_range(VimCursor& cursor, int amount, bool normalize_for_position)
{
    if (m_amount == START_OF_LINE) {
        m_start_column = 0;
    } else if (m_amount == END_OF_LINE) {
        m_end_column = cursor.current_line().length();
    } else if (m_amount == START_OF_NON_WHITESPACE) {
        // Find the first non-whitespace character and set the range from current
        // position to it.
        TextPosition cursor_copy = cursor.current_position();
        cursor.current_position().set_column(0);

        while (isspace(cursor.current_char())) {
            if (cursor.will_cross_line_boundary())
                break;

            cursor.move_forwards();
        }

        if (cursor_copy < cursor.current_position())
            m_end_column = cursor.current_position().column() + 1;
        else
            m_start_column = cursor.current_position().column();
    } else {
        while (amount > 0) {
            if (cursor.hit_edge() || cursor.will_cross_line_boundary())
                break;

            cursor.move();
            amount--;
        }

        if (cursor.forwards()) {
            m_end_column = cursor.current_position().column() + 1 + normalize_for_position;
        } else {
            m_start_column = cursor.current_position().column();
        }
    }
}

void VimMotion::calculate_find_range(VimCursor& cursor, int amount)
{
    // Find the searched character (case-insensitive).
    while (amount > 0) {
        cursor.move_forwards();

        while ((unsigned)tolower(cursor.current_char()) != m_next_character) {
            if (cursor.will_cross_line_boundary())
                break;

            cursor.move_forwards();
        }

        amount--;
    }

    // If we didn't find our character before reaching the end of the line, then
    // we want the range to be invalid so no operation is performed.
    if ((unsigned)tolower(cursor.current_char()) == m_next_character) {
        // We found our character.
        bool in_find_mode = m_find_mode == FindMode::Find;
        m_end_column = cursor.current_position().column() + in_find_mode;
    }

    m_find_mode = FindMode::None;
}

Optional<TextPosition> VimMotion::get_position(VimEditingEngine& engine, bool in_visual_mode)
{
    auto range_optional = get_range(engine, true);
    if (!range_optional.has_value())
        return {};

    auto range = range_optional.value();
    if (!range.is_valid())
        return {};

    TextEditor& editor = engine.editor();
    auto cursor_position = editor.cursor();

    switch (m_unit) {
    case Unit::Document: {
        if (range.start().line() < cursor_position.line()) {
            cursor_position.set_line(range.start().line());
        } else {
            cursor_position.set_line(range.end().line());
        }
        cursor_position.set_column(0);

        return { cursor_position };
    }
    case Unit::Line: {
        size_t line_number;
        // Because we select lines from start to end, we can't use that
        // to get the new position, so we do some correction here.
        if (range.start().line() < cursor_position.line() || m_amount < 0) {
            line_number = range.start().line();
        } else {
            line_number = range.end().line();
        }

        auto& line = editor.line(line_number);

        cursor_position.set_line(line_number);
        if (line.length() <= cursor_position.column()) {
            cursor_position.set_column(line.length() - 1);
        }

        return { cursor_position };
    }
    default: {
        if (range.start() < cursor_position) {
            return { range.start() };
        } else {
            // Ranges are end-exclusive. The normalize_for_position argument we pass
            // above in get_range normalizes some values which shouldn't be
            // end-exclusive during normal operations.
            bool is_at_start = range.end().column() == 0;
            auto& line = editor.line(range.end().line());

            size_t column = is_at_start ? 0 : range.end().column() - 1;
            column = min(column, line.length() - (in_visual_mode ? 0 : 1));
            // Need to not go beyond the last character, as standard in vim.

            return { TextPosition { range.end().line(), column } };
        }
    }
    }
}

void VimMotion::reset()
{
    m_unit = Unit::Unknown;
    m_amount = 0;
    m_is_complete = false;
}

CursorWidth VimEditingEngine::cursor_width() const
{
    return m_vim_mode == VimMode::Insert ? CursorWidth::NARROW : CursorWidth::WIDE;
}

bool VimEditingEngine::on_key(KeyEvent const& event)
{
    switch (m_vim_mode) {
    case (VimMode::Insert):
        return on_key_in_insert_mode(event);
    case (VimMode::Visual):
        return on_key_in_visual_mode(event);
    case (VimMode::VisualLine):
        return on_key_in_visual_line_mode(event);
    case (VimMode::Normal):
        return on_key_in_normal_mode(event);
    default:
        VERIFY_NOT_REACHED();
    }

    return false;
}

bool VimEditingEngine::on_key_in_insert_mode(KeyEvent const& event)
{
    if (EditingEngine::on_key(event))
        return true;

    if (event.ctrl()) {
        switch (event.key()) {
        case KeyCode::Key_W:
            m_editor->delete_previous_word();
            return true;
        case KeyCode::Key_H:
            m_editor->delete_previous_char();
            return true;
        case KeyCode::Key_U:
            m_editor->delete_from_line_start_to_cursor();
            return true;
        default:
            break;
        }
    }

    if (event.key() == KeyCode::Key_Escape || (event.ctrl() && event.key() == KeyCode::Key_LeftBracket) || (event.ctrl() && event.key() == KeyCode::Key_C)) {
        if (m_editor->cursor().column() > 0)
            move_one_left();
        switch_to_normal_mode();
        return true;
    }
    return false;
}

bool VimEditingEngine::on_key_in_normal_mode(KeyEvent const& event)
{
    // Ignore auxiliary keypress events.
    if (event.key() == KeyCode::Key_LeftShift
        || event.key() == KeyCode::Key_RightShift
        || event.key() == KeyCode::Key_LeftControl
        || event.key() == KeyCode::Key_LeftAlt) {
        return false;
    }

    if (m_previous_key == KeyCode::Key_D) {
        if (event.key() == KeyCode::Key_D && !m_motion.should_consume_next_character()) {
            if (m_motion.amount()) {
                auto range = m_motion.get_repeat_range(*this, VimMotion::Unit::Line);
                VERIFY(range.has_value());
                yank(*range, Line);
                m_editor->delete_text_range(*range);
            } else {
                yank(Line);
                delete_line();
            }
            m_motion.reset();
            m_previous_key = {};
        } else {
            m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
            if (m_motion.is_complete()) {
                if (!m_motion.is_cancelled()) {
                    auto range = m_motion.get_range(*this);
                    VERIFY(range.has_value());

                    if (range->is_valid()) {
                        m_editor->delete_text_range(*range);
                    }
                }

                m_motion.reset();
                m_previous_key = {};
            }
        }
    } else if (m_previous_key == KeyCode::Key_Y) {
        if (event.key() == KeyCode::Key_Y && !m_motion.should_consume_next_character()) {
            if (m_motion.amount()) {
                auto range = m_motion.get_repeat_range(*this, VimMotion::Unit::Line);
                VERIFY(range.has_value());
                yank(*range, Line);
            } else {
                yank(Line);
            }
            m_motion.reset();
            m_previous_key = {};
        } else {
            m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
            if (m_motion.is_complete()) {
                if (!m_motion.is_cancelled()) {
                    auto range = m_motion.get_range(*this);
                    VERIFY(range.has_value());

                    if (range->is_valid()) {
                        m_editor->set_selection(*range);
                        yank(Selection);
                        m_editor->clear_selection();
                    }
                }

                m_motion.reset();
                m_previous_key = {};
            }
        }
    } else if (m_previous_key == KeyCode::Key_C) {
        if (event.key() == KeyCode::Key_C && !m_motion.should_consume_next_character()) {
            // Needed because the code to replace the deleted line is called after delete_line() so
            // what was the second last line before the delete, is now the last line.
            bool was_second_last_line = m_editor->cursor().line() == m_editor->line_count() - 2;
            yank(Line);
            delete_line();
            if (was_second_last_line || (m_editor->cursor().line() != 0 && m_editor->cursor().line() != m_editor->line_count() - 1)) {
                move_one_up(event);
                move_to_logical_line_end();
                m_editor->add_code_point(0x0A);
            } else if (m_editor->cursor().line() == 0) {
                move_to_logical_line_beginning();
                m_editor->add_code_point(0x0A);
                move_one_up(event);
            } else if (m_editor->cursor().line() == m_editor->line_count() - 1) {
                m_editor->add_code_point(0x0A);
            }
            switch_to_insert_mode();
        } else {
            m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
            if (m_motion.is_complete()) {
                if (!m_motion.is_cancelled()) {
                    auto range = m_motion.get_range(*this);
                    VERIFY(range.has_value());

                    if (range->is_valid()) {
                        m_editor->set_selection(*range);
                        yank(Selection);
                        m_editor->delete_text_range(*range);
                        switch_to_insert_mode();
                    }
                }

                m_motion.reset();
                m_previous_key = {};
            }
        }
    } else {
        if (m_motion.should_consume_next_character()) {
            // We must consume the next character.
            // FIXME: deduplicate with code below.
            m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
            if (m_motion.is_complete()) {
                if (!m_motion.is_cancelled()) {
                    auto maybe_new_position = m_motion.get_position(*this);
                    if (maybe_new_position.has_value()) {
                        auto new_position = maybe_new_position.value();
                        m_editor->set_cursor(new_position);
                    }
                }

                m_motion.reset();
            }
            return true;
        }

        // Handle first any key codes that are to be applied regardless of modifiers.
        switch (event.key()) {
        case (KeyCode::Key_Escape):
            return false;
        default:
            break;
        }

        // SHIFT is pressed.
        if (event.shift() && !event.ctrl() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_A):
                move_to_logical_line_end();
                switch_to_insert_mode();
                return true;
            case (KeyCode::Key_D):
                m_editor->delete_text_range({ m_editor->cursor(), { m_editor->cursor().line(), m_editor->current_line().length() } });
                if (m_editor->cursor().column() != 0)
                    move_one_left();
                break;
            case (KeyCode::Key_I):
                move_to_logical_line_beginning();
                switch_to_insert_mode();
                return true;
            case (KeyCode::Key_O):
                move_to_logical_line_beginning();
                m_editor->add_code_point(0x0A);
                move_one_up(event);
                switch_to_insert_mode();
                return true;
            case (KeyCode::Key_LeftBrace): {
                auto amount = m_motion.amount() > 0 ? m_motion.amount() : 1;
                m_motion.reset();
                for (int i = 0; i < amount; i++)
                    move_to_previous_empty_lines_block();
                return true;
            }
            case (KeyCode::Key_RightBrace): {
                auto amount = m_motion.amount() > 0 ? m_motion.amount() : 1;
                m_motion.reset();
                for (int i = 0; i < amount; i++)
                    move_to_next_empty_lines_block();
                return true;
            }
            case (KeyCode::Key_J): {
                // Looks a bit strange, but join without a repeat, with 1 as the repeat or 2 as the repeat all join the current and next lines
                auto amount = (m_motion.amount() > 2) ? (m_motion.amount() - 1) : 1;
                m_motion.reset();
                for (int i = 0; i < amount; i++) {
                    if (m_editor->cursor().line() + 1 >= m_editor->line_count())
                        return true;
                    move_to_logical_line_end();
                    m_editor->add_code_point(' ');
                    TextPosition next_line = { m_editor->cursor().line() + 1, 0 };
                    m_editor->delete_text_range({ m_editor->cursor(), next_line });
                    move_one_left();
                }
                return true;
            }
            case (KeyCode::Key_P):
                put_before();
                break;
            case (KeyCode::Key_V):
                switch_to_visual_line_mode();
                return true;
            default:
                break;
            }
        }

        // CTRL is pressed.
        if (event.ctrl() && !event.shift() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_D):
                move_half_page_down();
                return true;
            case (KeyCode::Key_R):
                m_editor->redo();
                return true;
            case (KeyCode::Key_U):
                move_half_page_up();
                return true;
            default:
                break;
            }
        }

        // FIXME: H and L movement keys will move to the previous or next line when reaching the beginning or end
        //  of the line and pressed again.

        // No modifier is pressed.
        if (!event.ctrl() && !event.shift() && !event.alt()) {
            switch (event.key()) {
            case (KeyCode::Key_A):
                move_one_right();
                switch_to_insert_mode();
                return true;
            case (KeyCode::Key_C):
                m_previous_key = event.key();
                return true;
            case (KeyCode::Key_D):
                m_previous_key = event.key();
                return true;
            case (KeyCode::Key_I):
                switch_to_insert_mode();
                return true;
            case (KeyCode::Key_O):
                move_to_logical_line_end();
                m_editor->add_code_point(0x0A);
                switch_to_insert_mode();
                return true;
            case (KeyCode::Key_U):
                m_editor->undo();
                return true;
            case (KeyCode::Key_X): {
                TextRange range = { m_editor->cursor(), { m_editor->cursor().line(), m_editor->cursor().column() + 1 } };
                if (m_motion.amount()) {
                    auto opt = m_motion.get_repeat_range(*this, VimMotion::Unit::Character);
                    VERIFY(opt.has_value());
                    range = *opt;
                    m_motion.reset();
                }
                yank(range, Selection);
                m_editor->delete_text_range(range);
                return true;
            }
            case (KeyCode::Key_V):
                switch_to_visual_mode();
                return true;
            case (KeyCode::Key_Y):
                m_previous_key = event.key();
                return true;
            case (KeyCode::Key_P):
                put_after();
                return true;
            case (KeyCode::Key_PageUp):
                move_page_up();
                return true;
            case (KeyCode::Key_PageDown):
                move_page_down();
                return true;
            default:
                break;
            }
        }

        // If nothing else handled the key, we'll be feeding the motion state
        // machine instead.
        m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
        if (m_motion.is_complete()) {
            if (!m_motion.is_cancelled()) {
                auto maybe_new_position = m_motion.get_position(*this);
                if (maybe_new_position.has_value()) {
                    auto new_position = maybe_new_position.value();
                    m_editor->set_cursor(new_position);
                }
            }

            m_motion.reset();
        }
    }
    return true;
}

bool VimEditingEngine::on_key_in_visual_mode(KeyEvent const& event)
{
    // If the motion state machine requires the next character, feed it.
    if (m_motion.should_consume_next_character()) {
        m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
        if (m_motion.is_complete()) {
            if (!m_motion.is_cancelled()) {
                auto maybe_new_position = m_motion.get_position(*this, true);
                if (maybe_new_position.has_value()) {
                    auto new_position = maybe_new_position.value();
                    m_editor->set_cursor(new_position);
                    update_selection_on_cursor_move();
                }
            }

            m_motion.reset();
        }

        return true;
    }

    // Handle first any key codes that are to be applied regardless of modifiers.
    switch (event.key()) {
    case (KeyCode::Key_Escape):
        switch_to_normal_mode();
        return false;
    default:
        break;
    }

    // SHIFT is pressed.
    if (event.shift() && !event.ctrl() && !event.alt()) {
        switch (event.key()) {
        case (KeyCode::Key_A):
            move_to_logical_line_end();
            switch_to_insert_mode();
            return true;
        case (KeyCode::Key_I):
            move_to_logical_line_beginning();
            switch_to_insert_mode();
            return true;
        case (KeyCode::Key_U):
            casefold_selection(Casing::Uppercase);
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_Tilde):
            casefold_selection(Casing::Invertcase);
            switch_to_normal_mode();
            return true;
        default:
            break;
        }
    }

    // CTRL is pressed.
    if (event.ctrl() && !event.shift() && !event.alt()) {
        switch (event.key()) {
        case (KeyCode::Key_D):
            move_half_page_down();
            update_selection_on_cursor_move();
            return true;
        case (KeyCode::Key_U):
            move_half_page_up();
            update_selection_on_cursor_move();
            return true;
        default:
            break;
        }
    }

    // No modifier is pressed.
    if (!event.ctrl() && !event.shift() && !event.alt()) {
        switch (event.key()) {
        case (KeyCode::Key_D):
            yank(Selection);
            m_editor->do_delete();
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_X):
            yank(Selection);
            m_editor->do_delete();
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_V):
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_C):
            yank(Selection);
            m_editor->do_delete();
            switch_to_insert_mode();
            return true;
        case (KeyCode::Key_Y):
            yank(Selection);
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_U):
            casefold_selection(Casing::Lowercase);
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_PageUp):
            move_page_up();
            update_selection_on_cursor_move();
            return true;
        case (KeyCode::Key_PageDown):
            move_page_down();
            update_selection_on_cursor_move();
            return true;
        default:
            break;
        }
    }

    // By default, we feed the motion state machine.
    m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
    if (m_motion.is_complete()) {
        if (!m_motion.is_cancelled()) {
            auto maybe_new_position = m_motion.get_position(*this, true);
            if (maybe_new_position.has_value()) {
                auto new_position = maybe_new_position.value();
                m_editor->set_cursor(new_position);
                update_selection_on_cursor_move();
            }
        }

        m_motion.reset();
    }

    return true;
}

bool VimEditingEngine::on_key_in_visual_line_mode(KeyEvent const& event)
{
    // If the motion state machine requires the next character, feed it.
    if (m_motion.should_consume_next_character()) {
        m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
        if (m_motion.is_complete()) {
            if (!m_motion.is_cancelled()) {
                auto maybe_new_position = m_motion.get_position(*this, true);
                if (maybe_new_position.has_value()) {
                    auto new_position = maybe_new_position.value();
                    m_editor->set_cursor(new_position);
                    update_selection_on_cursor_move();
                }
            }

            m_motion.reset();
        }

        return true;
    }

    // Handle first any key codes that are to be applied regardless of modifiers.
    switch (event.key()) {
    case (KeyCode::Key_Escape):
        switch_to_normal_mode();
        return false;
    default:
        break;
    }

    // SHIFT is pressed.
    if (event.shift() && !event.ctrl() && !event.alt()) {
        switch (event.key()) {
        case (KeyCode::Key_U):
            casefold_selection(Casing::Uppercase);
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_Tilde):
            casefold_selection(Casing::Invertcase);
            switch_to_normal_mode();
            return true;
        default:
            break;
        }
    }

    // CTRL is pressed.
    if (event.ctrl() && !event.shift() && !event.alt()) {
        switch (event.key()) {
        case (KeyCode::Key_D):
            move_half_page_down();
            update_selection_on_cursor_move();
            return true;
        case (KeyCode::Key_U):
            move_half_page_up();
            update_selection_on_cursor_move();
            return true;
        default:
            break;
        }
    }

    // No modifier is pressed.
    if (!event.ctrl() && !event.shift() && !event.alt()) {
        switch (event.key()) {
        case (KeyCode::Key_D):
            yank(m_editor->selection(), Line);
            m_editor->do_delete();
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_X):
            yank(m_editor->selection(), Line);
            m_editor->do_delete();
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_C):
            yank(m_editor->selection(), Line);
            m_editor->do_delete();
            switch_to_insert_mode();
            return true;
        case (KeyCode::Key_Y):
            yank(m_editor->selection(), Line);
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_U):
            casefold_selection(Casing::Lowercase);
            switch_to_normal_mode();
            return true;
        case (KeyCode::Key_PageUp):
            move_page_up();
            update_selection_on_cursor_move();
            return true;
        case (KeyCode::Key_PageDown):
            move_page_down();
            update_selection_on_cursor_move();
            return true;
        default:
            break;
        }
    }

    // By default, we feed the motion state machine.
    m_motion.add_key_code(event.key(), event.ctrl(), event.shift(), event.alt());
    if (m_motion.is_complete()) {
        if (!m_motion.is_cancelled()) {
            auto maybe_new_position = m_motion.get_position(*this, true);
            if (maybe_new_position.has_value()) {
                auto new_position = maybe_new_position.value();
                m_editor->set_cursor(new_position);
                update_selection_on_cursor_move();
            }
        }

        m_motion.reset();
    }

    return true;
}

void VimEditingEngine::switch_to_normal_mode()
{
    m_vim_mode = VimMode::Normal;
    m_editor->reset_cursor_blink();
    m_previous_key = {};
    clear_visual_mode_data();
    m_motion.reset();
}

void VimEditingEngine::switch_to_insert_mode()
{
    m_vim_mode = VimMode::Insert;
    m_editor->reset_cursor_blink();
    m_previous_key = {};
    clear_visual_mode_data();
    m_motion.reset();
}

void VimEditingEngine::switch_to_visual_mode()
{
    m_vim_mode = VimMode::Visual;
    m_editor->reset_cursor_blink();
    m_previous_key = {};
    m_selection_start_position = m_editor->cursor();
    m_editor->selection().set(m_editor->cursor(), { m_editor->cursor().line(), m_editor->cursor().column() + 1 });
    m_editor->did_update_selection();
    m_motion.reset();
}

void VimEditingEngine::switch_to_visual_line_mode()
{
    m_vim_mode = VimMode::VisualLine;
    m_editor->reset_cursor_blink();
    m_previous_key = {};
    m_selection_start_position = TextPosition { m_editor->cursor().line(), 0 };
    m_editor->selection().set(m_selection_start_position, { m_editor->cursor().line(), m_editor->current_line().length() });
    m_editor->did_update_selection();
    m_motion.reset();
}

void VimEditingEngine::update_selection_on_cursor_move()
{
    auto cursor = m_editor->cursor();
    auto start = m_selection_start_position < cursor ? m_selection_start_position : cursor;
    auto end = m_selection_start_position < cursor ? cursor : m_selection_start_position;

    if (end.column() >= m_editor->current_line().length()) {
        if (end.line() != m_editor->line_count() - 1)
            end = { end.line() + 1, 0 };
    } else {
        end.set_column(end.column() + 1);
    }

    if (m_vim_mode == VimMode::VisualLine) {
        start = TextPosition { start.line(), 0 };
        end = TextPosition { end.line(), m_editor->line(end.line()).length() };
    }

    m_editor->selection().set(start, end);
    m_editor->did_update_selection();
}

void VimEditingEngine::clamp_cursor_position()
{
    auto cursor = m_editor->cursor();
    if (cursor.column() >= m_editor->current_line().length()) {
        cursor.set_column(m_editor->current_line().length() - 1);
        m_editor->set_cursor(cursor);
    }
}

void VimEditingEngine::clear_visual_mode_data()
{
    if (m_editor->has_selection()) {
        m_editor->selection().clear();
        m_editor->did_update_selection();
        clamp_cursor_position();
    }
    m_selection_start_position = {};
}

void VimEditingEngine::move_half_page_up()
{
    move_up(0.5);
}

void VimEditingEngine::move_half_page_down()
{
    move_down(0.5);
}

void VimEditingEngine::yank(YankType type)
{
    m_yank_type = type;
    if (type == YankType::Line) {
        m_yank_buffer = m_editor->current_line().to_utf8();
    } else {
        m_yank_buffer = m_editor->selected_text();
    }

    // When putting this, auto indentation (if enabled) will indent as far as
    // is necessary, then any whitespace captured before the yanked text will be placed
    // after the indentation, doubling the indentation.
    if (m_editor->is_automatic_indentation_enabled())
        m_yank_buffer = m_yank_buffer.trim_whitespace(TrimMode::Left);
}

void VimEditingEngine::yank(TextRange range, YankType yank_type)
{
    m_yank_type = yank_type;
    m_yank_buffer = m_editor->document().text_in_range(range).trim_whitespace(AK::TrimMode::Right);
}

void VimEditingEngine::put_before()
{
    auto amount = m_motion.amount() ? m_motion.amount() : 1;
    m_motion.reset();
    if (m_yank_type == YankType::Line) {
        move_to_logical_line_beginning();
        StringBuilder sb = StringBuilder(amount * (m_yank_buffer.length() + 1));
        for (auto i = 0; i < amount; i++) {
            sb.append(m_yank_buffer);
            sb.append_code_point(0x0A);
        }
        m_editor->insert_at_cursor_or_replace_selection(sb.to_byte_string());
        m_editor->set_cursor({ m_editor->cursor().line(), m_editor->current_line().first_non_whitespace_column() });
    } else {
        StringBuilder sb = StringBuilder(m_yank_buffer.length() * amount);
        for (auto i = 0; i < amount; i++) {
            sb.append(m_yank_buffer);
        }
        m_editor->insert_at_cursor_or_replace_selection(sb.to_byte_string());
        move_one_left();
    }
}

void VimEditingEngine::put_after()
{
    auto amount = m_motion.amount() ? m_motion.amount() : 1;
    m_motion.reset();
    if (m_yank_type == YankType::Line) {
        move_to_logical_line_end();
        StringBuilder sb = StringBuilder(m_yank_buffer.length() + 1);
        for (auto i = 0; i < amount; i++) {
            sb.append_code_point(0x0A);
            sb.append(m_yank_buffer);
        }
        m_editor->insert_at_cursor_or_replace_selection(sb.to_byte_string());
        m_editor->set_cursor({ m_editor->cursor().line(), m_editor->current_line().first_non_whitespace_column() });
    } else {
        // FIXME: If attempting to put on the last column a line,
        // the buffer will bne placed on the next line due to the move_one_left/right behavior.
        move_one_right();
        StringBuilder sb = StringBuilder(m_yank_buffer.length() * amount);
        for (auto i = 0; i < amount; i++) {
            sb.append(m_yank_buffer);
        }
        m_editor->insert_at_cursor_or_replace_selection(sb.to_byte_string());
        move_one_left();
    }
}

void VimEditingEngine::move_to_previous_empty_lines_block()
{
    VERIFY(!m_editor.is_null());
    size_t line_idx = m_editor->cursor().line();
    bool skipping_initial_empty_lines = true;
    while (line_idx > 0) {
        if (m_editor->document().line(line_idx).is_empty()) {
            if (!skipping_initial_empty_lines)
                break;
        } else {
            skipping_initial_empty_lines = false;
        }
        line_idx--;
    }

    TextPosition new_cursor = { line_idx, 0 };

    m_editor->set_cursor(new_cursor);
}

void VimEditingEngine::move_to_next_empty_lines_block()
{
    VERIFY(!m_editor.is_null());
    size_t line_idx = m_editor->cursor().line();
    bool skipping_initial_empty_lines = true;
    while (line_idx < m_editor->line_count() - 1) {
        if (m_editor->document().line(line_idx).is_empty()) {
            if (!skipping_initial_empty_lines)
                break;
        } else {
            skipping_initial_empty_lines = false;
        }
        line_idx++;
    }

    TextPosition new_cursor = { line_idx, 0 };

    m_editor->set_cursor(new_cursor);
}

void VimEditingEngine::casefold_selection(Casing casing)
{
    VERIFY(!m_editor.is_null());
    VERIFY(m_editor->has_selection());

    switch (casing) {
    case Casing::Uppercase:
        m_editor->insert_at_cursor_or_replace_selection(m_editor->selected_text().to_uppercase());
        return;
    case Casing::Lowercase:
        m_editor->insert_at_cursor_or_replace_selection(m_editor->selected_text().to_lowercase());
        return;
    case Casing::Invertcase:
        m_editor->insert_at_cursor_or_replace_selection(m_editor->selected_text().invert_case());
        return;
    }
}

}
