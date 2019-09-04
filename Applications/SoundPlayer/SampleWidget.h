#pragma once

#include <LibGUI/GFrame.h>

class ABuffer;

class SampleWidget final : public GFrame {
    C_OBJECT(SampleWidget)
public:
    explicit SampleWidget(GWidget* parent);
    virtual ~SampleWidget() override;

    void set_buffer(ABuffer*);

private:
    virtual void paint_event(GPaintEvent&) override;

    RefPtr<ABuffer> m_buffer;
};
