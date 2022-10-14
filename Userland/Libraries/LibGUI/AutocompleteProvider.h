/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCodeComprehension/Types.h>
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

    virtual void provide_completions(Function<void(Vector<CodeComprehension::AutocompleteResultEntry>)>) = 0;

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

    void update_suggestions(Vector<CodeComprehension::AutocompleteResultEntry>&& suggestions);
    bool is_visible() const;
    void show(Gfx::IntPoint suggestion_box_location);
    void close();

    bool has_suggestions() { return m_suggestion_view->model()->row_count() > 0; }
    void next_suggestion();
    void previous_suggestion();
    CodeComprehension::AutocompleteResultEntry::HideAutocompleteAfterApplying apply_suggestion();

private:
    WeakPtr<TextEditor> m_editor;
    RefPtr<GUI::Window> m_popup_window;
    RefPtr<GUI::TableView> m_suggestion_view;
    RefPtr<GUI::Label> m_no_suggestions_view;
};
}
