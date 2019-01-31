#pragma once

#include <AK/Types.h>
#include <Kernel/SyntheticFileSystem.h>

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

    void add_sys_file(String&&, Function<ByteBuffer(SynthFSInode&)>&& read_callback, Function<ssize_t(SynthFSInode&, const ByteBuffer&)>&& write_callback);
    void add_sys_bool(String&&, bool*, Function<void()>&& change_callback = nullptr);

private:
    ProcFS();

    HashMap<pid_t, InodeIndex> m_pid2inode;
    InodeIdentifier m_sys_dir;
};

