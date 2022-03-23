serenity_component(
    SystemMonitor
    REQUIRED
    TARGETS SystemMonitor Profiler Inspector
)

compile_gml(SystemMonitor.gml SystemMonitorGML.h system_monitor_gml)

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
    SystemMonitorGML.h
)

serenity_app(SystemMonitor ICON app-system-monitor)
target_link_libraries(SystemMonitor LibGUI LibSymbolication LibPCIDB LibMain)
