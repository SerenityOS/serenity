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
    virtual unsigned blockSize() const override;
    virtual bool readBlock(unsigned index, byte*) const override;
    virtual bool writeBlock(unsigned index, const byte*) override;

protected:
    IDEDiskDevice();

private:
    // ^IRQHandler
    virtual void handleIRQ() override;

    // ^DiskDevice
    virtual const char* className() const override;

    struct CHS {
        dword cylinder;
        word head;
        word sector;
    };
    CHS lba_to_chs(dword) const;

    void initialize();
    bool wait_for_irq();
    bool read_sectors(dword start_sector, word count, byte* outbuf);

    SpinLock m_lock;
    word m_cylinders { 0 };
    word m_heads { 0 };
    word m_sectors_per_track { 0 };
    mutable volatile bool m_interrupted { false };

};

