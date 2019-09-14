#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/String.h>

class GWindowServerConnection;

class GClipboard {
public:
    static GClipboard& the();

    String data() const { return data_and_type().data; }
    String type() const { return data_and_type().type; }
    void set_data(const StringView&, const String& data_type = "text");

    struct DataAndType {
        String data;
        String type;
    };

    DataAndType data_and_type() const;

    void did_receive_clipboard_contents_changed(Badge<GWindowServerConnection>, const String& data_type);

    Function<void(const String& data_type)> on_content_change;

private:
    GClipboard();
};
