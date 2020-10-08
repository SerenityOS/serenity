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
#include "Debugger/Debugger.h"
#include "EditorWrapper.h"
#include "HackStudio.h"
#include "Language.h"
#include <AK/ByteBuffer.h>
#include <AK/LexicalPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/CppSyntaxHighlighter.h>
#include <LibGUI/INISyntaxHighlighter.h>
#include <LibGUI/JSSyntaxHighlighter.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/ShellSyntaxHighlighter.h>
#include <LibGUI/SyntaxHighlighter.h>
#include <LibGUI/Window.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/OutOfProcessWebView.h>

// #define EDITOR_DEBUG

namespace HackStudio {

Editor::Editor()
{
    set_document(CodeDocument::create());
    m_documentation_tooltip_window = GUI::Window::construct();
    m_documentation_tooltip_window->set_rect(0, 0, 500, 400);
    m_documentation_tooltip_window->set_window_type(GUI::WindowType::Tooltip);
    m_documentation_page_view = m_documentation_tooltip_window->set_main_widget<Web::OutOfProcessWebView>();

    m_autocomplete_box = make<AutoCompleteBox>(make_weak_ptr());
}

Editor::~Editor()
{
}

EditorWrapper& Editor::wrapper()
{
    return static_cast<EditorWrapper&>(*parent());
}
const EditorWrapper& Editor::wrapper() const
{
    return static_cast<const EditorWrapper&>(*parent());
}

void Editor::focusin_event(GUI::FocusEvent& event)
{
    wrapper().set_editor_has_focus({}, true);
    if (on_focus)
        on_focus();
    GUI::TextEditor::focusin_event(event);
}

void Editor::focusout_event(GUI::FocusEvent& event)
{
    wrapper().set_editor_has_focus({}, false);
    GUI::TextEditor::focusout_event(event);
}

Gfx::IntRect Editor::breakpoint_icon_rect(size_t line_number) const
{
    auto ruler_line_rect = ruler_content_rect(line_number);

    auto scroll_value = vertical_scrollbar().value();
    ruler_line_rect = ruler_line_rect.translated({ 0, -scroll_value });
    auto center = ruler_line_rect.center().translated({ ruler_line_rect.width() - 10, -line_spacing() - 3 });
    constexpr int size = 32;
    return { center.x() - size / 2, center.y() - size / 2, size, size };
}

void Editor::paint_event(GUI::PaintEvent& event)
{
    GUI::TextEditor::paint_event(event);

    GUI::Painter painter(*this);
    if (is_focused()) {
        painter.add_clip_rect(event.rect());

        auto rect = frame_inner_rect();
        if (vertical_scrollbar().is_visible())
            rect.set_width(rect.width() - vertical_scrollbar().width());
        if (horizontal_scrollbar().is_visible())
            rect.set_height(rect.height() - horizontal_scrollbar().height());
        painter.draw_rect(rect, palette().selection());
    }

    if (ruler_visible()) {
        size_t first_visible_line = text_position_at(event.rect().top_left()).line();
        size_t last_visible_line = text_position_at(event.rect().bottom_right()).line();
        for (size_t line : breakpoint_lines()) {
            if (line < first_visible_line || line > last_visible_line) {
                continue;
            }
            const auto& icon = breakpoint_icon_bitmap();
            painter.blit(breakpoint_icon_rect(line).center(), icon, icon.rect());
        }
        if (execution_position().has_value()) {
            const auto& icon = current_position_icon_bitmap();
            painter.blit(breakpoint_icon_rect(execution_position().value()).center(), icon, icon.rect());
        }
    }
}

static HashMap<String, String>& man_paths()
{
    static HashMap<String, String> paths;
    if (paths.is_empty()) {
        // FIXME: This should also search man3, possibly other places..
        Core::DirIterator it("/usr/share/man/man2", Core::DirIterator::Flags::SkipDots);
        while (it.has_next()) {
            auto path = String::formatted("/usr/share/man/man2/{}", it.next_path());
            auto title = LexicalPath(path).title();
            paths.set(title, path);
        }
    }

    return paths;
}

void Editor::show_documentation_tooltip_if_available(const String& hovered_token, const Gfx::IntPoint& screen_location)
{
    auto it = man_paths().find(hovered_token);
    if (it == man_paths().end()) {
#ifdef EDITOR_DEBUG
        dbgln("no man path for {}", hovered_token);
#endif
        m_documentation_tooltip_window->hide();
        return;
    }

    if (m_documentation_tooltip_window->is_visible() && hovered_token == m_last_parsed_token) {
        return;
    }

#ifdef EDITOR_DEBUG
    dbgln("opening {}", it->value);
#endif
    auto file = Core::File::construct(it->value);
    if (!file->open(Core::File::ReadOnly)) {
        dbgln("failed to open {}, {}", it->value, file->error_string());
        return;
    }

    auto man_document = Markdown::Document::parse(file->read_all());

    if (!man_document) {
        dbgln("failed to parse markdown");
        return;
    }

    StringBuilder html;
    // FIXME: With the InProcessWebView we used to manipulate the document body directly,
    // With OutOfProcessWebView this isn't possible (at the moment). The ideal solution
    // is probably to tweak Markdown::Document::render_to_html() so we can inject styles
    // into the rendered HTML easily.
    html.append(man_document->render_to_html());
    html.append("<style>body { background-color: #dac7b5; }</style>");
    m_documentation_page_view->load_html(html.build(), {});

    m_documentation_tooltip_window->move_to(screen_location.translated(4, 4));
    m_documentation_tooltip_window->show();

    m_last_parsed_token = hovered_token;
}

void Editor::mousemove_event(GUI::MouseEvent& event)
{
    GUI::TextEditor::mousemove_event(event);

    if (document().spans().is_empty())
        return;

    auto text_position = text_position_at(event.position());
    if (!text_position.is_valid()) {
        m_documentation_tooltip_window->hide();
        return;
    }

    auto highlighter = wrapper().editor().syntax_highlighter();
    if (!highlighter)
        return;

    bool hide_tooltip = true;
    bool is_over_link = false;

    auto ruler_line_rect = ruler_content_rect(text_position.line());
    auto hovering_lines_ruler = (event.position().x() < ruler_line_rect.width());
    if (hovering_lines_ruler && !is_in_drag_select())
        set_override_cursor(Gfx::StandardCursor::Arrow);
    else if (m_hovering_editor)
        set_override_cursor(m_hovering_link && m_holding_ctrl ? Gfx::StandardCursor::Hand : Gfx::StandardCursor::IBeam);

    for (auto& span : document().spans()) {
        if (span.range.contains(m_previous_text_position) && !span.range.contains(text_position)) {
            if (highlighter->is_navigatable(span.data) && span.is_underlined) {
                span.is_underlined = false;
                wrapper().editor().update();
            }
        }

        if (span.range.contains(text_position)) {
            auto adjusted_range = span.range;
            auto end_line_length = document().line(span.range.end().line()).length();
            adjusted_range.end().set_column(min(end_line_length, adjusted_range.end().column() + 1));
            auto hovered_span_text = document().text_in_range(adjusted_range);
#ifdef EDITOR_DEBUG
            dbgln("Hovering: {} \"{}\"", adjusted_range, hovered_span_text);
#endif

            if (highlighter->is_navigatable(span.data)) {
                is_over_link = true;
                bool was_underlined = span.is_underlined;
                span.is_underlined = event.modifiers() & Mod_Ctrl;
                if (span.is_underlined != was_underlined) {
                    wrapper().editor().update();
                }
            }
            if (highlighter->is_identifier(span.data)) {
                show_documentation_tooltip_if_available(hovered_span_text, event.position().translated(screen_relative_rect().location()));
                hide_tooltip = false;
            }
        }
    }

    m_previous_text_position = text_position;
    if (hide_tooltip)
        m_documentation_tooltip_window->hide();

    m_hovering_link = is_over_link && (event.modifiers() & Mod_Ctrl);
}

void Editor::mousedown_event(GUI::MouseEvent& event)
{
    auto highlighter = wrapper().editor().syntax_highlighter();
    if (!highlighter) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    auto text_position = text_position_at(event.position());
    auto ruler_line_rect = ruler_content_rect(text_position.line());
    if (event.button() == GUI::MouseButton::Left && event.position().x() < ruler_line_rect.width()) {
        if (!breakpoint_lines().contains_slow(text_position.line())) {
            breakpoint_lines().append(text_position.line());
            Debugger::on_breakpoint_change(wrapper().filename_label().text(), text_position.line(), BreakpointChange::Added);
        } else {
            breakpoint_lines().remove_first_matching([&](size_t line) { return line == text_position.line(); });
            Debugger::on_breakpoint_change(wrapper().filename_label().text(), text_position.line(), BreakpointChange::Removed);
        }
    }

    if (!(event.modifiers() & Mod_Ctrl)) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    if (!text_position.is_valid()) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    for (auto& span : document().spans()) {
        if (span.range.contains(text_position)) {
            if (!highlighter->is_navigatable(span.data)) {
                GUI::TextEditor::mousedown_event(event);
                return;
            }

            auto adjusted_range = span.range;
            adjusted_range.end().set_column(adjusted_range.end().column() + 1);
            auto span_text = document().text_in_range(adjusted_range);
            auto header_path = span_text.substring(1, span_text.length() - 2);
#ifdef EDITOR_DEBUG
            dbgln("Ctrl+click: {} \"{}\"", adjusted_range, header_path);
#endif
            navigate_to_include_if_available(header_path);
            return;
        }
    }

    GUI::TextEditor::mousedown_event(event);
}

void Editor::keydown_event(GUI::KeyEvent& event)
{
    if (m_autocomplete_in_focus) {
        if (event.key() == Key_Escape) {
            m_autocomplete_in_focus = false;
            m_autocomplete_box->close();
            return;
        }
        if (event.key() == Key_Down) {
            m_autocomplete_box->next_suggestion();
            return;
        }
        if (event.key() == Key_Up) {
            m_autocomplete_box->previous_suggestion();
            return;
        }
        if (event.key() == Key_Return || event.key() == Key_Tab) {
            m_autocomplete_box->apply_suggestion();
            close_autocomplete();
            return;
        }
    }

    auto autocomplete_action = [this]() {
        auto data = get_autocomplete_request_data();
        if (data.has_value()) {
            update_autocomplete(data.value());
            if (m_autocomplete_in_focus)
                show_autocomplete(data.value());
        } else {
            close_autocomplete();
        }
    };

    if (event.key() == Key_Control)
        m_holding_ctrl = true;

    if (m_holding_ctrl && event.key() == Key_Space) {
        autocomplete_action();
    }
    GUI::TextEditor::keydown_event(event);

    if (m_autocomplete_in_focus) {
        autocomplete_action();
    }
}

void Editor::keyup_event(GUI::KeyEvent& event)
{
    if (event.key() == Key_Control)
        m_holding_ctrl = false;
    GUI::TextEditor::keyup_event(event);
}

void Editor::enter_event(Core::Event& event)
{
    m_hovering_editor = true;
    GUI::TextEditor::enter_event(event);
}

void Editor::leave_event(Core::Event& event)
{
    m_hovering_editor = false;
    GUI::TextEditor::leave_event(event);
}

static HashMap<String, String>& include_paths()
{
    static HashMap<String, String> paths;

    auto add_directory = [](String base, Optional<String> recursive, auto handle_directory) -> void {
        Core::DirIterator it(recursive.value_or(base), Core::DirIterator::Flags::SkipDots);
        while (it.has_next()) {
            auto path = it.next_full_path();
            if (!Core::File::is_directory(path)) {
                auto key = path.substring(base.length() + 1, path.length() - base.length() - 1);
#ifdef EDITOR_DEBUG
                dbgln("Adding header \"{}\" in path \"{}\"", key, path);
#endif
                paths.set(key, path);
            } else {
                handle_directory(base, path, handle_directory);
            }
        }
    };

    if (paths.is_empty()) {
        add_directory(".", {}, add_directory);
        add_directory("/usr/local/include", {}, add_directory);
        add_directory("/usr/local/include/c++/9.2.0", {}, add_directory);
        add_directory("/usr/include", {}, add_directory);
    }

    return paths;
}

void Editor::navigate_to_include_if_available(String path)
{
    auto it = include_paths().find(path);
    if (it == include_paths().end()) {
#ifdef EDITOR_DEBUG
        dbgln("no header {} found.", path);
#endif
        return;
    }

    on_open(it->value);
}

void Editor::set_execution_position(size_t line_number)
{
    code_document().set_execution_position(line_number);
    scroll_position_into_view({ line_number, 0 });
    update(breakpoint_icon_rect(line_number));
}

void Editor::clear_execution_position()
{
    if (!execution_position().has_value()) {
        return;
    }
    size_t previous_position = execution_position().value();
    code_document().clear_execution_position();
    update(breakpoint_icon_rect(previous_position));
}

const Gfx::Bitmap& Editor::breakpoint_icon_bitmap()
{
    static auto bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/breakpoint.png");
    return *bitmap;
}

const Gfx::Bitmap& Editor::current_position_icon_bitmap()
{
    static auto bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png");
    return *bitmap;
}

const CodeDocument& Editor::code_document() const
{
    const auto& doc = document();
    ASSERT(doc.is_code_document());
    return static_cast<const CodeDocument&>(doc);
}

CodeDocument& Editor::code_document()
{
    return const_cast<CodeDocument&>(static_cast<const Editor&>(*this).code_document());
}

void Editor::set_document(GUI::TextDocument& doc)
{
    ASSERT(doc.is_code_document());
    GUI::TextEditor::set_document(doc);

    CodeDocument& code_document = static_cast<CodeDocument&>(doc);
    switch (code_document.language()) {
    case Language::Cpp:
        set_syntax_highlighter(make<GUI::CppSyntaxHighlighter>());
        m_language_client = get_language_client<LanguageClients::Cpp::ServerConnection>(project().root_directory());
        break;
    case Language::JavaScript:
        set_syntax_highlighter(make<GUI::JSSyntaxHighlighter>());
        break;
    case Language::Ini:
        set_syntax_highlighter(make<GUI::IniSyntaxHighlighter>());
        break;
    case Language::Shell:
        set_syntax_highlighter(make<GUI::ShellSyntaxHighlighter>());
        m_language_client = get_language_client<LanguageClients::Shell::ServerConnection>(project().root_directory());
        break;
    default:
        set_syntax_highlighter(nullptr);
    }

    if (m_language_client)
        m_language_client->open_file(code_document.file_path().string());
}

Optional<Editor::AutoCompleteRequestData> Editor::get_autocomplete_request_data()
{
    if (!wrapper().editor().m_language_client)
        return {};

    return Editor::AutoCompleteRequestData { cursor() };
}

void Editor::update_autocomplete(const AutoCompleteRequestData& data)
{
    if (!m_language_client)
        return;

    m_language_client->on_autocomplete_suggestions = [=, this](auto suggestions) {
        if (suggestions.is_empty()) {
            close_autocomplete();
            return;
        }

        show_autocomplete(data);

        m_autocomplete_box->update_suggestions(move(suggestions));
        m_autocomplete_in_focus = true;
    };

    m_language_client->request_autocomplete(
        code_document().file_path().string(),
        data.position.line(),
        data.position.column());
}

void Editor::show_autocomplete(const AutoCompleteRequestData& data)
{
    auto suggestion_box_location = content_rect_for_position(data.position).bottom_right().translated(screen_relative_rect().top_left().translated(ruler_width(), 0).translated(10, 5));
    m_autocomplete_box->show(suggestion_box_location);
}

void Editor::close_autocomplete()
{
    m_autocomplete_box->close();
    m_autocomplete_in_focus = false;
}

void Editor::on_edit_action(const GUI::Command& command)
{
    if (!m_language_client)
        return;

    if (command.is_insert_text()) {
        const GUI::InsertTextCommand& insert_command = static_cast<const GUI::InsertTextCommand&>(command);
        m_language_client->insert_text(
            code_document().file_path().string(),
            insert_command.text(),
            insert_command.range().start().line(),
            insert_command.range().start().column());
        return;
    }

    if (command.is_remove_text()) {
        const GUI::RemoveTextCommand& remove_command = static_cast<const GUI::RemoveTextCommand&>(command);
        m_language_client->remove_text(
            code_document().file_path().string(),
            remove_command.range().start().line(),
            remove_command.range().start().column(),
            remove_command.range().end().line(),
            remove_command.range().end().column());
        return;
    }

    ASSERT_NOT_REACHED();
}

void Editor::undo()
{
    TextEditor::undo();
    flush_file_content_to_langauge_server();
}

void Editor::redo()
{
    TextEditor::redo();
    flush_file_content_to_langauge_server();
}

void Editor::flush_file_content_to_langauge_server()
{
    if (!m_language_client)
        return;

    m_language_client->set_file_content(
        code_document().file_path().string(),
        document().text());
}
}
