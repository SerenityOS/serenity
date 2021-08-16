#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# -gcoldcapacity 0
#
#   OGCMN        OGCMX         OGC           OC       YGC    FGC    FGCT     CGC    CGCT       GCT   
#         0.0    4194304.0       2048.0       2048.0      5     0     0.000     0     0.000     0.407
#
# -J-XX:+UseParallelGC -gcoldcpacity 0
#
#   OGCMN        OGCMX         OGC           OC       YGC    FGC    FGCT     CGC    CGCT       GCT   
#       512.0    2796544.0       5632.0       5632.0      5     0     0.000     -         -     0.180

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^   OGCMN        OGCMX         OGC           OC       YGC    FGC    FGCT     CGC    CGCT       GCT   $/	{
	    headerlines++;
	}

/^[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*(-|[0-9]+)[ ]*(-|[0-9]+\.[0-9]+)[ ]*[0-9]+\.[0-9]+$/	{
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
