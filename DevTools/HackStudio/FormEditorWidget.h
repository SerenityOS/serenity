#pragma once

#include <LibGUI/GScrollableWidget.h>

class FormWidget;

class FormEditorWidget final : public GScrollableWidget {
    C_OBJECT(FormEditorWidget)
public:
    virtual ~FormEditorWidget() override;

private:
    virtual void paint_event(GPaintEvent&) override;

    explicit FormEditorWidget(GWidget* parent);

    RefPtr<FormWidget> m_form_widget;
};
