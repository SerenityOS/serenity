#pragma once

#include <AK/AKString.h>
#include <AK/RefPtr.h>
#include <AK/RefCounted.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>

class AnonymousVMObject;

class SharedMemory : public File {
public:
    static KResultOr<NonnullRefPtr<SharedMemory>> open(const String& name, int flags, mode_t);
    static KResult unlink(const String& name);
    virtual ~SharedMemory() override;

    const String& name() const { return m_name; }
    virtual KResult truncate(off_t) override;
    AnonymousVMObject* vmobject() { return m_vmobject.ptr(); }
    const AnonymousVMObject* vmobject() const { return m_vmobject.ptr(); }
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
    RefPtr<AnonymousVMObject> m_vmobject;
};
