/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace HackStudio {

class WidgetTool final : public Tool {
public:
    explicit WidgetTool(FormEditorWidget& editor, const GUI::WidgetClassRegistration& meta_class)
        : Tool(editor)
        , m_meta_class(meta_class)
    {
    }
    virtual ~WidgetTool() override { }

private:
    virtual const char* class_name() const override { return "WidgetTool"; }
    virtual void on_mousedown(GUI::MouseEvent&) override;
    virtual void on_mouseup(GUI::MouseEvent&) override;
    virtual void on_mousemove(GUI::MouseEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;

    const GUI::WidgetClassRegistration& m_meta_class;
};

}
