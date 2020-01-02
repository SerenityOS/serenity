#pragma once

#include <AK/String.h>
#include <AK/RefCounted.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Types.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/VirtualAddress.h>

class FileDescription;
class Process;
class Region;

// File is the base class for anything that can be referenced by a FileDescription.
//
// The most important functions in File are:
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
//   - Optional. If unimplemented, ioctl() on this File will fail with -ENOTTY.
//   - Can be overridden in subclasses to implement arbitrary functionality.
//   - Subclasses should take care to validate incoming addresses before dereferencing.
//
// mmap()
//
//   - Optional. If unimplemented, mmap() on this File will fail with -ENODEV.
//   - Called by mmap() when userspace wants to memory-map this File somewhere.
//   - Should create a Region in the Process and return it if successful.

class File : public RefCounted<File> {
public:
    virtual ~File();

    virtual KResultOr<NonnullRefPtr<FileDescription>> open(int options);
    virtual void close();

    virtual bool can_read(const FileDescription&) const = 0;
    virtual bool can_write(const FileDescription&) const = 0;

    virtual ssize_t read(FileDescription&, u8*, ssize_t) = 0;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) = 0;
    virtual int ioctl(FileDescription&, unsigned request, unsigned arg);
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot);

    virtual String absolute_path(const FileDescription&) const = 0;

    virtual KResult truncate(off_t) { return KResult(-EINVAL); }

    virtual const char* class_name() const = 0;

    virtual bool is_seekable() const { return false; }

    virtual bool is_inode() const { return false; }
    virtual bool is_fifo() const { return false; }
    virtual bool is_device() const { return false; }
    virtual bool is_tty() const { return false; }
    virtual bool is_master_pty() const { return false; }
    virtual bool is_block_device() const { return false; }
    virtual bool is_character_device() const { return false; }
    virtual bool is_socket() const { return false; }

protected:
    File();
};
