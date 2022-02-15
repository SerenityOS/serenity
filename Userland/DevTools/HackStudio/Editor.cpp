/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Editor.h"
#include "Debugger/Debugger.h"
#include "Debugger/EvaluateExpressionDialog.h"
#include "EditorWrapper.h"
#include "HackStudio.h"
#include "Language.h"
#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibCpp/SemanticSyntaxHighlighter.h>
#include <LibCpp/SyntaxHighlighter.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/GML/AutocompleteProvider.h>
#include <LibGUI/GML/SyntaxHighlighter.h>
#include <LibGUI/GitCommitSyntaxHighlighter.h>
#include <LibGUI/INISyntaxHighlighter.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Window.h>
#include <LibJS/SyntaxHighlighter.h>
#include <LibMarkdown/Document.h>
#include <LibSQL/AST/SyntaxHighlighter.h>
#include <LibWeb/CSS/SyntaxHighlighter/SyntaxHighlighter.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/SyntaxHighlighter/SyntaxHighlighter.h>
#include <LibWeb/OutOfProcessWebView.h>
#include <Shell/SyntaxHighlighter.h>
#include <fcntl.h>

namespace HackStudio {

ErrorOr<NonnullRefPtr<Editor>> Editor::try_create()
{
    NonnullRefPtr<Editor> editor = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Editor()));
    TRY(editor->initialize_documentation_tooltip());
    TRY(editor->initialize_parameters_hint_tooltip());
    return editor;
}

Editor::Editor()
{
    create_tokens_info_timer();

    set_document(CodeDocument::create());
    m_evaluate_expression_action = GUI::Action::create("Evaluate expression", { Mod_Ctrl, Key_E }, [this](auto&) {
        VERIFY(is_program_running());
        auto dialog = EvaluateExpressionDialog::construct(window());
        dialog->exec();
    });
    m_move_execution_to_line_action = GUI::Action::create("Set execution point to line", [this](auto&) {
        VERIFY(is_program_running());
        auto success = Debugger::the().set_execution_position(currently_open_file(), cursor().line());
        if (success) {
            set_execution_position(cursor().line());
        } else {
            GUI::MessageBox::show(window(), "Failed to set execution position", "Error", GUI::MessageBox::Type::Error);
        }
    });

    set_debug_mode(false);

    add_custom_context_menu_action(*m_evaluate_expression_action);
    add_custom_context_menu_action(*m_move_execution_to_line_action);

    set_gutter_visible(true);
}

ErrorOr<void> Editor::initialize_documentation_tooltip()
{
    m_documentation_tooltip_window = GUI::Window::construct();
    m_documentation_tooltip_window->set_rect(0, 0, 500, 400);
    m_documentation_tooltip_window->set_window_type(GUI::WindowType::Tooltip);
    m_documentation_page_view = TRY(m_documentation_tooltip_window->try_set_main_widget<Web::OutOfProcessWebView>());
    return {};
}

