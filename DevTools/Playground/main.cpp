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

#include <LibCore/File.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/GMLLexer.h>
#include <LibGUI/GMLSyntaxHighlighter.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <string.h>

class GMLAutocompleteProvider final : public virtual GUI::AutocompleteProvider {
public:
    GMLAutocompleteProvider() { }
    virtual ~GMLAutocompleteProvider() override { }

private:
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

        for (auto& token : all_tokens) {
            if (token.m_start.line > cursor.line() || (token.m_start.line == cursor.line() && token.m_start.column > cursor.column()))
                break;

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
                state = AfterIdentifier;
                break;
            case AfterIdentifier:
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

        Vector<GUI::AutocompleteProvider::Entry> entries;
        switch (state) {
        case Free:
            GUI::WidgetClassRegistration::for_each([&](const GUI::WidgetClassRegistration& registration) {
                entries.empend(String::formatted("@{}", registration.class_name()), 0u);
            });
            break;
        case InClassName:
            if (class_names.is_empty())
                break;
            GUI::WidgetClassRegistration::for_each([&](const GUI::WidgetClassRegistration& registration) {
                if (registration.class_name().starts_with(class_names.last()))
                    entries.empend(registration.class_name(), class_names.last().length());
            });
            break;
        case InIdentifier:
            if (class_names.is_empty())
                break;
            if (auto registration = GUI::WidgetClassRegistration::find(class_names.last())) {
                auto instance = registration->construct();
                for (auto& it : instance->properties()) {
                    if (it.key.starts_with(identifier_string))
                        entries.empend(it.key, identifier_string.length());
                }
            }
            break;
        case AfterClassName:
            if (!class_names.is_empty()) {
                if (auto registration = GUI::WidgetClassRegistration::find(class_names.last())) {
                    auto instance = registration->construct();
                    for (auto& it : instance->properties()) {
                        entries.empend(it.key, 0u);
                    }
                }
            }
            GUI::WidgetClassRegistration::for_each([&](const GUI::WidgetClassRegistration& registration) {
                entries.empend(String::formatted("@{}", registration.class_name()), 0u);
            });
            break;
        case AfterIdentifier:
        default:
            break;
        }

        callback(move(entries));
    }
};

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
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
    editor.set_automatic_indentation_enabled(true);
    editor.set_text(R"~~~(@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
    }

    // Now add some widgets!
}
)~~~");
    editor.set_cursor(4, 28); // after "...widgets!"

    editor.on_change = [&] {
        preview.remove_all_children();
        preview.load_from_gml(editor.text());
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

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("GML Playground", app_icon.bitmap_for_size(32), window);
    }));

    app->set_menubar(move(menubar));

    window->show();
    return app->exec();
}
