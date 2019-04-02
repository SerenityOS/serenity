#pragma once

#include <Kernel/NetworkAdapter.h>

class LoopbackAdapter final : public NetworkAdapter {
public:
    static LoopbackAdapter& the();

    virtual ~LoopbackAdapter() override;

    virtual void send_raw(const byte*, int) override;
    virtual const char* class_name() const override { return "LoopbackAdapter"; }

private:
    LoopbackAdapter();
};
