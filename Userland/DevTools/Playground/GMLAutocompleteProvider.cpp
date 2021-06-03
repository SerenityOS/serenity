/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
        auto handle_class_child = [&] {
            if (token.m_type == GUI::GMLToken::Type::Identifier) {
                state = InIdentifier;
                identifier_string = token.m_view;
            } else if (token.m_type == GUI::GMLToken::Type::ClassMarker) {
                previous_states.append(AfterClassName);
                state = Free;
                should_push_state = false;
            }
        };

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
            if (token.m_type != GUI::GMLToken::Type::LeftCurly) {
                // Close empty class and immediately handle our parent's next child
                class_names.take_last();
                state = previous_states.take_last();

                if (state == AfterClassName)
                    handle_class_child();

                break;
            }
            state = AfterClassName;
            break;
        case AfterClassName:
            handle_class_child();

            if (token.m_type == GUI::GMLToken::Type::RightCurly) {
                class_names.take_last();
                state = previous_states.take_last();
                break;
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

    if (state == InClassName && last_seen_token && last_seen_token->m_end.line < cursor.line()) {
        // Close empty class
        class_names.take_last();
        state = previous_states.take_last();
    }

    auto& widget_class = *Core::ObjectClassRegistration::find("GUI::Widget");

    Vector<GUI::AutocompleteProvider::Entry> class_entries, identifier_entries;
    switch (state) {
    case Free:
        if (last_seen_token && last_seen_token->m_end.column != cursor.column() && last_seen_token->m_end.line == cursor.line()) {
            // After some token, but with extra space, not on a new line.
            // Nothing to put here.
            break;
        }
        Core::ObjectClassRegistration::for_each([&](const Core::ObjectClassRegistration& registration) {
            if (!registration.is_derived_from(widget_class))
                return;
            class_entries.empend(String::formatted("@{}", registration.class_name()), 0u);
        });
        break;
    case InClassName:
        if (class_names.is_empty())
            break;
        if (last_seen_token && last_seen_token->m_end.column != cursor.column() && last_seen_token->m_end.line == cursor.line()) {
            // After a class name, but haven't seen braces.
            // TODO: Suggest braces?
            break;
        }
        Core::ObjectClassRegistration::for_each([&](const Core::ObjectClassRegistration& registration) {
            if (!registration.is_derived_from(widget_class))
                return;
            if (registration.class_name().starts_with(class_names.last()))
                identifier_entries.empend(registration.class_name(), class_names.last().length());
        });
        break;
    case InIdentifier: {
        if (class_names.is_empty())
            break;
        if (last_seen_token && last_seen_token->m_end.column != cursor.column() && last_seen_token->m_end.line == cursor.line()) {
            // After an identifier, but with extra space
            // TODO: Maybe suggest a colon?
            break;
        }
        auto registration = Core::ObjectClassRegistration::find(class_names.last());
        if (registration && registration->is_derived_from(widget_class)) {
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
    }
    case AfterClassName: {
        if (last_seen_token && last_seen_token->m_end.line == cursor.line()) {
            if (last_seen_token->m_type != GUI::GMLToken::Type::Identifier || last_seen_token->m_end.column != cursor.column()) {
                // Inside braces, but on the same line as some other stuff (and not the continuation of one!)
                // The user expects nothing here.
                break;
            }
        }
        if (!class_names.is_empty()) {
            auto registration = Core::ObjectClassRegistration::find(class_names.last());
            if (registration && registration->is_derived_from(widget_class)) {
                auto instance = registration->construct();
                for (auto& it : instance->properties()) {
                    if (!it.value->is_readonly())
                        identifier_entries.empend(it.key, 0u);
                }
            }
        }
        Core::ObjectClassRegistration::for_each([&](const Core::ObjectClassRegistration& registration) {
            if (!registration.is_derived_from(widget_class))
                return;
            class_entries.empend(String::formatted("@{}", registration.class_name()), 0u);
        });
        break;
    }
    case AfterIdentifier:
        if (last_seen_token && last_seen_token->m_end.line != cursor.line()) {
            break;
        }
        if (identifier_string == "layout") {
            Core::ObjectClassRegistration::for_each([&](const Core::ObjectClassRegistration& registration) {
                if (!registration.is_derived_from(widget_class))
                    return;
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
