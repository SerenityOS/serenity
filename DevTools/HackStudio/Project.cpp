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
            return m_project.m_files.at(row).name();
        }
        if (role == Role::Font) {
            extern String g_currently_open_file;
            if (m_project.m_files.at(row).name() == g_currently_open_file)
                return Font::default_bold_font();
            return {};
        }
        return {};
    }
    virtual void update() override
    {
        did_update();
    }

private:
    Project& m_project;
};

Project::Project(const String& path, Vector<String>&& filenames)
    : m_path(path)
{
    for (auto& filename : filenames) {
        m_files.append(ProjectFile::construct_with_name(filename));
    }
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

    return OwnPtr(new Project(path, move(files)));
}

bool Project::add_file(const String& filename)
{
    auto project_file = CFile::construct(m_path);
    if (!project_file->open(CFile::WriteOnly))
        return false;

    for (auto& file : m_files) {
        // FIXME: Check for error here. CIODevice::printf() needs some work on error reporting.
        project_file->printf("%s\n", file.name().characters());
    }

    project_file->printf("%s\n", filename.characters());

    if (!project_file->close())
        return false;

    m_files.append(ProjectFile::construct_with_name(filename));
    m_model->update();
    return true;
}

ProjectFile* Project::get_file(const String& filename)
{
    for (auto& file : m_files) {
        if (file.name() == filename)
            return &file;
    }
    return nullptr;
}
