/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Optional.h>
#include <LibCore/Object.h>
#include <LibGUI/EditingEngine.h>
#include <LibGUI/TextRange.h>

namespace GUI {

class VimEditingEngine;

enum VimMode {
    Normal,
    Insert,
    Visual
};

enum class VimMatchType {
    None,
    Partial,
    Complete
};

// Wrapper over TextPosition that makes it easier to move it around as a cursor,
// and to get the current line or character.
class VimCursor {
public:
    VimCursor(TextEditor& editor, TextPosition initial_position, bool forwards)
        : m_editor(editor)
        , m_position(initial_position)
        , m_forwards(forwards)
    {
    }

    void move_forwards();
    void move_backwards();

    // Move a single character in the current direction.
    void move();
    // Move a single character in reverse.
    void move_reverse();
    // Peek a single character in the current direction.
    u32 peek();
    // Peek a single character in reverse.
    u32 peek_reverse();
    // Get the character the cursor is currently on.
    u32 current_char();
    // Get the line the cursor is currently on.
    TextDocumentLine& current_line();
    // Get the current position.
    TextPosition& current_position() { return m_position; }

    // Did we hit the edge of the document?
    bool hit_edge() { return m_hit_edge; }
    // Will the next move cross a line boundary?
    bool will_cross_line_boundary();
    // Did we cross a line boundary?
    bool crossed_line_boundary() { return m_crossed_line_boundary; }
    // Are we on an empty line?
    bool on_empty_line();
    // Are we going forwards?
    bool forwards() { return m_forwards; }

private:
    TextEditor& m_editor;
    TextPosition m_position;
    bool m_forwards;

    u32 m_cached_char { 0 };

    bool m_hit_edge { false };
    bool m_crossed_line_boundary { false };
};

class VimMotion {
public:
    enum class Unit {
        // The motion isn't complete yet, or was invalid.
        Unknown,
        // Document. Anything non-negative is counted as G while anything else is gg.
        Document,
        // Lines.
        Line,
        // A sequence of letters, digits and underscores, or a sequence of other
        // non-blank characters separated by whitespace.
        Word,
        // A sequence of non-blank characters separated by whitespace.
        // This is how Vim separates w from W.
        WORD,
        // End of a word. This is basically the same as a word but it doesn't
        // trim the spaces at the end.
        EndOfWord,
        // End of a WORD.
        EndOfWORD,
        // Characters (or Unicode code points based on how pedantic you want to
        // get).
        Character,
        // Used for find-mode.
        Find
    };
    enum class FindMode {
        /// Find mode is not enabled.
        None,
        /// Finding until the given character.
        To,
        /// Finding through the given character.
        Find
    };

    void add_key_code(KeyCode key, bool ctrl, bool shift, bool alt);
    Optional<TextRange> get_range(class VimEditingEngine& engine, bool normalize_for_position = false);
    Optional<TextRange> get_repeat_range(class VimEditingEngine& engine, Unit, bool normalize_for_position = false);
    Optional<TextPosition> get_position(VimEditingEngine& engine, bool in_visual_mode = false);
    void reset();

    /// Returns whether the motion should consume the next character no matter what.
    /// Used for f and t motions.
    bool should_consume_next_character() { return m_should_consume_next_character; }
    bool is_complete() { return m_is_complete; }
    bool is_cancelled() { return m_is_complete && m_unit == Unit::Unknown; }
    Unit unit() { return m_unit; }
    int amount() { return m_amount; }

    // FIXME: come up with a better way to signal start/end of line than sentinels?
    static constexpr int START_OF_LINE = NumericLimits<int>::min();
    static constexpr int START_OF_NON_WHITESPACE = NumericLimits<int>::min() + 1;
    static constexpr int END_OF_LINE = NumericLimits<int>::max();

private:
    void calculate_document_range(TextEditor&);
    void calculate_line_range(TextEditor&, bool normalize_for_position);
    void calculate_word_range(VimCursor&, int amount, bool normalize_for_position);
    void calculate_WORD_range(VimCursor&, int amount, bool normalize_for_position);
    void calculate_character_range(VimCursor&, int amount, bool normalize_for_position);
    void calculate_find_range(VimCursor&, int amount);

    Unit m_unit { Unit::Unknown };
    int m_amount { 0 };
    bool m_is_complete { false };
    bool m_guirky_mode { false };
    bool m_should_consume_next_character { false };

