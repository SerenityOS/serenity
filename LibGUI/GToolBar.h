#pragma once

#include <LibGUI/GWidget.h>

class GAction;

class GToolBar : public GWidget {
public:
    explicit GToolBar(GWidget* parent);
    virtual ~GToolBar() override;

    void add_action(Retained<GAction>&&);
    void add_separator();

private:
    virtual const char* class_name() const override { return "GToolBar"; }
    virtual void paint_event(GPaintEvent&) override;

    struct Item {
        enum Type { Invalid, Separator, Action };
        Type type { Invalid };
        RetainPtr<GAction> action;
    };
    Vector<OwnPtr<Item>> m_items;
};
