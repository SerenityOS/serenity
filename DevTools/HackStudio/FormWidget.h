#pragma once

#include <LibGUI/GWidget.h>

class FormEditorWidget;

class FormWidget final : public GWidget {
    C_OBJECT(FormWidget)
public:
    virtual ~FormWidget() override;

    FormEditorWidget& editor();
    const FormEditorWidget& editor() const;

    // FIXME: This should be an app-wide preference instead.
    int grid_size() const { return m_grid_size; }

private:
    virtual bool accepts_focus() const override { return true; }

    virtual void paint_event(GPaintEvent&) override;
    virtual void second_paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;

    explicit FormWidget(FormEditorWidget& parent);

    int m_grid_size { 5 };
};
