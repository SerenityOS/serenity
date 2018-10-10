#define _FILE_OFFSET_BITS 64

#include "FileBackedBlockDevice.h"
#include <cstring>
#include <sys/stat.h>

//#define FBBD_DEBUG
#define IGNORE_FILE_LENGTH // Useful for e.g /dev/hda2

RetainPtr<FileBackedBlockDevice> FileBackedBlockDevice::create(String&& imagePath, unsigned blockSize)
{
    return adopt(*new FileBackedBlockDevice(std::move(imagePath), blockSize));
}

FileBackedBlockDevice::FileBackedBlockDevice(String&& imagePath, unsigned blockSize)
    : m_imagePath(std::move(imagePath))
    , m_blockSize(blockSize)
{
    struct stat st;
    int result = stat(m_imagePath.characters(), &st);
    ASSERT(result != -1);
    m_fileLength = st.st_size;
    m_file = fopen(m_imagePath.characters(), "r+");
}

FileBackedBlockDevice::~FileBackedBlockDevice()
{
}

unsigned FileBackedBlockDevice::blockSize() const
{
    return m_blockSize;
}

bool FileBackedBlockDevice::readBlock(unsigned index, byte* out) const
{
    qword offset = index * m_blockSize;
    return read(offset, blockSize(), out);
}

bool FileBackedBlockDevice::writeBlock(unsigned index, const byte* data)
{
    qword offset = index * m_blockSize;
    return write(offset, blockSize(), data);
}

bool FileBackedBlockDevice::read(qword offset, unsigned length, byte* out) const
{
#ifndef IGNORE_FILE_LENGTH
    if (offset + length >= m_fileLength)
        return false;
#endif
#ifdef FBBD_DEBUG
    printf("[FileBackedBlockDevice] Read device @ offset %llx, length %u\n", offset, length);
#endif
    fseeko(m_file, offset, SEEK_SET);
    unsigned nread = fread(out, sizeof(byte), length, m_file);
    ASSERT(nread == length);
    return true;
}

bool FileBackedBlockDevice::write(qword offset, unsigned length, const byte* data)
{
#ifndef IGNORE_FILE_LENGTH
    if (offset + length >= m_fileLength)
        return false;
#endif
#ifdef FBBD_DEBUG
    printf("[FileBackedBlockDevice] Write device @ offset %llx, length %u\n", offset, length);
#endif
    fseeko(m_file, offset, SEEK_SET);
    // size_t fwrite(const void *ptr, size_t size, size_t nmemb, FILE *stream);
    unsigned nwritten = fwrite(data, sizeof(byte), length, m_file);
    ASSERT(nwritten == length);
    return true;
}

const char* FileBackedBlockDevice::className() const
{
    return "FileBackedBlockDevice";
}

