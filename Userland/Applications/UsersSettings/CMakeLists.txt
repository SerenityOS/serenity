serenity_component(
    UsersSettings
    RECOMMENDED
    TARGETS UsersSettings
)

compile_gml(UsersTab.gml UsersTabGML.cpp)
compile_gml(UserAddDialog.gml UserAddDialogGML.cpp)
compile_gml(UserDetailsWidget.gml UserDetailsWidgetGML.cpp)
compile_gml(ChangePasswordDialog.gml ChangePasswordDialogGML.cpp)

set(SOURCES
    main.cpp
    UsersTab.cpp
    UsersTabGML.cpp
    UserAddDialog.cpp
    UserAddDialogGML.cpp
    UserDetailsWidget.cpp
    UserDetailsWidgetGML.cpp
    ChangePasswordDialogGML.cpp
)

serenity_app(UsersSettings ICON app-user-settings)
target_link_libraries(UsersSettings PRIVATE LibCore LibGfx LibGUI LibMain)
