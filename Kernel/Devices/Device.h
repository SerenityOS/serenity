#pragma once

// Device is the base class of everything that lives in the /dev directory.
//
// All Devices will automatically register with the VFS.
// To expose a Device to the filesystem, simply pass two unique numbers to the constructor,
// and then mknod a file in /dev with those numbers.
//
// There are two main subclasses:
//   - BlockDevice (random access)
//   - CharacterDevice (sequential)
//
// The most important functions in Device are:
//
// class_name()
//   - Used in the /proc filesystem to identify the type of Device.
//
// read() and write()
//   - Implement reading and writing.
//   - Return the number of bytes read/written, OR a negative error code.
//
// can_read() and can_write()
//
//   - Used to implement blocking I/O, and the select() and poll() syscalls.
//   - Return true if read() or write() would succeed, respectively.
//   - Note that can_read() should return true in EOF conditions,
//     and a subsequent call to read() should return 0.
//
// ioctl()
//
//   - Optional. If unimplemented, ioctl() on the device will fail with -ENOTTY.
//   - Can be overridden in subclasses to implement arbitrary functionality.
//   - Subclasses should take care to validate incoming addresses before dereferencing.
//

#include <AK/Retainable.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileDescriptor.h>

class Process;

class Device : public Retainable<Device> {
public:
    virtual ~Device();

    InodeMetadata metadata() const { return { }; }

    virtual KResultOr<Retained<FileDescriptor>> open(int options);
    virtual void close();

    virtual bool can_read(Process&) const = 0;
    virtual bool can_write(Process&) const = 0;

    virtual ssize_t read(Process&, byte*, ssize_t) = 0;
    virtual ssize_t write(Process&, const byte*, ssize_t) = 0;

    unsigned major() const { return m_major; }
    unsigned minor() const { return m_minor; }

    virtual bool is_tty() const { return false; }
    virtual bool is_master_pty() const { return false; }

    virtual int ioctl(Process&, unsigned request, unsigned arg);

    virtual const char* class_name() const = 0;

    uid_t uid() const { return m_uid; }
    uid_t gid() const { return m_gid; }

    virtual bool is_block_device() const { return false; }
    virtual bool is_character_device() const { return false; }

protected:
    Device(unsigned major, unsigned minor);
    void set_uid(uid_t uid) { m_uid = uid; }
    void set_gid(gid_t gid) { m_gid = gid; }

private:
    unsigned m_major { 0 };
    unsigned m_minor { 0 };
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
};
