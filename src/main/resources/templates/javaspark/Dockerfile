FROM adoptopenjdk/openjdk11:latest
VOLUME /tmp

WORKDIR /app
ADD . /app

#set proxy
ENV http_proxy {3}
RUN echo $http_proxy
RUN echo "nameserver 8.8.8.8" | tee /etc/resolv.conf > /dev/null

RUN apt-get update && apt-get install -y unzip zip

#Install protoc
RUN curl -OL https://github.com/google/protobuf/releases/download/v3.4.0/protoc-3.4.0-linux-x86_64.zip
RUN unzip protoc-3.4.0-linux-x86_64.zip
RUN mv bin/* /usr/local/bin/
RUN mv include/* /usr/local/include/
RUN chown root /usr/local/bin/protoc
#RUN chown -R root /usr/local/include/google

#expose port for service
EXPOSE {0}

#Deploy Model service
RUN curl  --output SparkModelRunnerService.jar {1}
RUN cp SparkModelRunnerService.jar app.jar
RUN chmod 777 /app/app.jar
RUN pwd

RUN mkdir -p /app/modelCopy/

ADD {2} /app/models/Model.jar

ADD *.proto /app/models/
RUN cp *.proto default.proto

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.config.location=application.properties","-jar","/app/app.jar"]