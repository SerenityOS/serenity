serenity_component(
    LoginServer
    REQUIRED
    TARGETS LoginServer
)

compile_gml(LoginWindow.gml LoginWindowGML.h login_window_gml)

set(SOURCES
    LoginWindowGML.h
    LoginWindow.cpp
    main.cpp
)

serenity_bin(LoginServer)
target_link_libraries(LoginServer LibGUI LibMain)
