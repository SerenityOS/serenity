serenity_component(
    NetworkSettings
    REQUIRED
    TARGETS NetworkSettings
)

compile_gml(NetworkSettings.gml NetworkSettingsGML.h network_settings_gml)

set(SOURCES
    NetworkSettingsWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    NetworkSettingsGML.h
)

serenity_app(NetworkSettings ICON network)
target_link_libraries(NetworkSettings LibGUI LibMain)
