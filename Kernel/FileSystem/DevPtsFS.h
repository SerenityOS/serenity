#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/SyntheticFileSystem.h>

class Process;
class SlavePTY;

class DevPtsFS final : public SynthFS {
public:
    static DevPtsFS& the();

    virtual ~DevPtsFS() override;
    static NonnullRefPtr<DevPtsFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override;

    void register_slave_pty(SlavePTY&);
    void unregister_slave_pty(SlavePTY&);

private:
    DevPtsFS();

    NonnullRefPtr<SynthFSInode> create_slave_pty_device_file(unsigned index);

    HashTable<SlavePTY*> m_slave_ptys;
};
