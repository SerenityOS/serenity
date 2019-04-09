#pragma once

#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/AKString.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>

class VMObject;

class SharedMemory : public Retainable<SharedMemory> {
public:
    static KResultOr<Retained<SharedMemory>> open(const String& name, int flags, mode_t);
    static KResult unlink(const String& name);
    ~SharedMemory();

    String name() const { return m_name; }
    KResult truncate(int);
    VMObject* vmo() { return m_vmo.ptr(); }
    const VMObject* vmo() const { return m_vmo.ptr(); }
    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }

private:
    SharedMemory(const String& name, uid_t, gid_t, mode_t);

    String m_name;
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    mode_t m_mode { 0 };
    RetainPtr<VMObject> m_vmo;
};
