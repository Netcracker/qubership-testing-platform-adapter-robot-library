@echo off

(
	echo atp.url=%1
	echo atp.project=%2
	echo atp.project.testplan=%3
	echo atp.project.er.name=%4
	echo atp.project.recipients=%5
) > test.properties

set CLASSPATH=${CLASSPATH}

call jybot --listener org.qubership.atp.adapter.robot.RamListener --outputdir pyres\res --suite suite .
