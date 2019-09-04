FROM gcc:{0}
VOLUME /tmp
WORKDIR /app
ADD . /app
#set proxy - enable installation of dependencies
#ENV http_proxy http://10.53.130.56:8080
#RUN echo $http_proxy
RUN echo "nameserver 8.8.8.8" | tee /etc/resolv.conf > /dev/null
#RUN echo "All setup completed, removing the proxy address from the environment"
#ENV http_proxy=""
#RUN echo $http_proxy
#expose port for service
EXPOSE 3330
#Deploy Model service
#Deploy Model service
#RUN chmod 777 /app/model
#ADD *.proto /app/model/
#ENTRYPOINT ["./model/{1}"]