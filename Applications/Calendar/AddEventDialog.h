#pragma once

#include "Calendar.h"
#include <LibGUI/Dialog.h>
#include <LibGUI/Window.h>

class AddEventDialog final : public GUI::Dialog {
    C_OBJECT(AddEventDialog)
public:
    virtual ~AddEventDialog() override;

    static void show(Calendar* calendar, Window* parent_window = nullptr)
    {
        auto dialog = AddEventDialog::construct(calendar, parent_window);
        dialog->exec();
    }

private:
    AddEventDialog(Calendar* calendar, Window* parent_window = nullptr);

    Calendar* m_calendar;
};
