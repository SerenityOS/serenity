#pragma once

#include <AK/Types.h>
#include "Limits.h"
#include "FileDescriptor.h"

class Process;

class CharacterDevice {
public:
    virtual ~CharacterDevice();

    RetainPtr<FileDescriptor> open(int options);

    virtual bool has_data_available_for_reading() const = 0;

    virtual ssize_t read(byte* buffer, size_t bufferSize) = 0;
    virtual ssize_t write(const byte* buffer, size_t bufferSize) = 0;

    unsigned major() const { return m_major; }
    unsigned minor() const { return m_minor; }

    virtual bool is_tty() const { return false; }

    virtual int ioctl(Process&, unsigned request, unsigned arg);

protected:
    CharacterDevice(unsigned major, unsigned minor) : m_major(major), m_minor(minor) { }

private:
    unsigned m_major { 0 };
    unsigned m_minor { 0 };
};
