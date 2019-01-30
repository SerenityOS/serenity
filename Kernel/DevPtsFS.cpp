#include <Kernel/DevPtsFS.h>
#include <Kernel/SlavePTY.h>
#include <Kernel/VirtualFileSystem.h>
#include <AK/StringBuilder.h>

static DevPtsFS* s_the;

DevPtsFS& DevPtsFS::the()
{
    ASSERT(s_the);
    return *s_the;
}

RetainPtr<DevPtsFS> DevPtsFS::create()
{
    return adopt(*new DevPtsFS);
}

DevPtsFS::DevPtsFS()
{
    s_the = this;
}

DevPtsFS::~DevPtsFS()
{
}

bool DevPtsFS::initialize()
{
    SynthFS::initialize();
    return true;
}

const char* DevPtsFS::class_name() const
{
    return "DevPtsFS";
}

RetainPtr<SynthFSInode> DevPtsFS::create_slave_pty_device_file(unsigned index)
{
    auto file = adopt(*new SynthFSInode(*this, generate_inode_index()));

    StringBuilder builder;
    builder.appendf("%u", index);
    file->m_name = builder.build();

    file->m_metadata.size = 0;
    file->m_metadata.uid = 0;
    file->m_metadata.gid = 0;
    file->m_metadata.mode = 0020666;
    file->m_metadata.majorDevice = 11;
    file->m_metadata.minorDevice = index;
    file->m_metadata.mtime = mepoch;
    return file;
}

void DevPtsFS::register_slave_pty(SlavePTY& slave_pty)
{
    InterruptDisabler disabler;
    auto inode_id = add_file(create_slave_pty_device_file(slave_pty.index()));
    slave_pty.set_devpts_inode_id(inode_id);
}

void DevPtsFS::unregister_slave_pty(SlavePTY& slave_pty)
{
    InterruptDisabler disabler;
    remove_file(slave_pty.devpts_inode_id().index());
}
