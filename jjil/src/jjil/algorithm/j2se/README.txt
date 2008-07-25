jjil.algorithm.j2se contains classes that use features not supported under
j2me/cldc1.0, particularly floating point. When the classes have the same names
as the classes in jjil.algorithm they  can generally be used as a substitute
for the corresponding jjil.algorithm class, though they may be initialized differently
and may give different (probably more accurate) results. Sometimes the return type 
is different -- for example jjil.algorithm.j2se.Gray8Statistics returns floating 
point values for mean and variance instead of the scale integer values returned 
by jjil.algorithm.Gray8Statistics.