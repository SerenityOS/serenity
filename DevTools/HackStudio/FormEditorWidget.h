#pragma once

#include <LibGUI/GScrollableWidget.h>

class FormWidget;
class Tool;

class FormEditorWidget final : public GScrollableWidget {
    C_OBJECT(FormEditorWidget)
public:
    virtual ~FormEditorWidget() override;

    FormWidget& form_widget() { return *m_form_widget; }
    const FormWidget& form_widget() const { return *m_form_widget; }

    Tool& tool() { return *m_tool; }
    const Tool& tool() const { return *m_tool; }

    void set_tool(NonnullOwnPtr<Tool>);

    class WidgetSelection {
    public:
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
        }

        void add(GWidget& widget)
        {
            m_widgets.set(&widget);
        }

        void clear()
        {
            m_widgets.clear();
        }

        WidgetSelection() {}
    private:
        HashTable<GWidget*> m_widgets;
    };

    WidgetSelection& selection() { return m_selection; }

private:
    virtual void paint_event(GPaintEvent&) override;

    explicit FormEditorWidget(GWidget* parent);

    RefPtr<FormWidget> m_form_widget;
    NonnullOwnPtr<Tool> m_tool;
    WidgetSelection m_selection;
};
