#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# -gcmetacapacity 0
#
#   MCMN       MCMX        MC       CCSMN     CCSMX     CCSC     YGC    FGC    FGCT     CGC    CGCT       GCT   
#       0.0  1056768.0     8832.0       0.0 1048576.0     896.0      5     0     0.000     0     0.000     0.245
#
# -J-XX:+UseParallelGC -gcmetacapacity 0
#
#   MCMN       MCMX        MC       CCSMN     CCSMX     CCSC     YGC    FGC    FGCT     CGC    CGCT       GCT   
#       0.0  1056768.0     8064.0       0.0 1048576.0     896.0      4     0     0.000     -         -     0.113

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^   MCMN       MCMX        MC       CCSMN     CCSMX     CCSC     YGC    FGC    FGCT     CGC    CGCT       GCT   $/	{
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
