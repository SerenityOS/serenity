#pragma once

#include "BlockDevice.h"
#include <AK/RetainPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <stdio.h>

class FileBackedBlockDevice final : public BlockDevice {
public:
    static RetainPtr<FileBackedBlockDevice> create(String&& imagePath, unsigned blockSize);
    virtual ~FileBackedBlockDevice() override;

    bool isValid() const { return m_file; }

    virtual unsigned blockSize() const override;
    virtual bool readBlock(unsigned index, byte* out) const override;
    virtual bool writeBlock(unsigned index, const byte*) override;
    virtual bool read(qword offset, unsigned length, byte* out) const override;
    virtual bool write(qword offset, unsigned length, const byte* data) override;

private:
    virtual const char* className() const override;

    FileBackedBlockDevice(String&& imagePath, unsigned blockSize);

    String m_imagePath;
    FILE* m_file { nullptr };
    qword m_fileLength { 0 };
    unsigned m_blockSize { 0 };
};

