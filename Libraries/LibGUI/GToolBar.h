#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/GWidget.h>

class GAction;

class GToolBar : public GWidget {
    C_OBJECT(GToolBar)
public:
    virtual ~GToolBar() override;

    void add_action(GAction&);
    void add_separator();

    bool has_frame() const { return m_has_frame; }
    void set_has_frame(bool has_frame) { m_has_frame = has_frame; }

protected:
    explicit GToolBar(GWidget* parent);
    explicit GToolBar(Orientation, int button_size, GWidget* parent);

    virtual void paint_event(GPaintEvent&) override;

private:
    struct Item {
        enum Type {
            Invalid,
            Separator,
            Action
        };
        Type type { Invalid };
        RefPtr<GAction> action;
    };
    NonnullOwnPtrVector<Item> m_items;
    int m_button_size { 16 };
    bool m_has_frame { true };
};
