.. ===============LICENSE_START=======================================================
.. Acumos CC-BY-4.0
.. ===================================================================================
.. Copyright (C) 2017-2018 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
.. ===================================================================================
.. This Acumos documentation file is distributed by AT&T and Tech Mahindra
.. under the Creative Commons Attribution 4.0 International License (the "License");
.. you may not use this file except in compliance with the License.
.. You may obtain a copy of the License at
..
.. http://creativecommons.org/licenses/by/4.0
..
.. This file is distributed on an "AS IS" BASIS,
.. WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
.. See the License for the specific language governing permissions and
.. limitations under the License.
.. ===============LICENSE_END=========================================================

=====================================
Microservice Generation Release Notes
=====================================

These release notes cover the microservice generation project.

Version 5.2.3, 10 December 2021
-------------------------------
* Common Data Service client at version 3.1.1
* Retrieve docker URI and use it in the request towards external jenkins server when deploy=True `ACUMOS-4346 <https://jira.acumos.org/browse/ACUMOS-4346>`_

Version 5.1.1, 06 June 2021
---------------------------
* Common Data Service client at version 3.1.1
* acumos-r-client version is equal to 4.1 inside the MS it should be 4.3  `ACUMOS-4340 <https://jira.acumos.org/browse/ACUMOS-4340>`_
* not able to create microservice after on-boarding `ACUMOS-4339 <https://jira.acumos.org/browse/ACUMOS-4339>`_
* use of Json and Swagger UI with R models `ACUMOS-4326 <https://jira.acumos.org/browse/ACUMOS-4326>`_

Version 5.0.4, 14 April 2021
----------------------------
* Common Data Service client at version 3.1.1
* Create deployment backend `ACUMOS-4310 <https://jira.acumos.org/browse/ACUMOS-4310>`_

Version 4.7.1, 15 Sept 2020
---------------------------
* Common Data Service client at version 3.1.1
* Remove some install from packages.R `ACUMOS-4218 <https://jira.acumos.org/browse/ACUMOS-4218>`_
* Remove tensorFlow in requirements.txt `ACUMOS-4229 <https://jira.acumos.org/browse/ACUMOS-4229>`_
* Fixing Python Version Issue `ACUMOS-4260 <https://jira.acumos.org/browse/ACUMOS-4260>`_

Version 4.7.0, 08 May 2020
--------------------------
* Common Data Service client at version 3.1.1
* <IST>H2O- Java onboarding is failing with latest Java client 4.1.0 `ACUMOS-4106 <https://jira.acumos.org/browse/ACUMOS-4106>`_


Version 4.6.0, 27 April 2020
----------------------------
* Common Data Service client at version 3.1.1
* Use of Jenkins to trigger the micro-service Generation `ACUMOS-3841 <https://jira.acumos.org/browse/ACUMOS-3841>`_

Version 4.5.0, 3 April 2020
---------------------------
* Common Data Service client at version 3.1.1
* onboarding-common version 4.5.0
* TOSCAModelGeneratorClient version 2.0.8

Version 4.4.0, 16 March 2020
----------------------------
* Common Data Service client at version 3.1.1
* H2O Modelrunner nexus url externalization `ACUMOS-4057 <https://jira.acumos.org/browse/ACUMOS-4057>`_
* YML Changes –
	"modelrunnerUrl":{"h2o":"<NEXUS_REPO_URL>", "javaSpark":"<NEXUS_REPO_URL>"},
	"jenkins_config": {"solutionLocation":"/var/jenkins_home/ms"}
* YML Changes in volumes section:
   -  /var/acumos/ms:/var/jenkins_home/ms

Version 4.3.0, 25 Feb 2020
--------------------------
* Common Data Service client at version 3.1.1
* Code changes to trigger docker build using Jenkins `ACUMOS-3978 <https://jira.acumos.org/browse/ACUMOS-3978>`_
* Note: ACUMOS-3978 Disclaimer: In this release, Notifications will not be sent to CDS post the Docker Image is built and pushed to Nexus via Jenkins.
* YML Changes - "microService": {"createImageViaJenkins": "<Boolean>"},"jenkins_config": {"jenkinsUri": "<jenkinsUri>","jenkinsUsername":"<jenkinsUsername>","jenkinsPassword":"<jenkinsPassword>"}


