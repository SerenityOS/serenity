#pragma once

#include <LibGUI/GWidget.h>

class GProgressBar : public GWidget {
public:
    explicit GProgressBar(GWidget* parent);
    virtual ~GProgressBar() override;

    void set_range(int min, int max);
    void set_value(int);

    int value() const { return m_value; }

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
    int m_min { 0 };
    int m_max { 100 };
    int m_value { 0 };
};
