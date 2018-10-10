#pragma once

#include <AK/Retainable.h>
#include <AK/Types.h>

class BlockDevice : public Retainable<BlockDevice> {
public:
    virtual ~BlockDevice();

    virtual unsigned blockSize() const = 0;
    virtual bool readBlock(unsigned index, byte*) const = 0;
    virtual bool writeBlock(unsigned index, const byte*) = 0;
    virtual const char* className() const = 0;
    virtual bool read(qword offset, unsigned length, byte*) const = 0;
    virtual bool write(qword offset, unsigned length, const byte*) = 0;

protected:
    BlockDevice();
};

