#pragma once

#include <Kernel/Net/NetworkAdapter.h>

class LoopbackAdapter final : public NetworkAdapter {
    AK_MAKE_ETERNAL
public:
    static LoopbackAdapter& the();
    LoopbackAdapter();

    virtual void send_raw(const byte*, int) override;
    virtual const char* class_name() const override { return "LoopbackAdapter"; }

private:
    virtual ~LoopbackAdapter() override;
};
