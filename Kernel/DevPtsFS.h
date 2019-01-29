#pragma once

#include "SlavePTY.h"
#include <AK/Types.h>
#include <Kernel/SyntheticFileSystem.h>

class Process;

class DevPtsFS final : public SynthFS {
public:
    static DevPtsFS& the() PURE;

    virtual ~DevPtsFS() override;
    static RetainPtr<DevPtsFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override;

    void register_slave_pty(SlavePTY&);
    void unregister_slave_pty(SlavePTY&);

private:
    DevPtsFS();

    RetainPtr<SynthFSInode> create_slave_pty_device_file(unsigned index);

    HashTable<SlavePTY*> m_slave_ptys;
};

