#pragma once

#include <LibCore/CIODevice.h>
#include <AK/AKString.h>

class CFile final : public CIODevice {
public:
    CFile() { }
    explicit CFile(const String&);
    virtual ~CFile() override;

    String filename() const { return m_filename; }
    void set_filename(const String& filename) { m_filename = filename; }

    virtual bool open(CIODevice::OpenMode) override;

    virtual const char* class_name() const override { return "CFile"; }

private:
    String m_filename;
};
