#pragma once

#include "VBWidget.h"

class VBButtonWidget : public VBWidget {
public:
    static Retained<VBButtonWidget> create(VBForm& form) { return adopt(*new VBButtonWidget(form)); }
    virtual ~VBButtonWidget() override;

    virtual void paint(GPainter&) override;
    virtual const char* gwidget_name() const { return "GButton"; }

private:
    explicit VBButtonWidget(VBForm&);
};
