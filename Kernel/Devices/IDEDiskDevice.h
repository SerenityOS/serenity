#pragma once

#include <Kernel/Lock.h>
#include <AK/RetainPtr.h>
#include <Kernel/Devices/DiskDevice.h>
#include "IRQHandler.h"

class IDEDiskDevice final : public IRQHandler, public DiskDevice {
public:
    static Retained<IDEDiskDevice> create();
    virtual ~IDEDiskDevice() override;

    // ^DiskDevice
    virtual unsigned block_size() const override;
    virtual bool read_block(unsigned index, byte*) const override;
    virtual bool write_block(unsigned index, const byte*) override;

protected:
    IDEDiskDevice();

private:
    // ^IRQHandler
    virtual void handle_irq() override;

    // ^DiskDevice
    virtual const char* class_name() const override;

    void initialize();
    bool wait_for_irq();
    bool read_sectors(dword start_sector, word count, byte* buffer);
    bool write_sectors(dword start_sector, word count, const byte* data);

    Lock m_lock { "IDEDiskDevice" };
    word m_cylinders { 0 };
    word m_heads { 0 };
    word m_sectors_per_track { 0 };
    volatile bool m_interrupted { false };
    volatile byte m_device_error { 0 };

};

