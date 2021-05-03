/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <LibGUI/AbstractScrollableWidget.h>

namespace HackStudio {

class FormWidget;
class Tool;
class WidgetTreeModel;

class FormEditorWidget final : public GUI::AbstractScrollableWidget {
    C_OBJECT(FormEditorWidget)
public:
    virtual ~FormEditorWidget() override;

    FormWidget& form_widget() { return *m_form_widget; }
    const FormWidget& form_widget() const { return *m_form_widget; }

    Tool& tool() { return *m_tool; }
    const Tool& tool() const { return *m_tool; }

    void set_tool(NonnullOwnPtr<Tool>);

    WidgetTreeModel& model();

    class WidgetSelection {
    public:
        Function<void(GUI::Widget&)> on_remove;
        Function<void(GUI::Widget&)> on_add;
        Function<void()> on_clear;

        void enable_hooks() { m_hooks_enabled = true; }
        void disable_hooks() { m_hooks_enabled = false; }

        bool is_empty() const
        {
            return m_widgets.is_empty();
        }

        bool contains(GUI::Widget& widget) const
        {
            return m_widgets.contains(&widget);
        }

        void toggle(GUI::Widget& widget)
        {
            if (contains(widget))
                remove(widget);
            else
                add(widget);
        }

        void set(GUI::Widget& widget)
        {
            clear();
            add(widget);
        }

        void remove(GUI::Widget& widget)
        {
            VERIFY(m_widgets.contains(&widget));
            m_widgets.remove(&widget);
            if (m_hooks_enabled && on_remove)
                on_remove(widget);
        }

        void add(GUI::Widget& widget)
        {
            m_widgets.set(&widget);
            if (m_hooks_enabled && on_add)
                on_add(widget);
        }

        void clear()
        {
            m_widgets.clear();
            if (m_hooks_enabled && on_clear)
                on_clear();
        }

        template<typename Callback>
        void for_each(Callback callback)
        {
            for (auto& it : m_widgets) {
                if (callback(*it) == IterationDecision::Break)
                    break;
            }
        }

        WidgetSelection() { }

    private:
        HashTable<GUI::Widget*> m_widgets;
        bool m_hooks_enabled { true };
    };

    WidgetSelection& selection() { return m_selection; }

private:
    virtual void paint_event(GUI::PaintEvent&) override;

    FormEditorWidget();

    RefPtr<FormWidget> m_form_widget;
    RefPtr<WidgetTreeModel> m_widget_tree_model;
    NonnullOwnPtr<Tool> m_tool;
    WidgetSelection m_selection;
};

}
