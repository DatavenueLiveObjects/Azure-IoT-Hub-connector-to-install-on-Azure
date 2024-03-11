## Table of contents
* [General info](#general-info)
* [Technologies](#technologies)
* [Requirements](#requirements)
* [Getting the installation package](#getting-the-installation-package)
* [Installation](#installation)
* [Configuration](#configuration)
  * [Connector](#connector)
  * [Logging](#logging)
* [Launching](#launching)
* [Deploy on Azure Linux virtual machine](#deploy-on-azure-linux-virtual-machine)

## General info
![Architecture](./assets/architecture2.png)

This repository contains everything you need to create 'Live Objects to Azure IoT Hub' connector. This project is intended for Live Objects users wishing to explore integration patterns with Azure and for organizations already running business logic on Azure planning to work on events from IoT devices sourced via Live Objects.

Three main features are:
* **devices synchronization** - every device registered in Live Objects will appear  in IoT Hub and every device deleted from Live Objects will be also deleted from IoT Hub 
* **messages synchronization** - every message which will be send from device to Live Objects will appear in IoT Hub
* **commands synchronization** - every command from IoT Hub will be sent to the devices via Live Objects API

One connector can handle many Live Objects accounts and Iot Hubs. 

It can be only one instance of connector per Live Objects account. Two or more instances connected to the same Live Objects account will cause problems.

The software is an open source toolbox which has to be integrated into an end to end solution. The ordering of messages is not guaranteed to be preserved; the application uses thread pools to run its MQTT and IoT Hub adapters which may cause some messages to arrive in IoT Hub out of order in which they were kept within Live Objects’ MQTT queue.
Live Objects platform supports load balancing between multiple MQTT subscribers.

## Technologies
* Java 21
* Spring Boot 3.2.3
* Microsoft Iot Hub Java Service SDK 2.1.7
* Microsoft IoT Hub Java Device Client 2.4.1
* Microsoft Application Insights Java SDK Spring Boot Starter 2.6.4
* Azure Metrics Spring Boot Starter 2.3.5

## Requirements
In order to run the connector you need to have: 
* **Live Objects account** 
* **Azure account with an IoT Hub created (one or many)** (creation process is described in official [documentation](https://docs.microsoft.com/en-us/azure/iot-hub/iot-hub-create-through-portal) 
* **Azure CLI installed** (installation process is described in official [documentation](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest))
* **Application Insights resource created (per each Iot Hub)** (creation process is described in official [documentation](https://docs.microsoft.com/pl-pl/azure/azure-monitor/app/create-new-resource)) 
* **App Service plan created (per each Iot Hub)** (creation process is described in official [documentation](https://docs.microsoft.com/en-us/azure/app-service/app-service-plan-manage))
* **Development tools (only when building the package)**
* **Java SE Development Kit 21**
* **Apache Maven**

## Getting the installation package

The installation package can be acquired in one of two ways:

### Release

It can be downloaded from https://github.com/DatavenueLiveObjects/Azure-IoT-Hub-connector-to-install-on-Azure/releases

### Building
```
mvn clean package -Prelease
```
After running this command, the file  `lo2iot-[VERSION].zip` will be created in the target directory. This file should be placed where the connector will be started, and then unpacked. 

## Installation

The file downloaded/created above should be placed where the connector will be started, and then unpacked. You can deploy this connector wherever you want (local server, cloud provider etc.).

After unpacking the archive, you should get a structure similar to this:
```
bin/
conf/
data/
lib/
```

## Configuration

### Connector
All configuration can be found in **application.yaml** file located in src/main/resources

```
1    tenant-list:
2      - 
3        live-objects-properties:
4          hostname: liveobjects.orange-business.com
5          api-key: YOUR_API_KEY
6          username: application
7          connection-timeout: 30000
8          qos: 1
9          keep-alive-interval-seconds: 30
10         page-size: 20
11         device-synchronization: true
12         device-synchronization-interval: 86400 # in seconds - 24h
13         mqtt-persistence-dir: ${basedir:.}/temp/
14    
15      azure-iot-hub-list:
16        -
17          iot-connection-string: YOUR_IOT_CONNECTION_STRING
18          iot-host-name: YOUR_IOT_HOST_NAME
19          device-registration-thread-pool-size: 10
20          device-registration-period: 200 # in milliseconds - 200ms
21          device-reestablish-session-delay: 10000 # in milliseconds - 10s
22          device-client-connection-timeout: 5000
23          message-expiry-time: 60000 # in milliseconds - 60s
24          message-send-max-attempts: 10
25          message-resend-delay: 10000 # in milliseconds - 10s
26          tagPlatformKey: platform
27          tagPlatformValue: LiveObjectsGroupIoT1
28        
29          lo-messages-topic: MESSAGES_TOPIC
30          lo-devices-topic: DEVICES_TOPIC
31          lo-devices-group: DEVICES_GROUP
32    
33        -
34          iot-connection-string: YOUR_IOT_CONNECTION_STRING
35          iot-host-name: YOUR_IOT_HOST_NAME
36          device-registration-thread-pool-size: 10
37          device-registration-period: 200 # in milliseconds - 200ms
38          device-reestablish-session-delay: 10000 # in milliseconds - 10s
39          device-client-connection-timeout: 5000
40          message-expiry-time: 60000 # in milliseconds - 60s
41          message-send-max-attempts: 10
42          message-resend-delay: 10000 # in milliseconds - 10s
43          tagPlatformKey: platform
44          tagPlatformValue: LiveObjectsGroupIoT1
45    
46          lo-messages-topic: MESSAGES_TOPIC
47          lo-devices-topic: DEVICES_TOPIC
48          lo-devices-group: DEVICES_GROUP
49    
50     azure:
51       application-insights:
52         enabled: true
53         instrumentation-key: YOUR_INSTMENTATION_KEY
54      
...
```

#### hostname
Live Objects hostname

#### api-key
Live Objects API key with at least DEVICE_R, DEVICE_W and BUS_R roles

Login to Live Objects Web Portal an go to **Administration** -> **API keys**

![Api Keys 1](./assets/api_key_1.png)

Click **Add** button and fill fields.

![Api Keys 2](./assets/api_key_2_.png)

To  validate  the  creation  of  the  key,  click  on  the  **Create**  button.  Your  key  is  generated  in  the form of an alphanumeric sequence and aQR code.

#### username
Live Objects mqtt username (should be set to **application**)

#### connection-timeout
This value, measured in seconds, defines the maximum time interval the client will wait for the network connection to the MQTT server to be established

#### qos
Message QoS

#### keep-alive-interval-seconds
This value, measured in seconds, defines the maximum time interval between messages sent or received. It enables the client to detect if the server is no longer available, without having to wait for the TCP/IP timeout. The client will ensure that at least one message travels across the network within each keep alive period.  In the absence of a data-related message during the time period, the client sends a very small "ping" message, which the server will acknowledge. A value of 0 disables keepalive processing in the client.

#### page-size
Maximum number of devices in single response. Max 1000

#### device-synchronization-interval
Controls the interval (in seconds) at which device synchronization process starts.

#### iot-connection-string
This parameter value should contain IoT Hub host name, shared access key name and shared access key. It can be found in the Shared access policies tab:

![IoT Connection 1](./assets/iot_connection_string.png)

#### iot-host-name
The `iot-host-name` can be found in the details of IoT Hub:

![IoT Hub 1](./assets/iot_hub_1.png)

#### device-registration-thread-pool-size 
How many threads will be used in devices registration process

#### device-client-connection-timeout
The length of time, in milliseconds, that any given operation will expire in. These operations include reconnecting upon a connection drop and sending a message.

#### tagPlatformKey
All devices created in IoT Hub have a tag with given name

#### tagPlatformValue
All devices created in IoT Hub have a tag with given value

#### lo-devices-group
Name of device group in Live Objects
To create group you need to login to Orange Web Portal an go to **Devices**

![Groups 1](./assets/groups_1.png)

Click **Create a group of device** in the left menu, named it and confirm

![Groups 2](./assets/groups_2.png)

#### lo-messages-topic
Name of the FIFO queue for messages from devices belong to *lo-devices-group*.
To create such queue you need to login to Orange Web Portal an go to **Data -> FIFO** 

![FIFO 1](./assets/fifo_1.png)

Click **Add** button and fill fields

![FIFO 2](./assets/fifo_2.png)

And click **Register** button

Later you need to create routing. Go to **Data -> Routing** and click **Add a routing rule**

![Routing 1](./assets/routing_1.png)

Choose **Forward to your servers in MQTTs, through FIFO queues** by clicking **+ FIFO** button and select your fifo for storing messages. Click **Next** button

![Routing 1](./assets/routing_m_2.png)

Choose a message type of **Data message**, in **Filters** section select **A filtered selection of messages**, select **Group Criteria** and choose your device group. Click **Next** button

![Routing 1](./assets/routing_m_3.png)

Give a routing name and click **Complete** button

![Routing 1](./assets/routing_m_4.png)

#### lo-devices-topic
Name of the FIFO queue for the device created and deleted events   
To create such queue you need to login to Orange Web Portal an go to **Data -> FIFO** 

![FIFO 1](./assets/fifo_1.png)

Click **Add** button and fill fields

![FIFO 2](./assets/fifo_d_2.png)

And click **Register** button

Later you need to create routing. Go to **Data -> Routing** and click **Add a routing rule**

![Routing 1](./assets/routing_1.png)

Choose **Forward to your servers in MQTTs, through FIFO queues** by clicking **+ FIFO** button and select your fifo for storing devices events. Click **Next** button

![Routing 1](./assets/routing_d_2.png)

Choose a message type of **Device created event**, in **Filters** section select **A filtered selection of messages**, select **Group Criteria** and choose your device group. Click **Next** button

![Routing 1](./assets/routing_d_3.png)

Give a routing name and click **Complete** button

![Routing 1](./assets/routing_d_4.png)

Repeat last step and create new routing for **Device deleted event** (choose the same fifo as for Device created event)

![Routing 1](./assets/routing_d_5.png)

#### instrumentation-key
The value of this parameter can be found in the details of Application Insights:

![IoT Connection 1](./assets/instrumentation_key_1.png)


### Logging
Logging configuration can be found in **logback.xml** file located in src/main/resources. You can find more information about how to configure your logs [here](http://logback.qos.ch/manual/configuration.html)

## Launching
In order to run the connector, use the `app.sh` file for linux or `app.bat` if you are using windows. These files are located in the `bin/` directory.

## Deploy on Azure Linux virtual machine
You can deploy this connector wherever you want (local server, cloud provider etc.). As an example we provide instruction how to deploy this connector on Azure Virtual Machine. You can do this by following these steps:

- Install Azure CLI (installation process is described in official [documentation](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest)) and log in with the command:
  ```
  az login
  ```
- Create a [resource group](https://docs.microsoft.com/en-us/cli/azure/group?view=azure-cli-latest#az_group_create) and a [virtual machine](https://docs.microsoft.com/en-us/cli/azure/vm?view=azure-cli-latest#az_vm_create). Examples of command usage:
  ```
  az group create --name IoT-Hub-connector --location westeurope
  ```
  ```
  az vm create -n IoT-Hub-connector -g IoT-Hub-connector --image UbuntuLTS --admin-username azureuser
  ```
- Provide correct parameters in `application.yaml` and create an installation package with the command:
  ```
  mvn clean package -Prelease
  ```
- Copy the file to the virtual machine ([documentation](https://docs.microsoft.com/pl-pl/azure/virtual-machines/linux/copy-files-to-linux-vm-using-scp)):
  ```
  scp target/lo2iot-[VERSION].zip azureuser@YOUR_MACHINE_IP:~
  ```
- Install unzip and java  
  ```
  sudo apt-get install unzip
  ```
  ```
  sudo apt-get install openjdk-21-jdk -y
  ```
- Unpack the installation package:
  ```
  unzip lo2iot-[VERSION].zip 
  ```
- Run `start.sh` script:
  ```
  ./lo2iot-[VERSION]/bin/app.sh
  ```

If you want to stop and completely delete this machine you should run:
```
az group delete -n IoT-Hub-connector
```
