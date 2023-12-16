/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractView.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Model.h>

namespace GUI {

class ComboBoxEditor;

class ComboBox : public Frame {
    C_OBJECT(ComboBox);

public:
    virtual ~ComboBox() override;

    ByteString text() const;
    void set_text(ByteString const&, AllowCallback = AllowCallback::Yes);

    void open();
    void close();
    void select_all();

    Model* model();
    Model const* model() const;
    void set_model(NonnullRefPtr<Model>);

    size_t selected_index() const;
    void set_selected_index(size_t index, AllowCallback = AllowCallback::Yes);
    void clear_selection();

    bool only_allow_values_from_model() const { return m_only_allow_values_from_model; }
    void set_only_allow_values_from_model(bool);

    int model_column() const;
    void set_model_column(int);

    void set_editor_placeholder(StringView placeholder);
    ByteString const& editor_placeholder() const;

    int max_visible_items() const { return m_max_visible_items; }
    void set_max_visible_items(int max) { m_max_visible_items = max; }

    Function<void(ByteString const&, ModelIndex const&)> on_change;
    Function<void()> on_return_pressed;

protected:
    ComboBox();
    virtual void resize_event(ResizeEvent&) override;

private:
    void selection_updated(ModelIndex const&);
    void navigate(AbstractView::CursorMovement);
    void navigate_relative(int);

    RefPtr<ComboBoxEditor> m_editor;
    RefPtr<Button> m_open_button;
    RefPtr<Window> m_list_window;
    RefPtr<ListView> m_list_view;
    Optional<ModelIndex> m_selected_index;
    bool m_only_allow_values_from_model { false };
    bool m_updating_model { false };
    int m_max_visible_items { 15 };
};

}
