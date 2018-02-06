### CyberStation

**CyberStation** is a prototype interactive desktop application for 
creating, editing and sending [STIX 2.0](https://oasis-open.github.io/cti-documentation/stix/intro) objects 
to a [TAXII-2.0](https://oasis-open.github.io/cti-documentation/taxii/intro.html) server.

[STIX-2.0](https://oasis-open.github.io/cti-documentation/resources#stix-20-specification) 
stands for "Structured Threat Information Expression", it is an 
open JSON standard for representing cyber threat intelligence (CTI) objects.

[TAXII-2.0](https://oasis-open.github.io/cti-documentation/taxii/intro.html) 
stands for "Trusted Automated Exchange of Intelligence Information", it is an application  
protocol for exchanging CTI over HTTPS. ​TAXII defines a RESTful API 
(a set of services and message exchanges) and a set of requirements for TAXII Clients 
and Servers.

**CyberStation** is a prototype application to assist in manually creating and editing STIX-2.0 objects through a 
simple user interface, and sending those to a chosen TAXII-2 server for sharing cyber threat 
intelligence amongst the participating clients.


### Documentation

**CyberStation** user interface consists of three main tabs; the *Taxii servers* tab, where Taxii servers can be setup and selected, 
the *Stix objects* tab, where STIX-2 objects can be created and edited, and the *Graph view* tab, where the STIX objects 
can be displayed as a graph.
 
The work flow typically consists of selecting a server together with a collection endpoint to connect to.
 Then creating a bundle of STIX objects using the UI and sending that bundle to the selected server endpoint. 

**CyberStation** is written in Scala and runs on the Java Virtual Machine (JVM). 
It uses [ScalaFX](http://www.scalafx.org/) for the UI and 
[cti-stix-visualization](https://github.com/oasis-open/cti-stix-visualization) to display the graph view.

The following describes the different elements of **CyberStation**. 

#### Taxii servers

The UI allows for adding new servers by clicking the 
![+](/images/add.png?raw=true "Add") button or deleting previously defined servers 
by selecting the desired server and clicking on the ![-](/images/delete.png?raw=true "Delete") button.

A test server from [freetaxii-server](https://github.com/freetaxii/freetaxii-server)
is predefined for selection. Selecting a server (by clicking on it) brings a list of its api roots. 
One api root and associated collection endpoint should be selected to be able to send STIX objects to the server. 

To change the server URL or the user name or the password, double click on it and a dialog will be popup.

![Taxii servers](/images/taxiiservers.png?raw=true) 

#### Stix objects

This tab is where STIX objects can be created and edited. All STIX objects 
must belong to a bundle. As such, a bundle object must first be created from the **Bundle** tab before 
any STIX objects types can be added to it.

The UI allows for adding new bundles by clicking the ![+](/images/add.png?raw=true "Add") button or deleting previously defined bundles 
by selecting the desired bundle and clicking on the ![-](/images/delete.png?raw=true "Delete") button. In addition a **SEND TO SERVER** button 
allows for sending the bundle to the selected server. This button is disabled if no server and collection 
endpoints are selected or the selected collection does not allow writing to.

Other tabs on the STIX page allow for creating the different STIX types objects.
Currently only **malware, indicator and relationship** are implemented.

If a server/api root/collection has not yet been selected, a bundle of STIX objects can still be created. Such 
bundle can be saved to local storage, can then be further edited as desired and eventually sent to a TAXII-2 server.   

![Stix objects](/images/stixobjects.png?raw=true) 

![Stix objects](/images/indicator.png?raw=true) 

#### Graph view

*Graph view* is an experimental feature to display the created bundles of objects or those of 
the Taxii server.


![Graph view](/images/graphview.png?raw=true) 

#### Tools

In addition to the main viewing areas, various tools from the *Tools* menu are available to convert files of STIX objects into formats 
such as; MongoDB, Neo4jDB, Gelphi and GraphML. 
Also from the *Tools* menu, a user can send a file of STIX objects directly to the selected Taxii server.

Similarly, from the *File* menu, the created (or loaded) bundles of STIX objects can be saved into various formats.
Bundle of STIX objects can be loaded into *CyberStation* using the *File->Open* menu item.

##### Databases

*MongoDB* is used to store the current state of *CyberStation*, that is, the current list of bundles being edited, 
as well as the log all transactions to the Taxii servers. In addition, *MongoDB* can be used in the  
convertion of STIX files and the saving of bundles into a *MongoDB* database. 
To enable this, *MongoDB* must be installed and running. 


#### Installation and running

Install [SBT](https://www.scala-sbt.org/)

Download this repository, and adjust the *application.conf* file if need be.

    sbt run

### Requirements

Must have java 1.8.0_152 installed.

To use *MongoDB* for storage, *MongoDB* must be installed and running; e.g. type: mongod in a terminal 
(see also the settings in the *application.conf* file)

### Status

work in progress, unstable


### License 

Apache License, Version 2.0.


### Copyright 

Copyright 2018 R. Wathelet, All rights reserved.




