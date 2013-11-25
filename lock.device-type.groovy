metadata {
	simulator {
		status "locked": "command: 9881, payload: 00 62 03 FF 00 00 FE FE"
		status "unlocked": "command: 9881, payload: 00 62 03 00 00 00 FE FE"

		reply "9881006201FF,delay 4200,9881006202": "command: 9881, payload: 00 62 03 FF 00 00 FE FE"
		reply "988100620100,delay 4200,9881006202": "command: 9881, payload: 00 62 03 00 00 00 FE FE"
	}

	tiles {
		standardTile("toggle", "device.lock", width: 2, height: 2) {
			state "locked", label:'locked', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821", nextState:"unlocking"
			state "unlocked", label:'unlocked', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff", nextState:"locking"
			state "locking", label:'locking', icon:"st.locks.lock.locked", backgroundColor:"#79b821"
			state "unlocking", label:'unlocking', icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff"
		}
		standardTile("lock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'lock', action:"lock.lock", icon:"st.locks.lock.locked", nextState:"locking"
		}
		standardTile("unlock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'unlock', action:"lock.unlock", icon:"st.locks.lock.unlocked", nextState:"unlocking"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', action:"batteryupdate", unit:""
		}
        valueTile("code", "device.code", inactiveLabel: false, decoration: "flat") {
			state "code", label:'${currentValue} code', unit:""
		}
		standardTile("refresh", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main "toggle"
		details(["toggle", "lock", "unlock", "battery", "code", "configure", "refresh"])
	}
}

import physicalgraph.zwave.commands.doorlockv1.*

//parse
def parse(String description) {
	def result = null
	if (description.startsWith("Err")) {
	    result = createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description, [ 0x98: 1 ])
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
	log.debug "\"$description\" parsed to ${result.inspect()}"
	result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x62: 1, 0x71: 2, 0x80:1, 0x85: 2])
	log.debug "encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(DoorLockOperationReport cmd) {
	def map = [ name: "lock" ]
	if (cmd.doorLockMode == 0xFF) {
		map.value = "locked"
	} else if (cmd.doorLockMode >= 0x40) {
		map.value = "unknown"
	} else if (cmd.doorLockMode & 1) {
		map.value = "unlocked with timeout"
	} else {
		map.value = "unlocked"
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport cmd) {
	def map = null
	if (cmd.zwaveAlarmType == 6) {
		if (1 <= cmd.zwaveAlarmEvent && cmd.zwaveAlarmEvent < 10) {
			map = [ name: "lock", value: (cmd.zwaveAlarmEvent & 1) ? "locked" : "unlocked" ]
		}
		switch(cmd.zwaveAlarmEvent) {
			case 1:
				map.descriptionText = "$device.displayName was manually locked"
				break
			case 2:
				map.descriptionText = "$device.displayName was manually unlocked"
				break
			case 5:
				if (cmd.eventParameter) {
					map.descriptionText = "$device.displayName was locked with code ${cmd.eventParameter.first()}"
				}
				break
			case 6:
				if (cmd.eventParameter) {
					map.descriptionText = "$device.displayName was unlocked with code ${cmd.eventParameter.first()} zwaveAlarmEvent"
				}
				break
			case 9:
				map.descriptionText = "$device.displayName was autolocked"
				break
			case 7:
			case 8:
			case 0xA:
				map = [ name: "lock", value: "unknown", descriptionText: "$device.displayName was not locked fully" ]
				break
			case 0xB:
				map = [ name: "lock", value: "unknown", descriptionText: "$device.displayName is jammed" ]
				break
			case 0xC:
				map = [ descriptionText: "$device.displayName: all user codes deleted", display: true ]
				break
			case 0xD:
				map = [ descriptionText: "$device.displayName: user code deleted", display: true ]
				break
			case 0xE:
				map = [ descriptionText: "$device.displayName: user code added", display: true ]
				break
			default:
				map = map ?: [ descriptionText: "$device.displayName: alarm event $cmd.zwaveAlarmEvent", display: false ]
				break
		}
	} else switch(cmd.alarmType) {
        case 17:
			map = [ name: "lock", value: "unknown" ]
				map.descriptionText = "$device.displayName Secured at Keypad – Bolt Jammed"
			break
        case 18:
			map = [ name: "lock", value: "locked" ]
            if(cmd.alarmLevel) {
				map.descriptionText = "$device.displayName Secured by User ${cmd.alarmLevel} at Keypad"
                map = [ name: "code", value: ${cmd.alarmLevel} ]
            }
			break
		case 19:
			map = [ name: "lock", value: "unlocked" ]
			if(cmd.alarmLevel) {
				map.descriptionText = "$device.displayName Un-Secured by User ${cmd.alarmLevel} at Keypad"
			}
			break
        case 21:
			map = [ name: "lock", value: "locked" ]
				map.descriptionText = "$device.displayName Secured using Keyed cylinder or inside thumb-turn"
			break
        case 22:
			map = [ name: "lock", value: "unlocked" ]
				map.descriptionText = "$device.displayName Un-Secured using Keyed cylinder or inside thumb-turn"
			break
        case 23:
			map = [ name: "lock", value: "unknown" ]
				map.descriptionText = "$device.displayName Secured by Controller – Bolt Jammed"
			break
        case 24:
			map = [ name: "lock", value: "locked" ]
				map.descriptionText = "$device.displayName Secured by Controller"
			break
        case 25:
			map = [ name: "lock", value: "unlocked" ]
				map.descriptionText = "$device.displayName Un-Secured by Controller"
			break
        case 26:
			map = [ name: "lock", value: "unknown" ]
				map.descriptionText = "$device.displayName Lock Auto Secured – Bolt Jammed"
			break
        case 27:
			map = [ name: "lock", value: "locked" ]
				map.descriptionText = "$device.displayName Lock Auto Secured"
			break
        case 32:
			map = [ name: "lock", value: "unknown" ]
				map.descriptionText = "All User Codes deleted from $device.displayName"
			break
       case 112:
			map = [ name: "lock", value: "unknown" ]
			if(cmd.alarmLevel) {
				map.descriptionText = "New User: ${cmd.alarmLevel} added to $device.displayName"
			}
			break
       case 161:
			map = [ name: "lock", value: "unknown" ]
				map.descriptionText = "Failed User Code attempt at Keypad on $device.displayName"
			break
       case 162:
			map = [ name: "lock", value: "unknown" ]
			if(cmd.alarmLevel) {
				map.descriptionText = "Attempted access by user ${cmd.alarmLevel} outside of scheduled on $device.displayName"
			}
			break
        case 167:
			map = [ name: "lock", value: "unknown" ]
				map.descriptionText = "Low battery level on $device.displayName"
			break
       case 168:
			map = [ name: "lock", value: "unknown" ]
				map.descriptionText = "Critical battery level on $device.displayName"
			break
       case 169:
			map = [ name: "lock", value: "unknown" ]
				map.descriptionText = "Battery level too low to operate $device.displayName"
			break
		default:
			map = [ displayed: false, descriptionText: "$device.displayName: $cmd" ]
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	def result = []
	state.associationQuery = null
	if (cmd.nodeId.any { it == zwaveHubNodeId }) {
		log.debug "$device.displayName is associated to $zwaveHubNodeId"
		state.assoc = zwaveHubNodeId
	} else {
		result << response(secure(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId)))
	}
	result
}

