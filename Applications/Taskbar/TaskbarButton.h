#pragma once

#include "WindowIdentifier.h"
#include <LibGUI/GButton.h>

class TaskbarButton final : public GButton {
    C_OBJECT(TaskbarButton)
public:
    TaskbarButton(const WindowIdentifier&, GWidget* parent);
    virtual ~TaskbarButton() override;

private:
    virtual void context_menu_event(GContextMenuEvent&) override;
    virtual void resize_event(GResizeEvent&) override;

    WindowIdentifier m_identifier;
};
