#pragma once

#include "WindowIdentifier.h"
#include <LibGUI/GButton.h>

class TaskbarButton final : public GButton {
public:
    TaskbarButton(const WindowIdentifier&, GWidget* parent);
    virtual ~TaskbarButton() override;

private:
    virtual void context_menu_event(GContextMenuEvent&) override;

    GMenu& ensure_menu();

    WindowIdentifier m_identifier;
    OwnPtr<GMenu> m_menu;
};
