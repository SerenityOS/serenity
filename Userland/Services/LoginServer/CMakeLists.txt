serenity_component(
    LoginServer
    REQUIRED
    TARGETS LoginServer
)

stringify_gml(LoginWindow.gml LoginWindowGML.h login_window_gml)

set(SOURCES
    LoginWindow.cpp
    main.cpp
)

set(GENERATED_SOURCES
    LoginWindowGML.h
)

serenity_bin(LoginServer)
target_link_libraries(LoginServer PRIVATE LibCore LibGfx LibGUI LibMain)
