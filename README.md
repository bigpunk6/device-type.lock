device-type.lock
================

This is an custom version of the Z-Wave LockDevice Type code. This code adds support for 
## Installation

1. Create a new device type (https://graph.api.smartthings.com/ide/devices)
    * Name: Weather Station
    * Author: jnovack@gmail.com
    * Capabilities:
        * Polling
        * Relative Humidity Measurement
        * Temperature Measurement
        * Water Sensor

2. Create a new device (https://graph.api.smartthings.com/device/list)
    * Name: Weather Station
    * Device Network Id: WEATHERSTATION-001 (increase for each weather station you add)
    * Type: Weather Station (should be near the last option)
    * Location: Choose the correct location
    * Hub/Group: Choose a hub (this will autopopulate the zipcode)
