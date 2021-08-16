README:

This directory contains a program to read a DTD, and produce a compressed
representation of it.  It's intended that this program be run at build
time, and the resultant .bdtd binary DTD file be read at program startup.


			    .dtdb FILE FORMAT

file ::= version_no:int num_names:short name[]:string num_entities entity[]
	 num_elements element[]

entity ::= name_id:short type:byte data:string

element ::= name_id:short type:byte 
	    flags:byte (&0x01 = omit start, &0x02 = omit end)
	    content_model
	    num_exclusions:byte name_id[]
	    num_inclusions:byte name_id[]
	    num_attributes:byte attribute[]

attribute ::= name_id:short type:byte modifier:byte 
	      value:name_id (or -1 for null)
	      num_values:short name_id[]

content_model ::= content_c | content_e | content_null

content_null ::= flag:byte=0

content_c ::= flag:byte=1 type:int content:content_model next:content_model

content_e ::= flag:byte=2 type:int element_name_id next:content_model

string ::= modified UTF-8 encoding of a string

See the java.io.InputStream class description for the specification of modified
UTF-8.
