FROM eclipse-temurin:21
ARG TARGETARCH

ADD "https://github.com/tsduck/tsduck/releases/download/v3.43-4549/tsduck_3.43-4549.ubuntu25_$TARGETARCH.deb" /tmp/tsp.deb
ADD target/*.jar /opt/svc/service.jar
ADD target/dependency/ /opt/svc/dependency/

RUN apt-get update
RUN dpkg -i /tmp/tsp.deb || true
RUN apt-get --fix-broken -y install
RUN apt-get -y install ffmpeg
RUN apt-get clean

WORKDIR /opt/svc
RUN ln -rs ./data/settings.json .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "service.jar"]