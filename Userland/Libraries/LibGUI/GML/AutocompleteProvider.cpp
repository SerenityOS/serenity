/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, thislooksfun <tlf@thislooks.fun>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AutocompleteProvider.h"
#include "Lexer.h"
#include <AK/QuickSort.h>

namespace GUI::GML {

void AutocompleteProvider::provide_completions(Function<void(Vector<CodeComprehension::AutocompleteResultEntry>)> callback)
{
    auto cursor = m_editor->cursor();
    auto text = m_editor->text();
    Lexer lexer(text);
    // FIXME: Provide a begin() and end() for lexers PLEASE!
    auto all_tokens = lexer.lex();
    enum State {
        Free,
        InClassName,
        AfterClassName,
        InIdentifier,
        AfterIdentifier, // Can we introspect this?
    } state { Free };
    ByteString identifier_string;
    Vector<ByteString> class_names;
    Vector<State> previous_states;
    bool should_push_state { true };
    Token* last_seen_token { nullptr };
    Token* last_identifier_token { nullptr };

    for (auto& token : all_tokens) {
        auto handle_class_child = [&] {
            if (token.m_type == Token::Type::Identifier) {
                state = InIdentifier;
                identifier_string = token.m_view;
                last_identifier_token = &token;
            } else if (token.m_type == Token::Type::ClassMarker) {
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
            if (token.m_type == Token::Type::ClassName) {
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
            if (token.m_type != Token::Type::LeftCurly) {
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

            if (token.m_type == Token::Type::RightCurly) {
                class_names.take_last();
                state = previous_states.take_last();
                break;
            }
            break;
        case InIdentifier:
            if (token.m_type == Token::Type::Colon)
                state = AfterIdentifier;
            break;
        case AfterIdentifier:
            if (token.m_type == Token::Type::RightCurly || token.m_type == Token::Type::LeftCurly)
                break;
            if (token.m_type == Token::Type::ClassMarker) {
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

    auto& widget_class = *GUI::ObjectClassRegistration::find("GUI::Widget"sv);
    auto& layout_class = *GUI::ObjectClassRegistration::find("GUI::Layout"sv);

    // FIXME: Can this be done without a StringBuilder?
    auto make_fuzzy = [](StringView str) {
        auto fuzzy_str_builder = StringBuilder(str.length() * 2 + 1);
        fuzzy_str_builder.append('*');
        for (auto character : str) {
            fuzzy_str_builder.append(character);
            fuzzy_str_builder.append('*');
        }
        return fuzzy_str_builder.to_byte_string();
    };

    Vector<CodeComprehension::AutocompleteResultEntry> class_entries, identifier_entries;

    auto register_layouts_matching_pattern = [&](ByteString pattern, size_t partial_input_length) {
        GUI::ObjectClassRegistration::for_each([&](const GUI::ObjectClassRegistration& registration) {
            if (registration.is_derived_from(layout_class) && &registration != &layout_class && registration.class_name().matches(pattern))
                class_entries.empend(ByteString::formatted("@{}", registration.class_name()), partial_input_length);
        });
    };

    auto register_widgets_matching_pattern = [&](ByteString pattern, size_t partial_input_length) {
        GUI::ObjectClassRegistration::for_each([&](const GUI::ObjectClassRegistration& registration) {
            if (registration.is_derived_from(widget_class) && registration.class_name().matches(pattern))
                class_entries.empend(ByteString::formatted("@{}", registration.class_name()), partial_input_length);
        });
    };

    auto register_class_properties_matching_pattern = [&](ByteString pattern, size_t partial_input_length) {
        auto class_name = class_names.last();

        // FIXME: Don't show properties that are already specified in the scope.
        auto registration = GUI::ObjectClassRegistration::find(class_name);
        if (registration && (registration->is_derived_from(widget_class) || registration->is_derived_from(layout_class))) {
            if (auto instance = registration->construct(); !instance.is_error()) {
                for (auto& it : instance.value()->properties()) {
                    if (!it.value->is_readonly() && it.key.matches(pattern))
                        identifier_entries.empend(ByteString::formatted("{}: ", it.key), partial_input_length, CodeComprehension::Language::Unspecified, it.key);
                }
            }
        }

        if (can_have_declared_layout(class_names.last()) && "layout"sv.matches(pattern))
            identifier_entries.empend("layout: ", partial_input_length, CodeComprehension::Language::Unspecified, "layout", CodeComprehension::AutocompleteResultEntry::HideAutocompleteAfterApplying::No);
        if (class_names.last() == "GUI::ScrollableContainerWidget" && "content_widget"sv.matches(pattern))
            identifier_entries.empend("content_widget: ", partial_input_length, CodeComprehension::Language::Unspecified, "content_widget", CodeComprehension::AutocompleteResultEntry::HideAutocompleteAfterApplying::No);
    };

    auto register_properties_and_widgets_matching_pattern = [&](ByteString pattern, size_t partial_input_length) {
        if (!class_names.is_empty()) {
            register_class_properties_matching_pattern(pattern, partial_input_length);

            auto parent_registration = GUI::ObjectClassRegistration::find(class_names.last());
            if (parent_registration && parent_registration->is_derived_from(layout_class)) {
                // Layouts can't have child classes, so why suggest them?
                return;
            }
        }

        register_widgets_matching_pattern(pattern, partial_input_length);
    };

    bool after_token_on_same_line = last_seen_token && last_seen_token->m_end.column != cursor.column() && last_seen_token->m_end.line == cursor.line();
    switch (state) {
    case Free:
        if (after_token_on_same_line) {
            // After some token, but with extra space, not on a new line.
            // Nothing to put here.
            break;
        }

        register_widgets_matching_pattern("*", 0u);
        break;
    case InClassName: {
        if (class_names.is_empty())
            break;
        if (after_token_on_same_line) {
            // After a class name, but haven't seen braces.
            // TODO: Suggest braces?
            break;
        }

        auto class_name = class_names.last();
        auto fuzzy_class = make_fuzzy(class_name);
        if (last_identifier_token && last_identifier_token->m_end.line == last_seen_token->m_end.line && identifier_string == "layout")
            register_layouts_matching_pattern(fuzzy_class, class_name.length() + 1);
        else
            register_widgets_matching_pattern(fuzzy_class, class_name.length() + 1);

        break;
    }
    case InIdentifier:
        if (after_token_on_same_line) {
            // After an identifier, but with extra space
            // TODO: Maybe suggest a colon?
            break;
        }

        register_properties_and_widgets_matching_pattern(make_fuzzy(identifier_string), identifier_string.length());
        break;
    case AfterClassName:
        if (last_seen_token && last_seen_token->m_end.line == cursor.line()) {
            if (last_seen_token->m_type != Token::Type::Identifier || last_seen_token->m_end.column != cursor.column()) {
                // Inside braces, but on the same line as some other stuff (and not the continuation of one!)
                // The user expects nothing here.
                break;
            }
        }

        register_properties_and_widgets_matching_pattern("*", 0u);
        break;
    case AfterIdentifier:
        if (last_seen_token && last_seen_token->m_end.line != cursor.line())
            break;
        if (identifier_string == "layout")
            register_layouts_matching_pattern("*", 0u);
        if (identifier_string == "content_widget")
            register_widgets_matching_pattern("*", 0u);
        break;
    default:
        break;
    }

    quick_sort(class_entries, [](auto& a, auto& b) { return a.completion < b.completion; });
    quick_sort(identifier_entries, [](auto& a, auto& b) { return a.completion < b.completion; });

    Vector<CodeComprehension::AutocompleteResultEntry> entries;
    entries.extend(move(identifier_entries));
    entries.extend(move(class_entries));

    callback(move(entries));
}

}
