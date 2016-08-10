# Teletask-api

An open source java JSON REST API for Teletask domotics.

It is the purpose to generate a REST API for software developers or domotics enthusiasts, who are interested in generating their own control environment for the TELETASK domotics systems, so you can create your own user interface and connected solutions and services.

If you own a Teletask MICROS (note: not the latest MICROS+), you have access to the free DLL32 LIBRARY (TDS15132).  However, if you're a java programmer like myself, you don't want to use a windows dll :-)
The API also supports the MICROS+, but you'll have to buy a licence to be able to make TCP calls.
Bought me a RS232 > LAN converter (TDS10118) so I could access my MICROS server through regular IP.

Started discussing the possibilities on the Teletask forum: http://www.teletask.be/forum/default.aspx?g=posts&t=195
...and ended up programming a java interface based on IP Sockets, exposed by a basic JSON REST service.

Initially only setting and getting RELAYS, MTRUPDOWN, GENMOOD, LOCMOOD, COND, FLAG is supported.

Added a basic HTML 5 GUI for access on a mobile device.  Fastest implementation proved to be jQuery Mobile (at the time of creation).   
However I highly recommend using Home Assistant: https://home-assistant.io
Currently the API uses an internal MQTT broker, but support for an external broker will be added to bridge with Home Assistant.
You can however already use the REST interface to make calls from Home Assistant to the TeleTask server.

Read more how to make use of the API and web apps on the [here](http://xhibit.github.io/Teletask-api/).
