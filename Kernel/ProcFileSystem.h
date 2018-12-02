#pragma once

#include <AK/Types.h>
#include <VirtualFileSystem/SyntheticFileSystem.h>

class Process;

class ProcFS final : public SynthFS {
public:
    static ProcFS& the() PURE;

    virtual ~ProcFS() override;
    static RetainPtr<ProcFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override;

    void add_process(Process&);
    void remove_process(Process&);

private:
    ProcFS();

    HashMap<pid_t, InodeIndex> m_pid2inode;
};

