set(SOURCES
    Format.cpp
	ISOCalendar.cpp
    LocalDateTime.cpp
    ZonedDateTime.cpp
)

serenity_lib(LibDateTime datetime)
target_link_libraries(LibDateTime PRIVATE LibCore LibTimeZone)
