#pragma once

#include <AK/Types.h>
#include <VirtualFileSystem/SyntheticFileSystem.h>

class Process;

class ProcFileSystem final : public SyntheticFileSystem {
public:
    static ProcFileSystem& the() PURE;

    virtual ~ProcFileSystem() override;
    static RetainPtr<ProcFileSystem> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override;

    void addProcess(Process&);
    void removeProcess(Process&);

private:
    ProcFileSystem();

    HashMap<pid_t, InodeIndex> m_pid2inode;
};