//battery
def batteryupdate() {
    def result = secure(zwave.batteryV1.batteryGet())
    log.debug "battery $result"
    result
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName has a low battery"
	} else {
		map.value = cmd.batteryLevel
	}
	createEvent(map)
}

def usercodechange(user, code) {
    log.debug "Set $code for User $user"
    secure(zwave.userCodeV1.userCodeSet(userIdentifier: 3, userIdStatus: 1, code: 4587))
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	createEvent(displayed: false, descriptionText: "$device.displayName: $cmd")
}

def lockAndCheck(doorLockMode) {
	secureSequence([
		zwave.doorLockV1.doorLockOperationSet(doorLockMode: doorLockMode),
		zwave.doorLockV1.doorLockOperationGet()
	], 4200)
}

def lock() {
	lockAndCheck(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_SECURED)
}

def unlock() {
	lockAndCheck(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_UNSECURED)
}

def unlockwtimeout() {
	lockAndCheck(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_UNSECURED_WITH_TIMEOUT)
}

def refresh() {
	def result = [secure(zwave.doorLockV1.doorLockOperationGet())]
	if (state.assoc == zwaveHubNodeId) {
		//log.debug "$device.displayName is associated to ${state.assoc}"
	} else if (!state.associationQuery) {
		log.debug "checking association"
		result << "delay 4200"
		result << zwave.associationV1.associationGet(groupingIdentifier:2).format()  // old Schlage locks use group 2 and don't secure the Association CC
		result << secure(zwave.associationV1.associationGet(groupingIdentifier:1))
		state.associationQuery = new Date().time
	} else if (new Date().time - state.associationQuery.toLong() > 9000) {
		log.debug "setting association"
		result << "delay 6000"
		result << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId).format()
		result << secure(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId))
		result << zwave.associationV1.associationGet(groupingIdentifier:2).format()
		result << secure(zwave.associationV1.associationGet(groupingIdentifier:1))
		state.associationQuery = new Date().time
	}
	result
}

def updated() {
}

def secure(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

def secureSequence(commands, delay=4200) {
	delayBetween(commands.collect{ secure(it) }, delay)
}
