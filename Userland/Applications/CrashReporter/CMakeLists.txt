serenity_component(
    CrashReporter
    REQUIRED
    TARGETS CrashReporter
)

compile_gml(CrashReporterWindow.gml CrashReporterWindowGML.h crash_reporter_window_gml)


set(SOURCES
    main.cpp
    CrashReporterWindowGML.h
)

serenity_app(CrashReporter ICON app-crash-reporter)
target_link_libraries(CrashReporter LibCore LibCoredump LibDesktop LibGUI LibMain)
