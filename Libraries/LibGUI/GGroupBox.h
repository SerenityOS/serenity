#pragma once

#include <LibGUI/GWidget.h>

class GGroupBox : public GWidget {
    C_OBJECT(GGroupBox)
public:
    virtual ~GGroupBox() override;

    String title() const { return m_title; }
    void set_title(const StringView&);

protected:
    explicit GGroupBox(GWidget* parent);
    GGroupBox(const StringView& title, GWidget* parent);

    virtual void paint_event(GPaintEvent&) override;

private:
    String m_title;
};
