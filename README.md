device-type.lock
================

This is a custom version of the Z-Wave Lock Device Type code. This code adds support for user codes with the use of a Smartapp. 

This Device Type was designed for the Kwikset Deadbolt so might not be compatible with other locks.

## Installation

1. Create a new device type (https://graph.api.smartthings.com/ide/devices)
    * Capabilities:
        * Configuration
        * Battery
        * Polling
        * Lock
        * Custom Attribute
           - user1
           - code1
        * Custom Command
           - usercodechange