Version 4.2.0, 31 Jan 2020
--------------------------
* Common Data Service client at version 3.1.1


Version 4.1.1, 21 Jan 2020
--------------------------
* Enrich message response with Docker URI `ACUMOS-3771 <https://jira.acumos.org/browse/ACUMOS-3771>`_


Version 3.8.1, 23 Dec 2019
--------------------------
* Common Data Service client at version 3.1.0
* Security Verification at version 1.2.2
* miss new logging library `ACUMOS-3848 <https://jira.acumos.org/browse/ACUMOS-3848>`

Version 3.8.0, 13 Dec 2019
--------------------------
* Common Data Service client at version 3.1.0
* Security Verification at version 1.2.1
* License-Manager-Client Library at version 1.4.3
* Not able to on-board R model, fails at dockerized step `ACUMOS-3761 <https://jira.acumos.org/browse/ACUMOS-3761>`_
* Micro services should download model runner from nexus and package into the model docker image for H2O and java models. `ACUMOS-3759 <https://jira.acumos.org/browse/ACUMOS-3759>`_

Version 3.6.0, 07 Nov 2019
--------------------------
* Common Data Service client at version 3.0.0
* YML changes - "security":{"verificationEnableFlag":"<Boolean>"}
* IST2 - Onboarding block calling SV with a flag `ACUMOS-3676 <https://jira.acumos.org/browse/ACUMOS-3676/>`_


Version 3.5.1, 25 Oct 2019
--------------------------
* Common Data Service client at version 3.0.0
* <IST>Java spark model failing with error through CLI. : `ACUMOS-3569 <https://jira.acumos.org/browse/ACUMOS-3569/>`_

Version 3.5.0, 16 Oct 2019
--------------------------
* Common Data Service client at version 3.0.0
* YML changes - "security":{"verificationApiUrl":"<securityverificationurl>"},"modelrunnerVersion": {"javaSpark": "<version>"}
* Common Services - Java Code upgrade to Java 11 or 12 : `ACUMOS-3327 <https://jira.acumos.org/browse/ACUMOS-3327/>`_
* Java spark model failing with error through CLI : `ACUMOS-3569 <https://jira.acumos.org/browse/ACUMOS-3569/>`_

Version 3.4.0, 3 Oct 2019
-------------------------
* Common Data Service client at version 3.0.0
* As a User , I want to see an Enhance on-boarding processes to allow choice of new model vs new revision : `ACUMOS-1216 <https://jira.acumos.org/browse/ACUMOS-1216/>`_

Version 3.2.0, 20 Sept 2019
---------------------------
* Common Data Service client at version 3.0.0


Version 3.1.0, 04 Sept 2019
---------------------------
* Common Data Service client at version 2.2.6
* create micro service for c/c+ model : ACUMOS-3108 <https://jira.acumos.org/browse/ACUMOS-3108/>_
* Additional R packages needed by the model are not added : ACUMOS-3367 <https://jira.acumos.org/browse/ACUMOS-3367/>_
* Errored model is getting onboarded successfully : ACUMOS-3022 <https://jira.acumos.org/browse/ACUMOS-3022/>_

Version 3.0.0, 23  Aug 2019
---------------------------
* Common Data Service client at version 2.2.6
* attach a license profile as JSON during on-boarding with Artifact Type LI : `ACUMOS-3171 <https://jira.acumos.org/browse/ACUMOS-3171/>`_


Version 2.14.1, 6 August 2019
-----------------------------
* Common Data Service client at version 2.2.4
* Log files generated in application should display logs as per the log standardization : `ACUMOS-3278 <https://jira.acumos.org/browse/ACUMOS-3278/>`_

