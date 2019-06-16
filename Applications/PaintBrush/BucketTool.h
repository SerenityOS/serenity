#pragma once

#include "Tool.h"

class BucketTool final : public Tool {
public:
    BucketTool();
    virtual ~BucketTool() override;

    virtual void on_mousedown(GMouseEvent&) override;

private:
    virtual const char* class_name() const override { return "BucketTool"; }
};
