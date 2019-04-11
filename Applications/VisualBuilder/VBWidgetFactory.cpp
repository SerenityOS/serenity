#include "VBWidgetFactory.h"
#include "VBButtonWidget.h"

Retained<VBWidget> VBWidgetFactory::create(const String& widget_name, VBForm& form)
{
    if (widget_name == "GButton")
        return VBButtonWidget::create(form);
    return VBWidget::create(form);
}
