serenity_component(
    CertificateSettings
    REQUIRED
    TARGETS CertificateSettings
)

stringify_gml(CertificateStoreWidget.gml CertificateStoreWidgetGML.h certificate_store_widget_gml)

set(SOURCES
    CertificateStoreWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    CertificateStoreWidgetGML.h
)

serenity_app(CertificateSettings ICON certificate)
target_link_libraries(CertificateSettings PRIVATE LibCore LibCrypto LibFileSystem LibFileSystemAccessClient LibGfx LibGUI LibMain LibTLS)
