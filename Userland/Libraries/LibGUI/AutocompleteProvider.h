/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#pragma once

#include <LibGUI/Forward.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibIPC/Decoder.h>

namespace GUI {

class AutocompleteProvider {
    AK_MAKE_NONCOPYABLE(AutocompleteProvider);
    AK_MAKE_NONMOVABLE(AutocompleteProvider);

public:
    virtual ~AutocompleteProvider() { }

    enum class CompletionKind {
        Identifier,
        PreprocessorDefinition,
    };

    enum class Language {
        Unspecified,
        Cpp,
    };

    struct Entry {
        String completion;
        size_t partial_input_length { 0 };
        CompletionKind kind { CompletionKind::Identifier };
        Language language { Language::Unspecified };
    };

    struct ProjectLocation {
        String file;
        size_t line { 0 };
        size_t column { 0 };
    };

    enum class DeclarationType {
        Function,
        Struct,
        Class,
        Variable,
        PreprocessorDefinition,
    };

    struct Declaration {
        String name;
        ProjectLocation position;
        DeclarationType type;
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
    void show(Gfx::IntPoint suggstion_box_location);
    void close();

    void next_suggestion();
    void previous_suggestion();
    void apply_suggestion();

private:
    WeakPtr<TextEditor> m_editor;
    RefPtr<GUI::Window> m_popup_window;
    RefPtr<GUI::TableView> m_suggestion_view;
};

}
