#!/bin/bash
# Bulk API Script

DIR="/code/kafka-consumer-example/output/accountpe/accountPE.csv"
TARGETDIR="/code/kafka-consumer-example/bulkapiinput/"
ARCHIEVE="/code/kafka-consumer-example/bulkapiarchieve/"
JAR="/code/JAP_SALESFORCE/BulkAPI/"
HEADER="Account_Name__c,Account_Number__c,Account_Status__c,Account_Type__c,Account_Value__c,Contact__c,Date_Updated__c,Institution__c,Jemstep_Account_Id__c,Jemstep_Id__c"
date1=$(date +"%Y_%m_%d_%H%M%S%N")
FILENAME="accountPE.csv"
while true
do

	if [ -f $DIR ]
	then
		mv $DIR $TARGETDIR
		cd $JAR
		echo "Bulk Api Running"
  		sed -i "1i $HEADER" $TARGETDIR/${FILENAME}
		java -cp target/apiexample-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.jemstep.bulkapi.v2.client.UploadExample Account_PE__E $TARGETDIR/${FILENAME}
		if [ $? -eq 0 ]
		then
			echo "archieving"
			mv $TARGETDIR/${FILENAME} $ARCHIEVE/${date1}_${FILENAME}
		fi
	else
		echo "skipping"
	fi
	sleep 3 
done

