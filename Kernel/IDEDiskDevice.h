#pragma once

#include <AK/Lock.h>
#include <AK/RetainPtr.h>
#include <VirtualFileSystem/DiskDevice.h>
#include "IRQHandler.h"

class IDEDiskDevice final : public IRQHandler, public DiskDevice {
public:
    static RetainPtr<IDEDiskDevice> create();
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

    struct CHS {
        dword cylinder;
        word head;
        word sector;
    };
    CHS lba_to_chs(dword) const;

    void initialize();
    bool wait_for_irq();
    bool read_sectors(dword start_sector, word count, byte* buffer);
    bool write_sectors(dword start_sector, word count, const byte* data);

    SpinLock m_lock;
    word m_cylinders { 0 };
    word m_heads { 0 };
    word m_sectors_per_track { 0 };
    mutable volatile bool m_interrupted { false };

};

