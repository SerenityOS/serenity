#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Vector.h>
#include "VBWidget.h"

class VBForm : public GWidget {
public:
    explicit VBForm(const String& name, GWidget* parent = nullptr);
    virtual ~VBForm() override;

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
    String m_name;
    int m_grid_size { 5 };

    Vector<VBWidget*> m_widgets;
};
