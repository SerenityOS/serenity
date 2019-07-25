#pragma once

#include <AK/Function.h>
#include <LibGUI/GTextEditor.h>

class GTextBox final : public GTextEditor {
    C_OBJECT(GTextBox)
public:
    explicit GTextBox(GWidget* parent);
    virtual ~GTextBox() override;
};
