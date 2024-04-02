serenity_component(
    SQLStudio
    RECOMMENDED
    TARGETS SQLStudio
)

compile_gml(SQLStudio.gml SQLStudioGML.cpp)

set(SOURCES
    main.cpp
    MainWidget.cpp
    ScriptEditor.cpp
    SQLStudioGML.cpp
)

serenity_app(SQLStudio ICON app-sql-studio)
target_link_libraries(SQLStudio PRIVATE LibCore LibDesktop LibFileSystem LibGfx LibGUI LibIPC LibMain LibSQL LibSyntax LibURL)
