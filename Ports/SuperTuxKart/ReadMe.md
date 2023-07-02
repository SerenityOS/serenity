# Non-obvious OOM on default VM settings

If the game seems to initialize window fine, starts loading some of the
characters, but then crashes with a weird stack trace pointing somewhere to
LibGL, make sure that you have enough memory. This crash is verified not to
happen on 4GB of RAM.
