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

#include "GMLAutocompleteProvider.h"
#include <AK/QuickSort.h>
#include <LibGUI/GMLLexer.h>

void GMLAutocompleteProvider::provide_completions(Function<void(Vector<Entry>)> callback)
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
