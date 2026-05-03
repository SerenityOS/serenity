serenity_component(
    UsersSettings
    RECOMMENDED
    TARGETS UsersSettings
)

compile_gml(UsersTab.gml UsersTabGML.cpp)
compile_gml(UserAddDialog.gml UserAddDialogGML.cpp)
compile_gml(UserDetailsWidget.gml UserDetailsWidgetGML.cpp)
compile_gml(ChangePasswordDialog.gml ChangePasswordDialogGML.cpp)
compile_gml(GroupsTab.gml GroupsTabGML.cpp)
compile_gml(GroupAddDialog.gml GroupAddDialogGML.cpp)
compile_gml(GroupDetailsWidget.gml GroupDetailsWidgetGML.cpp)
compile_gml(AddMemberDialog.gml AddMemberDialogGML.cpp)

set(SOURCES
    main.cpp
    UsersTab.cpp
    UsersTabGML.cpp
    UserAddDialog.cpp
    UserAddDialogGML.cpp
    UserDetailsWidget.cpp
    UserDetailsWidgetGML.cpp
    ChangePasswordDialogGML.cpp
    GroupsTab.cpp
    GroupsTabGML.cpp
    GroupAddDialog.cpp
    GroupAddDialogGML.cpp
    GroupDetailsWidget.cpp
    GroupDetailsWidgetGML.cpp
    AddMemberDialog.cpp
    AddMemberDialogGML.cpp
)

serenity_app(UsersSettings ICON app-user-settings)
target_link_libraries(UsersSettings PRIVATE LibCore LibGfx LibGUI LibMain)
