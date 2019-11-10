#include "FormEditorWidget.h"
#include "CursorTool.h"
#include "FormWidget.h"
#include <LibGUI/GPainter.h>

FormEditorWidget::FormEditorWidget(GWidget* parent)
    : GScrollableWidget(parent)
    , m_tool(make<CursorTool>(*this))
{
    set_fill_with_background_color(true);
    set_background_color(Color::MidGray);

    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);

    m_form_widget = FormWidget::construct(*this);
}

FormEditorWidget::~FormEditorWidget()
{
}

void FormEditorWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
}

void FormEditorWidget::set_tool(NonnullOwnPtr<Tool> tool)
{
    m_tool->detach();
    m_tool = move(tool);
    m_tool->attach();
}
