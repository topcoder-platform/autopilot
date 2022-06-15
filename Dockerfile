FROM openjdk:8u332-jdk-slim

ENV JAVA_OPTS="-Xms1G -Xmx1G -XX:MaxPermSize=256M -server"
ENV TZ=America/Indiana/Indianapolis

RUN mkdir -p /opt/autopilot
WORKDIR /opt/autopilot

Add ./target/auto-pilot-1.0-SNAPSHOT-spring-boot.jar /opt/autopilot/
Add ./xml_phase_template.xsd /opt/autopilot/
Add ./templates /opt/autopilot/templates/

CMD ["java", "-jar", "auto-pilot-1.0-SNAPSHOT-spring-boot.jar", "-poll", "10"]