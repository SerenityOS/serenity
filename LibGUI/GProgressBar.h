#pragma once

#include <LibGUI/GFrame.h>

class GProgressBar : public GFrame {
public:
    explicit GProgressBar(GWidget* parent);
    virtual ~GProgressBar() override;

    void set_range(int min, int max);
    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }
    void set_value(int);

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }

    String caption() const { return m_caption; }
    void set_caption(const String& caption) { m_caption = caption; }

    enum Format
    {
        NoText,
        Percentage,
        ValueSlashMax
    };
    Format format() const { return m_format; }
    void set_format(Format format) { m_format = format; }

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
    Format m_format { Percentage };
    int m_min { 0 };
    int m_max { 100 };
    int m_value { 0 };
    String m_caption;
};
