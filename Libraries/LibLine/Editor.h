/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/BinarySearch.h>
#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/QuickSort.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <LibLine/Span.h>
#include <LibLine/Style.h>
#include <LibLine/SuggestionDisplay.h>
#include <LibLine/SuggestionManager.h>
#include <LibLine/VT.h>
#include <sys/stat.h>
#include <termios.h>

namespace Line {

struct Configuration {
    enum TokenSplitMechanism {
        Spaces,
        UnescapedSpaces,
    };
    enum RefreshBehaviour {
        Lazy,
        Eager,
    };
    enum OperationMode {
        Full,
        NoEscapeSequences,
    };

    Configuration()
    {
    }

    template<typename Arg, typename... Rest>
    Configuration(Arg arg, Rest... rest)
        : Configuration(rest...)
    {
        set(arg);
    }

    void set(RefreshBehaviour refresh) { refresh_behaviour = refresh; }
    void set(TokenSplitMechanism split) { split_mechanism = split; }
    void set(OperationMode mode) { operation_mode = mode; }

    RefreshBehaviour refresh_behaviour { RefreshBehaviour::Lazy };
    TokenSplitMechanism split_mechanism { TokenSplitMechanism::Spaces };
    OperationMode operation_mode { OperationMode::Full };
};

class Editor : public Core::Object {
    C_OBJECT(Editor);

public:
    enum class Error {
        ReadFailure,
        Empty,
        Eof,
    };

    ~Editor();

    Result<String, Error> get_line(const String& prompt);

    void initialize();

    void add_to_history(const String&);
    const Vector<String>& history() const { return m_history; }

    void register_character_input_callback(char ch, Function<bool(Editor&)> callback);
    size_t actual_rendered_string_length(const StringView& string) const;

    Function<Vector<CompletionSuggestion>(const Editor&)> on_tab_complete;
    Function<void()> on_interrupt_handled;
    Function<void(Editor&)> on_display_refresh;

    // FIXME: we will have to kindly ask our instantiators to set our signal handlers,
    // since we can not do this cleanly ourselves. (signal() limitation: cannot give member functions)
    void interrupted()
    {
        if (m_is_editing) {
            m_was_interrupted = true;
            handle_interrupt_event();
        }
    }
    void resized()
    {
        m_was_resized = true;
        refresh_display();
    }

    size_t cursor() const { return m_cursor; }
    const Vector<u32, 1024>& buffer() const { return m_buffer; }
    u32 buffer_at(size_t pos) const { return m_buffer.at(pos); }
    String line() const { return line(m_buffer.size()); }
    String line(size_t up_to_index) const;

    // Only makes sense inside a character_input callback or on_* callback.
    void set_prompt(const String& prompt)
    {
        if (m_cached_prompt_valid)
            m_old_prompt_length = m_cached_prompt_length;
        m_cached_prompt_valid = false;
        m_cached_prompt_length = actual_rendered_string_length(prompt);
        m_new_prompt = prompt;
    }

    void clear_line();
    void insert(const String&);
    void insert(const Utf32View&);
    void insert(const u32);
    void stylize(const Span&, const Style&);
    void strip_styles(bool strip_anchored = false);

    // Invariant Offset is an offset into the suggested data, hinting the editor what parts of the suggestion will not change
    // Static Offset is an offset into the token, signifying where the suggestions start
    // e.g.
    //    foobar<suggestion initiated>, on_tab_complete returns "barx", "bary", "barz"
    //       ^ ^
    //       +-|- static offset: the suggestions start here
    //         +- invariant offset: the suggestions do not change up to here
    //
    void suggest(size_t invariant_offset = 0, size_t static_offset = 0, Span::Mode offset_mode = Span::ByteOriented) const;

    const struct termios& termios() const { return m_termios; }
    const struct termios& default_termios() const { return m_default_termios; }

    void finish()
    {
        m_finish = true;
    }

    bool is_editing() const { return m_is_editing; }

private:
    explicit Editor(Configuration configuration = {});

    // ^Core::Object
    virtual void save_to(JsonObject&) override;

    struct KeyCallback {
        KeyCallback(Function<bool(Editor&)> cb)
            : callback(move(cb))
        {
        }
        Function<bool(Editor&)> callback;
    };

    void handle_interrupt_event();
    void handle_read_event();

    Vector<size_t, 2> vt_dsr();
    void remove_at_index(size_t);

    enum class ModificationKind {
        Insertion,
        Removal,
        ForcedOverlapRemoval,
    };
    void readjust_anchored_styles(size_t hint_index, ModificationKind);

    Style find_applicable_style(size_t offset) const;

