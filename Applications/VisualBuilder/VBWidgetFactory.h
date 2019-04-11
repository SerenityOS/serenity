#pragma once

#include <AK/AKString.h>
#include <AK/Retained.h>

class VBForm;
class VBWidget;

class VBWidgetFactory {
public:
    static Retained<VBWidget> create(const String& widget_name, VBForm&);
};
