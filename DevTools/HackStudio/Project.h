#pragma once

#include "ProjectFile.h"
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <LibGUI/GModel.h>

class Project {
    AK_MAKE_NONCOPYABLE(Project)
    AK_MAKE_NONMOVABLE(Project)
public:
    static OwnPtr<Project> load_from_file(const String& path);

    [[nodiscard]] bool add_file(const String& filename);

    ProjectFile* get_file(const String& filename);

    GModel& model() { return *m_model; }

    template<typename Callback>
    void for_each_text_file(Callback callback) const
    {
        for (auto& file : m_files) {
            callback(file);
        }
    }


private:
    friend class ProjectModel;
    explicit Project(const String& path, Vector<String>&& files);

    String m_path;
    RefPtr<GModel> m_model;
    NonnullRefPtrVector<ProjectFile> m_files;
};
