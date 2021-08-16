#
#
#

locale ja JP JP
tz Asia/Tokyo
new instance jcal
new gregorian gcal

test Normalize year 0 and -1 (Showa)
    use jcal
	clear all
	set date Showa 1 Jan 1
	check date Taisho 15 Jan 1

	clear all
	set date Showa 0 Jan 1
	check era Taisho
	check date 14 Jan 1

	clear all
	set date Showa -1 Jan 1
	check era Taisho
	check date 13 Jan 1

test Normalize year max and max+1 (Showa)
	clear all
	set date Showa 64 Aug 9
	check date Heisei 1 Aug 9

	clear all
	set date Showa 65 Aug 9
	check date Heisei 2 Aug 9

test Normalize year 0 and -1 (Heisei)
    use jcal
	clear all
	set date Heisei 1 Jan 1
	check date Showa 64 Jan 1

	clear all
	set date Heisei 0 Jan 1
	check date Showa 63 Jan 1

	clear all
	set date Heisei -1 Jan 1
	check date Showa 62 Jan 1

test Normalize year max and max+1 (Taisho)
	clear all
	set date Taisho 15 Dec 25
	check date Showa 1 Dec 25

	clear all
	set date Taisho 16 Dec 25
	check date Showa 2 Dec 25

test Normalize day of month 0 and -1 (Heisei)
    use jcal
	clear all
	set date Heisei 1 Jan 1
	check date Showa 64 Jan 1

	clear all
	set date Heisei 1 Jan 0
	check date Showa 63 Dec 31

	clear all
	set date Heisei 1 Jan -1
	check date Showa 63 Dec 30

test Normalize hour of day  -1:00 (Heisei)
	clear all
	set era Heisei
	set datetime 1 Jan 1 0 0 0
	check era Showa
	check datetime 64 Jan 1 0 0 0

	clear all
	set era Heisei
	set datetime 1 Jan 1 -1 0 0
	check era Showa
	check datetime 63 Dec 31 23 0 0

test Normalize hour of day 25:00 (Taisho)
	clear all
	set era Taisho
	set datetime 15 Dec 25 25 0 0
	check era Showa
	check datetime 1 Dec 26 1 0 0

test Normalize hour of day 25:00 (Showa)
	clear all
	set era Showa
	set datetime 64 Jan 7 25 0 0
	check era Heisei
	check datetime 1 Jan 8 1 0 0
