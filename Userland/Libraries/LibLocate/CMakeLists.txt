set(SOURCES
    Crawler.cpp
    LocateDB.cpp
)

serenity_lib(LibLocate locate)
target_link_libraries(LibLocate LibC LibCore)
