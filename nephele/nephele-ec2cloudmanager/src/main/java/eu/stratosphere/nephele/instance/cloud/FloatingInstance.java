/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.nephele.instance.cloud;

import java.util.LinkedList;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

import eu.stratosphere.nephele.instance.InstanceConnectionInfo;
import eu.stratosphere.nephele.instance.InstanceType;

/**
 * A FloatingInstance is an instance in the cloud allocated for a user. It is idle and carries out no task.
 * However, the owner of a floating instance can employ it for executing new jobs until it is terminated.
 */
public class FloatingInstance {
	
	/** The user pays fee for his instances every time unit. */
	private static final long TIMEUNIT = 60 * 60 * 1000; // 1 hour in ms.

	/** Timelimit to full next hour when instance is kicked. */
	private static final long TIMETHRESHOLD = 2 * 60 * 1000; // 2 mins in ms.
	
	/** The instance ID. */
	private final String instanceID;

	/** The information required to connect to the instance's task manager. */
	private final InstanceConnectionInfo instanceConnectionInfo;

	/** The time the instance was launched (in this case, the VM). */
	private final long launchTime;
	
	/** The AWS Access Key to access this machine */
	private String awsAccessKey;
	
	/** The AWS Secret Key to access this machine */
	private String awsSecretKey;
	
	/** The instance Type*/
	private InstanceType type;


	/** The last received heart beat. */
	private long lastHeartBeat;

	/**
	 * Creates a new floating instance.
	 * 
	 * @param instanceID
	 *        the instance ID assigned by the cloud management system
	 * @param instanceConnectionInfo
	 *        the information required to connect to the instance's task manager
	 * @param launchTime
	 *        the time the instance was allocated
	 * @param type
	 * 		  The type of this instance.
	 * @param awsAccessKey
	 * 		  The AWS Access Key to access this machine
	 * @param awsSecretKey 
	 * 	      The AWS Secret Key to access this machine
	 */
	public FloatingInstance(String instanceID, InstanceConnectionInfo instanceConnectionInfo, long launchTime, InstanceType type, String awsAccessKey, String awsSecretKey) {
		this.instanceID = instanceID;
		this.instanceConnectionInfo = instanceConnectionInfo;
		this.launchTime = launchTime;
		this.lastHeartBeat = System.currentTimeMillis();
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		this.type = type;
	}
	
	/**
	 * Checks, if this floating Instance is accessible via the provided credentials.
	 * @param awsAccessKey
	 * @param awsSecretKey
	 * @return
	 */
	public boolean isFromThisOwner(String awsAccessKey, String awsSecretKey){
		if(this.awsAccessKey.equals(awsAccessKey) && this.awsSecretKey.equals(awsSecretKey)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Returns the type of this floating instance.
	 * @return
	 */
	public InstanceType getType(){
		return this.type;
	}

	/***
	 * Returns the instance ID.
	 * 
	 * @return the instance ID
	 */
	public String getInstanceID() {
		return this.instanceID;
	}

	/**
	 * Returns the time of last received heart beat.
	 * 
	 * @return the time of last received heart beat
	 */
	public long getLastReceivedHeartBeat() {
		return this.lastHeartBeat;
	}

	/**
	 * Updates the time of last received heart beat to the current system time.
	 */
	public void updateLastReceivedHeartBeat() {
		this.lastHeartBeat = System.currentTimeMillis();
	}

	/**
	 * Returns the information required to connect to the instance's task manager.
	 * 
	 * @return the information required to connect to the instance's task manager
	 */
	public InstanceConnectionInfo getInstanceConnectionInfo() {
		return this.instanceConnectionInfo;
	}

	/**
	 * Returns the time the instance was launched.
	 * 
	 * @return the time the instance was allocated
	 */
	public long getLaunchTime() {
		return this.launchTime;
	}
	
	/**
	 * Returns this instance as Cloud Instance.
	 * @return
	 */
	public CloudInstance asCloudInstance(){
		return new CloudInstance(this.instanceID, this.type, this.getInstanceConnectionInfo(), this.launchTime, null , null, null, this.awsAccessKey, this.awsSecretKey);
	}
	
	
	/**
	 * This method checks, if this floating instance has reached the end of its lifecycle and - if so - terminates itself.
	 */
	public boolean checkIfLifeCycleEnded(){

		long currentTime = System.currentTimeMillis();
		long msremaining = TIMEUNIT - ((currentTime - this.launchTime) % TIMEUNIT);
		
		if (msremaining < TIMETHRESHOLD) {
			// Destroy this instance.
			AmazonEC2Client client = EC2ClientFactory.getEC2Client(this.awsAccessKey, this.awsSecretKey);
			TerminateInstancesRequest tr = new TerminateInstancesRequest();
			LinkedList<String> instanceIDlist = new LinkedList<String>();
			instanceIDlist.add(this.instanceID);
			tr.setInstanceIds(instanceIDlist);
			client.terminateInstances(tr);
			return true;
		}else{
			return false;
		}
	}


}
