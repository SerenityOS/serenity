#pragma once

#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <Kernel/KResult.h>

class VMObject;

class SharedMemory : public Retainable<SharedMemory> {
public:
    static Retained<SharedMemory> create();
    ~SharedMemory();

    KResult truncate(int);

private:
    SharedMemory();

    int m_uid { 0 };
    int m_gid { 0 };
    RetainPtr<VMObject> m_vmo;
};
