#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibGUI/GAbstractColumnView.h>
#include <LibGUI/GModel.h>

class GTableView : public GAbstractColumnView {
    C_OBJECT(GTableView)
public:
    virtual ~GTableView() override;

protected:
    explicit GTableView(GWidget* parent);

    virtual void paint_event(GPaintEvent&) override;
};
