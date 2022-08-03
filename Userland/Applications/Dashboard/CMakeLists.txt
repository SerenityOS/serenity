serenity_component(
    Dashboard
    REQUIRED
    TARGETS Dashboard
)

set(SOURCES
    main.cpp
    DashboardWindow.cpp
)

serenity_app(Dashboard ICON app-dashboard)
target_link_libraries(Dashboard LibGUI LibDesktop LibMain)
