#pragma once

#include <AK/CircularQueue.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/VM/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/WaitQueue.h>

class SB16;

class SB16 final : public IRQHandler
    , public CharacterDevice {
public:
    SB16();
    virtual ~SB16() override;

    static SB16& the();

    // ^CharacterDevice
    virtual bool can_read(const FileDescription&) const override;
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_write(const FileDescription&) const override { return true; }

private:
    // ^IRQHandler
    virtual void handle_irq() override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "SB16"; }

    void initialize();
    void wait_for_irq();
    void dma_start(uint32_t length);
    void set_sample_rate(uint16_t hz);
    void dsp_write(u8 value);
    u8 dsp_read();

    RefPtr<PhysicalPage> m_dma_buffer_page;
    int m_major_version { 0 };

    WaitQueue m_irq_queue;
};
