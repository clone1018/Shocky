Shocky (PircBotX 1.7)
======

##Building Shocky
1. ```git clone``` the project
2. Open Eclipse; go to File -> Import... -> Existing project and enter the directory you cloned to.
3. To build, right-click on packjar.xml, pick ```Actions```, and select the "run antfile" option.
4. Run shocky.jar from a terminal and go through the first-run setup.
5. Make yourself a bot controller and join a channel (```join #channel```, ```controller add MyNick```).

##SQL
Shocky requires a SQL server to be set up for certain modules to work.
The username, password, and database name will be prompted in the setup.  
If you do not wish to set up a SQL server, reply ```n``` to the prompt for whether you want to use SQL. (This will disable the Factoid and Rollback modules.) ( **TODO**: implement this )


Copyright Axxim, LLC 2012