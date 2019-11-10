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

private:
    virtual void paint_event(GPaintEvent&) override;

    explicit FormEditorWidget(GWidget* parent);

    RefPtr<FormWidget> m_form_widget;

    NonnullOwnPtr<Tool> m_tool;
};
