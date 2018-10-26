#pragma once

#include <AK/Types.h>
#include <VirtualFileSystem/SyntheticFileSystem.h>

class Task;

class ProcFileSystem final : public SyntheticFileSystem {
public:
    static ProcFileSystem& the() PURE;

    virtual ~ProcFileSystem() override;
    static RetainPtr<ProcFileSystem> create();

    virtual bool initialize() override;
    virtual const char* className() const override;

    void addProcess(Task&);
    void removeProcess(Task&);

private:
    ProcFileSystem();

    HashMap<pid_t, InodeIndex> m_pid2inode;
};