Version 2.14.0, 19 July 2019
----------------------------
* Common Data Service client at version 2.2.4
* Log files generated in application should display logs as per the log standardization : `ACUMOS-2923 <https://jira.acumos.org/browse/ACUMOS-2923/>`_

Version 2.13.0, 24 June 2019
----------------------------
* Common Data Service client at version 2.2.4
* error displayed while runnin R model : `ACUMOS-1942 <https://jira.acumos.org/browse/ACUMOS-2974>`
* Microservice entry is remaining InProgress after completing onboarding process : `ACUMOS-3012 <https://jira.acumos.org/browse/ACUMOS-3012/>`_
* Async MSGen Notification logs not getting generated : `ACUMOS-3088 <https://jira.acumos.org/browse/ACUMOS-3088/>`_

Version 2.12.0, 31 May 2019
---------------------------
* Common Data Service client at version 2.2.4

Version 2.11.0, 14 May 2019
---------------------------
* Common Data Service client at version 2.2.2
* Logs are not displayed as per the standardization : `ACUMOS-2779 <https://jira.acumos.org/browse/ACUMOS-2779/>`_
* Add non configurable parameters to application.properties file : `ACUMOS-2872 <https://jira.acumos.org/browse/ACUMOS-2872/>`_
* microServiceAsyncFlag is available in application.properties with 'false' default value. Async microsrvices will also work if flag key-value is removed from yml file.

Version 2.10.0, 19 April 2019
-----------------------------
* Common Data Service client at version 2.2.1
* `ACUMOS-2326 <https://jira.acumos.org/browse/ACUMOS-2326/>`_
* `ACUMOS-1559 <https://jira.acumos.org/browse/ACUMOS-1559/>`_
* `ACUMOS-2771 <https://jira.acumos.org/browse/ACUMOS-2771/>`_

Version 2.9.0, 12 April 2019
----------------------------
* Common Data Service client at version 2.1.2
* `ACUMOS-2697 <https://jira.acumos.org/browse/ACUMOS-2697/>`_

Version 2.8.0, 29 March 2019
----------------------------
* Common Data Service client at version 2.1.2
* `ACUMOS-2625 <https://jira.acumos.org/browse/ACUMOS-2625/>`_
* `ACUMOS-2626 <https://jira.acumos.org/browse/ACUMOS-2626/>`_

Version 2.7.0, 18 March 2019
----------------------------
* Common Data Service client at version 2.1.2
* `ACUMOS-2620 <https://jira.acumos.org/browse/ACUMOS-2620/>`_

Version 2.6.0, 8 March 2019
---------------------------
* Common Data Service client at version 2.1.2
* `ACUMOS-2611 <https://jira.acumos.org/browse/ACUMOS-2611/>`_
* `ACUMOS-2488 <https://jira.acumos.org/browse/ACUMOS-2488/>`_


Version 2.5.0, 4 March 2019
---------------------------
* Common Data Service client at version 2.1.1
* `ACUMOS-2588 <https://jira.acumos.org/browse/ACUMOS-2588/>`_
* `ACUMOS-2402 <https://jira.acumos.org/browse/ACUMOS-2402/>`_
* `ACUMOS-2566 <https://jira.acumos.org/browse/ACUMOS-2566/>`_

Version 2.3.0, 14 February 2019
-------------------------------
* Pointing to CDS-2.0.7

Version 2.2.0, 31 January 2019
------------------------------
* `ACUMOS-2379 <https://jira.acumos.org/browse/ACUMOS-2379/>`_

Version 2.1.0, 11 January 2019
------------------------------
* `ACUMOS-1935 <https://jira.acumos.org/browse/ACUMOS-1935/>`_
* `ACUMOS-1609 <https://jira.acumos.org/browse/ACUMOS-1609/>`_

Version 2.0.0, 11 December 2018
-------------------------------
* `ACUMOS-1801 <https://jira.acumos.org/browse/ACUMOS-1801/>`_
* `ACUMOS-2039 <https://jira.acumos.org/browse/ACUMOS-2039/>`_

Version 1.8.2, 15 October 2018
------------------------------
* `ACUMOS-1898 <https://jira.acumos.org/browse/ACUMOS-1898/>`_

