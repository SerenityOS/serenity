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
#include <Kernel/FileSystem/File.h>
#include <Kernel/UnixTypes.h>

class Device : public File {
public:
    virtual ~Device() override;

    unsigned major() const { return m_major; }
    unsigned minor() const { return m_minor; }

    virtual String absolute_path(const FileDescription&) const override;

    uid_t uid() const { return m_uid; }
    uid_t gid() const { return m_gid; }

    virtual bool is_device() const override { return true; }
    virtual bool is_disk_device() const { return false; }

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
