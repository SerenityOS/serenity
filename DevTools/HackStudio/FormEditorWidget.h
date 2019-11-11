#pragma once

#include <LibGUI/GScrollableWidget.h>

class FormWidget;
class Tool;
class WidgetTreeModel;

class FormEditorWidget final : public GScrollableWidget {
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
        Function<void(GWidget&)> on_remove;
        Function<void(GWidget&)> on_add;
        Function<void()> on_clear;

        void enable_hooks() { m_hooks_enabled = true; }
        void disable_hooks() { m_hooks_enabled = false; }

        bool is_empty() const
        {
            return m_widgets.is_empty();
        }

        bool contains(GWidget& widget) const
        {
            return m_widgets.contains(&widget);
        }

        void toggle(GWidget& widget)
        {
            if (contains(widget))
                remove(widget);
            else
                add(widget);
        }

        void set(GWidget& widget)
        {
            clear();
            add(widget);
        }

        void remove(GWidget& widget)
        {
            ASSERT(m_widgets.contains(&widget));
            m_widgets.remove(&widget);
            if (m_hooks_enabled && on_remove)
                on_remove(widget);
        }

        void add(GWidget& widget)
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

        WidgetSelection() {}

    private:
        HashTable<GWidget*> m_widgets;
        bool m_hooks_enabled { true };
    };

    WidgetSelection& selection() { return m_selection; }

private:
    virtual void paint_event(GPaintEvent&) override;

    explicit FormEditorWidget(GWidget* parent);

    RefPtr<FormWidget> m_form_widget;
    RefPtr<WidgetTreeModel> m_widget_tree_model;
    NonnullOwnPtr<Tool> m_tool;
    WidgetSelection m_selection;
};
