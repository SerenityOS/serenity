#pragma once

#include <Kernel/Net/NetworkAdapter.h>

class LoopbackAdapter final : public NetworkAdapter {
    AK_MAKE_ETERNAL
public:
    static LoopbackAdapter& the();

    virtual ~LoopbackAdapter() override;

    virtual void send_raw(const u8*, int) override;
    virtual const char* class_name() const override { return "LoopbackAdapter"; }

private:
    LoopbackAdapter();
};
