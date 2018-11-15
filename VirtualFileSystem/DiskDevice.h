#pragma once

#include <AK/Retainable.h>
#include <AK/Types.h>

#ifdef SERENITY
// FIXME: Support 64-bit DiskOffset
typedef dword DiskOffset;
#else
typedef qword DiskOffset;
#endif

class DiskDevice : public Retainable<DiskDevice> {
public:
    virtual ~DiskDevice();

    virtual unsigned blockSize() const = 0;
    virtual bool readBlock(unsigned index, byte*) const = 0;
    virtual bool writeBlock(unsigned index, const byte*) = 0;
    virtual const char* class_name() const = 0;
    bool read(DiskOffset, unsigned length, byte*) const;
    bool write(DiskOffset, unsigned length, const byte*);

protected:
    DiskDevice();
};

