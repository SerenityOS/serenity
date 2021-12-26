/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/QuickSort.h>
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/Property.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/GMLFormatter.h>
#include <LibGUI/GMLLexer.h>
#include <LibGUI/GMLSyntaxHighlighter.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <string.h>

namespace {

class UnregisteredWidget final : public GUI::Widget {
    C_OBJECT(UnregisteredWidget);

private:
    UnregisteredWidget(const String& class_name);

    virtual void paint_event(GUI::PaintEvent& event) override;

    String m_text;
};

UnregisteredWidget::UnregisteredWidget(const String& class_name)
{
    StringBuilder builder;
    builder.append(class_name);
    builder.append("\nnot registered");
    m_text = builder.to_string();
}

void UnregisteredWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Gfx::Color::DarkRed);
    painter.draw_text(rect(), m_text, Gfx::TextAlignment::Center, Color::White);
}

}

class GMLAutocompleteProvider final : public virtual GUI::AutocompleteProvider {
public:
    GMLAutocompleteProvider() { }
    virtual ~GMLAutocompleteProvider() override { }

private:
    static bool can_have_declared_layout(const StringView& class_name)
    {
        return class_name.is_one_of("GUI::Widget", "GUI::Frame");
    }

    virtual void provide_completions(Function<void(Vector<Entry>)> callback) override
    {
        auto cursor = m_editor->cursor();
        auto text = m_editor->text();
        GUI::GMLLexer lexer(text);
        // FIXME: Provide a begin() and end() for lexers PLEASE!
        auto all_tokens = lexer.lex();
        enum State {
            Free,
            InClassName,
            AfterClassName,
            InIdentifier,
            AfterIdentifier, // Can we introspect this?
        } state { Free };
        String identifier_string;
        Vector<String> class_names;
        Vector<State> previous_states;
        bool should_push_state { true };
        GUI::GMLToken* last_seen_token { nullptr };

        for (auto& token : all_tokens) {
            if (token.m_start.line > cursor.line() || (token.m_start.line == cursor.line() && token.m_start.column > cursor.column()))
                break;

            last_seen_token = &token;
            switch (state) {
            case Free:
                if (token.m_type == GUI::GMLToken::Type::ClassName) {
                    if (should_push_state)
                        previous_states.append(state);
                    else
                        should_push_state = true;
                    state = InClassName;
                    class_names.append(token.m_view);
                    break;
                }
                break;
            case InClassName:
                state = AfterClassName;
                break;
            case AfterClassName:
                if (token.m_type == GUI::GMLToken::Type::Identifier) {
                    state = InIdentifier;
                    identifier_string = token.m_view;
                    break;
                }
                if (token.m_type == GUI::GMLToken::Type::RightCurly) {
                    class_names.take_last();
                    state = previous_states.take_last();
                    break;
                }
                if (token.m_type == GUI::GMLToken::Type::ClassMarker) {
                    previous_states.append(AfterClassName);
                    state = Free;
                    should_push_state = false;
                }
                break;
            case InIdentifier:
                if (token.m_type == GUI::GMLToken::Type::Colon)
                    state = AfterIdentifier;
                break;
            case AfterIdentifier:
                if (token.m_type == GUI::GMLToken::Type::RightCurly || token.m_type == GUI::GMLToken::Type::LeftCurly)
                    break;
                if (token.m_type == GUI::GMLToken::Type::ClassMarker) {
                    previous_states.append(AfterClassName);
                    state = Free;
                    should_push_state = false;
                } else {
                    state = AfterClassName;
                }
                break;
            }
        }

        Vector<GUI::AutocompleteProvider::Entry> class_entries, identifier_entries;
        switch (state) {
        case Free:
            if (last_seen_token && last_seen_token->m_end.column + 1 != cursor.column() && last_seen_token->m_end.line == cursor.line()) {
                // After some token, but with extra space, not on a new line.
                // Nothing to put here.
                break;
            }
            GUI::WidgetClassRegistration::for_each([&](const GUI::WidgetClassRegistration& registration) {
                class_entries.empend(String::formatted("@{}", registration.class_name()), 0u);
            });
            break;
        case InClassName:
            if (class_names.is_empty())
                break;
            if (last_seen_token && last_seen_token->m_end.column + 1 != cursor.column() && last_seen_token->m_end.line == cursor.line()) {
                // After a class name, but haven't seen braces.
                // TODO: Suggest braces?
                break;
            }
            GUI::WidgetClassRegistration::for_each([&](const GUI::WidgetClassRegistration& registration) {
                if (registration.class_name().starts_with(class_names.last()))
                    identifier_entries.empend(registration.class_name(), class_names.last().length());
            });
            break;
        case InIdentifier:
            if (class_names.is_empty())
                break;
            if (last_seen_token && last_seen_token->m_end.column + 1 != cursor.column() && last_seen_token->m_end.line == cursor.line()) {
                // After an identifier, but with extra space
                // TODO: Maybe suggest a colon?
                break;
            }
            if (auto registration = GUI::WidgetClassRegistration::find(class_names.last())) {
                auto instance = registration->construct();
                for (auto& it : instance->properties()) {
                    if (it.key.starts_with(identifier_string))
                        identifier_entries.empend(it.key, identifier_string.length());
                }
            }
            if (can_have_declared_layout(class_names.last()) && StringView { "layout" }.starts_with(identifier_string))
                identifier_entries.empend("layout", identifier_string.length());
            // No need to suggest anything if it's already completely typed out!
            if (identifier_entries.size() == 1 && identifier_entries.first().completion == identifier_string)
                identifier_entries.clear();
            break;
        case AfterClassName:
            if (last_seen_token && last_seen_token->m_end.line == cursor.line()) {
                if (last_seen_token->m_type != GUI::GMLToken::Type::Identifier || last_seen_token->m_end.column + 1 != cursor.column()) {
                    // Inside braces, but on the same line as some other stuff (and not the continuation of one!)
                    // The user expects nothing here.
                    break;
                }
            }
            if (!class_names.is_empty()) {
                if (auto registration = GUI::WidgetClassRegistration::find(class_names.last())) {
                    auto instance = registration->construct();
                    for (auto& it : instance->properties()) {
                        if (!it.value->is_readonly())
                            identifier_entries.empend(it.key, 0u);
                    }
                }
            }
            GUI::WidgetClassRegistration::for_each([&](const GUI::WidgetClassRegistration& registration) {
                class_entries.empend(String::formatted("@{}", registration.class_name()), 0u);
            });
            break;
        case AfterIdentifier:
            if (last_seen_token && last_seen_token->m_end.line != cursor.line()) {
                break;
            }
            if (identifier_string == "layout") {
                GUI::WidgetClassRegistration::for_each([&](const GUI::WidgetClassRegistration& registration) {
                    if (registration.class_name().contains("Layout"))
                        class_entries.empend(String::formatted("@{}", registration.class_name()), 0u);
                });
            }
            break;
        default:
            break;
        }

        quick_sort(class_entries, [](auto& a, auto& b) { return a.completion < b.completion; });
        quick_sort(identifier_entries, [](auto& a, auto& b) { return a.completion < b.completion; });

        Vector<GUI::AutocompleteProvider::Entry> entries;
        entries.append(move(identifier_entries));
        entries.append(move(class_entries));

        callback(move(entries));
    }
};

