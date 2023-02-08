serenity_component(
    SystemMonitor
    REQUIRED
    TARGETS SystemMonitor Profiler
)

stringify_gml(SystemMonitor.gml SystemMonitorGML.h system_monitor_gml)
stringify_gml(ProcessWindow.gml ProcessWindowGML.h process_window_gml)

set(SOURCES
    GraphWidget.cpp
    main.cpp
    MemoryStatsWidget.cpp
    NetworkStatisticsWidget.cpp
    ProcessFileDescriptorMapWidget.cpp
    ProcessMemoryMapWidget.cpp
    ProcessModel.cpp
    ProcessUnveiledPathsWidget.cpp
    ProcessStateWidget.cpp
    ThreadStackWidget.cpp
)

set(GENERATED_SOURCES
    SystemMonitorGML.h
    ProcessWindowGML.h
)

serenity_app(SystemMonitor ICON app-system-monitor)
target_link_libraries(SystemMonitor PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibSymbolication LibThreading)
