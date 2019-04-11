#include "VBButtonWidget.h"
#include <SharedGraphics/StylePainter.h>
#include <LibGUI/GPainter.h>

VBButtonWidget::VBButtonWidget(VBForm& form)
    : VBWidget(form)
{
}

VBButtonWidget::~VBButtonWidget()
{
}

void VBButtonWidget::paint(GPainter& painter)
{
    StylePainter::paint_button(painter, rect(), ButtonStyle::Normal, false);
}
