#pragma once

#include <LibGUI/GWidget.h>

class GAction;

class GToolBar : public GWidget {
public:
    explicit GToolBar(GWidget* parent);
    virtual ~GToolBar() override;

    void add_action(Retained<GAction>&&);
    void add_separator();

    bool has_frame() const { return m_has_frame; }
    void set_has_frame(bool has_frame) { m_has_frame = has_frame; }

    virtual const char* class_name() const override { return "GToolBar"; }

private:
    virtual void paint_event(GPaintEvent&) override;

    struct Item {
        enum Type {
            Invalid,
            Separator,
            Action
        };
        Type type { Invalid };
        RetainPtr<GAction> action;
    };
    Vector<OwnPtr<Item>> m_items;
    bool m_has_frame { true };
};
