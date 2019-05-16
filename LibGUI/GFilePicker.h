#include <LibGUI/GDialog.h>
#include <LibGUI/GTableView.h>

class GDirectoryModel;

class GFilePicker final : public GDialog {
public:
    GFilePicker(const String& path = "/", CObject* parent = nullptr);
    virtual ~GFilePicker() override;

    // TODO: Should this return a FileSystemPath instead?
    String selected_file() const { return m_selected_file; }

    virtual const char* class_name() const override { return "GFilePicker"; }

private:
    GTableView* m_view { nullptr };
    Retained<GDirectoryModel> m_model;
    String m_selected_file;
};
