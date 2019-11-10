#pragma once

#include "Tool.h"

class GWidgetClassRegistration;

class WidgetTool final : public Tool {
public:
    explicit WidgetTool(FormEditorWidget& editor, const GWidgetClassRegistration& meta_class)
        : Tool(editor)
        , m_meta_class(meta_class)
    {
    }
    virtual ~WidgetTool() override {}

private:
    virtual const char* class_name() const override { return "WidgetTool"; }
    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;

    const GWidgetClassRegistration& m_meta_class;
};
