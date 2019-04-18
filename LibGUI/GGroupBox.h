#pragma once

#include <LibGUI/GWidget.h>

class GGroupBox : public GWidget {
public:
    GGroupBox(const String& name, GWidget* parent);
    virtual ~GGroupBox() override;

    String name() const { return m_name; }
    void set_name(const String&);

    virtual const char* class_name() const override { return "GGroupBox"; }

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
    String m_name;
};