int main(int argc, char** argv)
{
    if (pledge("stdio thread shared_buffer accept cpath rpath wpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio thread shared_buffer accept rpath cpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!Desktop::Launcher::add_allowed_handler_with_only_specific_urls(
            "/bin/Help",
            { URL::create_with_file_protocol("/usr/share/man/man1/Playground.md") })
        || !Desktop::Launcher::seal_allowlist()) {
        warnln("Failed to set up allowed launch URLs");
        return 1;
    }

    if (pledge("stdio thread shared_buffer accept rpath cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "GML file to edit", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app_icon = GUI::Icon::default_icon("app-playground");
    auto window = GUI::Window::construct();
    window->set_title("GML Playground");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(800, 600);

    auto& splitter = window->set_main_widget<GUI::HorizontalSplitter>();

    auto& editor = splitter.add<GUI::TextEditor>();
    auto& preview = splitter.add<GUI::Widget>();

    editor.set_syntax_highlighter(make<GUI::GMLSyntaxHighlighter>());
    editor.set_autocomplete_provider(make<GMLAutocompleteProvider>());
    editor.set_should_autocomplete_automatically(true);
    editor.set_automatic_indentation_enabled(true);

    if (String(path).is_empty()) {
        editor.set_text(R"~~~(@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
    }

    // Now add some widgets!
}
)~~~");
        editor.set_cursor(4, 28); // after "...widgets!"
    } else {
        auto file = Core::File::construct(path);
        if (!file->open(Core::IODevice::ReadOnly)) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: {}", path, strerror(errno)), "Error", GUI::MessageBox::Type::Error);
            return 1;
        }
        editor.set_text(file->read_all());
    }

    editor.on_change = [&] {
        preview.remove_all_children();
        preview.load_from_gml(editor.text(), [](const String& class_name) -> RefPtr<GUI::Widget> {
            return UnregisteredWidget::construct(class_name);
        });
    };

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("GML Playground");

    app_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window);

        if (!open_path.has_value())
            return;

        auto file = Core::File::construct(open_path.value());
        if (!file->open(Core::IODevice::ReadOnly) && file->error() != ENOENT) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: {}", open_path.value(), strerror(errno)), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        editor.set_text(file->read_all());
        editor.set_focus(true);
    }));

    app_menu.add_action(GUI::CommonActions::make_save_as_action([&](auto&) {
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, "Untitled", "gml");
        if (!save_path.has_value())
            return;

        if (!editor.write_to_file(save_path.value())) {
            GUI::MessageBox::show(window, "Unable to save file.\n", "Error", GUI::MessageBox::Type::Error);
            return;
        }
    }));

    app_menu.add_separator();

    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& edit_menu = menubar->add_menu("Edit");
    edit_menu.add_action(GUI::Action::create("Format GML", { Mod_Ctrl | Mod_Shift, Key_I }, [&](auto&) {
        auto source = editor.text();
        GUI::GMLLexer lexer(source);
        for (auto& token : lexer.lex()) {
            if (token.m_type == GUI::GMLToken::Type::Comment) {
                auto result = GUI::MessageBox::show(
                    window,
                    "Your GML contains comments, which currently are not supported by the formatter and will be removed. Proceed?",
                    "Warning",
                    GUI::MessageBox::Type::Warning,
                    GUI::MessageBox::InputType::OKCancel);
                if (result == GUI::MessageBox::ExecCancel)
                    return;
                break;
            }
        }
        auto formatted_gml = GUI::format_gml(source);
        if (!formatted_gml.is_null()) {
            editor.set_text(formatted_gml);
        } else {
            GUI::MessageBox::show(
                window,
                "GML could not be formatted, please check the debug console for parsing errors.",
                "Error",
                GUI::MessageBox::Type::Error);
        }
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Playground.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("GML Playground", app_icon, window));

    app->set_menubar(move(menubar));

    window->show();
    return app->exec();
}
