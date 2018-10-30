#pragma once

#include <AK/Types.h>
#include "Limits.h"
#include "FileHandle.h"

class CharacterDevice {
public:
    virtual ~CharacterDevice();

    virtual OwnPtr<FileHandle> open(int options);

    virtual bool hasDataAvailableForRead() const = 0;

    virtual Unix::ssize_t read(byte* buffer, Unix::size_t bufferSize) = 0;
    virtual Unix::ssize_t write(const byte* buffer, Unix::size_t bufferSize) = 0;

    unsigned major() const { return m_major; }
    unsigned minor() const { return m_minor; }

protected:
    CharacterDevice(unsigned major, unsigned minor) : m_major(major), m_minor(minor) { }

private:
    unsigned m_major { 0 };
    unsigned m_minor{ 0 };
};
