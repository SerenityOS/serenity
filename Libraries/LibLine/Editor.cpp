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

#include "Editor.h"
#include <AK/GenericLexer.h>
#include <AK/JsonObject.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Notifier.h>
#include <ctype.h>
#include <signal.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <sys/time.h>
#include <unistd.h>

// #define SUGGESTIONS_DEBUG

namespace {
constexpr u32 ctrl(char c) { return c & 0x3f; }
}

namespace Line {

Configuration Configuration::from_config(const StringView& libname)
{
    Configuration configuration;
    auto config_file = Core::ConfigFile::get_for_lib(libname);

    // Read behaviour options.
    auto refresh = config_file->read_entry("behaviour", "refresh", "lazy");
    auto operation = config_file->read_entry("behaviour", "operation_mode");

    if (refresh.equals_ignoring_case("lazy"))
        configuration.set(Configuration::Lazy);
    else if (refresh.equals_ignoring_case("eager"))
        configuration.set(Configuration::Eager);

    if (operation.equals_ignoring_case("full"))
        configuration.set(Configuration::OperationMode::Full);
    else if (operation.equals_ignoring_case("noescapesequences"))
        configuration.set(Configuration::OperationMode::NoEscapeSequences);
    else if (operation.equals_ignoring_case("noninteractive"))
        configuration.set(Configuration::OperationMode::NonInteractive);
    else
        configuration.set(Configuration::OperationMode::Unset);

    // Read keybinds.

    for (auto& binding_key : config_file->keys("keybinds")) {
        GenericLexer key_lexer(binding_key);
        auto has_ctrl = false;
        auto alt = false;
        unsigned key = 0;

        while (!key && !key_lexer.is_eof()) {
            if (key_lexer.next_is("alt+")) {
                alt = key_lexer.consume_specific("alt+");
                continue;
            }
            if (key_lexer.next_is("^[")) {
                alt = key_lexer.consume_specific("^[");
                continue;
            }
            if (key_lexer.next_is("^")) {
                has_ctrl = key_lexer.consume_specific("^");
                continue;
            }
            if (key_lexer.next_is("ctrl+")) {
                has_ctrl = key_lexer.consume_specific("ctrl+");
                continue;
            }
            // FIXME: Support utf?
            key = key_lexer.consume();
        }

        if (has_ctrl)
            key = ctrl(key);

        auto value = config_file->read_entry("keybinds", binding_key);
        if (value.starts_with("internal:")) {
            configuration.set(KeyBinding {
                Key { key, alt ? Key::Alt : Key::None },
                KeyBinding::Kind::InternalFunction,
                value.substring(9, value.length() - 9) });
        } else {
            configuration.set(KeyBinding {
                Key { key, alt ? Key::Alt : Key::None },
                KeyBinding::Kind::Insertion,
                value });
        }
    }

    return configuration;
}

void Editor::set_default_keybinds()
{
    register_key_input_callback(ctrl('N'), EDITOR_INTERNAL_FUNCTION(search_forwards));
    register_key_input_callback(ctrl('P'), EDITOR_INTERNAL_FUNCTION(search_backwards));
    // Normally ^W. `stty werase \^n` can change it to ^N (or something else), but Serenity doesn't have `stty` yet.
    register_key_input_callback(m_termios.c_cc[VWERASE], EDITOR_INTERNAL_FUNCTION(erase_word_backwards));
    // Normally ^U. `stty kill \^n` can change it to ^N (or something else), but Serenity doesn't have `stty` yet.
    register_key_input_callback(m_termios.c_cc[VKILL], EDITOR_INTERNAL_FUNCTION(kill_line));
    register_key_input_callback(ctrl('A'), EDITOR_INTERNAL_FUNCTION(go_home));
    register_key_input_callback(ctrl('B'), EDITOR_INTERNAL_FUNCTION(cursor_left_character));
    register_key_input_callback(ctrl('D'), EDITOR_INTERNAL_FUNCTION(erase_character_forwards));
    register_key_input_callback(ctrl('E'), EDITOR_INTERNAL_FUNCTION(go_end));
    register_key_input_callback(ctrl('F'), EDITOR_INTERNAL_FUNCTION(cursor_right_character));
    // ^H: ctrl('H') == '\b'
    register_key_input_callback(ctrl('H'), EDITOR_INTERNAL_FUNCTION(erase_character_backwards));
    register_key_input_callback(m_termios.c_cc[VERASE], EDITOR_INTERNAL_FUNCTION(erase_character_backwards));
    register_key_input_callback(ctrl('K'), EDITOR_INTERNAL_FUNCTION(erase_to_end));
    register_key_input_callback(ctrl('L'), EDITOR_INTERNAL_FUNCTION(clear_screen));
    register_key_input_callback(ctrl('R'), EDITOR_INTERNAL_FUNCTION(enter_search));
    register_key_input_callback(ctrl('T'), EDITOR_INTERNAL_FUNCTION(transpose_characters));
    register_key_input_callback('\n', EDITOR_INTERNAL_FUNCTION(finish));

    // ^[.: alt-.: insert last arg of previous command (similar to `!$`)
    register_key_input_callback({ '.', Key::Alt }, EDITOR_INTERNAL_FUNCTION(insert_last_words));
    register_key_input_callback({ 'b', Key::Alt }, EDITOR_INTERNAL_FUNCTION(cursor_left_word));
    register_key_input_callback({ 'f', Key::Alt }, EDITOR_INTERNAL_FUNCTION(cursor_right_word));
    // ^[^H: alt-backspace: backward delete word
    register_key_input_callback({ '\b', Key::Alt }, EDITOR_INTERNAL_FUNCTION(erase_alnum_word_backwards));
    register_key_input_callback({ 'd', Key::Alt }, EDITOR_INTERNAL_FUNCTION(erase_alnum_word_forwards));
    register_key_input_callback({ 'c', Key::Alt }, EDITOR_INTERNAL_FUNCTION(capitalize_word));
    register_key_input_callback({ 'l', Key::Alt }, EDITOR_INTERNAL_FUNCTION(lowercase_word));
    register_key_input_callback({ 'u', Key::Alt }, EDITOR_INTERNAL_FUNCTION(uppercase_word));
    register_key_input_callback({ 't', Key::Alt }, EDITOR_INTERNAL_FUNCTION(transpose_words));
}

Editor::Editor(Configuration configuration)
    : m_configuration(move(configuration))
{
    m_always_refresh = configuration.refresh_behaviour == Configuration::RefreshBehaviour::Eager;
    m_pending_chars = ByteBuffer::create_uninitialized(0);
    get_terminal_size();
    m_suggestion_display = make<XtermSuggestionDisplay>(m_num_lines, m_num_columns);
}

Editor::~Editor()
{
    if (m_initialized)
        restore();
}

void Editor::get_terminal_size()
{
    struct winsize ws;
    if (ioctl(STDERR_FILENO, TIOCGWINSZ, &ws) < 0) {
        m_num_columns = 80;
        m_num_lines = 25;
    } else {
        m_num_columns = ws.ws_col;
        m_num_lines = ws.ws_row;
    }
}

void Editor::add_to_history(const String& line)
{
    if (line.is_empty())
        return;
    if ((m_history.size() + 1) > m_history_capacity)
        m_history.take_first();
    m_history.append(line);
}

void Editor::clear_line()
{
    for (size_t i = 0; i < m_cursor; ++i)
        fputc(0x8, stderr);
    fputs("\033[K", stderr);
    fflush(stderr);
    m_buffer.clear();
    m_cursor = 0;
    m_inline_search_cursor = m_cursor;
}

void Editor::insert(const Utf32View& string)
{
    for (size_t i = 0; i < string.length(); ++i)
        insert(string.code_points()[i]);
}

void Editor::insert(const String& string)
{
    for (auto ch : Utf8View { string })
        insert(ch);
}

void Editor::insert(const StringView& string_view)
{
    for (auto ch : Utf8View { string_view })
        insert(ch);
}

void Editor::insert(const u32 cp)
{
    StringBuilder builder;
    builder.append(Utf32View(&cp, 1));
    auto str = builder.build();
    m_pending_chars.append(str.characters(), str.length());

    readjust_anchored_styles(m_cursor, ModificationKind::Insertion);

    if (m_cursor == m_buffer.size()) {
        m_buffer.append(cp);
        m_cursor = m_buffer.size();
        m_inline_search_cursor = m_cursor;
        return;
    }

    m_buffer.insert(m_cursor, cp);
    ++m_chars_inserted_in_the_middle;
    ++m_cursor;
    m_inline_search_cursor = m_cursor;
}

void Editor::register_key_input_callback(const KeyBinding& binding)
{
    if (binding.kind == KeyBinding::Kind::InternalFunction) {
        auto internal_function = find_internal_function(binding.binding);
        if (!internal_function) {
            dbg() << "LibLine: Unknown internal function '" << binding.binding << "'";
            return;
        }
        return register_key_input_callback(binding.key, move(internal_function));
    }

    return register_key_input_callback(binding.key, [binding = String(binding.binding)](auto& editor) {
        editor.insert(binding);
        return false;
    });
}

void Editor::register_key_input_callback(Key key, Function<bool(Editor&)> callback)
{
    m_key_callbacks.set(key, make<KeyCallback>(move(callback)));
}

static size_t code_point_length_in_utf8(u32 code_point)
{
    if (code_point <= 0x7f)
        return 1;
    if (code_point <= 0x07ff)
        return 2;
    if (code_point <= 0xffff)
        return 3;
    if (code_point <= 0x10ffff)
        return 4;
    return 3;
}

// buffer [ 0 1 2 3 . . . A . . . B . . . M . . . N ]
//                        ^       ^       ^       ^
//                        |       |       |       +- end of buffer
//                        |       |       +- scan offset = M
//                        |       +- range end = M - B
//                        +- range start = M - A
// This method converts a byte range defined by [start_byte_offset, end_byte_offset] to a code_point range [M - A, M - B] as shown in the diagram above.
// If `reverse' is true, A and B are before M, if not, A and B are after M.
Editor::CodepointRange Editor::byte_offset_range_to_code_point_offset_range(size_t start_byte_offset, size_t end_byte_offset, size_t scan_code_point_offset, bool reverse) const
{
    size_t byte_offset = 0;
    size_t code_point_offset = scan_code_point_offset + (reverse ? 1 : 0);
    CodepointRange range;

    for (;;) {
        if (!reverse) {
            if (code_point_offset >= m_buffer.size())
                break;
        } else {
            if (code_point_offset == 0)
                break;
        }

        if (byte_offset > end_byte_offset)
            break;

        if (byte_offset < start_byte_offset)
            ++range.start;

        if (byte_offset < end_byte_offset)
            ++range.end;

        byte_offset += code_point_length_in_utf8(m_buffer[reverse ? --code_point_offset : code_point_offset++]);
    }

    return range;
}

void Editor::stylize(const Span& span, const Style& style)
{
    if (style.is_empty())
        return;

    auto start = span.beginning();
    auto end = span.end();

    if (span.mode() == Span::ByteOriented) {
        auto offsets = byte_offset_range_to_code_point_offset_range(start, end, 0);

        start = offsets.start;
        end = offsets.end;
    }

    auto& spans_starting = style.is_anchored() ? m_anchored_spans_starting : m_spans_starting;
    auto& spans_ending = style.is_anchored() ? m_anchored_spans_ending : m_spans_ending;

    auto starting_map = spans_starting.get(start).value_or({});

    if (!starting_map.contains(end))
        m_refresh_needed = true;

    starting_map.set(end, style);

    spans_starting.set(start, starting_map);

    auto ending_map = spans_ending.get(end).value_or({});

    if (!ending_map.contains(start))
        m_refresh_needed = true;
    ending_map.set(start, style);

    spans_ending.set(end, ending_map);
}

void Editor::suggest(size_t invariant_offset, size_t static_offset, Span::Mode offset_mode) const
{
    auto internal_static_offset = static_offset;
    auto internal_invariant_offset = invariant_offset;
    if (offset_mode == Span::Mode::ByteOriented) {
        // FIXME: We're assuming that invariant_offset points to the end of the available data
        //        this is not necessarily true, but is true in most cases.
        auto offsets = byte_offset_range_to_code_point_offset_range(internal_static_offset, internal_invariant_offset + internal_static_offset, m_cursor - 1, true);

        internal_static_offset = offsets.start;
        internal_invariant_offset = offsets.end - offsets.start;
    }
    m_suggestion_manager.set_suggestion_variants(internal_static_offset, internal_invariant_offset, 0);
}

void Editor::initialize()
{
    if (m_initialized)
        return;

    struct termios termios;
    tcgetattr(0, &termios);
    m_default_termios = termios; // grab a copy to restore
    if (m_was_resized)
        get_terminal_size();

    if (m_configuration.operation_mode == Configuration::Unset) {
        auto istty = isatty(STDIN_FILENO) && isatty(STDERR_FILENO);
        if (!istty) {
            m_configuration.set(Configuration::NonInteractive);
        } else {
            auto* term = getenv("TERM");
            if (StringView { term }.starts_with("xterm"))
                m_configuration.set(Configuration::Full);
            else
                m_configuration.set(Configuration::NoEscapeSequences);
        }
    }

    // Because we use our own line discipline which includes echoing,
    // we disable ICANON and ECHO.
    if (m_configuration.operation_mode == Configuration::Full) {
        termios.c_lflag &= ~(ECHO | ICANON);
        tcsetattr(0, TCSANOW, &termios);
    }

    m_termios = termios;

    set_default_keybinds();
    for (auto& keybind : m_configuration.keybindings)
        register_key_input_callback(keybind);

    Core::EventLoop::register_signal(SIGINT, [this](int) {
        interrupted();
    });

    Core::EventLoop::register_signal(SIGWINCH, [this](int) {
        resized();
    });

    m_initialized = true;
}

void Editor::interrupted()
{
    if (!m_is_editing)
        return;

    m_was_interrupted = true;
    handle_interrupt_event();
    if (!m_finish)
        return;

    m_finish = false;
    reposition_cursor(true);
    if (m_suggestion_display->cleanup())
        reposition_cursor();
    cleanup();
    fprintf(stderr, "\n");
    fflush(stderr);
    m_buffer.clear();
    m_is_editing = false;
    restore();
    m_notifier->set_event_mask(Core::Notifier::None);
    deferred_invoke([this](auto&) {
        remove_child(*m_notifier);
        m_notifier = nullptr;
        Core::EventLoop::current().quit(Retry);
    });
}

void Editor::really_quit_event_loop()
{
    m_finish = false;
    reposition_cursor(true);
    fprintf(stderr, "\n");
    fflush(stderr);
    auto string = line();
    m_buffer.clear();
    m_is_editing = false;
    restore();

    m_returned_line = string;

    m_notifier->set_event_mask(Core::Notifier::None);
    deferred_invoke([this](auto&) {
        remove_child(*m_notifier);
        m_notifier = nullptr;
        Core::EventLoop::current().quit(Exit);
    });
}

auto Editor::get_line(const String& prompt) -> Result<String, Editor::Error>
{
    initialize();
    m_is_editing = true;

    if (m_configuration.operation_mode == Configuration::NoEscapeSequences || m_configuration.operation_mode == Configuration::NonInteractive) {
        // Do not use escape sequences, instead, use LibC's getline.
        size_t size = 0;
        char* line = nullptr;
        // Show the prompt only on interactive mode (NoEscapeSequences in this case).
        if (m_configuration.operation_mode != Configuration::NonInteractive)
            fputs(prompt.characters(), stderr);
        auto line_length = getline(&line, &size, stdin);
        // getline() returns -1 and sets errno=0 on EOF.
        if (line_length == -1) {
            if (line)
                free(line);
            if (errno == 0)
                return Error::Eof;

            return Error::ReadFailure;
        }
        restore();
        if (line) {
            String result { line, (size_t)line_length, Chomp };
            free(line);
            return result;
        }

        return Error::ReadFailure;
    }

    set_prompt(prompt);
    reset();
    set_origin();
    strip_styles(true);

    m_history_cursor = m_history.size();

    refresh_display();

    Core::EventLoop loop;

    m_notifier = Core::Notifier::construct(STDIN_FILENO, Core::Notifier::Read);
    add_child(*m_notifier);

    m_notifier->on_ready_to_read = [&] {
        if (m_was_interrupted) {
            handle_interrupt_event();
        }

        handle_read_event();

        if (m_always_refresh)
            m_refresh_needed = true;

        refresh_display();

        if (m_finish)
            really_quit_event_loop();
    };

    if (loop.exec() == Retry)
        return get_line(prompt);

    return m_input_error.has_value() ? Result<String, Editor::Error> { m_input_error.value() } : Result<String, Editor::Error> { m_returned_line };
}

void Editor::save_to(JsonObject& object)
{
    Core::Object::save_to(object);
    object.set("is_searching", m_is_searching);
    object.set("is_editing", m_is_editing);
    object.set("cursor_offset", m_cursor);
    object.set("needs_refresh", m_refresh_needed);
    object.set("unprocessed_characters", m_incomplete_data.size());
    object.set("history_size", m_history.size());
    object.set("current_prompt", m_new_prompt);
    object.set("was_interrupted", m_was_interrupted);
    JsonObject display_area;
    display_area.set("top_left_row", m_origin_row);
    display_area.set("top_left_column", m_origin_column);
    display_area.set("line_count", num_lines());
    object.set("used_display_area", move(display_area));
}

void Editor::handle_interrupt_event()
{
    m_was_interrupted = false;

    auto cb = m_key_callbacks.get(ctrl('C'));
    if (cb.has_value()) {
        if (!cb.value()->callback(*this)) {
            // Oh well.
            return;
        }
    }

    fprintf(stderr, "^C");
    fflush(stderr);

    if (on_interrupt_handled)
        on_interrupt_handled();

    m_buffer.clear();
    m_cursor = 0;

    finish();
}

void Editor::handle_read_event()
{
    char keybuf[16];
    ssize_t nread = 0;

    if (!m_incomplete_data.size())
        nread = read(0, keybuf, sizeof(keybuf));

    if (nread < 0) {
        if (errno == EINTR) {
            if (!m_was_interrupted) {
                if (m_was_resized)
                    return;

                finish();
                return;
            }

            handle_interrupt_event();
            return;
        }

        ScopedValueRollback errno_restorer(errno);
        perror("read failed");

        m_input_error = Error::ReadFailure;
        finish();
        return;
    }

    m_incomplete_data.append(keybuf, nread);
    nread = m_incomplete_data.size();

    if (nread == 0) {
        m_input_error = Error::Empty;
        finish();
        return;
    }

    auto reverse_tab = false;

    // Discard starting bytes until they make sense as utf-8.
    size_t valid_bytes = 0;
    while (nread) {
        Utf8View { StringView { m_incomplete_data.data(), (size_t)nread } }.validate(valid_bytes);
        if (valid_bytes)
            break;
        m_incomplete_data.take_first();
        --nread;
    }

    Utf8View input_view { StringView { m_incomplete_data.data(), valid_bytes } };
    size_t consumed_code_points = 0;

    Vector<u8, 4> csi_parameter_bytes;
    Vector<unsigned, 4> csi_parameters;
    Vector<u8> csi_intermediate_bytes;
    u8 csi_final;
    enum CSIMod {
        Shift = 1,
        Alt = 2,
        Ctrl = 4,
    };

    for (auto code_point : input_view) {
        if (m_finish)
            break;

        ++consumed_code_points;

        if (code_point == 0)
            continue;

        switch (m_state) {
        case InputState::GotEscape:
            switch (code_point) {
            case '[':
                m_state = InputState::CSIExpectParameter;
                continue;
            default: {
                m_state = InputState::Free;
                auto cb = m_key_callbacks.get({ code_point, Key::Alt });
                if (cb.has_value()) {
                    if (!cb.value()->callback(*this)) {
                        // There's nothing interesting to do here.
                    }
                }
                cleanup_suggestions();
                continue;
            }
            }
        case InputState::CSIExpectParameter:
            if (code_point >= 0x30 && code_point <= 0x3f) { // '0123456789:;<=>?'
                csi_parameter_bytes.append(code_point);
                continue;
            }
            m_state = InputState::CSIExpectIntermediate;
            [[fallthrough]];
        case InputState::CSIExpectIntermediate:
            if (code_point >= 0x20 && code_point <= 0x2f) { // ' !"#$%&\'()*+,-./'
                csi_intermediate_bytes.append(code_point);
                continue;
            }
            m_state = InputState::CSIExpectFinal;
            [[fallthrough]];
        case InputState::CSIExpectFinal: {
            m_state = InputState::Free;
            if (!(code_point >= 0x40 && code_point <= 0x7f)) {
                dbgprintf("LibLine: Invalid CSI: %02x (%c)\r\n", code_point, code_point);
                continue;
            }
            csi_final = code_point;

            for (auto& parameter : String::copy(csi_parameter_bytes).split(';')) {
                if (auto value = parameter.to_uint(); value.has_value())
                    csi_parameters.append(value.value());
                else
                    csi_parameters.append(0);
            }
            unsigned param1 = 0, param2 = 0;
            if (csi_parameters.size() >= 1)
                param1 = csi_parameters[0];
            if (csi_parameters.size() >= 2)
                param2 = csi_parameters[1];
            unsigned modifiers = param2 ? param2 - 1 : 0;

            if (csi_final == 'Z') {
                // 'reverse tab'
                reverse_tab = true;
                break;
            }
            cleanup_suggestions();

            switch (csi_final) {
            case 'A': // ^[[A: arrow up
                search_backwards();
                continue;
            case 'B': // ^[[B: arrow down
                search_forwards();
                continue;
            case 'D': // ^[[D: arrow left
                if (modifiers == CSIMod::Alt || modifiers == CSIMod::Ctrl)
                    cursor_left_word();
                else
                    cursor_left_character();
                continue;
            case 'C': // ^[[C: arrow right
                if (modifiers == CSIMod::Alt || modifiers == CSIMod::Ctrl)
                    cursor_right_word();
                else
                    cursor_right_character();
                continue;
            case 'H': // ^[[H: home
                go_home();
                continue;
            case 'F': // ^[[F: end
                go_end();
                continue;
            case '~':
                if (param1 == 3) { // ^[[3~: delete
                    if (modifiers == CSIMod::Ctrl)
                        erase_alnum_word_forwards();
                    else
                        erase_character_forwards();
                    m_search_offset = 0;
                    continue;
                }
                // ^[[5~: page up
                // ^[[6~: page down
                dbgprintf("LibLine: Unhandled '~': %d\r\n", param1);
                continue;
            default:
                dbgprintf("LibLine: Unhandled final: %02x (%c)\r\n", code_point, code_point);
                continue;
            }
            break;
        }
        case InputState::Free:
            if (code_point == 27) {
                m_state = InputState::GotEscape;
                continue;
            }
            break;
        }

        // There are no sequences past this point, so short of 'tab', we will want to cleanup the suggestions.
        ArmedScopeGuard suggestion_cleanup { [this] { cleanup_suggestions(); } };

        // Normally ^D. `stty eof \^n` can change it to ^N (or something else), but Serenity doesn't have `stty` yet.
        // Process this here since the keybinds might override its behaviour.
        // This only applies when the buffer is empty. at any other time, the behaviour should be configurable.
        if (code_point == m_termios.c_cc[VEOF] && m_buffer.size() == 0) {
            finish_edit();
            continue;
        }

        auto cb = m_key_callbacks.get(code_point);
        if (cb.has_value()) {
            if (!cb.value()->callback(*this)) {
                continue;
            }
        }
        m_search_offset = 0; // reset search offset on any key

        if (code_point == '\t' || reverse_tab) {
            suggestion_cleanup.disarm();

            if (!on_tab_complete)
                continue;

            // Reverse tab can count as regular tab here.
            m_times_tab_pressed++;

            int token_start = m_cursor;

            // Ask for completions only on the first tab
            // and scan for the largest common prefix to display,
            // further tabs simply show the cached completions.
            if (m_times_tab_pressed == 1) {
                m_suggestion_manager.set_suggestions(on_tab_complete(*this));
                m_prompt_lines_at_suggestion_initiation = num_lines();
                if (m_suggestion_manager.count() == 0) {
                    // There are no suggestions, beep.
                    fputc('\a', stderr);
                    fflush(stderr);
                }
            }

            // Adjust already incremented / decremented index when switching tab direction.
            if (reverse_tab && m_tab_direction != TabDirection::Backward) {
                m_suggestion_manager.previous();
                m_suggestion_manager.previous();
                m_tab_direction = TabDirection::Backward;
            }
            if (!reverse_tab && m_tab_direction != TabDirection::Forward) {
                m_suggestion_manager.next();
                m_suggestion_manager.next();
                m_tab_direction = TabDirection::Forward;
            }
            reverse_tab = false;

            auto completion_mode = m_times_tab_pressed == 1 ? SuggestionManager::CompletePrefix : m_times_tab_pressed == 2 ? SuggestionManager::ShowSuggestions : SuggestionManager::CycleSuggestions;

            auto completion_result = m_suggestion_manager.attempt_completion(completion_mode, token_start);

            auto new_cursor = m_cursor + completion_result.new_cursor_offset;
            for (size_t i = completion_result.offset_region_to_remove.start; i < completion_result.offset_region_to_remove.end; ++i)
                remove_at_index(new_cursor);

            m_cursor = new_cursor;
            m_inline_search_cursor = new_cursor;
            m_refresh_needed = true;

            for (auto& view : completion_result.insert)
                insert(view);

            if (completion_result.style_to_apply.has_value()) {
                // Apply the style of the last suggestion.
                readjust_anchored_styles(m_suggestion_manager.current_suggestion().start_index, ModificationKind::ForcedOverlapRemoval);
                stylize({ m_suggestion_manager.current_suggestion().start_index, m_cursor, Span::Mode::CodepointOriented }, completion_result.style_to_apply.value());
            }

            switch (completion_result.new_completion_mode) {
            case SuggestionManager::DontComplete:
                m_times_tab_pressed = 0;
                break;
            case SuggestionManager::CompletePrefix:
                break;
            default:
                ++m_times_tab_pressed;
                break;
            }

            if (m_times_tab_pressed > 1) {
                if (m_suggestion_manager.count() > 0) {
                    if (m_suggestion_display->cleanup())
                        reposition_cursor();

                    m_suggestion_display->set_initial_prompt_lines(m_prompt_lines_at_suggestion_initiation);

                    m_suggestion_display->display(m_suggestion_manager);

                    m_origin_row = m_suggestion_display->origin_row();
                }
            }

            if (m_times_tab_pressed > 2) {
                if (m_tab_direction == TabDirection::Forward)
                    m_suggestion_manager.next();
                else
                    m_suggestion_manager.previous();
            }

            if (m_suggestion_manager.count() < 2) {
                // We have none, or just one suggestion,
                // we should just commit that and continue
                // after it, as if it were auto-completed.
                suggest(0, 0, Span::CodepointOriented);
                m_times_tab_pressed = 0;
                m_suggestion_manager.reset();
                m_suggestion_display->finish();
            }
            continue;
        }

        insert(code_point);
    }

    if (consumed_code_points == m_incomplete_data.size()) {
        m_incomplete_data.clear();
    } else {
        for (size_t i = 0; i < consumed_code_points; ++i)
            m_incomplete_data.take_first();
    }
}

void Editor::cleanup_suggestions()
{
    if (m_times_tab_pressed) {
        // Apply the style of the last suggestion.
        readjust_anchored_styles(m_suggestion_manager.current_suggestion().start_index, ModificationKind::ForcedOverlapRemoval);
        stylize({ m_suggestion_manager.current_suggestion().start_index, m_cursor, Span::Mode::CodepointOriented }, m_suggestion_manager.current_suggestion().style);
        // We probably have some suggestions drawn,
        // let's clean them up.
        if (m_suggestion_display->cleanup()) {
            reposition_cursor();
            m_refresh_needed = true;
        }
        m_suggestion_manager.reset();
        suggest(0, 0, Span::CodepointOriented);
        m_suggestion_display->finish();
    }
    m_times_tab_pressed = 0; // Safe to say if we get here, the user didn't press TAB
}

bool Editor::search(const StringView& phrase, bool allow_empty, bool from_beginning)
{

    int last_matching_offset = -1;
    bool found = false;

    // Do not search for empty strings.
    if (allow_empty || phrase.length() > 0) {
        size_t search_offset = m_search_offset;
        for (size_t i = m_history_cursor; i > 0; --i) {
            auto& entry = m_history[i - 1];
            auto contains = from_beginning ? entry.starts_with(phrase) : entry.contains(phrase);
            if (contains) {
                last_matching_offset = i - 1;
                if (search_offset == 0) {
                    found = true;
                    break;
                }
                --search_offset;
            }
        }

        if (!found) {
            fputc('\a', stderr);
            fflush(stderr);
        }
    }

    if (found) {
        m_buffer.clear();
        m_cursor = 0;
        insert(m_history[last_matching_offset]);
        // Always needed, as we have cleared the buffer above.
        m_refresh_needed = true;
    }

    return found;
}

void Editor::recalculate_origin()
{
    // Changing the columns can affect our origin if
    // the new size is smaller than our prompt, which would
    // cause said prompt to take up more space, so we should
    // compensate for that.
    if (m_cached_prompt_metrics.max_line_length >= m_num_columns) {
        auto added_lines = (m_cached_prompt_metrics.max_line_length + 1) / m_num_columns - 1;
        m_origin_row += added_lines;
    }

    // We also need to recalculate our cursor position,
    // but that will be calculated and applied at the next
    // refresh cycle.
}
void Editor::cleanup()
{
    auto current_buffer_metrics = actual_rendered_string_metrics(buffer_view());
    auto new_lines = current_prompt_metrics().lines_with_addition(current_buffer_metrics, m_num_columns);
    auto shown_lines = num_lines();
    if (new_lines < shown_lines)
        m_extra_forward_lines = max(shown_lines - new_lines, m_extra_forward_lines);

    VT::move_relative(-m_extra_forward_lines, m_pending_chars.size() - m_chars_inserted_in_the_middle);
    auto current_line = cursor_line();

    // There's a newline at the top, don't clear that line.
    if (current_prompt_metrics().line_lengths.first() == 0)
        --current_line;
    VT::clear_lines(current_line - 1, num_lines() - current_line + m_extra_forward_lines);
    m_extra_forward_lines = 0;
    reposition_cursor();
};

void Editor::refresh_display()
{
    auto has_cleaned_up = false;
    // Someone changed the window size, figure it out
    // and react to it, we might need to redraw.
    if (m_was_resized) {
        if (m_previous_num_columns != m_num_columns) {
            // We need to cleanup and redo everything.
            m_cached_prompt_valid = false;
            m_refresh_needed = true;
            swap(m_previous_num_columns, m_num_columns);
            recalculate_origin();
            cleanup();
            swap(m_previous_num_columns, m_num_columns);
            has_cleaned_up = true;
        }
        m_was_resized = false;
    }
    // Do not call hook on pure cursor movement.
    if (m_cached_prompt_valid && !m_refresh_needed && m_pending_chars.size() == 0) {
        // Probably just moving around.
        reposition_cursor();
        m_cached_buffer_metrics = actual_rendered_string_metrics(buffer_view());
        return;
    }
    // We might be at the last line, and have more than one line;
    // Refreshing the display will cause the terminal to scroll,
    // so note that fact and bring origin up.
    auto current_num_lines = num_lines();
    if (m_origin_row + current_num_lines > m_num_lines + 1) {
        if (current_num_lines > m_num_lines)
            m_origin_row = 0;
        else
            m_origin_row = m_num_lines - current_num_lines + 1;
    }

    if (on_display_refresh)
        on_display_refresh(*this);

    if (m_cached_prompt_valid) {
        if (!m_refresh_needed && m_cursor == m_buffer.size()) {
            // Just write the characters out and continue,
            // no need to refresh the entire line.
            char null = 0;
            m_pending_chars.append(&null, 1);
            fputs((char*)m_pending_chars.data(), stderr);
            m_pending_chars.clear();
            m_drawn_cursor = m_cursor;
            m_cached_buffer_metrics = actual_rendered_string_metrics(buffer_view());
            fflush(stderr);
            return;
        }
    }

    // Ouch, reflow entire line.
    if (!has_cleaned_up) {
        cleanup();
    }
    VT::move_absolute(m_origin_row, m_origin_column);

    fputs(m_new_prompt.characters(), stderr);

    VT::clear_to_end_of_line();
    HashMap<u32, Style> empty_styles {};
    StringBuilder builder;
    for (size_t i = 0; i < m_buffer.size(); ++i) {
        auto ends = m_spans_ending.get(i).value_or(empty_styles);
        auto starts = m_spans_starting.get(i).value_or(empty_styles);

        auto anchored_ends = m_anchored_spans_ending.get(i).value_or(empty_styles);
        auto anchored_starts = m_anchored_spans_starting.get(i).value_or(empty_styles);

        if (ends.size() || anchored_ends.size()) {
            Style style;

            for (auto& applicable_style : ends)
                style.unify_with(applicable_style.value);

            for (auto& applicable_style : anchored_ends)
                style.unify_with(applicable_style.value);

            // Disable any style that should be turned off.
            VT::apply_style(style, false);

            // Reapply styles for overlapping spans that include this one.
            style = find_applicable_style(i);
            VT::apply_style(style, true);
        }
        if (starts.size() || anchored_starts.size()) {
            Style style;

            for (auto& applicable_style : starts)
                style.unify_with(applicable_style.value);

            for (auto& applicable_style : anchored_starts)
                style.unify_with(applicable_style.value);

            // Set new styles.
            VT::apply_style(style, true);
        }
        builder.clear();
        builder.append(Utf32View { &m_buffer[i], 1 });
        fputs(builder.to_string().characters(), stderr);
    }

    VT::apply_style(Style::reset_style()); // don't bleed to EOL

    m_pending_chars.clear();
    m_refresh_needed = false;
    m_cached_buffer_metrics = actual_rendered_string_metrics(buffer_view());
    m_chars_inserted_in_the_middle = 0;
    if (!m_cached_prompt_valid) {
        m_cached_prompt_valid = true;
    }

    reposition_cursor();
    fflush(stderr);
}

void Editor::strip_styles(bool strip_anchored)
{
    m_spans_starting.clear();
    m_spans_ending.clear();

    if (strip_anchored) {
        m_anchored_spans_starting.clear();
        m_anchored_spans_ending.clear();
    }

    m_refresh_needed = true;
}

void Editor::reposition_cursor(bool to_end)
{
    auto cursor = m_cursor;
    auto saved_cursor = m_cursor;
    if (to_end)
        cursor = m_buffer.size();

    m_cursor = cursor;
    m_drawn_cursor = cursor;

    auto line = cursor_line() - 1;
    auto column = offset_in_line();

    VT::move_absolute(line + m_origin_row, column + m_origin_column);

    m_cursor = saved_cursor;
}

void VT::move_absolute(u32 row, u32 col)
{
    fprintf(stderr, "\033[%d;%dH", row, col);
    fflush(stderr);
}

void VT::move_relative(int row, int col)
{
    char x_op = 'A', y_op = 'D';

    if (row > 0)
        x_op = 'B';
    else
        row = -row;
    if (col > 0)
        y_op = 'C';
    else
        col = -col;

    if (row > 0)
        fprintf(stderr, "\033[%d%c", row, x_op);
    if (col > 0)
        fprintf(stderr, "\033[%d%c", col, y_op);
}

Style Editor::find_applicable_style(size_t offset) const
{
    // Walk through our styles and merge all that fit in the offset.
    auto style = Style::reset_style();
    auto unify = [&](auto& entry) {
        if (entry.key >= offset)
            return;
        for (auto& style_value : entry.value) {
            if (style_value.key <= offset)
                return;
            style.unify_with(style_value.value, true);
        }
    };

    for (auto& entry : m_spans_starting) {
        unify(entry);
    }

    for (auto& entry : m_anchored_spans_starting) {
        unify(entry);
    }

    return style;
}

String Style::Background::to_vt_escape() const
{
    if (is_default())
        return "";

    if (m_is_rgb) {
        return String::format("\033[48;2;%d;%d;%dm", m_rgb_color[0], m_rgb_color[1], m_rgb_color[2]);
    } else {
        return String::format("\033[%dm", (u8)m_xterm_color + 40);
    }
}

String Style::Foreground::to_vt_escape() const
{
    if (is_default())
        return "";

    if (m_is_rgb) {
        return String::format("\033[38;2;%d;%d;%dm", m_rgb_color[0], m_rgb_color[1], m_rgb_color[2]);
    } else {
        return String::format("\033[%dm", (u8)m_xterm_color + 30);
    }
}

String Style::Hyperlink::to_vt_escape(bool starting) const
{
    if (is_empty())
        return "";

    return String::format("\033]8;;%s\033\\", starting ? m_link.characters() : "");
}

void Style::unify_with(const Style& other, bool prefer_other)
{
    // Unify colors.
    if (prefer_other || m_background.is_default())
        m_background = other.background();

    if (prefer_other || m_foreground.is_default())
        m_foreground = other.foreground();

    // Unify graphic renditions.
    if (other.bold())
        set(Bold);

    if (other.italic())
        set(Italic);

    if (other.underline())
        set(Underline);

    // Unify links.
    if (prefer_other || m_hyperlink.is_empty())
        m_hyperlink = other.hyperlink();
}

String Style::to_string() const
{
    StringBuilder builder;
    builder.append("Style { ");

    if (!m_foreground.is_default()) {
        builder.append("Foreground(");
        if (m_foreground.m_is_rgb) {
            builder.join(", ", m_foreground.m_rgb_color);
        } else {
            builder.appendf("(XtermColor) %d", m_foreground.m_xterm_color);
        }
        builder.append("), ");
    }

    if (!m_background.is_default()) {
        builder.append("Background(");
        if (m_background.m_is_rgb) {
            builder.join(' ', m_background.m_rgb_color);
        } else {
            builder.appendf("(XtermColor) %d", m_background.m_xterm_color);
        }
        builder.append("), ");
    }

    if (bold())
        builder.append("Bold, ");

    if (underline())
        builder.append("Underline, ");

    if (italic())
        builder.append("Italic, ");

    if (!m_hyperlink.is_empty())
        builder.appendf("Hyperlink(\"%s\"), ", m_hyperlink.m_link.characters());

    builder.append("}");

    return builder.build();
}

void VT::apply_style(const Style& style, bool is_starting)
{
    if (is_starting) {
        fprintf(stderr,
            "\033[%d;%d;%dm%s%s%s",
            style.bold() ? 1 : 22,
            style.underline() ? 4 : 24,
            style.italic() ? 3 : 23,
            style.background().to_vt_escape().characters(),
            style.foreground().to_vt_escape().characters(),
            style.hyperlink().to_vt_escape(true).characters());
    } else {
        fprintf(stderr, "%s", style.hyperlink().to_vt_escape(false).characters());
    }
}

void VT::clear_lines(size_t count_above, size_t count_below)
{
    // Go down count_below lines.
    if (count_below > 0)
        fprintf(stderr, "\033[%dB", (int)count_below);
    // Then clear lines going upwards.
    for (size_t i = count_below + count_above; i > 0; --i)
        fputs(i == 1 ? "\033[2K" : "\033[2K\033[A", stderr);
}

void VT::save_cursor()
{
    fputs("\033[s", stderr);
    fflush(stderr);
}

void VT::restore_cursor()
{
    fputs("\033[u", stderr);
    fflush(stderr);
}

void VT::clear_to_end_of_line()
{
    fputs("\033[K", stderr);
    fflush(stderr);
}

StringMetrics Editor::actual_rendered_string_metrics(const StringView& string)
{
    size_t length { 0 };
    StringMetrics metrics;
    VTState state { Free };
    Utf8View view { string };
    auto it = view.begin();

    for (; it != view.end(); ++it) {
        auto c = *it;
        auto it_copy = it;
        ++it_copy;
        auto next_c = it_copy == view.end() ? 0 : *it_copy;
        state = actual_rendered_string_length_step(metrics, length, c, next_c, state);
    }

    metrics.line_lengths.append(length);

    for (auto& line : metrics.line_lengths)
        metrics.max_line_length = max(line, metrics.max_line_length);

    return metrics;
}

StringMetrics Editor::actual_rendered_string_metrics(const Utf32View& view)
{
    size_t length { 0 };
    StringMetrics metrics;
    VTState state { Free };

    for (size_t i = 0; i < view.length(); ++i) {
        auto c = view.code_points()[i];
        auto next_c = i + 1 < view.length() ? view.code_points()[i + 1] : 0;
        state = actual_rendered_string_length_step(metrics, length, c, next_c, state);
    }

    metrics.line_lengths.append(length);

    for (auto& line : metrics.line_lengths)
        metrics.max_line_length = max(line, metrics.max_line_length);

    return metrics;
}

Editor::VTState Editor::actual_rendered_string_length_step(StringMetrics& metrics, size_t& length, u32 c, u32 next_c, VTState state)
{
    switch (state) {
    case Free:
        if (c == '\x1b') { // escape
            return Escape;
        }
        if (c == '\r') { // carriage return
            length = 0;
            if (!metrics.line_lengths.is_empty())
                metrics.line_lengths.last() = 0;
            return state;
        }
        if (c == '\n') { // return
            metrics.line_lengths.append(length);
            length = 0;
            return state;
        }
        // FIXME: This will not support anything sophisticated
        ++length;
        ++metrics.total_length;
        return state;
    case Escape:
        if (c == ']') {
            if (next_c == '0')
                state = Title;
            return state;
        }
        if (c == '[') {
            return Bracket;
        }
        // FIXME: This does not support non-VT (aside from set-title) escapes
        return state;
    case Bracket:
        if (isdigit(c)) {
            return BracketArgsSemi;
        }
        return state;
    case BracketArgsSemi:
        if (c == ';') {
            return Bracket;
        }
        if (!isdigit(c))
            state = Free;
        return state;
    case Title:
        if (c == 7)
            state = Free;
        return state;
    }
    return state;
}

Vector<size_t, 2> Editor::vt_dsr()
{
    char buf[16];
    u32 length { 0 };

    // Read whatever junk there is before talking to the terminal
    // and insert them later when we're reading user input.
    bool more_junk_to_read { false };
    timeval timeout { 0, 0 };
    fd_set readfds;
    FD_ZERO(&readfds);
    FD_SET(0, &readfds);

    do {
        more_junk_to_read = false;
        (void)select(1, &readfds, nullptr, nullptr, &timeout);
        if (FD_ISSET(0, &readfds)) {
            auto nread = read(0, buf, 16);
            if (nread < 0) {
                m_input_error = Error::ReadFailure;
                finish();
                break;
            }

            if (nread == 0)
                break;

            m_incomplete_data.append(buf, nread);
            more_junk_to_read = true;
        }
    } while (more_junk_to_read);

    if (m_input_error.has_value())
        return { 1, 1 };

    fputs("\033[6n", stderr);
    fflush(stderr);

    do {
        auto nread = read(0, buf + length, 16 - length);
        if (nread < 0) {
            if (errno == 0) {
                // ????
                continue;
            }
            dbg() << "Error while reading DSR: " << strerror(errno);
            m_input_error = Error::ReadFailure;
            finish();
            return { 1, 1 };
        }
        if (nread == 0) {
            m_input_error = Error::Empty;
            finish();
            dbg() << "Terminal DSR issue; received no response";
            return { 1, 1 };
        }
        length += nread;
    } while (buf[length - 1] != 'R' && length < 16);
    size_t row { 1 }, col { 1 };

    if (buf[0] == '\033' && buf[1] == '[') {
        auto parts = StringView(buf + 2, length - 3).split_view(';');
        auto row_opt = parts[0].to_int();
        if (!row_opt.has_value()) {
            dbg() << "Terminal DSR issue; received garbage row";
        } else {
            row = row_opt.value();
        }
        auto col_opt = parts[1].to_int();
        if (!col_opt.has_value()) {
            dbg() << "Terminal DSR issue; received garbage col";
        } else {
            col = col_opt.value();
        }
    }
    return { row, col };
}

String Editor::line(size_t up_to_index) const
{
    StringBuilder builder;
    builder.append(Utf32View { m_buffer.data(), min(m_buffer.size(), up_to_index) });
    return builder.build();
}

void Editor::remove_at_index(size_t index)
{
    // See if we have any anchored styles, and reposition them if needed.
    readjust_anchored_styles(index, ModificationKind::Removal);
    auto cp = m_buffer[index];
    m_buffer.remove(index);
    if (cp == '\n')
        ++m_extra_forward_lines;
}

void Editor::readjust_anchored_styles(size_t hint_index, ModificationKind modification)
{
    struct Anchor {
        Span old_span;
        Span new_span;
        Style style;
    };
    Vector<Anchor> anchors_to_relocate;
    auto index_shift = modification == ModificationKind::Insertion ? 1 : -1;
    auto forced_removal = modification == ModificationKind::ForcedOverlapRemoval;

    for (auto& start_entry : m_anchored_spans_starting) {
        for (auto& end_entry : start_entry.value) {
            if (forced_removal) {
                if (start_entry.key <= hint_index && end_entry.key > hint_index) {
                    // Remove any overlapping regions.
                    continue;
                }
            }
            if (start_entry.key >= hint_index) {
                if (start_entry.key == hint_index && end_entry.key == hint_index + 1 && modification == ModificationKind::Removal) {
                    // Remove the anchor, as all its text was wiped.
                    continue;
                }
                // Shift everything.
                anchors_to_relocate.append({ { start_entry.key, end_entry.key, Span::Mode::CodepointOriented }, { start_entry.key + index_shift, end_entry.key + index_shift, Span::Mode::CodepointOriented }, end_entry.value });
                continue;
            }
            if (end_entry.key > hint_index) {
                // Shift just the end.
                anchors_to_relocate.append({ { start_entry.key, end_entry.key, Span::Mode::CodepointOriented }, { start_entry.key, end_entry.key + index_shift, Span::Mode::CodepointOriented }, end_entry.value });
                continue;
            }
            anchors_to_relocate.append({ { start_entry.key, end_entry.key, Span::Mode::CodepointOriented }, { start_entry.key, end_entry.key, Span::Mode::CodepointOriented }, end_entry.value });
        }
    }

    m_anchored_spans_ending.clear();
    m_anchored_spans_starting.clear();
    // Pass over the relocations and update the stale entries.
    for (auto& relocation : anchors_to_relocate) {
        stylize(relocation.new_span, relocation.style);
    }
}

size_t StringMetrics::lines_with_addition(const StringMetrics& offset, size_t column_width) const
{
    size_t lines = 0;

    for (size_t i = 0; i < line_lengths.size() - 1; ++i)
        lines += (line_lengths[i] + column_width) / column_width;

    auto last = line_lengths.last();
    last += offset.line_lengths.first();
    lines += (last + column_width) / column_width;

    for (size_t i = 1; i < offset.line_lengths.size(); ++i)
        lines += (offset.line_lengths[i] + column_width) / column_width;

    return lines;
}
}
