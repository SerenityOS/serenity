#pragma once

#include <AK/RefPtr.h>
#include <AK/URL.h>

class Download;
class PSClientConnection;

class Protocol {
public:
    virtual ~Protocol();

    const String& name() const { return m_name; }
    virtual RefPtr<Download> start_download(PSClientConnection&, const URL&) = 0;

    static Protocol* find_by_name(const String&);

protected:
    explicit Protocol(const String& name);

private:
    String m_name;
};