Version 1.8.1, 12 October 2018
------------------------------
* `ACUMOS-1896 <https://jira.acumos.org/browse/ACUMOS-1896/>`_

Version 1.8.0, 11 October 2018
------------------------------
* `ACUMOS-1879 <https://jira.acumos.org/browse/ACUMOS-1879/>`_
* `ACUMOS-1830 <https://jira.acumos.org/browse/ACUMOS-1830/>`_

Version 1.7.1, 05 October 2018
------------------------------
* `ACUMOS-1829 <https://jira.acumos.org/browse/ACUMOS-1829/>`_

Version 1.7.0, 04 October 2018
------------------------------
* Common Data Service client at version 1.18.2
* TOSCA model generator client at version 1.33.1
* There is a change in yml. rimage version changed from 1.0 to 1.0.0
* `ACUMOS-1736 <https://jira.acumos.org/browse/ACUMOS-1736/>`_
* `ACUMOS-1639 <https://jira.acumos.org/browse/ACUMOS-1639/>`_

Version 1.6.0, 28 September 2018
--------------------------------
* `ACUMOS-1771 <https://jira.acumos.org/browse/ACUMOS-1771/>`_
* `ACUMOS-1786 <https://jira.acumos.org/browse/ACUMOS-1786/>`_

Version 1.5.1, 24 September 2018
--------------------------------
* Pointing to CDS-1.18.1
* TOSCA pointing to 0.0.33
* `ACUMOS-622 <https://jira.acumos.org/browse/ACUMOS-622/>`_
* `ACUMOS-1754 <https://jira.acumos.org/browse/ACUMOS-1754/>`_

Version 1.5.0, 21 September 2018
--------------------------------
* TOSCA pointing to 0.0.33
* `ACUMOS-622 <https://jira.acumos.org/browse/ACUMOS-622/>`_
* `ACUMOS-1754 <https://jira.acumos.org/browse/ACUMOS-1754/>`_

Version 1.4.0, 14 September 2018
--------------------------------
* TOSCA pointing to 0.0.31
* `ACUMOS-1266 <https://jira.acumos.org/browse/ACUMOS-1266/>`_
* `ACUMOS-1638 <https://jira.acumos.org/browse/ACUMOS-1638/>`_
* `ACUMOS-1628 <https://jira.acumos.org/browse/ACUMOS-1628/>`_
* `ACUMOS-1583 <https://jira.acumos.org/browse/ACUMOS-1583/>`_
* `ACUMOS-1746 <https://jira.acumos.org/browse/ACUMOS-1746/>`_

Version 1.3.0, 7 September 2018
-------------------------------
* Pointing to CDS-1.18.0
* `ACUMOS-1628 <https://jira.acumos.org/browse/ACUMOS-1628/>`_

Version 1.2.0, 5 September 2018
-------------------------------
* Patch release to update nexus client version to 2.2.1
* `ACUMOS-1678 <https://jira.acumos.org/browse/ACUMOS-1678/>`_
* `ACUMOS-1629 <https://jira.acumos.org/browse/ACUMOS-1629/>`_

Version 1.1.0, 31 August 2018
-----------------------------
* `ACUMOS-1638 <https://jira.acumos.org/browse/ACUMOS-1638/>`_
* `ACUMOS-1628 <https://jira.acumos.org/browse/ACUMOS-1628/>`_
* `ACUMOS-1629 <https://jira.acumos.org/browse/ACUMOS-1629/>`_


Version 1.0.0, 20 August 2018
-----------------------------
* Pointing to CDS-1.17.1
* `ACUMOS-1070 <https://jira.acumos.org/browse/ACUMOS-1070/>`_
* `ACUMOS-1253 <https://jira.acumos.org/browse/ACUMOS-1253/>`_
* `ACUMOS-1252 <https://jira.acumos.org/browse/ACUMOS-1252/>`_
* `ACUMOS-1245 <https://jira.acumos.org/browse/ACUMOS-1245/>`_
