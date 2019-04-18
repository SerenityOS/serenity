#pragma once

#include "VBWidgetType.h"
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/AKString.h>

class GWidget;
class VBProperty;
class VBWidget;

class VBWidgetRegistry {
public:
    template<typename Callback> static void for_each_widget_type(Callback callback)
    {
        for (unsigned i = 1; i < (unsigned)VBWidgetType::__Count; ++i)
            callback((VBWidgetType)i);
    }

    static GWidget* build_gwidget(VBWidget&, VBWidgetType, GWidget* parent, Vector<OwnPtr<VBProperty>>&);
};
