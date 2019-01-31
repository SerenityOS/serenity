#pragma once

#include <AK/Retainable.h>
#include <AK/Types.h>
#include "Limits.h"
#include "FileDescriptor.h"

class Process;

class CharacterDevice : public Retainable<CharacterDevice> {
public:
    virtual ~CharacterDevice();

    InodeMetadata metadata() const { return { }; }

    virtual RetainPtr<FileDescriptor> open(int& error, int options);
    virtual void close();

    virtual bool can_read(Process&) const = 0;
    virtual bool can_write(Process&) const = 0;

    virtual ssize_t read(Process&, byte* buffer, size_t bufferSize) = 0;
    virtual ssize_t write(Process&, const byte* buffer, size_t bufferSize) = 0;

    unsigned major() const { return m_major; }
    unsigned minor() const { return m_minor; }

    virtual bool is_tty() const { return false; }
    virtual bool is_master_pty() const { return false; }

    virtual int ioctl(Process&, unsigned request, unsigned arg);

    virtual const char* class_name() const = 0;

    uid_t uid() const { return m_uid; }
    uid_t gid() const { return m_gid; }

protected:
    CharacterDevice(unsigned major, unsigned minor) : m_major(major), m_minor(minor) { }
    void set_uid(uid_t uid) { m_uid = uid; }
    void set_gid(gid_t gid) { m_gid = gid; }

private:
    unsigned m_major { 0 };
    unsigned m_minor { 0 };
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
};
