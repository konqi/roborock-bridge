# roborock-bridge

This service connects to RR's servers and provides status information from your robot to your mqtt broker.
It can also send simple commands and requests to the robot.

## Setup & Start

- configure authentication
- configure mqtt

Good question... So far I've only started it in IntelliJ ;-)

TODO: what's required, download, configure, run

## How to use

What you'll get:

| topic                                        | description |
|----------------------------------------------|-------------|
| `home/<homeId>`                              |             |
| `home/<homeId>/rooms`                        |             |
| `home/<homeId>/routine/<routineId>`          |             |
| `home/<homeId>/device/<deviceId>`            |             |
| `home/<homeId>/device/<deviceId>/<property>` |             |

List of some of the available properties:
- map
- path
- virtual_walls
- robot_position
- charger_position
- state
- battery
- ...

Commands:

| postfix   | description |
|-----------|-------------|
| `/get`    |             |
| `/action` |             |

## Something is wrong

Try resetting the authentication by deleting the auth.json file.

## Short excursion on RR's APIs <small style="font-size: 0.5em;">(completely optional read)</small>

Sadly RR does not provide a public interface (i.e. APIs) for developers to create their own solutions.
Some people smarter than me started to decompile RR's apps and reverse engineered a lot of the protocol.
Today, with a little help by a couple of extracted secrets and a bit of mitm traffic sniffing, we can see what the
official app does and recreate some of it.

A few things you should know about the communication between the app and roborock servers (even if it's only to
understand some of the errors this application might produce) are:

- There are a couple of REST APIs that are used for
    - Authentication (login api), is where you log in and get a set of credentials for other operations
    - Home (home api), is where you get information about a home, which is the root node where everything else is
      assigned to
    - Interaction with devices (user api), requests available devices and their status, also allows starting predefined
      cleanups
- There also is an MQTT connection. This is used for
    - IPC request/responses. Used for live updates of the robot status (e.g. idle/cleaning/drying/charging)
    - There are also "special" IPC requests that request the map of your place as seen by the robot

This application hides all the different ways of communication and provides a single way of communication with your
device via mqtt.
IF you already have a mqtt broker running for things like zigbee2mqtt, this service should be right up your alley.
However, if you have never heard of mqtt, and you have no idea what it is, you may want to continue RR's app.

## Todo-List

| Status | What                                                         |
|:------:|--------------------------------------------------------------|
|   📝   | Use request memory to determine avg request to response time |
|   ✅    | Create "idle mode"                                           |
|   ✅    | Disconnect roborock mqtt when idle, reconnect on activity    |
|   ❌    | ~~Detect routine finished to send bridge into idle mode~~    |
|   ✅    | poll frequent updates during active phase                    | 
|   📝   | Room cleaning                                                |
|   📝   | Selected area cleaning (via mqtt? tricky!)                   |

## Help reverse engineering the protocol

[Instructions on breaking and entering the protocol](./HOWTO_MITM.md)

I only own an S8 Ultra, and I have no idea how the protocol varies between models.
Theoretically you should be able to use this service for other robots as well, if not, you can help by adding the
functionality for it.

Most likely you will have to create a `SchemaValueInterpreter` for your robot, which should be a simple copy & paste &
adapt (you might even find the data you need in some other services that do something similar).
In case the application doesn't work at all for you, bigger changes might be necessary.
You can help by figuring out what these changes are on the protocol level by following the instruction behind the link
above.

Feel free to

- open a pull request for
    - compatibility patches
    - bugfixes
    - new features / improvements
- report issues

## Other ways to support

If you have no idea how to code, but really want to use this application, you can sponsor me an additional roborock
robot or lend me yours for a while.
The actual extraction of what I need shouldn't take long.
I cannot give any guarantees to how long it might take to make the required adjustments to the application.
Contact me if interested.