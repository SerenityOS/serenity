#pragma once

#include <LibGUI/GWidget.h>

class GLazyWidget : public GWidget {
    C_OBJECT(GLazyWidget)
public:
    virtual ~GLazyWidget() override;

    Function<void(GLazyWidget&)> on_first_show;

protected:
    explicit GLazyWidget(GWidget* parent = nullptr);

private:
    virtual void show_event(GShowEvent&) override;

    bool m_has_been_shown { false };
};
