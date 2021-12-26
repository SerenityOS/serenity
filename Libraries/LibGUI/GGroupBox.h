#pragma once

#include <LibGUI/GWidget.h>

class GGroupBox : public GWidget {
public:
    explicit GGroupBox(GWidget* parent);
    GGroupBox(const StringView& title, GWidget* parent);
    virtual ~GGroupBox() override;

    String title() const { return m_title; }
    void set_title(const StringView&);

    virtual const char* class_name() const override { return "GGroupBox"; }

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
    String m_title;
};
