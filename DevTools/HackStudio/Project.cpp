#include "Project.h"
#include <LibCore/CFile.h>

class ProjectModel final : public GModel {
public:
    explicit ProjectModel(Project& project)
        : m_project(project)
    {
    }

    virtual int row_count(const GModelIndex& = GModelIndex()) const override { return m_project.m_files.size(); }
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return 1; }
    virtual GVariant data(const GModelIndex& index, Role role = Role::Display) const override
    {
        int row = index.row();
        if (role == Role::Display) {
            return m_project.m_files.at(row);
        }
        if (role == Role::Font) {
            extern String g_currently_open_file;
            if (m_project.m_files.at(row) == g_currently_open_file)
                return Font::default_bold_font();
            return {};
        }
        return {};
    }
    virtual void update() override {}

private:
    Project& m_project;
};

Project::Project(Vector<String>&& files)
    : m_files(move(files))
{
    m_model = adopt(*new ProjectModel(*this));
}

OwnPtr<Project> Project::load_from_file(const String& path)
{
    auto file = CFile::construct(path);
    if (!file->open(CFile::ReadOnly))
        return nullptr;

    Vector<String> files;
    for (;;) {
        auto line = file->read_line(1024);
        if (line.is_null())
            break;
        files.append(String::copy(line, Chomp));
    }

    return OwnPtr(new Project(move(files)));
}
