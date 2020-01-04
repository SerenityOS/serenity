#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileDescription.h>

File::File()
{
}

File::~File()
{
}

KResultOr<NonnullRefPtr<FileDescription>> File::open(int options)
{
    auto description = FileDescription::create(*this);
    description->set_rw_mode(options);
    return description;
}

void File::close()
{
}

int File::ioctl(FileDescription&, unsigned, unsigned)
{
    return -ENOTTY;
}

KResultOr<Region*> File::mmap(Process&, FileDescription&, VirtualAddress, size_t, size_t, int)
{
    return KResult(-ENODEV);
}
