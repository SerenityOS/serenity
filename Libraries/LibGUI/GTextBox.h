#pragma once

#include <AK/Function.h>
#include <LibGUI/GTextEditor.h>

class GTextBox : public GTextEditor {
    C_OBJECT(GTextBox)
public:
    explicit GTextBox(GWidget* parent);
    virtual ~GTextBox() override;
};
