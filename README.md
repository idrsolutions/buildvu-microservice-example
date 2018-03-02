# BuildVu Microservice Example #

IDRSolutions' BuildVu Microservice Example is an open source project that allows you to run BuildVu as an online service, which can be interacted with via the REST API.

**Please note that BuildVu is paid-for, commercial software - however you can use the trial version for free. Just rename the trial .jar to "buildvu.jar".**

-----

# How do I get set up? #

### What you'll need: ###

* [BuildVu](https://www.idrsolutions.com/buildvu/download/) - this provides all the conversion functionality.
* [Maven](https://maven.apache.org/download.cgi) - used to build the .war file.

### Build: ###

Clone a copy of the buildvu-microservice-example git repo:

```
git clone git://github.com/idrsolutions/buildvu-microservice-example
```

Add the BuildVu jar to the project by copying it into the /lib directory.

Open up a terminal / command prompt window in the base directory of the buildvu-microservice-example project, and build the .war file by running the command:
```
mvn compile war:war
```

This will generate the buildvu.war file inside the /target directory.

### Deployment: ###

Refer back to our application server tutorials for instructions on deployment:
[GlassFish](https://support.idrsolutions.com/hc/en-us/articles/360001058611) / (More coming soon...)

Support for cloud platforms coming soon... 

-----

### Usage: ###

You can interact with the BuildVu Microservice Example using the REST API.

A complete list of raw requests that can be used can be found [here](/API.md).

For specific languages, see our tutorials:
[NodeJS](https://support.idrsolutions.com/hc/en-us/articles/360001084471) / (More coming soon...)

-----

# Who do I talk to? #

Found a bug, or have a suggestion / improvement? Let us know through the Issues page.

Got questions? You can contact us [here](https://idrsolutions.zendesk.com/hc/en-us/requests/new).

-----

Copyright 2018 IDRsolutions

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.