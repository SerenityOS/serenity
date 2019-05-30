#include <Kernel/File.h>
#include <Kernel/FileSystem/FileDescriptor.h>

File::File()
{
}

File::~File()
{
}

KResultOr<Retained<FileDescriptor>> File::open(int options)
{
    UNUSED_PARAM(options);
    return FileDescriptor::create(this);
}

void File::close()
{
}

int File::ioctl(FileDescriptor&, unsigned, unsigned)
{
    return -ENOTTY;
}

KResultOr<Region*> File::mmap(Process&, LinearAddress, size_t, size_t, int)
{
    return KResult(-ENODEV);
}

