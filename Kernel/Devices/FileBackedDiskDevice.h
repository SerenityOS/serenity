#pragma once

#include <AK/String.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/DiskDevice.h>
#include <stdio.h>

class FileBackedDiskDevice final : public DiskDevice {
public:
    static RefPtr<FileBackedDiskDevice> create(const String& image_path, size_t block_size);
    virtual ~FileBackedDiskDevice() override;

    bool is_valid() const { return m_file; }

    virtual bool read_block(unsigned index, u8* out) const override;
    virtual bool write_block(unsigned index, const u8*) override;

private:
    virtual const char* class_name() const override;

    bool read_internal(DiskOffset, unsigned length, u8* out) const;
    bool write_internal(DiskOffset, unsigned length, const u8* data);

    FileBackedDiskDevice(const String& image_path, size_t block_size);

    String m_image_path;
    FILE* m_file { nullptr };
    DiskOffset m_file_length { 0 };
};
