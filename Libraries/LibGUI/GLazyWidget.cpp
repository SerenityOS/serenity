#include <LibGUI/GLazyWidget.h>

GLazyWidget::GLazyWidget(GWidget* parent)
    : GWidget(parent)
{
}

GLazyWidget::~GLazyWidget()
{
}

void GLazyWidget::show_event(GShowEvent&)
{
    if (m_has_been_shown)
        return;
    m_has_been_shown = true;

    ASSERT(on_first_show);
    on_first_show(*this);
}
