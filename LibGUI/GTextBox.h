#pragma once

#include <LibGUI/GTextEditor.h>
#include <AK/Function.h>

class GTextBox final : public GTextEditor {
public:
    explicit GTextBox(GWidget* parent);
    virtual ~GTextBox() override;

    virtual const char* class_name() const override { return "GTextBox"; }
};

