#
#
#

locale ja JP JP

# Use jcal in non-lenient mode for all test cases.
set non-lenient
new instance jcal

use jcal
clear all

test Invalid BeforeMeiji dates
    set date BeforeMeiji 1868 Jan 1
    get millis
    exception IllegalArgumentException
    set date BeforeMeiji 1868 Jan 32
    get millis
    exception IllegalArgumentException
    set date BeforeMeiji 2005 Mar 9
    get millis
    exception IllegalArgumentException

test Invalid Meiji dates
    set date Meiji -1 Jan 1
    get millis
    exception IllegalArgumentException
    set date Meiji 1 Feb 30
    get millis
    exception IllegalArgumentException
    set date Meiji 45 Jul 30
    get millis
    exception IllegalArgumentException
    set date Meiji 46 Jan 1
    get millis
    exception IllegalArgumentException

test Invalid Taisho dates
    set date Taisho -1 Jan 1
    get millis
    exception IllegalArgumentException
    set date Taisho 1 Jan 1
    get millis
    exception IllegalArgumentException
    set date Taisho 1 Apr 1
    get millis
    exception IllegalArgumentException
    set date Taisho 15 Dec 30
    get millis
    exception IllegalArgumentException
    set date Taisho 15 Feb 29
    get millis
    exception IllegalArgumentException

test Invalid Showa dates
    set date Showa -11 Jan 1
    get millis
    exception IllegalArgumentException
    set date Showa 1 Jan 1
    get millis
    exception IllegalArgumentException
    set date Showa 1 Jun 1
    get millis
    exception IllegalArgumentException
    set date Showa 1 Jul 29
    get millis
    exception IllegalArgumentException
    set date Showa 64 Jan 8
    get millis
    exception IllegalArgumentException
    set date Showa 64 Dec 8
    get millis
    exception IllegalArgumentException
    set date Showa 65 Jan 1
    get millis
    exception IllegalArgumentException

test Invalid Heisei dates
    clear all
    set date Heisei -1 Jan 1
    get millis
    exception IllegalArgumentException
    set date Heisei 1 Jan 1
    get millis
    exception IllegalArgumentException
    set date Heisei 1 Jan 7
    get millis
    exception IllegalArgumentException
    set date Heisei 1 Jan 8
    get max year
    eval $result + 1
    set date Heisei $result Jan 1
    get millis
    exception IllegalArgumentException

test Invalid ERA
    get max era
    eval $result + 1
    set era $result # max era + 1
    get era
    exception IllegalArgumentException
    set era 100
    get era
    exception IllegalArgumentException
    set era -100
    get era
    exception IllegalArgumentException

test Invalid HOUR_OF_DAY
    clear all
    set date Heisei 17 Mar 14
    set hour_of_day 25
    get millis
    exception IllegalArgumentException
    set hour_of_day -9
    get millis
    exception IllegalArgumentException

test Invalid AMPM
    clear all
    set date Heisei 17 Mar 14
    set ampm -1
    set hour 1
    get millis
    exception IllegalArgumentException
    set ampm 5
    set hour 1
    get millis
    exception IllegalArgumentException

test Invalid HOUR
    clear all
    set date Heisei 17 Mar 14
    set ampm AM
    set hour 13
    get millis
    exception IllegalArgumentException
    set ampm PM
    set hour -1
    get millis
    exception IllegalArgumentException

test Invalid MINUTE
    clear all
    set date Heisei 17 Mar 14
    set minute 61
    get millis
    exception IllegalArgumentException
    set minute -2
    get millis
    exception IllegalArgumentException

test Invalid SECOND
    clear all
    set date Heisei 17 Mar 14
    set second 61
    get millis
    exception IllegalArgumentException
    set second -2
    get millis
    exception IllegalArgumentException

test Invalid MILLISECOND
    clear all
    set date Heisei 17 Mar 14
    set millisecond 1000
    get millis
    exception IllegalArgumentException
    set millisecond -2
    get millis
    exception IllegalArgumentException

test Invalid ZONE_OFFSET
    clear all
    set date Heisei 17 Mar 14
    set zone_offset -360000000
    get millis
    exception IllegalArgumentException
    set zone_offset -360000000
    get year
    exception IllegalArgumentException
    set zone_offset 360000000
    get millis
    exception IllegalArgumentException
    set zone_offset 360000000
    get year
    exception IllegalArgumentException

test Invalid DST_OFFSET
    clear all
    set date Heisei 17 Mar 14
    set dst_offset -360000000
    get millis
    exception IllegalArgumentException
    set dst_offset -360000000
    get year
    exception IllegalArgumentException
    set dst_offset 360000000
    get millis
    exception IllegalArgumentException
    set dst_offset 360000000
    get year
    exception IllegalArgumentException
