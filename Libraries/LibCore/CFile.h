#pragma once

#include <AK/AKString.h>
#include <LibCore/CIODevice.h>

class CFile final : public CIODevice {
    C_OBJECT(CFile)
public:
    CFile(CObject* parent = nullptr)
        : CIODevice(parent)
    {
    }
    explicit CFile(const StringView&, CObject* parent = nullptr);
    virtual ~CFile() override;

    String filename() const { return m_filename; }
    void set_filename(const StringView& filename) { m_filename = filename; }

    virtual bool open(CIODevice::OpenMode) override;

    enum class ShouldCloseFileDescription {
        No = 0,
        Yes
    };
    bool open(int fd, CIODevice::OpenMode, ShouldCloseFileDescription);

private:
    String m_filename;
    ShouldCloseFileDescription m_should_close_file_descriptor { ShouldCloseFileDescription::Yes };
};
