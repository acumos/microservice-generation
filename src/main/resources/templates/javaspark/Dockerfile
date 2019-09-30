FROM java:8
VOLUME /tmp

WORKDIR /app
ADD . /app

#set proxy
#ENV http_proxy http://10.53.130.56:8080
#RUN echo $http_proxy
RUN echo "nameserver 8.8.8.8" | tee /etc/resolv.conf > /dev/null

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
#RUN curl --output h2o-genericjava-modelrunner.jar https://nexus.acumos.org/content/repositories/releases/org/acumos/generic-model-runner/h2o-genericjava-modelrunner/2.2.3/h2o-genericjava-modelrunner-2.2.3.jar
#RUN cp h2o-genericjava-modelrunner.jar app.jar
#RUN pwd

ADD SparkModelRunnerService.jar app.jar
RUN chmod 777 /app/app.jar
RUN pwd

RUN mkdir -p /app/modelCopy/

ADD {2} /app/models/Model.jar

ADD *.proto /app/models/
RUN cp *.proto default.proto

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.config.location=application.properties","-jar","/app/app.jar"]