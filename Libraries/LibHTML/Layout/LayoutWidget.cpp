#include <LibDraw/Font.h>
#include <LibDraw/StylePainter.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibHTML/Layout/LayoutWidget.h>

LayoutWidget::LayoutWidget(const Element& element, GWidget& widget)
    : LayoutReplaced(element, StyleProperties::create())
    , m_widget(widget)
{
}

LayoutWidget::~LayoutWidget()
{
    widget().remove_from_parent();
}

void LayoutWidget::layout()
{
    rect().set_size(FloatSize(widget().width(), widget().height()));
    LayoutReplaced::layout();
    widget().move_to(rect().x(), rect().y());
}

void LayoutWidget::render(RenderingContext& context)
{
    LayoutReplaced::render(context);
}
