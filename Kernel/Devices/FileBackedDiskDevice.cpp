#define _FILE_OFFSET_BITS 64

#include <Kernel/Devices/FileBackedDiskDevice.h>
#include <cstring>
#include <sys/stat.h>

//#define FBBD_DEBUG
#define IGNORE_FILE_LENGTH // Useful for e.g /dev/hda2

RetainPtr<FileBackedDiskDevice> FileBackedDiskDevice::create(String&& image_path, unsigned block_size)
{
    return adopt(*new FileBackedDiskDevice(move(image_path), block_size));
}

FileBackedDiskDevice::FileBackedDiskDevice(String&& image_path, unsigned block_size)
    : m_image_path(move(image_path))
    , m_block_size(block_size)
{
    struct stat st;
    int result = stat(m_image_path.characters(), &st);
    ASSERT(result != -1);
    m_file_length = st.st_size;
    m_file = fopen(m_image_path.characters(), "r+");
}

FileBackedDiskDevice::~FileBackedDiskDevice()
{
}

unsigned FileBackedDiskDevice::block_size() const
{
    return m_block_size;
}

bool FileBackedDiskDevice::read_block(unsigned index, byte* out) const
{
    DiskOffset offset = index * m_block_size;
    return read_internal(offset, block_size(), out);
}

bool FileBackedDiskDevice::write_block(unsigned index, const byte* data)
{
    DiskOffset offset = index * m_block_size;
    return write_internal(offset, block_size(), data);
}

bool FileBackedDiskDevice::read_internal(DiskOffset offset, unsigned length, byte* out) const
{
#ifndef IGNORE_FILE_LENGTH
    if (offset + length >= m_file_length)
        return false;
#endif
#ifdef FBBD_DEBUG
    printf("[FileBackedDiskDevice] Read device @ offset %llx, length %u\n", offset, length);
#endif
    fseeko(m_file, offset, SEEK_SET);
    unsigned nread = fread(out, sizeof(byte), length, m_file);
    ASSERT(nread == length);
    return true;
}

bool FileBackedDiskDevice::write_internal(DiskOffset offset, unsigned length, const byte* data)
{
#ifndef IGNORE_FILE_LENGTH
    if (offset + length >= m_file_length)
        return false;
#endif
#ifdef FBBD_DEBUG
    printf("[FileBackedDiskDevice] Write device @ offset %llx, length %u\n", offset, length);
#endif
    fseeko(m_file, offset, SEEK_SET);
    // size_t fwrite(const void *ptr, size_t size, size_t nmemb, FILE *stream);
    unsigned nwritten = fwrite(data, sizeof(byte), length, m_file);
    ASSERT(nwritten == length);
    return true;
}

const char* FileBackedDiskDevice::class_name() const
{
    return "FileBackedDiskDevice";
}

