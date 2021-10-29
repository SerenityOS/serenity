/*
 * Copyright (c) 2020, the SerenityOS developers.
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
    virtual ~AutocompleteProvider() { }

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

        bool operator==(const ProjectLocation&) const;
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

        bool operator==(const Declaration&) const;
    };

    virtual void provide_completions(Function<void(Vector<Entry>)>) = 0;

    void attach(TextEditor& editor)
    {
        VERIFY(!m_editor);
        m_editor = editor;
    }
    void detach() { m_editor.clear(); }

protected:
    AutocompleteProvider() { }

    WeakPtr<TextEditor> m_editor;
};

class AutocompleteBox final {
public:
    explicit AutocompleteBox(TextEditor&);
    ~AutocompleteBox();

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
