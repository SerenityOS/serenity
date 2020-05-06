set(SOURCES
    main.cpp
    RemoteObject.cpp
    RemoteObjectGraphModel.cpp
    RemoteObjectPropertyModel.cpp
    RemoteProcess.cpp
)

serenity_bin(Inspector)
target_link_libraries(Inspector LibGUI)
