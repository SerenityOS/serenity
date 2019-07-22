#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/TTY/SlavePTY.h>

static DevPtsFS* s_the;

DevPtsFS& DevPtsFS::the()
{
    ASSERT(s_the);
    return *s_the;
}

NonnullRefPtr<DevPtsFS> DevPtsFS::create()
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

NonnullRefPtr<SynthFSInode> DevPtsFS::create_slave_pty_device_file(unsigned index)
{
    auto file = adopt(*new SynthFSInode(*this, generate_inode_index()));
    file->m_name = String::number(index);

    auto* device = VFS::the().get_device(11, index);
    ASSERT(device);

    file->m_metadata.size = 0;
    file->m_metadata.uid = device->uid();
    file->m_metadata.gid = device->gid();
    file->m_metadata.mode = 0020644;
    file->m_metadata.major_device = device->major();
    file->m_metadata.minor_device = device->minor();
    file->m_metadata.mtime = mepoch;
    return file;
}

void DevPtsFS::register_slave_pty(SlavePTY& slave_pty)
{
    auto inode_id = add_file(create_slave_pty_device_file(slave_pty.index()));
    slave_pty.set_devpts_inode_id(inode_id);
}

void DevPtsFS::unregister_slave_pty(SlavePTY& slave_pty)
{
    remove_file(slave_pty.devpts_inode_id().index());
}
