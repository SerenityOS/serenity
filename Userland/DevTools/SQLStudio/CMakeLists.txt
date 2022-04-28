serenity_component(
    SQLStudio
    RECOMMENDED
    TARGETS SQLStudio
)

set(SOURCES
    main.cpp
    MainWidget.cpp
    ScriptEditor.cpp
)

serenity_app(SQLStudio ICON app-sql-studio)
target_link_libraries(SQLStudio LibCore LibDesktop LibGUI LibMain LibSQL)
