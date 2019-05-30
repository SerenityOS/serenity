#pragma once

#include <AK/AKString.h>
#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <AK/Types.h>
#include <Kernel/KResult.h>
#include <Kernel/LinearAddress.h>
#include <Kernel/UnixTypes.h>

class FileDescriptor;
class Process;
class Region;

class File : public Retainable<File> {
public:
    virtual ~File();

    virtual KResultOr<Retained<FileDescriptor>> open(int options);
    virtual void close();

    virtual bool can_read(FileDescriptor&) const = 0;
    virtual bool can_write(FileDescriptor&) const = 0;

    virtual ssize_t read(FileDescriptor&, byte*, ssize_t) = 0;
    virtual ssize_t write(FileDescriptor&, const byte*, ssize_t) = 0;
    virtual int ioctl(FileDescriptor&, unsigned request, unsigned arg);
    virtual KResultOr<Region*> mmap(Process&, LinearAddress preferred_laddr, size_t offset, size_t size, int prot);

    virtual String absolute_path(FileDescriptor&) const = 0;

    virtual KResult truncate(off_t) { return KResult(-EINVAL); }

    virtual const char* class_name() const = 0;

    virtual bool is_seekable() const { return false; }

    virtual bool is_inode() const { return false; }
    virtual bool is_shared_memory() const { return false; }
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
