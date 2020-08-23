# Use an official Python runtime as a base image
FROM python:{0}

# Set label for knowing which REST api version this model runner is following
# this is to help configure the nginx proxy
LABEL micro-service-rest-api-version="2.0"

# Set the working directory to /app
WORKDIR /app

# Copy the current directory contents into the container at /app
ADD . /app

ENV http_proxy {3}
RUN echo $http_proxy
ENV https_proxy {3}
RUN echo $https_proxy
RUN echo "nameserver 8.8.8.8" | tee /etc/resolv.conf > /dev/null

#--proxy http://pb00365848:!PB123456a%40@10.254.40.121:8080
RUN apt-get clean && apt-get update && apt-get -y install libgtk2.0-dev

RUN pip install --no-cache-dir -r requirements.txt
RUN pip install --no-cache-dir acumos-model-runner

ENV http_proxy=""
RUN echo $http_proxy
ENV https_proxy=""
RUN echo $https_proxy

#Make port 80 available to the world outside this container
EXPOSE 3330

# Run runner.py when the container launches
ENTRYPOINT ["acumos_model_runner", "."]
CMD []