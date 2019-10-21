#pragma once

#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <LibGUI/GModel.h>

class Project {
    AK_MAKE_NONCOPYABLE(Project)
    AK_MAKE_NONMOVABLE(Project)
public:
    static OwnPtr<Project> load_from_file(const String& path);

    GModel& model() { return *m_model; }

private:
    friend class ProjectModel;
    explicit Project(Vector<String>&& files);

    RefPtr<GModel> m_model;
    Vector<String> m_files;
};
