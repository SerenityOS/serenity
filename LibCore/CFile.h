#pragma once

#include <AK/AKString.h>
#include <LibCore/CIODevice.h>

class CFile final : public CIODevice {
public:
    CFile() {}
    explicit CFile(const String&);
    virtual ~CFile() override;

    String filename() const { return m_filename; }
    void set_filename(const String& filename) { m_filename = filename; }

    virtual bool open(CIODevice::OpenMode) override;

    enum class ShouldCloseFileDescriptor
    {
        No = 0,
        Yes
    };
    bool open(int fd, CIODevice::OpenMode, ShouldCloseFileDescriptor);

    virtual const char* class_name() const override { return "CFile"; }

private:
    String m_filename;
    ShouldCloseFileDescriptor m_should_close_file_descriptor { ShouldCloseFileDescriptor::Yes };
};
