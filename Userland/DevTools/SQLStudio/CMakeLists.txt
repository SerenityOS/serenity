serenity_component(
    SQLStudio
    RECOMMENDED
    TARGETS SQLStudio
)

stringify_gml(SQLStudio.gml SQLStudioGML.h sql_studio_gml)

set(SOURCES
    main.cpp
    MainWidget.cpp
    ScriptEditor.cpp
)

set(GENERATED_SOURCES
    SQLStudioGML.h
)

serenity_app(SQLStudio ICON app-sql-studio)
target_link_libraries(SQLStudio PRIVATE LibCore LibDesktop LibFileSystem LibGfx LibGUI LibIPC LibMain LibSQL LibSyntax)
