#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Function.h>
#include <sys/stat.h>

class GScrollBar;

class DirectoryView final : public GWidget {
public:
    DirectoryView(GWidget* parent);
    virtual ~DirectoryView() override;

    void open(const String& path);
    void reload();

    Function<void(const String&)> on_path_change;
    Function<void(String)> on_status_message;

    int item_height() const { return 16; }
    int item_count() const { return m_directories.size() + m_files.size(); }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;

    void set_status_message(String&&);

    Rect row_rect(int item_index) const;

    struct Entry {
        String name;
        size_t size { 0 };
        mode_t mode { 0 };

        bool is_directory() const { return S_ISDIR(mode); }
    };

    const Entry& entry(int index) const
    {
        if (index < m_directories.size())
            return m_directories[index];
        return m_files[index - m_directories.size()];
    }
    const GraphicsBitmap& icon_for(const Entry&) const;
    bool should_show_size_for(const Entry&) const;

    Vector<Entry> m_files;
    Vector<Entry> m_directories;

    String m_path;
    RetainPtr<GraphicsBitmap> m_directory_icon;
    RetainPtr<GraphicsBitmap> m_file_icon;
    RetainPtr<GraphicsBitmap> m_symlink_icon;
    RetainPtr<GraphicsBitmap> m_socket_icon;

    GScrollBar* m_scrollbar { nullptr };
};
