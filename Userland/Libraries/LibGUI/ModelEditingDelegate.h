/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

namespace GUI {

class ModelEditingDelegate {
public:
    enum SelectionBehavior {
        DoNotSelect,
        SelectAll,
    };

    virtual ~ModelEditingDelegate() = default;

    void bind(Model& model, ModelIndex const& index)
    {
        if (m_model.ptr() == &model && m_index == index)
            return;
        m_model = model;
        m_index = index;
        m_widget = create_widget();
    }

    Widget* widget() { return m_widget; }
    Widget const* widget() const { return m_widget; }

    Function<void()> on_commit;
    Function<void()> on_rollback;
    Function<void()> on_change;

    virtual Variant value() const = 0;
    virtual void set_value(Variant const&, SelectionBehavior selection_behavior = SelectionBehavior::SelectAll) = 0;

    virtual void will_begin_editing() { }

protected:
    ModelEditingDelegate() = default;

    virtual RefPtr<Widget> create_widget() = 0;
    void commit()
    {
        if (on_commit)
            on_commit();
    }
    void rollback()
    {
        if (on_rollback)
            on_rollback();
    }
    void change()
    {
        if (on_change)
            on_change();
    }

    ModelIndex const& index() const { return m_index; }

private:
    RefPtr<Model> m_model;
    ModelIndex m_index;
    RefPtr<Widget> m_widget;
};

class StringModelEditingDelegate : public ModelEditingDelegate {
public:
    StringModelEditingDelegate() = default;
    virtual ~StringModelEditingDelegate() override = default;

    virtual RefPtr<Widget> create_widget() override
    {
        auto textbox = TextBox::construct();
        textbox->set_frame_style(Gfx::FrameStyle::NoFrame);

        textbox->on_return_pressed = [this] {
            commit();
        };
        textbox->on_escape_pressed = [this] {
            rollback();
        };
        textbox->on_change = [this] {
            change();
        };
        return textbox;
    }
    virtual Variant value() const override { return static_cast<TextBox const*>(widget())->text(); }
    virtual void set_value(Variant const& value, SelectionBehavior selection_behavior) override
    {
        auto& textbox = static_cast<TextBox&>(*widget());
        if (value.is_valid())
            textbox.set_text(value.to_byte_string());
        else
            textbox.clear();
        if (selection_behavior == SelectionBehavior::SelectAll)
            textbox.select_all();
    }
};

}
