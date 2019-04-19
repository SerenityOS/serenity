#pragma once

#include <LibGUI/GWidget.h>

class SnakeGame : public GWidget {
public:
    explicit SnakeGame(GWidget* parent);

private:
    virtual void paint_event(GPaintEvent&) override;
};