    FindMode m_find_mode { FindMode::None };
    u32 m_next_character { 0 };

    size_t m_start_line { 0 };
    size_t m_start_column { 0 };
    size_t m_end_line { 0 };
    size_t m_end_column { 0 };
};

class VimKey {
public:
    VimKey(KeyCode key_code, const String& str, bool shift)
        : m_key_code(key_code)
        , m_string(str)
        , m_shift(shift)
        , m_ctrl(false)
    {
    }
    VimKey(KeyCode key_code, const String& str, bool shift, bool ctrl)
        : m_key_code(key_code)
        , m_string(str)
        , m_shift(shift)
        , m_ctrl(ctrl)
    {
    }

    bool operator==(const VimKey& key)
    {
        return m_key_code == key.m_key_code && m_shift == key.m_shift && m_ctrl == key.m_ctrl;
    }

    String to_string() const;

private:
    const KeyCode m_key_code;
    const String m_string;
    const bool m_shift;
    const bool m_ctrl;
};

class VimCommand {
public:
    template<typename Callable>
    VimCommand(const String& keys, const String& vim_modes, Callable callable)
        : m_keys(parse_keys(keys))
        , m_vim_modes(vim_modes)
        , m_execute_command(move(callable))
    {
    }

    static Vector<VimKey> parse_keys(const String& keys);

    void execute() const;
    VimMatchType check_match(Vector<VimKey> keys, VimMode vim_mode) const;
    String to_string() const;

private:
    static VimKey parse_modified_key(GenericLexer&);

    const Vector<VimKey> m_keys {};
    // A string representing the modes in which this command is allowed.
    // For example, if this string is "NV" (Normal, Visual) and the mode is insert,
    // Even if the keys match, the command cannot be executed.
    const String m_vim_modes;
    Function<void()> m_execute_command;
};

class VimEditingEngine final : public EditingEngine {
public:
    VimEditingEngine()
    {
        initialize_commands();
    }

    static bool key_requires_shift(u32 code_point, KeyCode key);
    static bool special_character_requires_shift(KeyCode key);

    virtual CursorWidth cursor_width() const override;

    virtual bool on_key(const KeyEvent& event) override;

private:
    enum YankType {
        Line,
        Selection
    };

    // A list of keys entered by the user as they build up to the command
    // they are trying to execute.
    Vector<VimKey> m_keys {};
    // A list of all commands that this engine supports.
    Vector<VimCommand> m_commands {};
    // Called in the constructor of the VimEditingEngine to populate the m_commands list
    // with its supported commands and their actions.
    void initialize_commands();
    // Would replace the virtual override on_key which handles the user's keypress.
    // If not in insert mode (with some exceptions) would convert the KeyEvent to a VimKey
    // and add it to m_keys, then check if the new m_keys matches any commands in m_commands,
    // if a complete match is found, execute the matching command.
    void add_key(const KeyEvent&);

    VimMode m_vim_mode { VimMode::Normal };
    VimMotion m_motion;

    YankType m_yank_type {};
    String m_yank_buffer {};
    void yank(YankType);
    void yank(TextRange, YankType yank_type);
    void put_before();
    void put_after();

    TextPosition m_selection_start_position = {};
    void update_selection_on_cursor_move();
    void clamp_cursor_position();
    void clear_visual_mode_data();

    KeyCode m_previous_key {};
    void switch_to_normal_mode();
    void switch_to_insert_mode();
    void switch_to_visual_mode();
    void move_half_page_up();
    void move_half_page_down();
    void move_to_previous_empty_lines_block();
    void move_to_next_empty_lines_block();

    bool on_key_in_insert_mode(const KeyEvent& event);
    bool on_key_in_normal_mode(const KeyEvent& event);
    bool on_key_in_visual_mode(const KeyEvent& event);
};

}

namespace AK {

template<>
struct Formatter<GUI::VimKey> : Formatter<StringView> {
    void format(FormatBuilder& builder, const GUI::VimKey& value)
    {
        Formatter<StringView>::format(builder, value.to_string());
    }
};

template<>
struct Formatter<GUI::VimCommand> : Formatter<StringView> {
    void format(FormatBuilder& builder, const GUI::VimCommand& value)
    {
        Formatter<StringView>::format(builder, value.to_string());
    }
};

}
