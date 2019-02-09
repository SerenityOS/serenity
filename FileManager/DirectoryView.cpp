#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>
#include <AK/FileSystemPath.h>
#include "DirectoryView.h"

DirectoryView::DirectoryView(GWidget* parent)
    : GWidget(parent)
{
    m_directory_icon = GraphicsBitmap::load_from_file("/res/icons/folder16.rgb", { 16, 16 });
    m_file_icon = GraphicsBitmap::load_from_file("/res/icons/file16.rgb", { 16, 16 });
    m_symlink_icon = GraphicsBitmap::load_from_file("/res/icons/link16.rgb", { 16, 16 });
}

DirectoryView::~DirectoryView()
{
}

void DirectoryView::open(const String& path)
{
    if (m_path == path)
        return;
    m_path = path;
    reload();
    if (on_path_change)
        on_path_change(m_path);
    update();
}

void DirectoryView::reload()
{
    DIR* dirp = opendir(m_path.characters());
    if (!dirp) {
        perror("opendir");
        exit(1);
    }
    m_directories.clear();
    m_files.clear();
    while (auto* de = readdir(dirp)) {
        Entry entry;
        entry.name = de->d_name;
        struct stat st;
        int rc = lstat(String::format("%s/%s", m_path.characters(), de->d_name).characters(), &st);
        if (rc < 0) {
            perror("lstat");
            continue;
        }
        entry.size = st.st_size;
        entry.mode = st.st_mode;
        auto& entries = S_ISDIR(st.st_mode) ? m_directories : m_files;
        entries.append(move(entry));
    }
    closedir(dirp);
}

const GraphicsBitmap& DirectoryView::icon_for(const Entry& entry) const
{
    if (S_ISDIR(entry.mode))
        return *m_directory_icon;
    if (S_ISLNK(entry.mode))
        return *m_symlink_icon;
    return *m_file_icon;
}

static String pretty_byte_size(size_t size)
{
    return String::format("%u", size);
}

bool DirectoryView::should_show_size_for(const Entry& entry) const
{
    return S_ISREG(entry.mode);
}

Rect DirectoryView::row_rect(int item_index) const
{
    return { 0, item_index * item_height(), width(), item_height() };
}

void DirectoryView::mousedown_event(GMouseEvent& event)
{
    for (int i = 0; i < item_count(); ++i) {
        if (row_rect(i).contains(event.position())) {
            auto& entry = this->entry(i);
            if (entry.is_directory()) {
                FileSystemPath new_path(String::format("%s/%s", m_path.characters(), entry.name.characters()));
                open(new_path.string());
            }
        }
    }
}

void DirectoryView::paint_event(GPaintEvent&)
{
    Painter painter(*this);

    int horizontal_padding = 5;
    int icon_size = 16;
    int painted_item_index = 0;

    auto process_entries = [&] (const Vector<Entry>& entries) {
        for (size_t i = 0; i < entries.size(); ++i, ++painted_item_index) {
            auto& entry = entries[i];
            int y = painted_item_index * item_height();
            Rect icon_rect(horizontal_padding, y, icon_size, item_height());
            Rect name_rect(icon_rect.right() + horizontal_padding, y, 100, item_height());
            Rect size_rect(name_rect.right() + horizontal_padding, y, 64, item_height());
            painter.fill_rect(row_rect(painted_item_index), i % 2 ? Color::LightGray : Color::White);
            painter.blit_with_alpha(icon_rect.location(), icon_for(entry), { 0, 0, icon_size, icon_size });
            painter.draw_text(name_rect, entry.name, Painter::TextAlignment::CenterLeft, Color::Black);
            if (should_show_size_for(entry))
                painter.draw_text(size_rect, pretty_byte_size(entry.size), Painter::TextAlignment::CenterRight, Color::Black);
        }
    };

    process_entries(m_directories);
    process_entries(m_files);

    Rect unpainted_rect(0, painted_item_index * item_height(), width(), height());
    unpainted_rect.intersect(rect());
    painter.fill_rect(unpainted_rect, Color::White);
}
