#pragma once

#include <AK/RetainPtr.h>
#include <VirtualFileSystem/DiskDevice.h>

class IDEDiskDevice final : public DiskDevice {
public:
    static RetainPtr<IDEDiskDevice> create();
    virtual ~IDEDiskDevice();

    virtual unsigned blockSize() const override;
    virtual bool readBlock(unsigned index, byte*) const override;
    virtual bool writeBlock(unsigned index, const byte*) override;

protected:
    IDEDiskDevice();

private:
    virtual const char* className() const override;
};

