#pragma once

#include "DiskDevice.h"
#include <AK/RetainPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <stdio.h>

class FileBackedDiskDevice final : public DiskDevice {
public:
    static RetainPtr<FileBackedDiskDevice> create(String&& imagePath, unsigned blockSize);
    virtual ~FileBackedDiskDevice() override;

    bool isValid() const { return m_file; }

    virtual unsigned blockSize() const override;
    virtual bool readBlock(unsigned index, byte* out) const override;
    virtual bool writeBlock(unsigned index, const byte*) override;

private:
    virtual const char* class_name() const override;

    bool readInternal(DiskOffset, unsigned length, byte* out) const;
    bool writeInternal(DiskOffset, unsigned length, const byte* data);

    FileBackedDiskDevice(String&& imagePath, unsigned blockSize);

    String m_imagePath;
    FILE* m_file { nullptr };
    DiskOffset m_fileLength { 0 };
    unsigned m_blockSize { 0 };
};

