#pragma once

#include <LibGUI/GFrame.h>

class ABuffer;

class SampleWidget final : public GFrame {
    C_OBJECT(SampleWidget)
public:
    virtual ~SampleWidget() override;

    void set_buffer(ABuffer*);

private:
    explicit SampleWidget(GWidget* parent);
    virtual void paint_event(GPaintEvent&) override;

    RefPtr<ABuffer> m_buffer;
};
