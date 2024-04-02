/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>

namespace GUI {

class IncrementalSearchBanner final : public Widget {
    C_OBJECT(IncrementalSearchBanner);

public:
    static ErrorOr<NonnullRefPtr<IncrementalSearchBanner>> try_create(TextEditor& editor);
    ErrorOr<void> initialize();

    virtual ~IncrementalSearchBanner() override = default;

    void show();
    void hide();

protected:
    virtual void paint_event(PaintEvent&) override;
    virtual Optional<UISize> calculated_min_size() const override;

private:
    IncrementalSearchBanner() = default;

    static ErrorOr<NonnullRefPtr<IncrementalSearchBanner>> try_create();
    void search(TextEditor::SearchDirection);

    RefPtr<TextEditor> m_editor;
    RefPtr<Button> m_close_button;
    RefPtr<Button> m_next_button;
    RefPtr<Button> m_previous_button;
    RefPtr<Button> m_wrap_search_button;
    RefPtr<Button> m_match_case_button;
    RefPtr<Label> m_index_label;
    RefPtr<TextBox> m_search_textbox;

    TextDocument::SearchShouldWrap m_wrap_search { true };
    bool m_match_case { false };
};

}
