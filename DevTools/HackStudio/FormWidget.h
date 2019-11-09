#pragma once

#include <LibGUI/GWidget.h>

class FormEditorWidget;

class FormWidget final : public GWidget {
    C_OBJECT(FormWidget)
public:
    virtual ~FormWidget() override;

    FormEditorWidget& editor();
    const FormEditorWidget& editor() const;

private:
    virtual void paint_event(GPaintEvent&) override;

    explicit FormWidget(FormEditorWidget& parent);

    // FIXME: This should be an app-wide preference instead.
    int m_grid_size { 5 };
};
