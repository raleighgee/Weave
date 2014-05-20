1. Ant Builder runs twice creating a folder pathvariable...
Reason:
Happens due to Eclipse builder running first and the ant builder running again externally
Solution: 
a.Enable build befor launch setting in external tools configurations from the build.xml RunAs
b. Make sure the Weave_DOCROOt is set at the project level or workspace level
c. Build the project from the clipse top menu instead of right click on build.xml and RunAs.



