FROM java:8
VOLUME /tmp

WORKDIR /app
ADD . /app

#set proxy
ENV http_proxy {3}
RUN echo $http_proxy

RUN echo "nameserver 8.8.8.8" | tee /etc/resolv.conf > /dev/null

#Install protoc
RUN curl -OL https://github.com/google/protobuf/releases/download/v3.4.0/protoc-3.4.0-linux-x86_64.zip
RUN unzip protoc-3.4.0-linux-x86_64.zip
RUN mv bin/* /usr/local/bin/
RUN mv include/* /usr/local/include/
RUN chown root /usr/local/bin/protoc
#RUN chown -R root /usr/local/include/google

#RUN echo $PATH
#set env variable for protoc if need be

#RUN echo "All setup completed, removing the proxy address from the environment"
#ENV http_proxy=""
#RUN echo $http_proxy

#expose port for service
EXPOSE {0}

#Deploy Model service
RUN curl  --output H2o-genericjavaModelRunnerService.jar {1}
RUN cp H2o-genericjavaModelRunnerService.jar app.jar
RUN chmod 777 /app/app.jar
RUN pwd

#Deploy Model service
ADD {2} /app/models/Model.zip
ADD *.proto /app/models/
#RUN mkdir plugin_root /app/plugin_root
#RUN mkdir src /app/plugin_root/src
#ADD classes /app/plugin_root/classes
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.config.location=application.properties","-jar","/app/app.jar"]