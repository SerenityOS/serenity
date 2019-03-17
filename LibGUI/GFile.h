#pragma once

#include <LibGUI/GIODevice.h>
#include <AK/AKString.h>

class GFile final : public GIODevice {
public:
    GFile() { }
    explicit GFile(const String&);
    virtual ~GFile() override;

    String filename() const { return m_filename; }
    void set_filename(const String& filename) { m_filename = filename; }

    virtual bool open(GIODevice::OpenMode) override;
    virtual bool close() override;

    virtual const char* class_name() const override { return "GFile"; }

private:
    String m_filename;
};
