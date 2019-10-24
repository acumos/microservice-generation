FROM gcc:{0}
VOLUME /tmp
WORKDIR /app
ADD . /app
#set proxy - enable installation of dependencies
ENV http_proxy {2}
RUN echo $http_proxy

RUN echo "nameserver 8.8.8.8" | tee /etc/resolv.conf > /dev/null

#RUN echo "All setup completed, removing the proxy address from the environment"
#ENV http_proxy=""
#RUN echo $http_proxy
#expose port for service
EXPOSE 3330
#Deploy Model service

# Even after adding model to /app/model folder it unzip on /app as its WORKDIR
#ADD model.zip /app/model/model.zip
#RUN unzip /app/model/model.zip

#RUN chmod 777 /app/model
#RUN cp -r /app/model/*.* /app/
#RUN cp -r /app/model/lib /app/
#RUN cp /app/model/{1} /app/

#RUN unzip model.zip
#ADD *.proto /app/model/
# Acces for executable run access

ENV LD_LIBRARY_PATH /app/lib
RUN ldconfig

RUN chmod 777 /app/*
ENTRYPOINT ["./{1}"]