ErrorOr<void> Editor::initialize_parameters_hint_tooltip()
{
    m_parameters_hint_tooltip_window = GUI::Window::construct();
    m_parameters_hint_tooltip_window->set_rect(0, 0, 280, 35);
    m_parameters_hint_tooltip_window->set_window_type(GUI::WindowType::Tooltip);
    m_parameter_hint_page_view = TRY(m_parameters_hint_tooltip_window->try_set_main_widget<Web::OutOfProcessWebView>());
    return {};
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

Gfx::IntRect Editor::gutter_icon_rect(size_t line_number) const
{
    return gutter_content_rect(line_number).translated(ruler_width() + gutter_width() + frame_thickness(), -vertical_scrollbar().value());
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

    if (gutter_visible()) {
        size_t first_visible_line = text_position_at(event.rect().top_left()).line();
        size_t last_visible_line = text_position_at(event.rect().bottom_right()).line();

        for (size_t line : breakpoint_lines()) {
            if (line < first_visible_line || line > last_visible_line) {
                continue;
            }
            const auto& icon = breakpoint_icon_bitmap();
            painter.blit(gutter_icon_rect(line).top_left(), icon, icon.rect());
        }
        if (execution_position().has_value()) {
            const auto& icon = current_position_icon_bitmap();
            painter.blit(gutter_icon_rect(execution_position().value()).top_left(), icon, icon.rect());
        }

        if (wrapper().git_repo()) {
            for (auto& hunk : wrapper().hunks()) {
                auto start_line = hunk.target_start_line;
                auto finish_line = start_line + hunk.added_lines.size();

                auto additions = hunk.added_lines.size();
                auto deletions = hunk.removed_lines.size();

                for (size_t line_offset = 0; line_offset < additions; line_offset++) {
                    auto line = start_line + line_offset;
                    if (line < first_visible_line || line > last_visible_line) {
                        continue;
                    }
                    char const* sign = (line_offset < deletions) ? "!" : "+";
                    painter.draw_text(gutter_icon_rect(line), sign, font(), Gfx::TextAlignment::Center);
                }
                if (additions < deletions) {
                    auto deletions_line = min(finish_line, line_count() - 1);
                    if (deletions_line <= last_visible_line) {
                        painter.draw_text(gutter_icon_rect(deletions_line), "-", font(), Gfx::TextAlignment::Center);
                    }
                }
            }
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
            auto path = it.next_full_path();
            auto title = LexicalPath::title(path);
            paths.set(title, path);
        }
    }

    return paths;
}

void Editor::show_documentation_tooltip_if_available(const String& hovered_token, const Gfx::IntPoint& screen_location)
{
    auto it = man_paths().find(hovered_token);
    if (it == man_paths().end()) {
        dbgln_if(EDITOR_DEBUG, "no man path for {}", hovered_token);
        m_documentation_tooltip_window->hide();
        return;
    }

    if (m_documentation_tooltip_window->is_visible() && hovered_token == m_last_parsed_token) {
        return;
    }

    dbgln_if(EDITOR_DEBUG, "opening {}", it->value);
    auto file = Core::File::construct(it->value);
    if (!file->open(Core::OpenMode::ReadOnly)) {
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
    bool is_over_clickable = false;

    auto ruler_line_rect = ruler_content_rect(text_position.line());
    auto hovering_lines_ruler = (event.position().x() < ruler_line_rect.width());
    if (hovering_lines_ruler && !is_in_drag_select())
        set_override_cursor(Gfx::StandardCursor::Arrow);
    else if (m_hovering_editor)
        set_override_cursor(m_hovering_clickable && event.ctrl() ? Gfx::StandardCursor::Hand : Gfx::StandardCursor::IBeam);

    for (auto& span : document().spans()) {
        bool is_clickable = (highlighter->is_navigatable(span.data) || highlighter->is_identifier(span.data));
        if (span.range.contains(m_previous_text_position) && !span.range.contains(text_position)) {
            if (is_clickable && span.attributes.underline) {
                span.attributes.underline = false;
                wrapper().editor().update();
            }
        }

        if (span.range.contains(text_position)) {
            auto adjusted_range = span.range;
            auto end_line_length = document().line(span.range.end().line()).length();
            adjusted_range.end().set_column(min(end_line_length, adjusted_range.end().column() + 1));
            auto hovered_span_text = document().text_in_range(adjusted_range);
            dbgln_if(EDITOR_DEBUG, "Hovering: {} \"{}\"", adjusted_range, hovered_span_text);

            if (is_clickable) {
                is_over_clickable = true;
                bool was_underlined = span.attributes.underline;
                span.attributes.underline = event.modifiers() & Mod_Ctrl;
                if (span.attributes.underline != was_underlined) {
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

    m_hovering_clickable = (is_over_clickable) && (event.modifiers() & Mod_Ctrl);
}

void Editor::mousedown_event(GUI::MouseEvent& event)
{
    m_parameters_hint_tooltip_window->hide();

    auto highlighter = wrapper().editor().syntax_highlighter();
    if (!highlighter) {
        GUI::TextEditor::mousedown_event(event);
        return;
    }

    auto text_position = text_position_at(event.position());
    auto ruler_line_rect = ruler_content_rect(text_position.line());
    if (event.button() == GUI::MouseButton::Primary && event.position().x() < ruler_line_rect.width()) {
        if (!breakpoint_lines().contains_slow(text_position.line())) {
            breakpoint_lines().append(text_position.line());
            Debugger::the().on_breakpoint_change(wrapper().filename_label().text(), text_position.line(), BreakpointChange::Added);
        } else {
            breakpoint_lines().remove_first_matching([&](size_t line) { return line == text_position.line(); });
            Debugger::the().on_breakpoint_change(wrapper().filename_label().text(), text_position.line(), BreakpointChange::Removed);
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

    if (auto* span = document().span_at(text_position)) {
        if (highlighter->is_navigatable(span->data)) {
            on_navigatable_link_click(*span);
            return;
        }
        if (highlighter->is_identifier(span->data)) {
            on_identifier_click(*span);
            return;
        }
    }

    GUI::TextEditor::mousedown_event(event);
}

void Editor::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        window()->move_to_front();
        if (urls.size() > 1) {
            GUI::MessageBox::show(window(), "HackStudio can only open one file at a time!", "One at a time please!", GUI::MessageBox::Type::Error);
            return;
        }
        set_current_editor_wrapper(static_cast<EditorWrapper*>(parent()));
        open_file(urls.first().path());
    }
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
                dbgln_if(EDITOR_DEBUG, "Adding header \"{}\" in path \"{}\"", key, path);
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
        dbgln_if(EDITOR_DEBUG, "no header {} found.", path);
        return;
    }

    on_open(it->value);
}

void Editor::set_execution_position(size_t line_number)
{
    code_document().set_execution_position(line_number);
    scroll_position_into_view({ line_number, 0 });
    update(gutter_icon_rect(line_number));
}

void Editor::clear_execution_position()
{
    if (!execution_position().has_value()) {
        return;
    }
    size_t previous_position = execution_position().value();
    code_document().clear_execution_position();
    update(gutter_icon_rect(previous_position));
}

const Gfx::Bitmap& Editor::breakpoint_icon_bitmap()
{
    static auto bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/breakpoint.png").release_value_but_fixme_should_propagate_errors();
    return *bitmap;
}

const Gfx::Bitmap& Editor::current_position_icon_bitmap()
{
    static auto bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors();
    return *bitmap;
}

const CodeDocument& Editor::code_document() const
{
    const auto& doc = document();
    VERIFY(doc.is_code_document());
    return static_cast<const CodeDocument&>(doc);
}

CodeDocument& Editor::code_document()
{
    return const_cast<CodeDocument&>(static_cast<const Editor&>(*this).code_document());
}

void Editor::set_document(GUI::TextDocument& doc)
{
    if (has_document() && &document() == &doc)
        return;

    VERIFY(doc.is_code_document());
    GUI::TextEditor::set_document(doc);

    set_override_cursor(Gfx::StandardCursor::IBeam);

    auto& code_document = static_cast<CodeDocument&>(doc);

    set_language_client_for(code_document);
    set_syntax_highlighter_for(code_document);

    if (m_language_client) {
        set_autocomplete_provider(make<LanguageServerAidedAutocompleteProvider>(*m_language_client));
        // NOTE:
        // When a file is opened for the first time in HackStudio, its content is already synced with the filesystem.
        // Otherwise, if the file has already been opened before in some Editor instance, it should exist in the LanguageServer's
        // FileDB, and the LanguageServer should already have its up-to-date content.
        // So it's OK to just pass an fd here (rather than the TextDocument's content).
        int fd = open(code_document.file_path().characters(), O_RDONLY | O_NOCTTY);
        if (fd < 0) {
            perror("open");
            return;
        }
        m_language_client->open_file(code_document.file_path(), fd);
        close(fd);
    } else {
        set_autocomplete_provider_for(code_document);
    }
}

Optional<Editor::AutoCompleteRequestData> Editor::get_autocomplete_request_data()
{
    if (!wrapper().editor().m_language_client)
        return {};

    return Editor::AutoCompleteRequestData { cursor() };
}

void Editor::LanguageServerAidedAutocompleteProvider::provide_completions(Function<void(Vector<Entry>)> callback)
{
    auto& editor = static_cast<Editor&>(*m_editor).wrapper().editor();
    auto data = editor.get_autocomplete_request_data();
    if (!data.has_value())
        callback({});

    m_language_client.on_autocomplete_suggestions = [callback = move(callback)](auto suggestions) {
        callback(suggestions);
    };

    m_language_client.request_autocomplete(
        editor.code_document().file_path(),
        data.value().position.line(),
        data.value().position.column());
}

void Editor::will_execute(GUI::TextDocumentUndoCommand const& command)
{
    if (!m_language_client)
        return;

    if (is<GUI::InsertTextCommand>(command)) {
        auto const& insert_command = static_cast<GUI::InsertTextCommand const&>(command);
        m_language_client->insert_text(
            code_document().file_path(),
            insert_command.text(),
            insert_command.range().start().line(),
            insert_command.range().start().column());
        return;
    }

    if (is<GUI::RemoveTextCommand>(command)) {
        auto const& remove_command = static_cast<GUI::RemoveTextCommand const&>(command);
        m_language_client->remove_text(
            code_document().file_path(),
            remove_command.range().start().line(),
            remove_command.range().start().column(),
            remove_command.range().end().line(),
            remove_command.range().end().column());
        return;
    }

    VERIFY_NOT_REACHED();
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
        code_document().file_path(),
        document().text());
}

void Editor::on_navigatable_link_click(const GUI::TextDocumentSpan& span)
{
    auto span_text = document().text_in_range(span.range);
    auto header_path = span_text.substring(1, span_text.length() - 2);
    dbgln_if(EDITOR_DEBUG, "Ctrl+click: {} \"{}\"", span.range, header_path);
    navigate_to_include_if_available(header_path);
}

void Editor::on_identifier_click(const GUI::TextDocumentSpan& span)
{
    if (!m_language_client)
        return;

    m_language_client->on_declaration_found = [](const String& file, size_t line, size_t column) {
        HackStudio::open_file(file, line, column);
    };
    m_language_client->search_declaration(code_document().file_path(), span.range.start().line(), span.range.start().column());
}

void Editor::set_cursor(const GUI::TextPosition& a_position)
{
    TextEditor::set_cursor(a_position);
}

void Editor::set_syntax_highlighter_for(const CodeDocument& document)
{
    switch (document.language()) {
    case Language::Cpp:
        if (m_use_semantic_syntax_highlighting) {
            set_syntax_highlighter(make<Cpp::SemanticSyntaxHighlighter>());
            on_token_info_timer_tick();
            m_tokens_info_timer->restart();
        } else
            set_syntax_highlighter(make<Cpp::SyntaxHighlighter>());

        break;
    case Language::CSS:
        set_syntax_highlighter(make<Web::CSS::SyntaxHighlighter>());
        break;
    case Language::GitCommit:
        set_syntax_highlighter(make<GUI::GitCommitSyntaxHighlighter>());
        break;
    case Language::GML:
        set_syntax_highlighter(make<GUI::GML::SyntaxHighlighter>());
        break;
    case Language::HTML:
        set_syntax_highlighter(make<Web::HTML::SyntaxHighlighter>());
        break;
    case Language::JavaScript:
        set_syntax_highlighter(make<JS::SyntaxHighlighter>());
        break;
    case Language::Ini:
        set_syntax_highlighter(make<GUI::IniSyntaxHighlighter>());
        break;
    case Language::Shell:
        set_syntax_highlighter(make<Shell::SyntaxHighlighter>());
        break;
    case Language::SQL:
        set_syntax_highlighter(make<SQL::AST::SyntaxHighlighter>());
        break;
    default:
        set_syntax_highlighter({});
    }

    force_rehighlight();
}

void Editor::set_autocomplete_provider_for(CodeDocument const& document)
{
    switch (document.language()) {
    case Language::GML:
        set_autocomplete_provider(make<GUI::GML::AutocompleteProvider>());
        break;
    default:
        set_autocomplete_provider({});
    }
}

void Editor::set_language_client_for(const CodeDocument& document)
{
    if (m_language_client && m_language_client->language() == document.language())
        return;

    if (document.language() == Language::Cpp)
        m_language_client = get_language_client<LanguageClients::Cpp::ServerConnection>(project().root_path());

    if (document.language() == Language::Shell)
        m_language_client = get_language_client<LanguageClients::Shell::ServerConnection>(project().root_path());

    if (m_language_client) {
        m_language_client->on_tokens_info_result = [this](Vector<GUI::AutocompleteProvider::TokenInfo> const& tokens_info) {
            on_tokens_info_result(tokens_info);
        };
    }
}

void Editor::keydown_event(GUI::KeyEvent& event)
{
    TextEditor::keydown_event(event);

    m_parameters_hint_tooltip_window->hide();

    if (!event.shift() && !event.alt() && event.ctrl() && event.key() == KeyCode::Key_P) {
        handle_function_parameters_hint_request();
    }

    m_tokens_info_timer->restart();
}

void Editor::handle_function_parameters_hint_request()
{
    if (!m_language_client)
        return;

    m_language_client->on_function_parameters_hint_result = [this](Vector<String> const& params, size_t argument_index) {
        dbgln("on_function_parameters_hint_result");

        StringBuilder html;
        for (size_t i = 0; i < params.size(); ++i) {
            if (i == argument_index)
                html.append("<b>");

            html.appendff("{}", params[i]);

            if (i == argument_index)
                html.append("</b>");

            if (i < params.size() - 1)
                html.append(", ");
        }
        html.append("<style>body { background-color: #dac7b5; }</style>");

        m_parameter_hint_page_view->load_html(html.build(), {});

        auto cursor_rect = current_editor().cursor_content_rect().location().translated(screen_relative_rect().location());

        Gfx::Rect content(cursor_rect.x(), cursor_rect.y(), m_parameter_hint_page_view->children_clip_rect().width(), m_parameter_hint_page_view->children_clip_rect().height());
        m_parameters_hint_tooltip_window->move_to(cursor_rect.x(), cursor_rect.y() - m_parameters_hint_tooltip_window->height() - vertical_scrollbar().value());

        m_parameters_hint_tooltip_window->show();
    };

    m_language_client->get_parameters_hint(
        code_document().file_path(),
        cursor().line(),
        cursor().column());
}

void Editor::set_debug_mode(bool enabled)
{
    m_evaluate_expression_action->set_enabled(enabled);
    m_move_execution_to_line_action->set_enabled(enabled);
}

void Editor::on_token_info_timer_tick()
{
    if (!semantic_syntax_highlighting_is_enabled())
        return;
    if (!m_language_client || !m_language_client->is_active_client())
        return;

    m_language_client->get_tokens_info(code_document().file_path());
}

void Editor::on_tokens_info_result(Vector<GUI::AutocompleteProvider::TokenInfo> const& tokens_info)
{
    auto highlighter = syntax_highlighter();
    if (highlighter && highlighter->is_cpp_semantic_highlighter()) {
        auto& semantic_cpp_highlighter = verify_cast<Cpp::SemanticSyntaxHighlighter>(*highlighter);
        semantic_cpp_highlighter.update_tokens_info(tokens_info);
        force_rehighlight();
    }
}

void Editor::create_tokens_info_timer()
{
    static constexpr size_t token_info_timer_interval_ms = 1000;
    m_tokens_info_timer = Core::Timer::create_repeating((int)token_info_timer_interval_ms, [this] {
        on_token_info_timer_tick();
        m_tokens_info_timer->stop();
    });
    m_tokens_info_timer->start();
}

void Editor::set_semantic_syntax_highlighting(bool value)
{
    m_use_semantic_syntax_highlighting = value;
    set_syntax_highlighter_for(code_document());
}

}
