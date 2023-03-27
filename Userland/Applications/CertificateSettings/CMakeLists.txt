serenity_component(
    CertificateSettings
    REQUIRED
    TARGETS CertificateSettings
)

compile_gml(CertificateStore.gml CertificateStoreGML.h certificate_store_gml)

set(SOURCES
    CertificateStore.cpp
    main.cpp
)

set(GENERATED_SOURCES
    CertificateStoreGML.h
)

serenity_app(CertificateSettings ICON certificate)
target_link_libraries(CertificateSettings PRIVATE LibCore LibCrypto LibGfx LibGUI LibMain LibTLS)
