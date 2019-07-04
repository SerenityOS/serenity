#pragma once

#include <AK/AKString.h>

class GClipboard {
public:
    static GClipboard& the();

    String data() const;
    void set_data(const StringView&);

private:
    GClipboard();
};
