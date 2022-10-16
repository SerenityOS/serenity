## Name

fortunes - Fortune cookies

## Description

`/res/fortunes.json` is a simple JSON file, used by [`fortune`(1)](help://man/1/fortune), composed
of an array of objects of the form:
- `quote`: The fortune.
- `author`: The person who said the quote.
- `url`: Source of the quote.
- `utc_time`: The date at which the fortune was said, as a UNIX timestamp.
- `context`: A small sentence to help understanding the fortune.

Fortunes are usually said by SerenityOS developers on the Discord server, private channels, or (previously) IRC.
Buggie Bot helps adding new ones.

## Files

* /res/fortunes.json

## See also

* [`fortune`(1)](help://man/1/fortune)
* [Buggie Bot](https://github.com/SerenityOS/discord-bot)

