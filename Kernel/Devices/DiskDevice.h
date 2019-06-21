#pragma once

#include <AK/RefCounted.h>
#include <AK/Types.h>

// FIXME: Support 64-bit DiskOffset
typedef dword DiskOffset;

class DiskDevice : public RefCounted<DiskDevice> {
public:
    virtual ~DiskDevice();

    virtual unsigned block_size() const = 0;
    virtual bool read_block(unsigned index, byte*) const = 0;
    virtual bool write_block(unsigned index, const byte*) = 0;
    virtual const char* class_name() const = 0;
    bool read(DiskOffset, unsigned length, byte*) const;
    bool write(DiskOffset, unsigned length, const byte*);

    virtual bool read_blocks(unsigned index, word count, byte*) = 0;
    virtual bool write_blocks(unsigned index, word count, const byte*) = 0;

protected:
    DiskDevice();
};
