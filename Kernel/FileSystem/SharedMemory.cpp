#include <AK/HashMap.h>
#include <Kernel/FileSystem/SharedMemory.h>
#include <Kernel/Lock.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>

Lockable<HashMap<String, RefPtr<SharedMemory>>>& shared_memories()
{
    static Lockable<HashMap<String, RefPtr<SharedMemory>>>* map;
    if (!map)
        map = new Lockable<HashMap<String, RefPtr<SharedMemory>>>;
    return *map;
}

KResultOr<NonnullRefPtr<SharedMemory>> SharedMemory::open(const String& name, int flags, mode_t mode)
{
    UNUSED_PARAM(flags);
    LOCKER(shared_memories().lock());
    auto it = shared_memories().resource().find(name);
    if (it != shared_memories().resource().end()) {
        auto shared_memory = it->value;
        // FIXME: Improved access checking.
        if (shared_memory->uid() != current->process().uid())
            return KResult(-EACCES);
        return *shared_memory;
    }
    auto shared_memory = adopt(*new SharedMemory(name, current->process().uid(), current->process().gid(), mode));
    shared_memories().resource().set(name, shared_memory.ptr());
    return shared_memory;
}

KResult SharedMemory::unlink(const String& name)
{
    LOCKER(shared_memories().lock());
    auto it = shared_memories().resource().find(name);
    if (it == shared_memories().resource().end())
        return KResult(-ENOENT);
    shared_memories().resource().remove(it);
    return KSuccess;
}

SharedMemory::SharedMemory(const String& name, uid_t uid, gid_t gid, mode_t mode)
    : m_name(name)
    , m_uid(uid)
    , m_gid(gid)
    , m_mode(mode)
{
}

SharedMemory::~SharedMemory()
{
}

KResult SharedMemory::truncate(int length)
{
    if (!length) {
        m_vmobject = nullptr;
        return KSuccess;
    }

    if (!m_vmobject) {
        m_vmobject = AnonymousVMObject::create_with_size(length);
        return KSuccess;
    }

    // FIXME: Support truncation.
    ASSERT_NOT_REACHED();
    return KResult(-ENOTIMPL);
}

String SharedMemory::absolute_path(const FileDescription&) const
{
    return String::format("shm:%u", this);
}

int SharedMemory::read(FileDescription&, u8* buffer, int buffer_size)
{
    UNUSED_PARAM(buffer);
    UNUSED_PARAM(buffer_size);
    // FIXME: Implement.
    ASSERT_NOT_REACHED();
}

int SharedMemory::write(FileDescription&, const u8* data, int data_size)
{
    UNUSED_PARAM(data);
    UNUSED_PARAM(data_size);
    // FIXME: Implement.
    ASSERT_NOT_REACHED();
}

KResultOr<Region*> SharedMemory::mmap(Process& process, FileDescription&, VirtualAddress vaddr, size_t offset, size_t size, int prot)
{
    if (!vmobject())
        return KResult(-ENODEV);
    return process.allocate_region_with_vmobject(vaddr, size, *vmobject(), offset, name(), prot);
}
