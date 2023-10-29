include(GNUInstallDirs)

install(TARGETS LibAudio LibCore LibFileSystem LibGfx LibIPC LibJS LibWeb LibWebView LibWebSocket LibProtocol LibGUI LibMarkdown LibGemini LibHTTP LibGL LibSoftGPU LibVideo LibWasm LibXML LibIDL LibTextCodec LibCrypto LibLocale LibRegex LibSyntax LibUnicode LibCompress LibTLS LibGLSL LibGPU LibThreading LibSQL LibImageDecoderClient LibX86 LibJIT
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

install(TARGETS ladybird WebContent ImageDecoder
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
)

install(FILES
    org.serenityos.Ladybird-gtk4.svg
  DESTINATION "${CMAKE_INSTALL_DATADIR}/icons/hicolor/scalable/apps"
)
install(FILES
    org.serenityos.Ladybird-gtk4.desktop
  DESTINATION "${CMAKE_INSTALL_DATADIR}/applications"
)
install(FILES
    org.serenityos.Ladybird-gtk4.service
  DESTINATION "${CMAKE_INSTALL_DATADIR}/dbus-1/services"
)

install(DIRECTORY
    "${SERENITY_SOURCE_DIR}/Base/res/html"
    "${SERENITY_SOURCE_DIR}/Base/res/fonts"
    "${SERENITY_SOURCE_DIR}/Base/res/icons"
    "${SERENITY_SOURCE_DIR}/Base/res/themes"
    "${SERENITY_SOURCE_DIR}/Base/res/color-palettes"
    "${SERENITY_SOURCE_DIR}/Base/res/cursor-themes"
  DESTINATION "${CMAKE_INSTALL_DATADIR}/res"
  USE_SOURCE_PERMISSIONS MESSAGE_NEVER
)

install(FILES
    "${SERENITY_SOURCE_DIR}/Base/home/anon/.config/BrowserAutoplayAllowlist.txt"
    "${SERENITY_SOURCE_DIR}/Base/home/anon/.config/BrowserContentFilters.txt"
    "${Lagom_BINARY_DIR}/cacert.pem"
  DESTINATION "${CMAKE_INSTALL_DATADIR}/res/ladybird"
)
