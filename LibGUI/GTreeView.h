#pragma once

#include <LibGUI/GAbstractView.h>

class GTreeView : public GAbstractView {
public:
    explicit GTreeView(GWidget*);
    virtual ~GTreeView() override;

    virtual const char* class_name() const override { return "GTreeView"; }

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
};
