#pragma once

#include <LibGUI/GLayout.h>
#include <LibGUI/GWidget.h>

class GBoxLayout final : public GLayout {
public:
    explicit GBoxLayout(Orientation);
    virtual ~GBoxLayout() override;

    Orientation orientation() const { return m_orientation; }

    virtual void run(GWidget&) override;

private:
    Orientation m_orientation;
};
