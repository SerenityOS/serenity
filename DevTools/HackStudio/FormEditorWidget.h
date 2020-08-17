/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/HashTable.h>
#include <LibGUI/ScrollableWidget.h>

namespace HackStudio {

class FormWidget;
class Tool;
class WidgetTreeModel;

class FormEditorWidget final : public GUI::ScrollableWidget {
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
            ASSERT(m_widgets.contains(&widget));
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
