/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Forward.h>
#include <LibGUI/Label.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibIPC/Decoder.h>

namespace GUI {

class AutocompleteProvider {
    AK_MAKE_NONCOPYABLE(AutocompleteProvider);
    AK_MAKE_NONMOVABLE(AutocompleteProvider);

public:
    virtual ~AutocompleteProvider() = default;

    enum class Language {
        Unspecified,
        Cpp,
    };

    struct Entry {
        String completion;
        size_t partial_input_length { 0 };
        Language language { Language::Unspecified };
        String display_text {};

        enum class HideAutocompleteAfterApplying {
            No,
            Yes,
        };
        HideAutocompleteAfterApplying hide_autocomplete_after_applying { HideAutocompleteAfterApplying::Yes };
    };

    struct ProjectLocation {
        String file;
        size_t line { 0 };
        size_t column { 0 };

        bool operator==(ProjectLocation const&) const;
    };

    enum class DeclarationType {
        Function,
        Struct,
        Class,
        Variable,
        PreprocessorDefinition,
        Namespace,
        Member,
    };

    struct Declaration {
        String name;
        ProjectLocation position;
        DeclarationType type;
        String scope;

        bool operator==(Declaration const&) const;
    };

    virtual void provide_completions(Function<void(Vector<Entry>)>) = 0;

#define FOR_EACH_SEMANTIC_TYPE        \
    __SEMANTIC(Unknown)               \
    __SEMANTIC(Regular)               \
    __SEMANTIC(Keyword)               \
    __SEMANTIC(Type)                  \
    __SEMANTIC(Identifier)            \
    __SEMANTIC(String)                \
    __SEMANTIC(Number)                \
    __SEMANTIC(IncludePath)           \
    __SEMANTIC(PreprocessorStatement) \
    __SEMANTIC(Comment)               \
    __SEMANTIC(Whitespace)            \
    __SEMANTIC(Function)              \
    __SEMANTIC(Variable)              \
    __SEMANTIC(CustomType)            \
    __SEMANTIC(Namespace)             \
    __SEMANTIC(Member)                \
    __SEMANTIC(Parameter)             \
    __SEMANTIC(PreprocessorMacro)

    struct TokenInfo {

        enum class SemanticType : u32 {
#define __SEMANTIC(x) x,
            FOR_EACH_SEMANTIC_TYPE
#undef __SEMANTIC

        } type { SemanticType::Unknown };
        size_t start_line { 0 };
        size_t start_column { 0 };
        size_t end_line { 0 };
        size_t end_column { 0 };

        static constexpr char const* type_to_string(SemanticType t)
        {
            switch (t) {
#define __SEMANTIC(x)     \
    case SemanticType::x: \
        return #x;
                FOR_EACH_SEMANTIC_TYPE
#undef __SEMANTIC
            }
            VERIFY_NOT_REACHED();
        };
    };

    void attach(TextEditor& editor)
    {
        VERIFY(!m_editor);
        m_editor = editor;
    }
    void detach() { m_editor.clear(); }

protected:
    AutocompleteProvider() = default;

    WeakPtr<TextEditor> m_editor;
};

class AutocompleteBox final {
public:
    explicit AutocompleteBox(TextEditor&);
    ~AutocompleteBox() = default;

    void update_suggestions(Vector<AutocompleteProvider::Entry>&& suggestions);
    bool is_visible() const;
    void show(Gfx::IntPoint suggestion_box_location);
    void close();

    bool has_suggestions() { return m_suggestion_view->model()->row_count() > 0; }
    void next_suggestion();
    void previous_suggestion();
    AutocompleteProvider::Entry::HideAutocompleteAfterApplying apply_suggestion();

private:
    WeakPtr<TextEditor> m_editor;
    RefPtr<GUI::Window> m_popup_window;
    RefPtr<GUI::TableView> m_suggestion_view;
    RefPtr<GUI::Label> m_no_suggestions_view;
};

}
