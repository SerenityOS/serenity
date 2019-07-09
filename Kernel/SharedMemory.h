#pragma once

#include <AK/AKString.h>
#include <AK/RefPtr.h>
#include <AK/RefCounted.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>

class VMObject;

class SharedMemory : public File {
public:
    static KResultOr<NonnullRefPtr<SharedMemory>> open(const String& name, int flags, mode_t);
    static KResult unlink(const String& name);
    virtual ~SharedMemory() override;

    const String& name() const { return m_name; }
    virtual KResult truncate(off_t) override;
    VMObject* vmo() { return m_vmo.ptr(); }
    const VMObject* vmo() const { return m_vmo.ptr(); }
    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }

private:
    // ^File
    virtual bool can_read(FileDescription&) const override { return true; }
    virtual bool can_write(FileDescription&) const override { return true; }
    virtual int read(FileDescription&, u8*, int) override;
    virtual int write(FileDescription&, const u8*, int) override;
    virtual String absolute_path(const FileDescription&) const override;
    virtual const char* class_name() const override { return "SharedMemory"; }
    virtual bool is_shared_memory() const override { return true; }
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, VirtualAddress, size_t offset, size_t size, int prot) override;

    SharedMemory(const String& name, uid_t, gid_t, mode_t);

    String m_name;
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    mode_t m_mode { 0 };
    RefPtr<VMObject> m_vmo;
};
