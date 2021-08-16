#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# -gcold 0
#
#    MC         MU       CCSC      CCSU         OC           OU       YGC    FGC    FGCT     CGC    CGCT       GCT   
#    7680.0     7202.3     768.0     638.6       2048.0          0.0      4     0     0.000     0     0.000     0.240
#
# -J-XX:+UseParallelGC -gcold 0
#
#    MC         MU       CCSC      CCSU         OC           OU       YGC    FGC    FGCT     CGC    CGCT       GCT   
#    8320.0     7759.4     896.0     718.3       5632.0        904.1      5     0     0.000     -         -     0.175

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^    MC         MU       CCSC      CCSU         OC           OU       YGC    FGC    FGCT     CGC    CGCT       GCT   $/	{
	    headerlines++;
	}

/^[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*(-|[0-9]+)[ ]*(-|[0-9]+\.[0-9]+)[ ]*[0-9]+\.[0-9]+$/	{
	    datalines++;
	}

	{ totallines++; print $0 }

END	{
	    if ((headerlines == 1) && (datalines == 1)) {
	        exit 0
	    }
	    else {
	        exit 1
	    }
	}
