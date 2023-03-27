serenity_component(
    NetworkSettings
    REQUIRED
    TARGETS NetworkSettings
)

compile_gml(NetworkSettings.gml NetworkSettingsGML.h network_settings_gml)
compile_gml(CertStoreDialog.gml CertStoreDialogGML.h cert_store_dialog_gml)

set(SOURCES
    NetworkSettingsWidget.cpp
    CertStoreDialog.cpp
    main.cpp
)

set(GENERATED_SOURCES
    NetworkSettingsGML.h
    CertStoreDialogGML.h
)

serenity_app(NetworkSettings ICON network)
target_link_libraries(NetworkSettings PRIVATE LibCore LibGfx LibGUI LibMain LibCrypto LibTLS)