    bool search(const StringView&, bool allow_empty = false, bool from_beginning = false);
    inline void end_search()
    {
        m_is_searching = false;
        m_refresh_needed = true;
        m_search_offset = 0;
        if (m_reset_buffer_on_search_end) {
            m_buffer.clear();
            for (auto ch : m_pre_search_buffer)
                m_buffer.append(ch);
            m_cursor = m_pre_search_cursor;
        }
        m_reset_buffer_on_search_end = true;
        m_search_editor = nullptr;
    }

    void reset()
    {
        m_cached_buffer_size = 0;
        m_cached_prompt_valid = false;
        m_cursor = 0;
        m_drawn_cursor = 0;
        m_inline_search_cursor = 0;
        m_old_prompt_length = m_cached_prompt_length;
        set_origin(0, 0);
        m_prompt_lines_at_suggestion_initiation = 0;
        m_refresh_needed = true;
        m_input_error.clear();
        m_returned_line = String::empty();
    }

    void refresh_display();
    void cleanup();

    void restore()
    {
        ASSERT(m_initialized);
        tcsetattr(0, TCSANOW, &m_default_termios);
        m_initialized = false;
    }

    size_t current_prompt_length() const
    {
        return m_cached_prompt_valid ? m_cached_prompt_length : m_old_prompt_length;
    }

    size_t num_lines() const
    {
        return (m_cached_buffer_size + m_num_columns + current_prompt_length() - 1) / m_num_columns;
    }

    size_t cursor_line() const
    {
        return (m_drawn_cursor + m_num_columns + current_prompt_length() - 1) / m_num_columns;
    }

    size_t offset_in_line() const
    {
        return (m_drawn_cursor + current_prompt_length()) % m_num_columns;
    }

    void set_origin()
    {
        auto position = vt_dsr();
        set_origin(position[0], position[1]);
    }

    void set_origin(int x, int y)
    {
        m_origin_x = x;
        m_origin_y = y;
        m_suggestion_display->set_origin(x, y, {});
    }

    bool should_break_token(Vector<u32, 1024>& buffer, size_t index);

    void recalculate_origin();
    void reposition_cursor();

    struct CodepointRange {
        size_t start { 0 };
        size_t end { 0 };
    };
    CodepointRange byte_offset_range_to_codepoint_offset_range(size_t byte_start, size_t byte_end, size_t codepoint_scan_offset, bool reverse = false) const;

    bool m_finish { false };

    RefPtr<Editor> m_search_editor;
    bool m_is_searching { false };
    bool m_reset_buffer_on_search_end { true };
    size_t m_search_offset { 0 };
    bool m_searching_backwards { true };
    size_t m_pre_search_cursor { 0 };
    Vector<u32, 1024> m_pre_search_buffer;

    Vector<u32, 1024> m_buffer;
    ByteBuffer m_pending_chars;
    Vector<char, 512> m_incomplete_data;
    Optional<Error> m_input_error;
    String m_returned_line;

    size_t m_cursor { 0 };
    size_t m_drawn_cursor { 0 };
    size_t m_inline_search_cursor { 0 };
    size_t m_chars_inserted_in_the_middle { 0 };
    size_t m_times_tab_pressed { 0 };
    size_t m_num_columns { 0 };
    size_t m_num_lines { 1 };
    size_t m_cached_prompt_length { 0 };
    size_t m_old_prompt_length { 0 };
    size_t m_cached_buffer_size { 0 };
    size_t m_prompt_lines_at_suggestion_initiation { 0 };
    bool m_cached_prompt_valid { false };

    // Exact position before our prompt in the terminal.
    size_t m_origin_x { 0 };
    size_t m_origin_y { 0 };

    OwnPtr<SuggestionDisplay> m_suggestion_display;

    String m_new_prompt;

    SuggestionManager m_suggestion_manager;

    bool m_always_refresh { false };

    enum class TabDirection {
        Forward,
        Backward,
    };
    TabDirection m_tab_direction { TabDirection::Forward };

    HashMap<char, NonnullOwnPtr<KeyCallback>> m_key_callbacks;

    // TODO: handle signals internally.
    struct termios m_termios, m_default_termios;
    bool m_was_interrupted { false };
    bool m_was_resized { false };

    // FIXME: This should be something more take_first()-friendly.
    Vector<String> m_history;
    size_t m_history_cursor { 0 };
    size_t m_history_capacity { 100 };

    enum class InputState {
        Free,
        ExpectBracket,
        ExpectFinal,
        ExpectTerminator,
    };
    InputState m_state { InputState::Free };

    HashMap<u32, HashMap<u32, Style>> m_spans_starting;
    HashMap<u32, HashMap<u32, Style>> m_spans_ending;

    HashMap<u32, HashMap<u32, Style>> m_anchored_spans_starting;
    HashMap<u32, HashMap<u32, Style>> m_anchored_spans_ending;

    RefPtr<Core::Notifier> m_notifier;

    bool m_initialized { false };
    bool m_refresh_needed { false };

    bool m_is_editing { false };

    Configuration m_configuration;
};

}
