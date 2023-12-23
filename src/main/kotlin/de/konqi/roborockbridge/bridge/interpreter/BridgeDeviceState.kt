package de.konqi.roborockbridge.bridge.interpreter

enum class BridgeDeviceState {
    /**
     * In idle state only infrequent or unimportant changes are expected
     * e.g. docked sleeping, charging, drying, ...
     * While device is idle the status will be polled via REST API only
     */
    IDLE,

    /**
     * With the device is active frequent changes to the state are expected
     * e.g. cleaning, moving, returning home, mapping, cleaning mop, ...
     * While in this state the status will be polled frequently via MQTT
     */
    ACTIVE,

    /**
     * The device requires some kind of attention, but is still performing a
     * task as good as it can.
     * e.g. fresh water tank empty, dirt water tank full, ...
     * The bridge will still poll the state frequently, just like in ACTIVE
     */
    ERROR_ACTIVE,

    /**
     * The device requires immediate attention to continue work
     * e.g. device stuck, lost, locked in a closet under staircase, ...
     * Since a change without human interaction is unlikely the bridge will
     * poll for updates infrequently via REST
     */
    ERROR_IDLE,

    /**
     * The state of the device is unknown.
     * The bridge will very rarely attempt to poll for a new state unless it
     * has just started.
     * This should be the initial state to force an initial polling of state
     * for the device.
     */
    UNKNOWN
}