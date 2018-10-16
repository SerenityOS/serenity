#pragma once

#include <AK/Retainable.h>
#include <AK/Types.h>

class DiskDevice : public Retainable<DiskDevice> {
public:
    virtual ~DiskDevice();

    virtual unsigned blockSize() const = 0;
    virtual bool readBlock(unsigned index, byte*) const = 0;
    virtual bool writeBlock(unsigned index, const byte*) = 0;
    virtual const char* className() const = 0;
    bool read(qword offset, unsigned length, byte*) const;
    bool write(qword offset, unsigned length, const byte*);

protected:
    DiskDevice();
};

